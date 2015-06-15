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

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.StringUtils;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.Gmail.Users.GetProfile;
import com.google.api.services.gmail.model.History;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListHistoryResponse;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.ListThreadsResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.ModifyMessageRequest;
import com.google.api.services.gmail.model.Thread;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Method;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;
import com.google.gson.Gson;
import com.niroshpg.android.gcm.Datastore;
import com.niroshpg.android.gcm.SendMessageServlet;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Sample Google+ servlet that loads user credentials and then shows their profile link.
 *
 * @author Nick Miceli
 */
public class PlusSampleServlet extends HttpServlet {

  private static final long serialVersionUID = 1;
  private static final String APP_NAME = "earthquakemonitorapp";
  
  private static final Logger logger =
	      Logger.getLogger(MessageUtilityTest.class.getName());

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException, ServletException {
    // Check if we have stored credentials using the Authorization Flow.
    // Note that we only check if there are stored credentials, but not if they are still valid.
    // The user may have revoked authorization, in which case we would need to go through the
    // authorization flow again, which this implementation does not handle.
    GoogleAuthorizationCodeFlow authFlow = GmailUtils.newFlow();
    
    UserService userService = UserServiceFactory.getUserService();
    
    Credential credential = authFlow.loadCredential(userService.getCurrentUser().getUserId());

    //
    if (credential == null) {
    	//
      // If we don't have a token in store, redirect to authorization screen.
    	logger.warning("auth flow started ...");
      resp.sendRedirect(
          authFlow.newAuthorizationUrl().setRedirectUri(GmailUtils.getRedirectUri(req)).build());
      return;
    }
//    try{
//    	credential.refreshToken();
//    }
//    catch(TokenResponseException e){
//        resp.sendRedirect(
//                authFlow.newAuthorizationUrl().setRedirectUri(GmailUtils.getRedirectUri(req)).build());
//        return;
//    }
    
	// Create a new authorized Gmail API client
	Gmail service = new Gmail.Builder(GmailUtils.HTTP_TRANSPORT, GmailUtils.JSON_FACTORY, credential)
		    .setApplicationName(APP_NAME).build();
   // Make the API call
	BigInteger startHistoryId = null;
	
	//service.users().getProfile("me").setRequestHeaders(service.users().getProfile("me").getRequestHeaders().)
	
	startHistoryId = getHistoryId(service, "me",credential);
	logger.warning("hid[url]= " + startHistoryId);
	List<Label> lableList = listLabels(service,"me");
	
	List<Message> messegeList = listMessagesWithLabels(
			service,
			"me",
			Arrays.asList(getLableIdForName(lableList,"EQM")/*,
					getLableIdForName(lableList,"UNREAD")*/
					)
			);
	
	logger.warning("store messages for processing ... ");
	for(Message message : messegeList)
	{
		
		//Message detailMessage = getMessage(service, "me", message.getId());
		String messageBody ="";
		try {
			MimeMessage mimeMessage = getMimeMessage(service, "me", message.getId());
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			mimeMessage.writeTo(baos);
			messageBody = baos.toString();
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//logger.warning("working "+detailMessage.getSnippet()+" ... ");
		//logger.warning("messageBody= "+messageBody+" ... ");
		
		//String messageBody = StringUtils.newStringUtf8(Base64.decodeBase64(detailMessage.getRaw()));//StringUtils.newStringUtf8(detailMessage.getPayload().getBody().decodeData()/*Base64.decodeBase64(detailMessage.getPayload().getBody().decodeData())*/);
		//String messageBody = StringUtils.newStringUtf8(detailMessage.getPayload().getBody().decodeData());
		String extractedMsgBody = MessageUtility.extractData(messageBody);
		//logger.warning("adding "+extractedMsgBody+" ... ");
		Datastore.addMessage(extractedMsgBody);		
	}
	
	logger.warning("invoke send all");
	sendMessagesToAll();
	logger.warning("removing label from messages ...");
	removeUnRead(service, "me", messegeList);
	
	//List<History> historyList = null;
	//if(messegeList != null && messegeList.size() > 1)
	//{ 
	//	logger.warning("messege count = " + messegeList.size());
	//	
	//	for(Message amsg : messegeList)
	//	{
			//logger.warning("id= " + amsg.getId());
		//	if(amsg.getHistoryId() != null)
		//	{
		//		startHistoryId = amsg.getHistoryId();
				//logger.warning("hid= " + amsg.getHistoryId());
		//		break;
		//	}
		//}
//		if(startHistoryId != null)
//		{		
//			historyList = listHistory(service, "me", startHistoryId);
//		}
//		else
//		{
//			logger.warning("could not find start history id");
//			
//			//historyList = listHistory(service, "me", BigInteger.valueOf(1));
//			
//		}
//	}
	
 		
 		
 		resp.setContentType("text/html");
 		resp.setCharacterEncoding("UTF-8");
 		    PrintWriter writer = resp.getWriter();
 		    writer.println("<!doctype html><html><head>");
 		    writer.println("<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\">");
 		    writer.println("<title>" + APP_NAME + "</title>");

 		    writer.println("</head><body>");
 		    
 		   //printThreadIds(service,writer);
		    if(messegeList != null && messegeList.size() >0)
 		    {
		    	writer.println("<p> msg count = "+messegeList.size()+"</p>");
	 		    //for(Message msg : messegeList){	 		    	
	 		    	//writer.println("<p>"+msg.toPrettyString()+"</p>");
	 		    //}
 		    }
// 		    if(historyList != null && historyList.size() >0)
// 		    {
//	 		    for(History history : historyList){	 		    	
//	 		    	writer.println("<p>"+history.toPrettyString()+"</p>");
//	 		    }
// 		    }
 		    else{
 		    	writer.println("<p>history not found</p>");
 		    }
 		    
 		    writer.println("<div class=\"header\"><b>" + req.getUserPrincipal().getName() + "</b> | "
 		        + "<a href=\"" + userService.createLogoutURL(req.getRequestURL().toString())
 		        + "\">Log out</a> | "
 		        + "<a href=\"http://code.google.com/p/google-api-java-client/source/browse"
 		        + "/calendar-appengine-sample?repo=samples\">See source code for "
 		        + "this sample</a></div>");
 		    writer.println("<div id=\"main\"/>");
 		    writer.println("</body></html>");
 		    

  }
  private void sendMessagesToAll()
  {
	  List<String> devices = Datastore.getDevices();
	    String status="";
	    if (devices.isEmpty()) {
	      status = "Message ignored as there is no device registered!";
	      logger.warning(status);
	    } else {
	      Queue queue = QueueFactory.getQueue("gcm");
	      // NOTE: check below is for demonstration purposes; a real application
	      // could always send a multicast, even for just one recipient
	     // if (devices.size() == 1) {
	        // send a single message using plain post
	     //   String device = devices.get(0);
	        //queue.add(withUrl("/send").param(
	       //     SendMessageServlet.PARAMETER_DEVICE, device));
	       // status = "Single message queued for registration id " + device;
	        
	       // logger.warning(status);
	     // } else {
	        // send a multicast message using JSON
	        // must split in chunks of 1000 devices (GCM limit)
	        int total = devices.size();
	        List<String> partialDevices = new ArrayList<String>(total);
	        int counter = 0;
	        int tasks = 0;
	        for (String device : devices) {
	        	logger.warning("sending messages to device " + device);
	          counter++;
	          partialDevices.add(device);
	          int partialSize = partialDevices.size();
	          if (partialSize == Datastore.MULTICAST_SIZE || counter == total) {
	            String multicastKey = Datastore.createMulticast(partialDevices);
	            logger.fine("Queuing " + partialSize + " devices on multicast " +
	                multicastKey);
	            TaskOptions taskOptions = TaskOptions.Builder
	                .withUrl("/send")
	                .param(SendMessageServlet.PARAMETER_MULTICAST, multicastKey)
	                .method(Method.POST);
	            queue.add(taskOptions);
	            partialDevices.clear();
	            tasks++;
	          }
	      //  }
	        status = "Queued tasks to send " + tasks + " multicast messages to " +
	            total + " devices";
	      }
	    }
  }
  private String getLableIdForName(List<Label> lables, String name)
  {
	  String id = null;
	  
	  for(Label aLabel : lables)
	  {
		  if(aLabel.getName().contains(name))
		  {
			  id = aLabel.getId();
			  break;
		  }
	  }
	  return id;
  }

  public  BigInteger getHistoryIdXX(Gmail service, String userId, Credential credential) throws IOException {
	  BigInteger historyId = null;

          try {
        	  URL url = new URL("https://www.googleapis.com/gmail/v1/users/"+userId+"/profile");
        	  
        	    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        	    //credential.refreshToken();
        	    connection.setRequestProperty("Authorization", "Bearer " + credential.getAccessToken());
        	    connection.setRequestProperty("Content-Type", "application/json");
        	    

        	    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
        	        // OK
        	        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        	        StringBuffer res = new StringBuffer();
        	        String line;
        	        while ((line = reader.readLine()) != null) {
        	            res.append(line);
        	            logger.warning(line);
		                
        	        }
        	        reader.close();

        	        JSONObject jsonObj = new JSONObject(res);
        	         historyId = BigInteger.valueOf(jsonObj.getLong("historyId"));
        	       

        	    } else {
        	        // Server returned HTTP error code.
        	    	logger.warning("failed : " + connection.getResponseCode());
        	    	
        	    	 BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                
		                String error = "";
		                String text;
		                while ((text = br.readLine()) != null){
		                	error += text;
		                }
		                
		                logger.warning("error : " + error);
        	    }

        	} catch (Exception e) {
        		logger.warning("exception : " +e.getMessage()+" , " + e.getStackTrace().toString());
        	}
	  return historyId;
  
  }
  
  
  public  BigInteger getHistoryId(Gmail service, String userId, Credential credential) throws IOException {
	  BigInteger historyId = null;

	  if(getStartHistoyId() != null)
	  {
		  historyId = getStartHistoyId();
	  }
	  else
	  {
	  HttpResponse response = service.users().getProfile("me").buildHttpRequest().execute();
	   try {
	  	BufferedReader reader = new BufferedReader(new InputStreamReader(response.getContent()));
	     StringBuffer res = new StringBuffer();
	     String line;
	     while ((line = reader.readLine()) != null) {
	         res.append(line);
	         //logger.warning(line);
	         
	     }
	     reader.close();

	    // JSONObject jsonObj = new JSONObject(res);
	     
	     Gson gson = new Gson();
	     GmailProfileData gmailProfileData = gson.fromJson(res.toString(),GmailProfileData.class);
	     
	      logger.warning(" historyId : " + gmailProfileData.getHistoryId());
	      
	      historyId = BigInteger.valueOf(Long.parseLong(gmailProfileData.getHistoryId()));
	    //  historyId = BigInteger.valueOf(Long.parseLong("14085"));
	      
	   } finally {
	     response.disconnect();
	   }
	  }
	  
	  return historyId; 
	 // return  BigInteger.valueOf(Long.parseLong("14085"));
  
  }
  
 
  
  
  /**
   * List all Messages of the user's mailbox with labelIds applied.
   *
   * @param service Authorized Gmail API instance.
   * @param userId User's email address. The special value "me"
   * can be used to indicate the authenticated user.
   * @param labelIds Only return Messages with these labelIds applied.
   * @throws IOException
   */
  public  List<Message> listMessagesWithLabels(Gmail service, String userId,
      List<String> labelIds) throws IOException {
	  
	  
    ListMessagesResponse response = service.users().messages().list(userId)
        .setLabelIds(labelIds).execute();
    

    List<Message> messages = new ArrayList<Message>();
    while (response.getMessages() != null) {
      messages.addAll(response.getMessages());
      if (response.getNextPageToken() != null) {
        String pageToken = response.getNextPageToken();
        response = service.users().messages().list(userId).setLabelIds(labelIds)
            .setPageToken(pageToken).execute();
      } else {
        break;
      }
    }

    //for (Message message : messages) {
     // System.out.println(message.toPrettyString());
    	// logger.warning("mid: "+ message.getId() + ", hid : " + message.getHistoryId()); 
    	// logger.warning(message.toPrettyString());
    //}

    return messages;
  }
  
