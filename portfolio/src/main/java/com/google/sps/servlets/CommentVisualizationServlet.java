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
import java.util.HashMap;
import java.util.Scanner;

/** Servlet that handles the visualization of comment data */
@WebServlet("/visualize-comments")
public class CommentVisualizationServlet extends HttpServlet {
 
  private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
  private HashMap<String, Integer> wordCounter;

  @Override
  public void init() {
    datastore = DatastoreServiceFactory.getDatastoreService();
    wordCounter = new HashMap<String, Integer>();
    Scanner scanner = new Scanner(getServletContext().getResourceAsStream(
        "/WEB-INF/comment-words-freqeuncy.csv"));
    System.out.println("scanner opened");
    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();
      String[] cells = line.split(",");

      Integer count = Integer.valueOf(cells[1]);

      wordCounter.put(cells[0], count);
    }
    scanner.close();
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    Gson gson = new Gson();
    String json = gson.toJson(wordCounter);
    response.getWriter().println(json);
  }
  
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Query commentKeysQuery = new Query("Comment").setKeysOnly();
    PreparedQuery commentKeysResults =  datastore.prepare(commentKeysQuery);
    for(Entity commentKey : commentKeysResults.asIterable()){
      datastore.delete(commentKey.getKey());
    }
    response.sendRedirect("/comments.html");
  }
}
