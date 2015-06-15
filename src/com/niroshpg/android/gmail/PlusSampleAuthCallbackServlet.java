/*
 * Copyright (c) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.niroshpg.android.gmail;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Sample servlet that exchanges the authorization code for user credentials and then redirects back
 * to the main servlet.
 *
 * @author Nick Miceli
 */
public class PlusSampleAuthCallbackServlet extends HttpServlet {

  private static final long serialVersionUID = 1;

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException, ServletException {
    // Note that this implementation does not handle the user denying authorization.
    GoogleAuthorizationCodeFlow authFlow = GmailUtils.newFlow();
    // Exchange authorization code for user credentials.
    GoogleTokenResponse tokenResponse = authFlow.newTokenRequest(req.getParameter("code"))
        .setRedirectUri(GmailUtils.getRedirectUri(req)).execute();
    // Save the credentials for this user so we can access them from the main servlet.
    authFlow.createAndStoreCredential(tokenResponse, GmailUtils.getUserId(req));
    resp.sendRedirect(GmailUtils.MAIN_SERVLET_PATH);
  }
}
