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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;
import com.google.appengine.api.datastore.FetchOptions;
import org.owasp.html.Sanitizers;
import org.owasp.html.PolicyFactory;

/** Servlet that handles comments data */
@WebServlet("/data")
public class DataServlet extends HttpServlet {
  private static class Comment {
    private final String email, comment, date;
    public Comment(String email, String comment, String date){
      this.email = email;
      this.comment = comment;
      this.date = date;
    }
  }
  private static final String COMMENT_DATE_FORMAT = "MMM dd,yyyy HH:mm";
  private DatastoreService datastore;
  private PolicyFactory sanitizer;
  
  @Override
  public void init() {
    datastore = DatastoreServiceFactory.getDatastoreService();
    sanitizer = Sanitizers.FORMATTING.and(Sanitizers.BLOCKS).and(Sanitizers.STYLES).and(Sanitizers.LINKS);
  }
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    int maxComments = Integer.parseInt(request.getParameter("max_comments"));
    Query comments_query = new Query("Comment").addSort("time_millis", SortDirection.DESCENDING);
    List<Entity> results =  datastore.prepare(comments_query).asList(FetchOptions.Builder.withLimit(maxComments));
    List<Comment> comments = new ArrayList<Comment>();
    for(Entity comment : results){
      comments.add(makeComment(comment));
    }
    response.setContentType("application/json;");
    response.getWriter().println(convertToJsonUsingGson(comments));
  }
  
  private static Comment makeComment(Entity comment){
    SimpleDateFormat sdf = new SimpleDateFormat();    
    Date resultDate = new Date((Long)comment.getProperty("time_millis"));
    return new Comment( (String)comment.getProperty("email"), 
                        (String)comment.getProperty("comment"),
                        sdf.format(resultDate));
  }
  
  private static String convertToJsonUsingGson(List<Comment> comments) {
    Gson gson = new Gson();
    return gson.toJson(comments);
  }
  
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Entity commentEntity = new Entity("Comment");
    commentEntity.setProperty("email", request.getParameter("user_email"));
    commentEntity.setProperty("comment", sanitizer.sanitize(request.getParameter("user_comment")));
    commentEntity.setProperty("time_millis", System.currentTimeMillis());    
    datastore.put(commentEntity);
    response.sendRedirect("/comments.html");
  }
}
