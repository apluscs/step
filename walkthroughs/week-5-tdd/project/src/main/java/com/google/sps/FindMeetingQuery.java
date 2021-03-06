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
import java.util.List;
import java.util.Set;

/** Lists possible meeting times based on meeting information it takes in. */
public final class FindMeetingQuery {
  private static final int END_OF_DAY = TimeRange.getTimeInMinutes(23, 59);

  private static final Comparator<Event> ORDER_BY_START_ASC =
      new Comparator<Event>() {
        @Override
        public int compare(Event a, Event b) {
          return Long.compare(a.getWhen().start(), b.getWhen().start());
        }
      };

  /**
   * Returns a list of time periods in which the meeting, specified by request, could happen. If one
   * or more time slots exists so that both mandatory and optional attendees can attend, it returns
   * those time slots. Otherwise, it returns the time slots that fit just the mandatory attendees.
   *
   * @param eventsCollection the events we know about
   * @param request information about the meeting, including attendees, optional attendees, and how
   *     long it needs to be
   */
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    Collection<TimeRange> withOptionalAttendees =
        getMeetingTimes(events, request, /*includeOptionalAttendees=*/true);

    // Special case: if no mandatory attendees and optional attendees' schedules cannot fit in a
    // meeting, no meeting times are possible.
    if(!withOptionalAttendees.isEmpty() || request.getAttendees().isEmpty()){
      return withOptionalAttendees;
    }
    return getMeetingTimes(events, request, /*includeOptionalAttendees=*/false);
  }

  private Collection<TimeRange> getMeetingTimes(
      Collection<Event> eventsCollection,
      MeetingRequest request,
      boolean includeOptionalAttendees) {
    HashSet<String> attendees = new HashSet<String>(request.getAttendees());
    if (includeOptionalAttendees) {
      attendees.addAll(request.getOptionalAttendees());
    }
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
