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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Lists possible meeting times based on meeting information it takes in. */
public final class FindMeetingQuery {
  private static final int END_OF_DAY = TimeRange.getTimeInMinutes(23, 59);
  // Todo: remove this and adjust.

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
   * @param eventsCollection the events we know about
   * @param request information about the meeting, including attendees, optional attendees, and how
   *     long it needs to be
   */
  public Collection<TimeRange> query(Collection<Event> eventsCollection, MeetingRequest request) {
    ArrayList<TimeRange> mandatoryAttendeesMeetingTimes =
        getMeetingsSatisfyingAllAttendees(eventsCollection, new HashSet<String>(request.getAttendees()));
    HashMap<String, ArrayList<TimeRange>> optionalAttendeesFreeTimes =
        getOptionalAttendeesFreeTimes(eventsCollection, new HashSet<String>(request.getOptionalAttendees()));
    return getOptimalMeetingTimes(
        mandatoryAttendeesMeetingTimes, optionalAttendeesFreeTimes, request.getDuration());
  }

  /**
   * Returns all meeting times that allow the most optional attendees to attend. These times must
   * also fall in a good window and must be at least minTime long. Utilizes two pointers: one to
   * goodWindows, one to changeLog.
   *
   * @param goodWindows all times in which a meeting can be scheduled
   * @param optional all optional attendees to be considered
   * @param minTime the minimum time a meeting must last for
   */
  private Collection<TimeRange> getOptimalMeetingTimes(
      ArrayList<TimeRange> goodWindows,
      HashMap<String, ArrayList<TimeRange>> optionalAttendeesFreeTimes,
      long minTime) {

    // If there are no optional attendees free times, return all good windows.
    if (optionalAttendeesFreeTimes.isEmpty()){
      return goodWindows;
    }

    ArrayList<TimeRange> optimalMeetingTimes = new ArrayList<TimeRange>();
    TreeMap<Integer, Integer> changeLog = getChanges(optionalAttendeesFreeTimes);
    int currAttendance = 0, bestAttendance = 0,prevTime = 0, currentGoodWindow = 0;
    
    for (Map.Entry e : changeLog.entrySet()) {
      
      // The time spanned from prevTime to the current time before it is ahead of the current good
      // window. Move onto next window.
      if (prevTime >= goodWindows.get(currentGoodWindow).end()) {
        currentGoodWindow++;
      }

      // All good windows have been considered.
      if (currentGoodWindow >= goodWindows.size()) {
        break;
      }

      bestAttendance =
          updateOptimalTimes(
              TimeRange.fromStartEnd(
                  Math.max(goodWindows.get(currentGoodWindow).start(), prevTime),
                  Math.min(goodWindows.get(currentGoodWindow).end(), (Integer) e.getKey()),
                  false),
              bestAttendance,
              currAttendance,
              optimalMeetingTimes,
              minTime);

      currAttendance += (Integer) e.getValue();
      prevTime = (Integer) e.getKey();
    }

    // Do this so we don't access out of bounds later.
    if (currentGoodWindow >= goodWindows.size()) {
      return optimalMeetingTimes.isEmpty() ? goodWindows : optimalMeetingTimes;
    }

    bestAttendance =
        updateOptimalTimes(
            TimeRange.fromStartEnd(
                Math.max(goodWindows.get(currentGoodWindow).start(), prevTime),
                Math.min(goodWindows.get(currentGoodWindow).end(), TimeRange.END_OF_DAY),
                true),
            bestAttendance,
            currAttendance,
            optimalMeetingTimes,
            minTime);

    // If there are no meeting times with at least one optional attendee, just return the good windows.
    return optimalMeetingTimes.isEmpty() ? goodWindows : optimalMeetingTimes;
  }

