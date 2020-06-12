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

package com.google.sps.servlets;

import com.google.gson.Gson;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;


@WebServlet("/markers")
public class MarkerServlet extends HttpServlet {
  
  /** Represents a marker on the map. */
  private static class Marker {

    private final double lat;
    private final double lng;

    public Marker(double lat, double lng) {
      this.lat = lat;
      this.lng = lng;
    }
  }
  private DatastoreService datastore;

  @Override
  public void init() {
    datastore = DatastoreServiceFactory.getDatastoreService();
  }
  
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    List<Marker> markers = new ArrayList<>();
  
    for (Entity entity : (datastore.prepare(new Query("Marker")).asIterable())) {
      markers.add(new Marker(Double.parseDouble((String) entity.getProperty("lat")),
          Double.parseDouble((String) entity.getProperty("lng"))));
    }
    response.setContentType("application/json");
    response.getWriter().println(new Gson().toJson(markers));
  }
  
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Entity markerEntity = new Entity("Marker");
    markerEntity.setProperty("lat", request.getParameter("lat"));
    markerEntity.setProperty("lng", request.getParameter("lng"));
    datastore.put(markerEntity);
    response.sendRedirect("/map.html");
  }
}