  /**
   * Get Message with given ID.
   *
   * @param service Authorized Gmail API instance.
   * @param userId User's email address. The special value "me"
   * can be used to indicate the authenticated user.
   * @param messageId ID of Message to retrieve.
   * @return Message Retrieved Message.
   * @throws IOException
   */
  public  Message getMessage(Gmail service, String userId, String messageId)
      throws IOException {
    Message message = service.users().messages().get(userId, messageId).set("format", "full")/*.set("fields", "payload")*/.execute();
    
    Properties props = new Properties();
	Session session = Session.getDefaultInstance(props , null);
	//MimeMessage im = new MimeMessage(session, message.getPayload().getBody().decodeData());
    System.out.println("Message snippet: " + message.getSnippet());
    logger.warning("Message snippet: " + message.getSnippet());
    logger.warning("Message payload: " + message.getSnippet());

    return message;
  }
  
  
  /**
   * Get a Message and use it to create a MimeMessage.
   *
   * @param service Authorized Gmail API instance.
   * @param userId User's email address. The special value "me"
   * can be used to indicate the authenticated user.
   * @param messageId ID of Message to retrieve.
   * @return MimeMessage MimeMessage populated from retrieved Message.
   * @throws IOException
   * @throws MessagingException
   */
  public static MimeMessage getMimeMessage(Gmail service, String userId, String messageId)
      throws IOException, MessagingException {
    Message message = service.users().messages().get(userId, messageId).setFormat("raw").execute();

    byte[] emailBytes = Base64.decodeBase64(message.getRaw());

    Properties props = new Properties();
    Session session = Session.getDefaultInstance(props, null);

    MimeMessage email = new MimeMessage(session, new ByteArrayInputStream(emailBytes));

    return email;
  }
  

public  void printThreadIds(Gmail service,PrintWriter writer ) throws IOException{
	// Retrieve a page of Threads; max of 100 by default.
	 		ListThreadsResponse threadsResponse = service.users().threads().list("me").execute();

	 		List<Thread> threads = threadsResponse.getThreads();
	 		logger.warning("No of threads : " + threads.size());
			 
		// Print ID of each Thread.
		for (Thread thread : threads) {
		 // System.out.println("Thread ID: " + thread.getId()  );
		// logger.warning("Thread ID: " + thread.getId() +" history id: " +  thread.getHistoryId());
		 
		  if(thread.getSnippet() != null)
		  {
			 // System.out.println("Thread ID:  " + thread.getId() + " snippet : "+  thread.getSnippet()  );
			// logger.warning("Thread ID:  " + thread.getId() + " snippet : "+  thread.getSnippet()  );
			writer.println("<p> Thread ID:  " + thread.getId() + " snippet : "+  thread.getSnippet()  + "</p>");
		  }
		  if( thread.getMessages() != null)
		  {
			 
		  for(Message msg: thread.getMessages())
		  {
			  if(msg != null && msg.getSnippet()!= null)
			  {
				  //System.out.println("Thread ID:  " + thread.getId() + " msg snippet : "+  msg.getSnippet()  );
				// logger.warning("Thread ID:  " + thread.getId() + " msg snippet : "+  msg.getSnippet()   );
				writer.println("<p> Thread ID:  " + thread.getId() + " msg snippet : "+  msg.getSnippet() +  "</p>");
			  }
		  }
		  }
		}
  }
  
