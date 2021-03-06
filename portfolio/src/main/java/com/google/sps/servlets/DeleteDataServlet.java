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
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Key;
import com.google.gson.Gson;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.sps.data.WordCountUpdater;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet that handles comments data */
@WebServlet("/delete-data")
public class DeleteDataServlet extends HttpServlet {

  private DatastoreService datastore;
  private WordCountUpdater wordCountUpdater;

  @Override
  public void init() {
    datastore = DatastoreServiceFactory.getDatastoreService();
    wordCountUpdater = new WordCountUpdater();
  }

  @Override
  public void doDelete(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    Key commentKey = KeyFactory.createKey("Comment", Long.parseLong(request.getParameter("id")));
    try {
      Entity comment = datastore.get(commentKey);

      // Verify the deleter is the author of the comment being deleted. If not, early return.
      UserService userService = UserServiceFactory.getUserService();
      if (!userService.isUserLoggedIn()
          || !comment.getProperty("email").equals(userService.getCurrentUser().getEmail())) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        return;
      }
      wordCountUpdater.updateWordCount(
          (String) comment.getProperty("comment"), WordCountUpdater.UpdateOp.REMOVE_WORDS);
      datastore.delete(commentKey);
    } catch (com.google.appengine.api.datastore.EntityNotFoundException e) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    response.sendRedirect("/comments.html");
  }
}
