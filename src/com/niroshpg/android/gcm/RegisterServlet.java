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

import java.io.BufferedReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;

/**
 * Servlet that registers a device, whose registration id is identified by
 * {@link #PARAMETER_REG_ID}.
 *
 * <p>
 * The client app should call this servlet everytime it receives a
 * {@code com.google.android.c2dm.intent.REGISTRATION C2DM} intent without an
 * error or {@code unregistered} extra.
 */
@SuppressWarnings("serial")
public class RegisterServlet extends BaseServlet {
	  
  private static final String PARAMETER_REG_ID = "regId";
  
  public static final Double MIN_MAGNITUDE_DEFAULT = 5.0;

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException {
	  logger.warning(" start registering doPost");
	  
	  String regId = null;
    //String regId = getParameter(req, PARAMETER_REG_ID);
   // logger.warning(" start registering " + regId);
    
    
    StringBuffer jb = new StringBuffer();
    logger.warning(" start registering : sb created" );
    String line = null;
    try {
      BufferedReader reader = req.getReader();
      logger.warning(" start registering : reader created" );
      while ((line = reader.readLine()) != null)
        jb.append(line);
    } catch (Exception e) { /*report an error*/ }

    logger.warning(" start registering : jb append completed" );
    JSONObject polygonJsonObject = null;
    
    Double minMagnitude = MIN_MAGNITUDE_DEFAULT; // default
    
      try {
    	  String encodeStr = new String(jb.toString().getBytes(), "UTF-8");
    	  //logger.warning(" start registering : encodeStr = " + encodeStr );
    	  String decodeStr = URLDecoder.decode(encodeStr,"UTF-8");
    	 // logger.warning(" start registering : decodeStr = " + decodeStr );
    	  JSONObject decodedJsonObj = new JSONObject(decodeStr);
    	  
		 polygonJsonObject = decodedJsonObj.getJSONObject("polygon");
		 Double minM = decodedJsonObj.getDouble("minMagnitude");
		 if(minM != null)
		 {
			 minMagnitude = minM;
		 }

		 logger.warning(" start registering : from json regId = " + decodedJsonObj.getString("regId")
				 + ", minM = "  + minMagnitude);
		 regId = decodedJsonObj.getString("regId");
	} catch (JSONException e) {
		// TODO Auto-generated catch block
		 logger.warning(" start registering :json error : " + e.getMessage() );
		e.printStackTrace();
	} catch (UnsupportedEncodingException e) {
		 logger.warning(" start registering :encoding error" );
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
  
      if(polygonJsonObject == null)
      {
    	  logger.warning(" start registering : could not read json data");
      }

    // Work with the data using methods like...
    // int someInt = jsonObject.getInt("intParamName");
     // String someString = jsonObject.getString("stringParamName");
    // JSONObject nestedObj = jsonObject.getJSONObject("nestedObjName");
    // JSONArray arr = jsonObject.getJSONArray("arrayParamName");
    // etc...
    if(regId != null && regId.length() >0)
    {
    	Datastore.register(regId, polygonJsonObject,minMagnitude);
    }
    else
    {
    	 logger.warning(" regId not found or invalid");
    }
    setSuccess(resp);
  }

}
