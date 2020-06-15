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

public final class FindMeetingQuery {
  public static final int START_OF_DAY = TimeRange.getTimeInMinutes(0, 0);
  public static final int END_OF_DAY = TimeRange.getTimeInMinutes(23, 59);

  public static final Comparator<Event> ORDER_BY_START =
      new Comparator<Event>() {
        @Override
        public int compare(Event a, Event b) {
          return Long.compare(a.getWhen().start(), b.getWhen().start());
        }
      };

  public Collection<TimeRange> query(Collection<Event> eventsCollection, MeetingRequest request) {
    HashSet<String> requestAttendees = new HashSet<String>(request.getAttendees());
    ArrayList<Event> events =
        removeIrrelevantEvents(requestAttendees, new ArrayList<Event>(eventsCollection));
    List<TimeRange> res = new ArrayList<TimeRange>();
    if (events.isEmpty()) {
      addIfValid(TimeRange.fromStartEnd(0, END_OF_DAY, true), res, request.getDuration());
      return res;
    }

    Collections.sort(events, ORDER_BY_START);

    // Add first gap.
    addIfValid(
        TimeRange.fromStartEnd(0, events.get(0).getWhen().start(), false),
        res,
        request.getDuration());
    int end = events.get(0).getWhen().end();
    for (Event event : events) {
      // event can be merged with current time range
      if (event.getWhen().start() <= end) {
        end = Math.max(end, event.getWhen().end());
      } else {
        // Add the time range we were tracking, start a new one from event.
        addIfValid(
            TimeRange.fromStartEnd(end, event.getWhen().start(), false),
            res,
            request.getDuration());
        end = event.getWhen().end();
      }
    }

    // Add the last one we were tracking.
    addIfValid(TimeRange.fromStartEnd(end, END_OF_DAY, true), res, request.getDuration());
    return res;
  }

  void addIfValid(TimeRange range, List<TimeRange> ranges, long duration) {
    if (range.duration() >= duration) {
      ranges.add(range);
    }
  }

  ArrayList<Event> removeIrrelevantEvents(
      HashSet<String> requestAttendees, ArrayList<Event> events) {
    ArrayList<Event> res = new ArrayList<Event>();
    for (Event event : events) {
      Set<String> eventAttendees = new HashSet<String>(event.getAttendees());

      // This event has at least one relevant attendee, must be recognized.
      Set<String> intersection = new HashSet<String>(eventAttendees);
      intersection.retainAll(requestAttendees);
      if (!intersection.isEmpty()) {
        res.add(event);
      }
    }
    return res;
  }
}