  /**
   * Print message changes.
   *
   * @param service Authorized Gmail API instance.
   * @param userId User's email address. The special value "me"
   * can be used to indicate the authenticated user.
   * @param startHistoryId Only return Histories at or after startHistoryId.
   * @throws IOException
   */
  public  List<History> listHistory(Gmail service, String userId, BigInteger startHistoryId)
      throws IOException {
	  logger.warning("in list history" );
	  
	  

    List<History> histories = new ArrayList<History>();
    ListHistoryResponse response = service.users().history().list(userId)
        .setStartHistoryId(startHistoryId).execute();
    
    if(response!= null  )
    {
    	logger.warning("response : " +  response.toPrettyString());
    	if(response.getHistory() == null)
    	{
    		 logger.warning(" storing history id [ no history]..." );
       	  	storeStartHistoryId(response.getHistoryId());
    	}
    }
    else
    {
    	logger.warning("response is empty or null" );
    }

    
    while (response.getHistory() != null) {
    	 logger.warning("adding entries ..." );
      histories.addAll(response.getHistory());
      if (response.getNextPageToken() != null) {
        String pageToken = response.getNextPageToken();
        response = service.users().history().list(userId).setPageToken(pageToken)
            .setStartHistoryId(startHistoryId).execute();
      } else {
    	  logger.warning(" storing history id [no next token] ..." );
    	  storeStartHistoryId(response.getHistoryId());
        break;
      }
    }

    for (History history : histories) {
      //System.out.println(history.toPrettyString());
      logger.warning(history.toPrettyString() );
    }
    
    logger.warning("in list history : done" );
    return histories;
  }
  
