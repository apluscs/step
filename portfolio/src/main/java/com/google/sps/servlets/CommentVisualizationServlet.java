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

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.Key;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.gson.Gson;
import com.google.appengine.api.datastore.FetchOptions;
import java.util.List;
import java.util.ArrayList;

/** Servlet that handles the visualization of comment data */
@WebServlet("/visualize-comments")
public class CommentVisualizationServlet extends HttpServlet {
 
  private DatastoreService datastore;

  @Override
  public void init() {
    datastore = DatastoreServiceFactory.getDatastoreService();
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Query wordsQuery = new Query("word").addSort("count", SortDirection.DESCENDING);
    List<Entity> results = datastore.prepare(wordsQuery).asList(FetchOptions.Builder.withLimit(10));
    response.setContentType("application/json");
    response.getWriter().println(convertToJsonUsingGson(results));
  }
  
  private static String convertToJsonUsingGson(List<Entity> results) {
    Gson gson = new Gson();
    return gson.toJson(results);
  }
}
