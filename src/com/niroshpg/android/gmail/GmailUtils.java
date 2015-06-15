/*
 * Copyright (c) 2010 Google Inc.
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

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import com.google.api.client.extensions.appengine.datastore.AppEngineDataStoreFactory;
import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Preconditions;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.services.gmail.GmailScopes;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.niroshpg.android.gcm.Datastore;

/**
 * Utility class for JDO persistence, OAuth flow helpers, and others.
 *
 * @author Yaniv Inbar
 */
public class GmailUtils {

  /**
   * Global instance of the {@link DataStoreFactory}. The best practice is to make it a single
   * globally shared instance across your application.
   */
  private static final AppEngineDataStoreFactory DATA_STORE_FACTORY =
      AppEngineDataStoreFactory.getDefaultInstance();
  
  /** Global instance of the HTTP transport. */
  static final HttpTransport HTTP_TRANSPORT = new UrlFetchTransport();

  /** Global instance of the JSON factory. */
  static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  
  public static final String MAIN_SERVLET_PATH = "/plussampleservlet";

  private static GoogleClientSecrets clientSecrets = null;
  
  private static final Logger logger =
	      Logger.getLogger(GmailUtils.class.getName());

  static GoogleClientSecrets getClientCredential() throws IOException {
    if (clientSecrets == null) {
      clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
          new InputStreamReader(GmailUtils.class.getResourceAsStream("/client_secret.json")));
      Preconditions.checkArgument(!clientSecrets.getDetails().getClientId().startsWith("Enter ")
          && !clientSecrets.getDetails().getClientSecret().startsWith("Enter "),
          "Download client_secret.json file from https://code.google.com/apis/console/"
          + "?api=calendar into calendar-appengine-sample/src/main/resources/client_secret.json");
    }
    return clientSecrets;
  }

  static String getRedirectUri(HttpServletRequest req) {
    GenericUrl url = new GenericUrl(req.getRequestURL().toString());
    url.setRawPath("/oauth2callback");
    
    logger.warning("URL: " + url.build());
    
    return url.build();
  }

  public static GoogleAuthorizationCodeFlow newFlow() throws IOException {
    return new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
        getClientCredential(),Collections.singleton(GmailScopes.GMAIL_MODIFY)).setDataStoreFactory(
        DATA_STORE_FACTORY).setAccessType("offline").build();
  }

//  static Calendar loadCalendarClient() throws IOException {
//    String userId = UserServiceFactory.getUserService().getCurrentUser().getUserId();
//    Credential credential = newFlow().loadCredential(userId);
//    return new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).build();
//  }

  static String getUserId(HttpServletRequest req) {
	    UserService userService = UserServiceFactory.getUserService();
	    User user = userService.getCurrentUser();
	    return user.getUserId();
	  }
  /**
   * Returns an {@link IOException} (but not a subclass) in order to work around restrictive GWT
   * serialization policy.
   */
  static IOException wrappedIOException(IOException e) {
    if (e.getClass() == IOException.class) {
      return e;
    }
    return new IOException(e.getMessage());
  }

  private GmailUtils() {
  }
}