  private void storeStartHistoryId(BigInteger startHistoryId)
  {
	    Key startHistoryEntityKey = KeyFactory.createKey("StartHistroyEntityKey", "StartHistoryEntityValue");
	    Entity startHistroyEntity = new Entity("StartHistroyEntity", startHistoryEntityKey);
	    startHistroyEntity.setProperty("id", startHistoryId.toString());
	    
	 
	    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	    logger.warning("stored value : " + startHistoryId.toString() );
	    datastore.put(startHistroyEntity); //save it
  }
  
  private BigInteger getStartHistoyId()
  {
	  BigInteger startHistoyId = null;
	Query query = new Query("StartHistroyEntity");
		
	query.setFilter(FilterOperator.EQUAL.of("StartHistroyEntityKey","StartHistoryEntityValue"));
	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	PreparedQuery pq = datastore.prepare(query);
	Entity startHistroyEntity = pq.asSingleEntity();
	try{
		if(startHistroyEntity.hasProperty("id"))
		{
			startHistoyId = new BigInteger((String) startHistroyEntity.getProperty("id"));
			 logger.warning("read value : " + startHistoyId.toString() );
		}
	}
	catch(NullPointerException e){
		logger.warning("id property is not defined in StartHistroyEntity");
	}
	
	return startHistoyId;
  }
  
