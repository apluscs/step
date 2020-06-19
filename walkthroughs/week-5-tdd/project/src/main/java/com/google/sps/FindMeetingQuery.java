// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Supports a query function that lists optimal meeting times based on meeting information it takes
 * in.
 */
public final class FindMeetingQuery {
  private static final Comparator<Event> ORDER_BY_START_ASC =
      new Comparator<Event>() {
        @Override
        public int compare(Event a, Event b) {
          return Long.compare(a.getWhen().start(), b.getWhen().start());
        }
      };

  // All logic needed to find optimal meeting times with one instance of meeting information and
  // events to be considered.
  private static class SingleMeetingResolver {
    // all events to be considered
    private final Collection<Event> events;

    // information about the meeting, including attendees, optional attendees, and how long it needs
    // to be
    private final MeetingRequest request;

    private ArrayList<TimeRange> optimalMeetingTimes = new ArrayList<TimeRange>();

    // best attendance of optional attendees seen so far
    private int bestAttendance;

    // attendance optional attendees of current meeting
    private int currAttendance;

    // minimum meeting time
    private long minTime;

    SingleMeetingResolver(Collection<Event> events, MeetingRequest request) {
      this.events = events;
      this.request = request;
      this.bestAttendance = this.currAttendance = 0;
      this.minTime = request.getDuration();
    }

    /**
     * Returns a list of time periods in which the meeting, specified by request, could happen. If
     * no time exists for all optional and mandatory attendees, find the time slot(s) that allow
     * mandatory attendees and the greatest possible number of optional attendees to attend.
     */
    Collection<TimeRange> resolveBestTime() {
      ArrayList<TimeRange> mandatoryAttendeesMeetingTimes =
          getMandatoryAttendeesMeetingTimes(new HashSet<String>(request.getAttendees()));
      getOptimalMeetingTimes(
          mandatoryAttendeesMeetingTimes,
          getChangesInOptionalAttendeesAttendance(
              getOptionalAttendeesFreeTimes(new HashSet<String>(request.getOptionalAttendees()))));

      // If there are no meeting times with at least one optional attendee, just return
      // mandatoryAttendeesMeetingTimes.
      return optimalMeetingTimes.isEmpty() ? mandatoryAttendeesMeetingTimes : optimalMeetingTimes;
    }

    /**
     * Returns all meeting times that allow the most optional attendees to attend. These times must
     * also fall in a time satisfying all mandatory attendees and must be at least minTime long.
     * Utilizes two pointers: one to mandatoryAttendeesMeetingTimes, one to changeLog.
     *
     * @param mandatoryAttendeesMeetingTimes all meeting times satisfying mandatory attendees
     * @param changes a change log of the number of available optional attendees over time
     */
    private void getOptimalMeetingTimes(
        ArrayList<TimeRange> mandatoryAttendeesMeetingTimes, TreeMap<Integer, Integer> changes) {
      int mandatoryAttendeesMeetingTimesIndex = 0, prevTime = 0;
      for (Map.Entry changeEntry : changes.entrySet()) {
        // First need to back up  mandatoryAttendeesMeetingTimesIndex in case we missed a time range
        // in
        // mandatoryAttendeesMeetingTimes. Then Compares time range from previous time in changeLog
        // to current time in changeLog with mandatoryAttendeesMeetingTimes that overlap with this
        // time range.
        for (mandatoryAttendeesMeetingTimesIndex =
                Math.max(0, mandatoryAttendeesMeetingTimesIndex - 1);
            mandatoryAttendeesMeetingTimesIndex < mandatoryAttendeesMeetingTimes.size()
                && mandatoryAttendeesMeetingTimes.get(mandatoryAttendeesMeetingTimesIndex).start()
                    < (Integer) changeEntry.getKey();
            mandatoryAttendeesMeetingTimesIndex++) {
          updateOptimalTimes(
              TimeRange.fromStartEnd(
                  Math.max(
                      mandatoryAttendeesMeetingTimes
                          .get(mandatoryAttendeesMeetingTimesIndex)
                          .start(),
                      prevTime),
                  Math.min(
                      mandatoryAttendeesMeetingTimes.get(mandatoryAttendeesMeetingTimesIndex).end(),
                      (Integer) changeEntry.getKey()),
                  false));
        }
        prevTime = (Integer) changeEntry.getKey();
        currAttendance += (Integer) changeEntry.getValue();
      }
    }

