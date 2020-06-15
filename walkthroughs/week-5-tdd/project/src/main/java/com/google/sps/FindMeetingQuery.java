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
import java.util.List;

public final class FindMeetingQuery {
  public static final int START_OF_DAY = TimeRange.getTimeInMinutes(0, 0);
  public static final int END_OF_DAY = TimeRange.getTimeInMinutes(23, 59);
  
  public static final Comparator<Event> ORDER_BY_START = new Comparator<Event>() {
    @Override
    public int compare(Event a, Event b) {
      return Long.compare(a.getWhen().start(), b.getWhen().start());
    }
  };
  
  public Collection<TimeRange> query(Collection<Event> events_collection, MeetingRequest request) {
    List<Event> events = new ArrayList<Event>(events_collection);
    for(Event e:events){
      System.out.println(e.getWhen().start() + " to " + e.getWhen().end());
    }
    System.out.println("---------------");
    Collections.sort(events, ORDER_BY_START);
    for(Event e:events){
      System.out.println(e.getWhen().start() + " to " + e.getWhen().end());
    }
    System.out.println();
    List<TimeRange> res = new ArrayList<TimeRange>();
    return res;
  }
}
