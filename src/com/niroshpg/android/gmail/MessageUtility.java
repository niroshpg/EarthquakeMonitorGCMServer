package com.niroshpg.android.gmail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;

public class MessageUtility {
	
	public static  String extractData(String raw)
		      throws IOException {
	/*
	 * 	Region:                           NEAR NORTH COAST OF NEW GUINEA, P.N.G.
		Geographic coordinates:            3.245S, 143.844E
		Magnitude:                        5.6
		Depth:                            10 km
		Universal Time (UTC):             13 Jun 2015  07:08:58
		Location with respect to nearby cities:
		41 km (25 mi) NE of Wewak, Papua New Guinea

	 */Map<String,String> extractedData = new HashMap<String, String>();
		  if(raw!= null && !raw.isEmpty())
		  {
		  
		  //Pattern regexCoordinates = Pattern.compile("^\\s*Geographic\\s*coordinates:\\s+(.*)", Pattern.DOTALL);
		  Pattern regexCoordinates = Pattern.compile("^(\\s*)(.*)(Geographic)(.*)(coordinates:)(\\s*)(.*)$", Pattern.MULTILINE);
		  Matcher regexMatcherCoordinates = regexCoordinates.matcher(raw);
		  if (regexMatcherCoordinates.find()) {
			  extractedData.put("coordinates", regexMatcherCoordinates.group(7));
		  } 
		  Pattern regexMagnitude = Pattern.compile("^(\\s*)(.*)(Magnitude:)(\\s*)(.*)$", Pattern.MULTILINE);		  
		  Matcher  regexMatcherMagnitude = regexMagnitude.matcher(raw);
		  if (regexMatcherMagnitude.find()) {
			  extractedData.put("magnitude", regexMatcherMagnitude.group(5));
		  } 
		  Pattern regexDepth = Pattern.compile("^(\\s*)(.*)(Depth:)(\\s*)(.*)$", Pattern.MULTILINE);
		  
		  Matcher regexMatcherDepth = regexDepth.matcher(raw);
		  if (regexMatcherDepth.find()) {
			  extractedData.put("depth", regexMatcherDepth.group(5));
		  } 
		  }
		  Gson gson = new Gson();
		  String json = gson.toJson(extractedData);
		    return json;
		  }

}
