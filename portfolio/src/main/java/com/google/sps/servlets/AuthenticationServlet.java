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

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.gson.Gson;

@WebServlet("/authenticate")
public class AuthenticationServlet extends HttpServlet {
  // For when user is logged in.
  private static class LoggedInResponse {
    private boolean isUserLoggedIn;
    private String logoutUrl, userEmail;
    public LoggedInResponse(String logoutUrl, String userEmail) {
      this.isUserLoggedIn = true;
      this.logoutUrl = logoutUrl;
      this.userEmail = userEmail;
    }
  }

  // For when user is logged out.
  private static class LoggedOutResponse {
    private boolean isUserLoggedIn;
    private String loginUrl;
    public LoggedOutResponse(String loginUrl) {
      this.isUserLoggedIn = false;
      this.loginUrl = loginUrl;
    }
  }

  private static final String ROOT_URL = "/";

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");

    UserService userService = UserServiceFactory.getUserService();
    Gson gson = new Gson();
    if (userService.isUserLoggedIn()) {
      response.getWriter().println(gson.toJson(
        new LoggedInResponse(userService.createLogoutURL(ROOT_URL),
        userService.getCurrentUser().getEmail())));
    } else {
      response.getWriter().println(gson.toJson(
        new LoggedOutResponse(userService.createLoginURL(ROOT_URL))));
    }
  }
}