    /**
     * Returns a mapping of optional attendees to their free times.
     *
     * @param events all events to be considered
     * @param optionalAttendees everyone who needs to attend this meeting
     */
    private HashMap<String, ArrayList<TimeRange>> getOptionalAttendeesFreeTimes(
        HashSet<String> optionalAttendees) {
      HashMap<String, ArrayList<TimeRange>> times = new HashMap<String, ArrayList<TimeRange>>();
      for (String attendee : optionalAttendees) {
        HashSet<String> attendeeSet = new HashSet<String>();
        attendeeSet.add(attendee);

        // Find all possible meeting times for just this one attendee. Must do this to deal with
        // double bookings.
        times.put(attendee, getMandatoryAttendeesMeetingTimes(attendeeSet));
      }
      return times;
    }

    /**
     * Returns a change log of the number of available optional attendees over time. First part of a
     * sweep-line algorithm.
     *
     * @param optionalAttendeesFreeTimes mapping of all attendees to their free times
     */
    private TreeMap<Integer, Integer> getChangesInOptionalAttendeesAttendance(
        HashMap<String, ArrayList<TimeRange>> optionalAttendeesFreeTimes) {
      TreeMap<Integer, Integer> changes = new TreeMap<Integer, Integer>();
      for (Map.Entry e : optionalAttendeesFreeTimes.entrySet()) {
        for (TimeRange time : (ArrayList<TimeRange>) e.getValue()) {
          changes.put(time.start(), changes.getOrDefault(time.start(), 0) + 1);
          changes.put(time.end(), changes.getOrDefault(time.end(), 0) - 1);
        }
      }
      return changes;
    }

    /**
     * Updates optimalMeetingTimes based on current meeting time's attendance.
     *
     * @param time current meeting time range
     */
    private void updateOptimalTimes(TimeRange time) {
      if (time.duration() < minTime) {
        return;
      }

      // Clear out all former optimal meeting times. They aren't the most optimal anymore.
      if (currAttendance > bestAttendance) {
        bestAttendance = currAttendance;
        optimalMeetingTimes.clear();
      }
      if (currAttendance == bestAttendance) {
        optimalMeetingTimes.add(time);
      }
    }

    /**
     * Returns all meeting times that satisfy all mandatory attendees of request. Sorted in
     * ascending order.
     *
     * @param mandatoryAttendees everyone who needs to attend this meeting
     */
    private ArrayList<TimeRange> getMandatoryAttendeesMeetingTimes(
        HashSet<String> mandatoryAttendees) {
      ArrayList<Event> relevantEvents = getRelevantEvents(mandatoryAttendees);
      ArrayList<TimeRange> possibleMeetingTimes = new ArrayList<TimeRange>();

      // Need to check this so we don't access out of bounds when we add first gap.
      if (relevantEvents.isEmpty()) {
        addIfLongEnough(
            TimeRange.fromStartEnd(0, TimeRange.END_OF_DAY, true), possibleMeetingTimes);
        return possibleMeetingTimes;
      }
      Collections.sort(relevantEvents, ORDER_BY_START_ASC);

      // Add first gap.
      addIfLongEnough(
          TimeRange.fromStartEnd(0, relevantEvents.get(0).getWhen().start(), false),
          possibleMeetingTimes);
      int end = relevantEvents.get(0).getWhen().end();
      for (Event event : relevantEvents) {
        // event can be merged with current time range
        if (event.getWhen().start() <= end) {
          end = Math.max(end, event.getWhen().end());
          continue;
        }
        // Add the time range we were tracking, start a new one from event.
        addIfLongEnough(
            TimeRange.fromStartEnd(end, event.getWhen().start(), false), possibleMeetingTimes);
        end = event.getWhen().end();
      }

      // Add the last one we were tracking.
      addIfLongEnough(
          TimeRange.fromStartEnd(end, TimeRange.END_OF_DAY, true), possibleMeetingTimes);
      return possibleMeetingTimes;
    }

    /**
     * Adds range to ranges if it is long enough to fit in a meeting.
     *
     * @param range the range being considered
     * @param ranges the list of ranges >= meetingDuration
     */
    private void addIfLongEnough(TimeRange range, List<TimeRange> ranges) {
      if (range.duration() >= minTime) {
        ranges.add(range);
      }
    }

    /**
     * Returns only those events that are attended by at least one attendee that is attending the
     * meeting we are trying to schedule. More intuitively, an event is "relevant" if it is attended
     * by at least one "relevant" attendee.
     *
     * @param relevantAttendees the set of attendees attending the meeting ("relevant" people)
     */
    private ArrayList<Event> getRelevantEvents(HashSet<String> relevantAttendees) {
      return new ArrayList<Event>(
          events.stream()
              .filter(
                  e ->
                      e.getAttendees().stream()
                          .filter(relevantAttendees::contains)
                          .findAny()
                          .isPresent())
              .collect(Collectors.toList()));
    }
  }

  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    return new SingleMeetingResolver(events, request).resolveBestTime();
  }
}
