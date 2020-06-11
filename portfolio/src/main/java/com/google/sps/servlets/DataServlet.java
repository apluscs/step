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
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
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
  private static class Response {
    private List<Comment> comments;
    private int lastPage;
    public Response(List<Comment> comments, int lastPage) {
      this.comments = comments;
      this.lastPage = lastPage;
    }
  }
  private static final String COMMENT_DATE_FORMAT = "MMM dd,yyyy HH:mm";
  private static final int TRANSACTION_RETRIES = 4;
  private DatastoreService datastore;
  private PolicyFactory sanitizer;
  
  @Override
  public void init() {
    datastore = DatastoreServiceFactory.getDatastoreService();
    sanitizer = Sanitizers.FORMATTING.and(Sanitizers.BLOCKS).and(Sanitizers.STYLES).and(Sanitizers.LINKS);
  }
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    int commentsPerPage = Integer.parseInt(request.getParameter("comments_per_page"));
    int pgNumber = Integer.parseInt(request.getParameter("pg_number")) - 1;
    Query commentsQuery = new Query("Comment").addSort("time_millis", SortDirection.DESCENDING);
    List<Entity> results = 
      datastore.prepare(commentsQuery).asList(
        FetchOptions.Builder
          .withLimit(commentsPerPage)
          .offset(commentsPerPage * pgNumber));
    List<Comment> comments = new ArrayList<Comment>();
    for(Entity comment : results){
      comments.add(makeComment(comment));
    }
    response.setContentType("application/json;");
    response.getWriter().println(convertToJsonUsingGson(new Response(comments, getLastPage(commentsPerPage))));
  }
  
  // Not very efficient, can improve with another entity to track total number of comments.
  private int getLastPage(double commentsPerPage){
    Query commentsQuery = new Query("Comment");
    int entitiesCount =  datastore.prepare(commentsQuery).countEntities(FetchOptions.Builder.withDefaults());
    return (int) Math.ceil(entitiesCount / commentsPerPage);
  }
  
  private static Comment makeComment(Entity comment){
    SimpleDateFormat sdf = new SimpleDateFormat();    
    Date resultDate = new Date((Long)comment.getProperty("time_millis"));
    return new Comment( (String)comment.getProperty("email"), 
                        (String)comment.getProperty("comment"),
                        sdf.format(resultDate));
  }
  
  private static String convertToJsonUsingGson(Response response) {
    Gson gson = new Gson();
    return gson.toJson(response);
  }
  
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Entity commentEntity = new Entity("Comment");
    String comment = sanitizer.sanitize(request.getParameter("user_comment"));
    commentEntity.setProperty("email", request.getParameter("user_email"));
    commentEntity.setProperty("comment", comment);
    commentEntity.setProperty("time_millis", System.currentTimeMillis());    
    datastore.put(commentEntity);
    updateWordCount(comment);
    response.sendRedirect("/comments.html");
  }

  private void updateWordCount(String comment) {
    String[] words = comment.replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+");
    
    for(String word : words){
      Entity wordEntity;
      Key wordKey = KeyFactory.createKey("word", word + "_word");
      Transaction txn = datastore.beginTransaction(TransactionOptions.Builder.withXG(true));
      try {
        wordEntity = datastore.get(txn, wordKey);
      } catch (com.google.appengine.api.datastore.EntityNotFoundException e) {
        // In case word is not yet in datastore...
        wordEntity = new Entity(wordKey);
        wordEntity.setProperty("count", 0L);
      }
      wordEntity.setProperty("count", (Long) wordEntity.getProperty("count") + 1);
      datastore.put(txn, wordEntity);
      
      int retry = 0;
      while(true){
        try {
          txn.commit();
          break;
        } catch (Exception e) {
          System.out.println("Transaction to update count did not commit for word: " + word);
          if (retry++ == TRANSACTION_RETRIES) {
            System.out.println("Giving up trying to commit count for word: " + word);
            return;
          }
        }
      }
    }
  }
}
