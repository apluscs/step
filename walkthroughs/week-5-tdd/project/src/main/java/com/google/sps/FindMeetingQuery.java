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
        getMeetingTimes(eventsCollection, request);
    HashMap<String, ArrayList<TimeRange>> optionalAttendeesFreeTimes =
        getFreeTimes(eventsCollection, request);
    return optimalMeetingTimes(mandatoryAttendeesMeetingTimes, optionalAttendeesFreeTimes);
    // System.out.println();
    // return mandatoryAttendeesMeetingTimes;
  }
  
  private Collection<TimeRange> optimalMeetingTimes(ArrayList<TimeRange> windows, HashMap<String, ArrayList<TimeRange>> optional){
    ArrayList<TimeRange> res = new ArrayList<TimeRange>();
    TreeMap<Integer, Integer> sweep = new TreeMap<Integer, Integer>();
    for(Map.Entry e : optional.entrySet()){
      ArrayList<TimeRange> value = e.getValue();
      for(TimeRange t:value){
        sweep.put(t.start(),sweep.getOrDefault(t.start(),0)+1);
        sweep.put(t.end(),sweep.getOrDefault(t.end(),0)-1);
      }
    }
    int sum=0,i=0;
    for(Map.Entry e : sweep.entrySet()){
      sum+=e.getValue();
      
    }
    return res;
  }

  private HashMap<String, ArrayList<TimeRange>> getFreeTimes(
      Collection<Event> eventsCollection, MeetingRequest request) {
    HashMap<String, ArrayList<TimeRange>> busy = new HashMap<String, ArrayList<TimeRange>>();
    HashSet<String> attendees = new HashSet<String>(request.getOptionalAttendees());
    for (Event event : eventsCollection) {
      for (String attendee : event.getAttendees()) {
        busy.putIfAbsent(attendee, new ArrayList<TimeRange>());
        busy.get(attendee).add(event.getWhen());
      }
    }
    for (Map.Entry e : busy.entrySet()) {
      String key = (String) e.getKey();

      ArrayList<TimeRange> value = getComplement((ArrayList<TimeRange>) e.getValue());
      System.out.print(key + " : ");
      for (TimeRange t : value) {
        System.out.print(t.toString() + " ");
      }
      System.out.println();
      e.setValue(value);
    }

    return busy;
  }

  private ArrayList<TimeRange> getComplement(ArrayList<TimeRange> times) {
    Collections.sort(times, TimeRange.ORDER_BY_START);
    ArrayList<TimeRange> res = new ArrayList<TimeRange>();
    int end = TimeRange.START_OF_DAY;
    for (TimeRange t : times) {
      if (end < t.start()) {
        res.add(TimeRange.fromStartEnd(end, t.start(), false));
      }
      end = t.end();
    }
    if (end <= END_OF_DAY) {
      res.add(TimeRange.fromStartEnd(end, TimeRange.END_OF_DAY, true));
    }
    return res;
  }

  private ArrayList<TimeRange> getMeetingTimes(
      Collection<Event> eventsCollection, MeetingRequest request) {
    HashSet<String> attendees = new HashSet<String>(request.getAttendees());
    ArrayList<Event> events = getRelevantEvents(attendees, new ArrayList<Event>(eventsCollection));
    List<TimeRange> possibleMeetingTimes = new ArrayList<TimeRange>();

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