  /**
   * List the Labels in the user's mailbox.
   *
   * @param service Authorized Gmail API instance.
   * @param userId User's email address. The special value "me"
   * can be used to indicate the authenticated user.
   * @throws IOException
   */
  public  List<Label> listLabels(Gmail service, String userId) throws IOException {
    ListLabelsResponse response = service.users().labels().list(userId).execute();
    List<Label> labels = response.getLabels();
    //for (Label label : labels) {
      //System.out.println(label.toPrettyString());
      //logger.warning("Label:  " + label.toPrettyString()  );
   // }
	return labels;
	
  }
  
  private void removeUnRead(Gmail service, String userId,List<Message> messages)
  {
		
	  try {	    
		  List<Label> lableList = listLabels(service,"me");
		  for(Message messagege: messages)
		  {			 
			  logger.warning("removing lable UNREAD from message " + messagege.getId() + " ...");
			modifyMessage(service, userId, messagege.getId(), Arrays.asList(getLableIdForName(lableList,"EQMR")), Arrays.asList(getLableIdForName(lableList,"EQM")));			
		  }		 
	  } catch (IOException e) {
		  logger.warning("exception : " + e.getMessage()); 
	  }		  
  }
/**
   * Modify the labels a message is associated with.
   *
   * @param service Authorized Gmail API instance.
   * @param userId User's email address. The special value "me"
   * can be used to indicate the authenticated user.
   * @param messageId ID of Message to Modify.
   * @param labelsToAdd List of label ids to add.
   * @param labelsToRemove List of label ids to remove.
   * @throws IOException
   */
  public void modifyMessage(Gmail service, String userId, String messageId,
      List<String> labelsToAdd, List<String> labelsToRemove) throws IOException {
    ModifyMessageRequest mods = new ModifyMessageRequest().setAddLabelIds(labelsToAdd)
        .setRemoveLabelIds(labelsToRemove);
    Message message = service.users().messages().modify(userId, messageId, mods).execute();

    //logger.warning("Message id: " + message.getId());
   // logger.warning(message.toPrettyString());
  }
}
