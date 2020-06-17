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
        getMeetingsSatisfyingAllAttendees(eventsCollection, request);
    HashMap<String, ArrayList<TimeRange>> optionalAttendeesFreeTimes =
        getFreeTimes(eventsCollection, new HashSet<String>(request.getOptionalAttendees()));
    return optimalMeetingTimes(
        mandatoryAttendeesMeetingTimes, optionalAttendeesFreeTimes, request.getDuration());
  }

  private Collection<TimeRange> optimalMeetingTimes(
      ArrayList<TimeRange> windows, HashMap<String, ArrayList<TimeRange>> optional, long duration) {
    if (optional.isEmpty()) return windows;
    ArrayList<TimeRange> res = new ArrayList<TimeRange>();
    TreeMap<Integer, Integer> sweep = new TreeMap<Integer, Integer>();
    for (Map.Entry e : optional.entrySet()) {
      ArrayList<TimeRange> value = (ArrayList<TimeRange>) e.getValue();
      System.out.println(e.getKey() + " optionals:");
      for (TimeRange t : value) {
        sweep.put(t.start(), sweep.getOrDefault(t.start(), 0) + 1);
        sweep.put(t.end(), sweep.getOrDefault(t.end(), 0) - 1);
        System.out.println(t.toString());
      }
    }
    System.out.println();
    int sum = 0, prev = 0, j = 0, best = 0; // j=
    for (Map.Entry e : sweep.entrySet()) {
      if (prev >= windows.get(j).end()) j++; // need to move onto next window
      if (j >= windows.size()) break;
      TimeRange next =
          TimeRange.fromStartEnd(
              Math.max(windows.get(j).start(), prev),
              Math.min(windows.get(j).end(), (Integer) e.getKey()),
              false);
      if (next.duration() >= duration) {
        if (sum > best) {
          best = sum;
          res.clear();
          System.out.println("cleared");
        }
        if (sum == best) res.add(next);
        System.out.println("adding " + next.toString());
      }
      sum += (Integer) e.getValue(); // for next window
      prev = (Integer) e.getKey();
    }
    if (j >= windows.size()) {
      return res.isEmpty() ? windows : res;
    }
    TimeRange next =
        TimeRange.fromStartEnd(
            Math.max(windows.get(j).start(), prev),
            Math.min(windows.get(j).end(), TimeRange.END_OF_DAY),
            true);
    if (next.duration() >= duration) {
      if (sum > best) {
        best = sum;
        res.clear();
      }
      if (sum == best) res.add(next);
    }
    return res.isEmpty() ? windows : res;
  }

  /**
   * Returns a mapping of each attendee to the time intervals they are free in a day.
   *
   * @param eventsCollection all events to be considered.
   * @param attendees all attendees to be considered
   */
  private HashMap<String, ArrayList<TimeRange>> getFreeTimes(
      Collection<Event> eventsCollection, HashSet<String> attendees) {
    HashMap<String, ArrayList<TimeRange>> times = new HashMap<String, ArrayList<TimeRange>>();
    for (Event event : eventsCollection) {
      for (String attendee : attendees) {

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

      // To save space, we change the range of times to its complement instead of creating a new
      // HashMap.
      e.setValue(value);
    }

    // Remove attendees that have no free time (if they were busy the whole day).
    times.entrySet().removeIf(e -> e.getValue().isEmpty());
    return times;
  }

  /**
   * Returns all times that don't overlap with given times in a day.
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

  private ArrayList<TimeRange> getMeetingsSatisfyingAllAttendees(
      Collection<Event> eventsCollection, MeetingRequest request) {
    HashSet<String> attendees = new HashSet<String>(request.getAttendees());
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
