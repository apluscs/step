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
    List<TimeRange> taken = mergeIntervals(events);
    List<TimeRange> res = new ArrayList<TimeRange>();
    long d = request.getDuration();
    int end = 0;
    for (TimeRange range : taken) {
      if (range.start() - end >= d) {
        res.add(TimeRange.fromStartEnd(end, range.start(), false));
      }
      end = range.end();
    }
    if (END_OF_DAY + 1 - end >= d) {
      res.add(TimeRange.fromStartEnd(end, END_OF_DAY, true));
    }
    return res;
  }

  ArrayList<Event> removeIrrelevantEvents(
      HashSet<String> requestAttendees, ArrayList<Event> events) {
    ArrayList<Event> res = new ArrayList<Event>();
    for (Event event : events) {
      Set<String> eventAttendees = new HashSet<String>(event.getAttendees());

      // This event has at least one relevant attendee, must be recognized.
      Set<String> intersection = new HashSet<String>(eventAttendees); // use the copy constructor
      intersection.retainAll(requestAttendees);
      if (!intersection.isEmpty()) {
        res.add(event);
      }
    }
    return res;
  }

  List<TimeRange> mergeIntervals(ArrayList<Event> events) {
    Collections.sort(events, ORDER_BY_START);
    List<TimeRange> res = new ArrayList<TimeRange>();
    if (events.isEmpty()) {
      return res;
    }
    int start = events.get(0).getWhen().start(), end = events.get(0).getWhen().end();
    for (Event event : events) {
      if (event.getWhen().start() <= end) {
        end = Math.max(end, event.getWhen().end());
      } else {
        res.add(TimeRange.fromStartEnd(start, end, false));
        start = event.getWhen().start();
        end = event.getWhen().end();
      }
    }
    res.add(TimeRange.fromStartEnd(start, end, false));
    return res;
  }
}
