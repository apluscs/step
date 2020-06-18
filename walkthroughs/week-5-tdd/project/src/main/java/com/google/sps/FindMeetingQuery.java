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
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Lists optimal meeting times based on meeting information it takes in. */
public final class FindMeetingQuery {

  private static final Comparator<Event> ORDER_BY_START_ASC =
      new Comparator<Event>() {
        @Override
        public int compare(Event a, Event b) {
          return Long.compare(a.getWhen().start(), b.getWhen().start());
        }
      };

  /**
   * Returns a list of time periods in which the meeting, specified by request, could happen. If no
   * time exists for all optional and mandatory attendees, find the time slot(s) that allow
   * mandatory attendees and the greatest possible number of optional attendees to attend.
   *
   * @param events the events to be considered
   * @param request information about the meeting, including attendees, optional attendees, and how
   *     long it needs to be
   */
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    return getOptimalMeetingTimes(
        getMandatoryAttendeesMeetingTimes(
            events, request.getDuration(), new HashSet<String>(request.getAttendees())),
        getChanges(
            getOptionalAttendeesFreeTimes(
                events,
                new HashSet<String>(request.getOptionalAttendees()),
                request.getDuration())),
        request.getDuration());
  }

  /**
   * Returns all meeting times that allow the most optional attendees to attend. These times must
   * also fall in a good window and must be at least minTime long. Utilizes two pointers: one to
   * goodWindows, one to changeLog.
   *
   * @param goodWindows all meeting times satisfying mandatory attendees
   * @param changeLog a change log of the number of available optional attendees over time
   * @param minTime the minimum time a meeting must last for
   */
  private Collection<TimeRange> getOptimalMeetingTimes(
      ArrayList<TimeRange> goodWindows,
      ArrayList<Map.Entry<Integer, Integer>> changeLog,
      long minTime) {

    ArrayList<TimeRange> optimalMeetingTimes = new ArrayList<TimeRange>();

    // i is a pointer in changeLog, j is a poitner in goodWindows.
    for (int j = 0, i = 0, currAttendance = 0, bestAttendance = 0, prevTime = 0;
        i < changeLog.size();
        ++i) {
      // Need to back up j in case we missed a good window.
      j = Math.max(0, j - 1);

      // Compares time range from changeLog[i-1] to changeLog[i] to all good windows
      // that overlap with this time range.
      while (j < goodWindows.size()
          && goodWindows.get(j).start() < (Integer) changeLog.get(i).getKey()) {
        bestAttendance =
            updateOptimalTimes(
                TimeRange.fromStartEnd(
                    Math.max(goodWindows.get(j).start(), prevTime),
                    Math.min(goodWindows.get(j).end(), (Integer) changeLog.get(i).getKey()),
                    false),
                bestAttendance,
                currAttendance,
                optimalMeetingTimes,
                minTime);
        j++;
      }
      prevTime = (Integer) changeLog.get(i).getKey();
      currAttendance += (Integer) changeLog.get(i).getValue();
    }

    // If there are no meeting times with at least one optional attendee, just return the good
    // windows.
    return optimalMeetingTimes.isEmpty() ? goodWindows : optimalMeetingTimes;
  }

  /**
   * Returns a mapping of optional attendees to their free times.
   *
   * @param events all events to be considered
   * @param optionalAttendees everyone who needs to attend this meeting
   * @param minTime minimum length of time for meeting
   */
  private HashMap<String, ArrayList<TimeRange>> getOptionalAttendeesFreeTimes(
      Collection<Event> events, HashSet<String> optionalAttendees, long minTime) {
    HashMap<String, ArrayList<TimeRange>> times = new HashMap<String, ArrayList<TimeRange>>();
    for (String attendee : optionalAttendees) {
      HashSet<String> attendeeSet = new HashSet<String>();
      attendeeSet.add(attendee);

      // Find all possible meeting times for just this one attendee. Must do this to deal with
      // double bookings.
      times.put(attendee, getMandatoryAttendeesMeetingTimes(events, minTime, attendeeSet));
    }
    return times;
  }

  /**
   * Returns a change log of the number of available optional attendees over time. First part of a
   * sweep-line algorithm.
   *
   * @param optionalAttendeesFreeTimes mapping of all attendees to their free times
   */
  private ArrayList<Map.Entry<Integer, Integer>> getChanges(
      HashMap<String, ArrayList<TimeRange>> optionalAttendeesFreeTimes) {
    TreeMap<Integer, Integer> changes = new TreeMap<Integer, Integer>();
    for (Map.Entry e : optionalAttendeesFreeTimes.entrySet()) {
      for (TimeRange time : (ArrayList<TimeRange>) e.getValue()) {
        changes.put(time.start(), changes.getOrDefault(time.start(), 0) + 1);
        changes.put(time.end(), changes.getOrDefault(time.end(), 0) - 1);
      }
    }
    return new ArrayList<Map.Entry<Integer, Integer>>(changes.entrySet());
  }

  /**
   * Updates optimalMeetingTimes based on current meeting time's attendance.
   *
   * @param time current meeting time range
   * @param bestAttendance best attendance of optional attendees seen so far
   * @param currAttendance attendance optional attendees of current meeting
   * @param optionalAttendeesFreeTimes all attendees and their free times
   * @param minTime minimum meeting time
   */
  private int updateOptimalTimes(
      TimeRange time,
      int bestAttendance,
      int currAttendance,
      ArrayList<TimeRange> optimalMeetingTimes,
      long minTime) {
    if (time.duration() >= minTime) {

      // Clear out all former optimal meeting times. They aren't the most optimal anymore.
      if (currAttendance > bestAttendance) {
        bestAttendance = currAttendance;
        optimalMeetingTimes.clear();
      }
      if (currAttendance == bestAttendance) {
        optimalMeetingTimes.add(time);
      }
    }
    return bestAttendance;
  }

  /**
   * Returns all meeting times that satisfy all mandatory attendees of request. Sorted in ascending
   * order.
   *
   * @param events all events to be considered
   * @param minTime minimum length of time for meeting
   * @param mandatoryAttendees everyone who needs to attend this meeting
   */
  private ArrayList<TimeRange> getMandatoryAttendeesMeetingTimes(
      Collection<Event> events, long minTime, HashSet<String> mandatoryAttendees) {
    ArrayList<Event> relevantEvents =
        getRelevantEvents(mandatoryAttendees, new ArrayList<Event>(events));
    ArrayList<TimeRange> possibleMeetingTimes = new ArrayList<TimeRange>();

    // Need to check this so we don't access out of bounds when we add first gap.
    if (relevantEvents.isEmpty()) {
      addIfLongEnough(
          TimeRange.fromStartEnd(0, TimeRange.END_OF_DAY, true), possibleMeetingTimes, minTime);
      return possibleMeetingTimes;
    }
    Collections.sort(relevantEvents, ORDER_BY_START_ASC);

    // Add first gap.
    addIfLongEnough(
        TimeRange.fromStartEnd(0, relevantEvents.get(0).getWhen().start(), false),
        possibleMeetingTimes,
        minTime);
    int end = relevantEvents.get(0).getWhen().end();
    for (Event event : relevantEvents) {
      // event can be merged with current time range
      if (event.getWhen().start() <= end) {
        end = Math.max(end, event.getWhen().end());
        continue;
      }
      // Add the time range we were tracking, start a new one from event.
      addIfLongEnough(
          TimeRange.fromStartEnd(end, event.getWhen().start(), false),
          possibleMeetingTimes,
          minTime);
      end = event.getWhen().end();
    }

    // Add the last one we were tracking.
    addIfLongEnough(
        TimeRange.fromStartEnd(end, TimeRange.END_OF_DAY, true), possibleMeetingTimes, minTime);
    return possibleMeetingTimes;
  }

  /**
   * Adds range to ranges if it is long enough to fit in a meeting.
   *
   * @param range the range being considered
   * @param ranges the list of ranges >= meetingDuration
   * @param meetingDuration the duration of meeting to be scheduled
   */
  private static void addIfLongEnough(
      TimeRange range, List<TimeRange> ranges, long meetingDuration) {
    if (range.duration() >= meetingDuration) {
      ranges.add(range);
    }
  }

  /**
   * Returns only those events that are attended by at least one attendee that is attending the
   * meeting we are trying to schedule. More intuitively, an event is "relevant" if it is attended
   * by at least one "relevant" attendee.
   *
   * @param requestAttendees the set of attendees attending the meeting ("relevant" people)
   */
  private static ArrayList<Event> getRelevantEvents(
      HashSet<String> relevantAttendees, ArrayList<Event> events) {
    ArrayList<Event> relevantEvents = new ArrayList<Event>();
    for (Event event : events) {
      boolean isRelevant = false;
      for (String person : event.getAttendees()) {
        if (relevantAttendees.contains(person)) {
          isRelevant = true;
          break;
        }
      }
      if (isRelevant) {
        relevantEvents.add(event);
      }
    }
    return relevantEvents;
  }
}