  /**
   * Updates optimalMeetingTimes based on current meeting time's attendance.
   *
   * @param optionalAttendeesFreeTimes all attendees and their free times
   TODO
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
   * Returns a change log of how many optional attendees are available in at a certain time. Uses sweep-line
   * algorithm.
   *
   * @param optionalAttendeesFreeTimes mapping of all attendees to their free times
   */
  private TreeMap<Integer, Integer> getChanges(
      HashMap<String, ArrayList<TimeRange>> optionalAttendeesFreeTimes) {
    TreeMap<Integer, Integer> changes = new TreeMap<Integer, Integer>();
    for (Map.Entry e : optionalAttendeesFreeTimes.entrySet()) {
      ArrayList<TimeRange> freeTimes = (ArrayList<TimeRange>) e.getValue();
      for (TimeRange time : freeTimes) {
        changes.put(time.start(), changes.getOrDefault(time.start(), 0) + 1);
        changes.put(time.end(), changes.getOrDefault(time.end(), 0) - 1);
      }
    }
    return changes;
  }

  /**
   * Returns a mapping of each optional attendee to the time intervals they are free in a day.
   *
   * @param eventsCollection all events to be considered.
   * @param optionalAttendees all optional attendees to be considered
   */
  private HashMap<String, ArrayList<TimeRange>> getOptionalAttendeesFreeTimes(
      Collection<Event> eventsCollection, HashSet<String> optionalAttendees) {
    HashMap<String, ArrayList<TimeRange>> times = new HashMap<String, ArrayList<TimeRange>>();
    for (Event event : eventsCollection) {
      for (String attendee : optionalAttendees) {

        // attendee is not going to this event
        if (!event.getAttendees().contains(attendee)) {
          continue;
        }
        times.putIfAbsent(attendee, new ArrayList<TimeRange>());
        times.get(attendee).add(event.getWhen());
      }
    }
    for (Map.Entry e : times.entrySet()) {
      ArrayList<TimeRange> value = getComplement((ArrayList<TimeRange>) e.getValue());

      // To save space, we change the range of times to its complement instead of adding to a new
      // HashMap.
      e.setValue(value);
    }

    // Remove attendees that have no free time (if they were busy the whole day).
    times.entrySet().removeIf(e -> e.getValue().isEmpty());
    return times;
  }

  /**
   * Returns all times that don't overlap with the given times in a day.
   *
   * @param times non-overlapping intervals of times in a 24-hour day
   */
  private ArrayList<TimeRange> getComplement(ArrayList<TimeRange> times) {
    Collections.sort(times, TimeRange.ORDER_BY_START);
    ArrayList<TimeRange> res = new ArrayList<TimeRange>();

    // end tracks the end of the last time period, and thus the beginning of the next one.
    int end = TimeRange.START_OF_DAY;
    for (TimeRange time : times) {
      if (end < time.start()) {
        res.add(TimeRange.fromStartEnd(end, time.start(), false));
      }
      end = time.end();
    }
    if (end <= END_OF_DAY) {
      res.add(TimeRange.fromStartEnd(end, TimeRange.END_OF_DAY, true));
    }
    return res;
  }

/**
   * TODO
   *
   * @param 
   */
  private ArrayList<TimeRange> getMeetingsSatisfyingAllAttendees(
      Collection<Event> eventsCollection, HashSet<String> attendees) {
    ArrayList<Event> events = getRelevantEvents(attendees, new ArrayList<Event>(eventsCollection));
    ArrayList<TimeRange> possibleMeetingTimes = new ArrayList<TimeRange>();

    // Need to check this so we don't access out of bounds when we add first gap.
    if (events.isEmpty()) {
      addIfLongEnough(
          TimeRange.fromStartEnd(0, END_OF_DAY, true), possibleMeetingTimes, request.getDuration());
      return possibleMeetingTimes;
    }
    Collections.sort(events, ORDER_BY_START_ASC);

    // Add first gap.
    addIfLongEnough(
        TimeRange.fromStartEnd(0, events.get(0).getWhen().start(), false),
        possibleMeetingTimes,
        request.getDuration());
    int end = events.get(0).getWhen().end();
    for (Event event : events) {
      // event can be merged with current time range
      if (event.getWhen().start() <= end) {
        end = Math.max(end, event.getWhen().end());
        continue;
      }
      // Add the time range we were tracking, start a new one from event.
      addIfLongEnough(
          TimeRange.fromStartEnd(end, event.getWhen().start(), false),
          possibleMeetingTimes,
          request.getDuration());
      end = event.getWhen().end();
    }

    // Add the last one we were tracking.
    addIfLongEnough(
        TimeRange.fromStartEnd(end, END_OF_DAY, true), possibleMeetingTimes, request.getDuration());
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
