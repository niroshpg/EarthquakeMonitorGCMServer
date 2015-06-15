package com.niroshpg.android.gmail;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;



public class MessageUtilityTest {
	
	@Test
	public void testExtractDataFromGmailMessage() 
	{
		
		 try(BufferedReader br = new BufferedReader(new FileReader("resources\\test.email.txt"))) {
		        StringBuilder sb = new StringBuilder();
		        String line = br.readLine();

		        while (line != null) {
		            sb.append(line);
		            sb.append(System.lineSeparator());
		            line = br.readLine();
		        }
		        String everything = sb.toString();
		        
		        String extractedData = MessageUtility.extractData(everything);
		        
		        Gson gson = new Gson();
		        Type type = new TypeToken<Map<String, String>>(){}.getType();
		        Map<String, String> map = gson.fromJson(extractedData, type);
		        assertTrue(map.get("magnitude").contains("5.0"));
		        

		        
		    } catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		
		
	}
	

}
