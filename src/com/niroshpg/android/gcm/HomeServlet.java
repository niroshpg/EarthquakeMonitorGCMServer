/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.niroshpg.android.gcm;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListThreadsResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.Thread;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.niroshpg.android.gmail.GmailUtils;

/**
 * Servlet that adds display number of devices and button to send a message.
 * <p>
 * This servlet is used just by the browser (i.e., not device) and contains the
 * main page of the demo app.
 */
@SuppressWarnings("serial")
public class HomeServlet extends BaseServlet {

  static final String ATTRIBUTE_STATUS = "status";
  
  private static final String APP_NAME = "Gmail API Quickstart";

private static final String USER = "me";

  /**
   * Displays the existing messages and offer the option to send a new one.
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    resp.setContentType("text/html");
    PrintWriter out = resp.getWriter();

    out.print("<html><body>");
    out.print("<head>");
    out.print("  <title>GCM Demo</title>");
    out.print("  <link rel='icon' href='favicon.png'/>");
    out.print("</head>");
    String status = (String) req.getAttribute(ATTRIBUTE_STATUS);
    if (status != null) {
      out.print(status);
    }
    int total = Datastore.getTotalDevices();
    if (total == 0) {
      out.print("<h2>No devices registered!</h2>");
    } else {
      out.print("<h2>" + total + " device(s) registered!</h2>");
      out.print("<form name='form' method='POST' action='sendAll'>");
      out.print("<input type='submit' value='Send Message' />");
      out.print("</form>");
    }
    
 
    
    UserService userService = UserServiceFactory.getUserService();
    if(userService != null && req.getRequestURL() != null)
    {
	    out.println("<div class=\"header\"><b>" +"user" + "</b> | "
	        + "<a href=\"" + userService.createLogoutURL(req.getRequestURL().toString())
	        + "\">Log out</a></div>");
	    HttpTransport httpTransport = new NetHttpTransport();
	    JsonFactory jsonFactory = new com.google.api.client.json.jackson2.JacksonFactory();
	    
	    Credential credential = GmailUtils.newFlow().loadCredential(userService.getCurrentUser().getUserId());
	
		// Create a new authorized Gmail API client
		Gmail service = new Gmail.Builder(httpTransport, jsonFactory, credential)
		    .setApplicationName(APP_NAME).build();
		
		// Retrieve a page of Threads; max of 100 by default.
		ListThreadsResponse threadsResponse = service.users().threads().list(USER).execute();
		List<Thread> threads = threadsResponse.getThreads();
		
		// Print ID of each Thread.
		for (Thread thread : threads) {
		  System.out.println("Thread ID: " + thread.getId()  );
		  if(thread.getSnippet() != null)
		  {
			  System.out.println("Thread ID:  " + thread.getId() + " snippet : "+  thread.getSnippet()  );
		  }
		  if( thread.getMessages() != null)
		  {
			 
		  for(Message msg: thread.getMessages())
		  {
			  if(msg != null && msg.getSnippet()!= null)
			  {
				  System.out.println("Thread ID:  " + thread.getId() + " msg snippet : "+  msg.getSnippet()  );
			  }
		  }
		  }
		}
    }
    else
    {
    	out.println("<p> User service is not available </p>");
    }
    
   

    out.print("</body></html>");
    resp.setStatus(HttpServletResponse.SC_OK);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    doGet(req, resp);
  }

}
