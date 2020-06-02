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

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import com.google.gson.Gson;

/** Servlet that returns some example content. TODO: modify this file to handle comments data */
@WebServlet("/data")
public class DataServlet extends HttpServlet {
  class Comment {
    String email, comment;
    public Comment(String email, String comment){
      this.email=email;
      this.comment=comment;
    }
  }
  ArrayList<Comment> comments = new ArrayList<Comment>();

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String comments_json = convertToJsonUsingGson(comments);
    response.setContentType("application/json;");
    response.getWriter().println(comments_json);
  }
  
  private String convertToJsonUsingGson(ArrayList<Comment> arr) {
    Gson gson = new Gson();
    String json = gson.toJson(arr);
    return json;
  }
  
    @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

    String user_email = request.getParameter("user_email");
    String user_comment = request.getParameter("user_comment");
    comments.add(new Comment(user_email, user_comment));
    response.sendRedirect("/comments.html");
  }
}
