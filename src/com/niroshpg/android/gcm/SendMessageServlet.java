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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.google.appengine.labs.repackaged.org.json.JSONArray;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

/**
 * Servlet that sends a message to a device.
 * <p>
 * This servlet is invoked by AppEngine's Push Queue mechanism.
 */
@SuppressWarnings("serial")
public class SendMessageServlet extends BaseServlet {

  private static final String HEADER_QUEUE_COUNT = "X-AppEngine-TaskRetryCount";
  private static final String HEADER_QUEUE_NAME = "X-AppEngine-QueueName";
  private static final int MAX_RETRY = 3;

  public static final String PARAMETER_DEVICE = "device";
  public static final String PARAMETER_MULTICAST = "multicastKey";

  private Sender sender;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    sender = newSender(config);
  }

  /**
   * Creates the {@link Sender} based on the servlet settings.
   */
  protected Sender newSender(ServletConfig config) {
    String key = (String) config.getServletContext()
        .getAttribute(ApiKeyInitializer.ATTRIBUTE_ACCESS_KEY);
    return new Sender(key);
  }

  /**
   * Indicates to App Engine that this task should be retried.
   */
  private void retryTask(HttpServletResponse resp) {
    resp.setStatus(500);
  }

  /**
   * Indicates to App Engine that this task is done.
   */
  private void taskDone(HttpServletResponse resp) {
    resp.setStatus(200);
  }

  /**
   * Processes the request to add a new message.
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
	  logger.warning("in send messages ....");
    if (req.getHeader(HEADER_QUEUE_NAME) == null) {
      throw new IOException("Missing header " + HEADER_QUEUE_NAME);
    }
    String retryCountHeader = req.getHeader(HEADER_QUEUE_COUNT);
    logger.fine("retry count: " + retryCountHeader);
    if (retryCountHeader != null) {
      int retryCount = Integer.parseInt(retryCountHeader);
      if (retryCount > MAX_RETRY) {
          logger.severe("Too many retries, dropping task");
          taskDone(resp);
          return;
      }
    }
//    String regId = req.getParameter(PARAMETER_DEVICE);
//    if (regId != null) {
//      sendSingleMessage(regId, resp);
//      return;
//    }
    String multicastKey = req.getParameter(PARAMETER_MULTICAST);
    if (multicastKey != null) {
      sendMulticastMessage(multicastKey, resp);
      return;
    }
    logger.severe("Invalid request!");
    taskDone(resp);
    return;
  }

  private Message createMessage() {
    Message message = new Message.Builder().addData("content", "test-value").build();
    return message;
  }
  
  private List<Message> createMessages() {
	  List<String> messagesList = Datastore.getMessages();
	  List<Message> messages = new ArrayList<Message>();
	  for(String messageContent: messagesList)
	  {
		  if(messageContent!= null && !messageContent.isEmpty())
		  {
		  Gson gson = new Gson();
	        Type type = new TypeToken<Map<String, String>>(){}.getType();
	        Map<String, String> map = gson.fromJson(messageContent, type);
	        logger.warning("building gcm with content : " + messageContent +" ...");
	    Message message = new Message.Builder().addData("content", messageContent)
	    		.addData("magnitude", map.get("magnitude"))
	    		.addData("cordinates",  map.get("cordinates"))
	    		.addData("depth",  map.get("depth")).build();
	    messages.add(message);
		  }
	   
	  }
	    return messages;
	  }

  private void sendSingleMessage(String regId, HttpServletResponse resp) {
    logger.info("Sending message to device " + regId);
    Message message = createMessage();
    
    Result result;
    try {
      result = sender.sendNoRetry(message, regId);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Exception posting " + message, e);
      taskDone(resp);
      return;
    }
    if (result == null) {
      retryTask(resp);
      return;
    }
    if (result.getMessageId() != null) {
      logger.info("Succesfully sent message to device " + regId);
      String canonicalRegId = result.getCanonicalRegistrationId();
      if (canonicalRegId != null) {
        // same device has more than on registration id: update it
        logger.finest("canonicalRegId " + canonicalRegId);
        Datastore.updateRegistration(regId, canonicalRegId);
      }
    } else {
      String error = result.getErrorCodeName();
      if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
        // application has been removed from device - unregister it
        Datastore.unregister(regId);
      } else {
        logger.severe("Error sending message to device " + regId
            + ": " + error);
      }
    }
  }

  private Polygon createPolygon(JSONObject polygonJson)
  {
	final GeometryFactory geometryFactory = new GeometryFactory();
	final ArrayList<Coordinate> points = new ArrayList<Coordinate>();	    
	    
    try {
    	JSONArray regionPoygon = (JSONArray) polygonJson.get("regionPolygon");
    	logger.warning("regionPoygon  points = " + regionPoygon.length() );
    	for(int i=0; i<regionPoygon.length();i++ )
    	{
    		JSONArray coordPpoint = (JSONArray) regionPoygon.get(i);
    		 // logger.warning("processing  " + coordPpoint.toString() + " ...");
    		  
    		  JSONObject latJsonObj = (JSONObject) coordPpoint.get(0);
    		  JSONObject lngJsonObj = (JSONObject) coordPpoint.get(0);
    		  
    		Double lat = latJsonObj.getDouble("lat");
    		Double lng = lngJsonObj.getDouble("lng"); 
    		//logger.warning("latitude = "+ lat + ", longitude =  " +lng);
    		
    		points.add(new Coordinate(lat,lng));
    	}
		
	} catch (JSONException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
 

    final Polygon polygon = geometryFactory.createPolygon(new LinearRing(new CoordinateArraySequence(points
        .toArray(new Coordinate[points.size()])), geometryFactory), null);

    return polygon;
  }
  
  protected boolean isLocationWithinPolygonBounds(Polygon polygon,String coordStr)
  {
	  final GeometryFactory geometryFactory = new GeometryFactory();
	  
	  Double lat = 0.0;
	  Double lng = 0.0;
	  
	  String[] coordArrayStrs = coordStr.split(",");
	  String latStr = coordArrayStrs[0].trim();
	 
	  lat = Double.parseDouble(latStr.substring(0,latStr.length()-1));
	
	  if( latStr.charAt(latStr.length()-1) == 'S')
	  {
		  lat = -lat;
	  }
	  
	  String lngStr = coordArrayStrs[1].trim();
	  lng = Double.parseDouble(lngStr.substring(0,lngStr.length()-1));
	  if(lngStr.charAt(lngStr.length()-1) == 'W')
	  {
		  lng = -lng;
	  }

      final Coordinate coord = new Coordinate(lat, lng);
      final Point point = geometryFactory.createPoint(coord);
      
      logger.warning(" point : X = " + point.getX() +", Y = "+ point.getY()+", str =  "+ point.toString() );
      
      boolean isWithinPoint =point.within(polygon);
      
      if(!isWithinPoint)
      {
    	  logger.warning(" point is outside the polygon : " + polygon.toString() ); 
      }

     return point.within(polygon);
  }
  
  
  protected List<String> findDevicesToSend(Message message,  List<String> regIds )
  {
	  List<String> regIdsForMessage = new ArrayList<String>();
		
  		String coordinateStr = message.getData().get("coordinates");//coordinates
  		
  		String magnitudeStr = message.getData().get("magnitude");  		
  		
  		String messageContent = message.getData().get("content");  
  		
  		logger.warning(" Content = " + messageContent +", Coordinates = "+ coordinateStr + ", Magnitude =  " + magnitudeStr);
  		
  		if(coordinateStr == null )
  		{
  			try {
				JSONObject contentJson = new JSONObject(messageContent);
				
				coordinateStr = contentJson.getString("coordinates");
				logger.warning(" Coordinates [ replaced by content ] = " + coordinateStr );
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
  		}
  		
  		Double minMagnitudeForMessage = RegisterServlet.MIN_MAGNITUDE_DEFAULT;
  		
  		if(minMagnitudeForMessage != null)
  		{
  			minMagnitudeForMessage = Double.parseDouble(magnitudeStr);
  		}
  		
  		for(String aRegId : regIds)
  		{
  			JSONObject ploygonJson = Datastore.getPolygon(aRegId);
  			
  			if (ploygonJson != null && ploygonJson.names() != null && ploygonJson.names().length() > 0)
  			{  			
	  			Polygon ploygon =  createPolygon(ploygonJson);
	  			
	  			Double minMagnitudeForDevice = Datastore.getMinMagnitude(aRegId);
	  			
	  			/**
	  			 * only send devices within a specified region
	  			 */
	  			if(isLocationWithinPolygonBounds(ploygon,coordinateStr))
	  			{
	  				/**
	  				 * only messages significant more than minimum magnitude for the device
	  				 */
	  				if(minMagnitudeForDevice <=  minMagnitudeForMessage)
	  				{
	  					logger.warning(" adding device to send the message "  + message.toString());
	  					regIdsForMessage.add(aRegId);
	  				}
	  				else{
	  					logger.warning(" message maginitude not significant to device " );
	  				}
	  			}
	  			else
	  			{
	  				logger.warning(" message location not within polygon " );
	  			}
  			}
  		}
	  return regIdsForMessage;
  }
  
  private void sendMulticastMessage(String multicastKey,
      HttpServletResponse resp) {
    // Recover registration ids from datastore
    List<String> regIds = Datastore.getMulticast(multicastKey);

    List<Message> messages = createMessages();
    
    logger.warning( "sendMulticastMessage : Devices = " + ((regIds!= null) ? regIds.size() : 0 ) 
    				+ " , Messages = " +((messages!= null) ? messages.size() : 0 ) );
           
    MulticastResult multicastResult = null;
    try {
    	if(regIds != null && regIds.size() > 0)
    	{
    		if(messages != null && messages.size() > 0)
    		{
		    	for(Message message : messages)
		    	{
		    		//logger.warning("FIXME: handle errors here");
		    		List<String> regIdsFound = findDevicesToSend(message,regIds);
		    		if(regIdsFound != null && regIdsFound.size() > 0)
		    		{
		    			logger.warning( "calling gcm sender with message : " + message.toString() + " for " + regIdsFound.size() + " device[s] " );
		    			multicastResult = sender.sendNoRetry(message,regIdsFound );
		    		}
		    		else
		    		{
		    			logger.warning( "No valid devices found to send the message : " + message.toString());
		    		}
		    	}
    		}
    		else
    		{
    			logger.warning( "No messages to send");
    		}
    	}
    	else
    	{
    		logger.warning( "No devices to send messages ");
    	}
    } catch (IOException e) {
    	logger.warning( "Exception posting " + messages);
     // logger.log(Level.SEVERE, "Exception posting " + messages, e);
      multicastDone(resp, multicastKey);
      return;
    }
    boolean allDone = true;
    // check if any registration id must be updated
    if (multicastResult != null && multicastResult.getCanonicalIds() != 0) {
      List<Result> results = multicastResult.getResults();
      for (int i = 0; i < results.size(); i++) {
        String canonicalRegId = results.get(i).getCanonicalRegistrationId();
        if (canonicalRegId != null) {
          String regId = regIds.get(i);
          Datastore.updateRegistration(regId, canonicalRegId);
        }
      }
    }
    if (multicastResult != null && multicastResult.getFailure() != 0) {
      // there were failures, check if any could be retried
      List<Result> results = multicastResult.getResults();
      List<String> retriableRegIds = new ArrayList<String>();
      for (int i = 0; i < results.size(); i++) {
        String error = results.get(i).getErrorCodeName();
        if (error != null) {
          String regId = regIds.get(i);
          logger.warning("Got error (" + error + ") for regId " + regId);
          if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
            // application has been removed from device - unregister it
            Datastore.unregister(regId);
          }
          if (error.equals(Constants.ERROR_UNAVAILABLE)) {
            retriableRegIds.add(regId);
          }
        }
      }
      if (!retriableRegIds.isEmpty()) {
        // update task
        Datastore.updateMulticast(multicastKey, retriableRegIds);
        allDone = false;
        retryTask(resp);
      }
    }
    if (allDone) {
      multicastDone(resp, multicastKey);
    } else {
      retryTask(resp);
    }
  }

  private void multicastDone(HttpServletResponse resp, String encodedKey) {
    Datastore.deleteMulticast(encodedKey);
    taskDone(resp);
  }

}
