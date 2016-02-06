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

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Simple implementation of a data store using standard Java collections.
 * <p>
 * This class is neither persistent (it will lost the data when the app is
 * restarted) nor thread safe.
 */
public final class Datastore {

  public static final int MULTICAST_SIZE = 1000;
  private static final String DEVICE_TYPE = "Device";
  private static final String MESSAGE_TYPE = "Message";
  private static final String USERID_TYPE = "UserId";
  private static final String MESSAGE_CONTENT_PROPERTY = "messageContent";
  private static final String DEVICE_REG_ID_PROPERTY = "regId";
  private static final String DEVICE_MIN_MAG_PROPERTY = "minMagnitude";
  private static final String USERID_CONTENT_PROPERTY = "userIdContent";
  
  private static final String POLYGON_TYPE = "Polygon";
  private static final String COORD_PROPERTY = "PolygonCoordinates";

  private static final String MULTICAST_TYPE = "Multicast";
  private static final String MULTICAST_REG_IDS_PROPERTY = "regIds";

  private static final FetchOptions DEFAULT_FETCH_OPTIONS = FetchOptions.Builder
      .withPrefetchSize(MULTICAST_SIZE).chunkSize(MULTICAST_SIZE);

  private static final Logger logger =
      Logger.getLogger(Datastore.class.getName());
  private static final DatastoreService datastore =
      DatastoreServiceFactory.getDatastoreService();



  private Datastore() {
    throw new UnsupportedOperationException();
  }

  /**
   * Registers a device.
   *
   * @param regId device's registration id.
   * @param minMagnitude 
   */
  public static void register(String regId,JSONObject polygonObj, Double minMagnitude) {
    logger.warning("Registering " + regId);
    Transaction txn = datastore.beginTransaction();
    try {
    	/**
    	 * Save device
    	 */
      Entity deviceToBeRegistered = findDeviceByRegId(regId);
      if (deviceToBeRegistered != null) {
        logger.warning(regId + " is already registered; ignoring.");
        return;
      }
      deviceToBeRegistered = new Entity(DEVICE_TYPE);
      deviceToBeRegistered.setProperty(DEVICE_REG_ID_PROPERTY, regId);
      deviceToBeRegistered.setProperty(DEVICE_MIN_MAG_PROPERTY, minMagnitude);      
      datastore.put(deviceToBeRegistered);
      
      /**
       * save polygon
       */
      Entity polygon = new Entity(POLYGON_TYPE,deviceToBeRegistered.getKey());
      polygon.setProperty(COORD_PROPERTY, new Text(polygonObj.toString()));
      datastore.put(polygon);
      
      txn.commit();
    } finally {
    	logger.warning("Registering " + regId + "[finally]");
      if (txn.isActive()) {
        txn.rollback();
      }
    }
    logger.warning("Registering " + regId + "completed.");
  }
  
  public static void addMessage(String message) {
	    logger.warning("Adding " + message);
	    Transaction txn = datastore.beginTransaction();
	    try {
//	      Entity entity = findDeviceByRegId(regId);
//	      if (entity != null) {
//	        logger.fine(regId + " is already registered; ignoring.");
//	        return;
//	      }
	      Entity entity = new Entity(MESSAGE_TYPE);
	      entity.setProperty(MESSAGE_CONTENT_PROPERTY, message);
	      datastore.put(entity);
	      txn.commit();
	    } finally {
	      if (txn.isActive()) {
	        txn.rollback();
	      }
	    }
	  }
  
   public static void saveUserId(String id) {
	    
	    Transaction txn = datastore.beginTransaction();
	    try {
	      Entity entity = new Entity(USERID_TYPE);
	      entity.setProperty(USERID_CONTENT_PROPERTY, id);
	      datastore.put(entity);
	      txn.commit();
	    } finally {
	      if (txn.isActive()) {
	        txn.rollback();
	      }
	    }
	  }

  /**
   * Unregisters a device.
   *
   * @param regId device's registration id.
   */
  public static void unregister(String regId) {
    logger.info("Unregistering " + regId);
    Transaction txn = datastore.beginTransaction();
    try {
      Entity entity = findDeviceByRegId(regId);
      if (entity == null) {
        logger.warning("Device " + regId + " already unregistered");
      } else {
        Key key = entity.getKey();
        datastore.delete(key);
      }
      txn.commit();
    } finally {
      if (txn.isActive()) {
        txn.rollback();
      }
    }
  }
  
  public static void deleteMessage(String message) {
	    logger.info("deleteting " + message);
	    Transaction txn = datastore.beginTransaction();
	    try {
	      Entity entity = findMessageByContent(message);
	      if (entity == null) {
	        logger.warning("Message not found");
	      } else {
	        Key key = entity.getKey();
	        datastore.delete(key);
	      }
	      txn.commit();
	    } finally {
	      if (txn.isActive()) {
	        txn.rollback();
	      }
	    }
	  }

  private static Entity findMessageByContent(String message) {
	  Query query = new Query(MESSAGE_TYPE)
      .addFilter(MESSAGE_CONTENT_PROPERTY, FilterOperator.EQUAL, message);
  PreparedQuery preparedQuery = datastore.prepare(query);
  List<Entity> entities = preparedQuery.asList(DEFAULT_FETCH_OPTIONS);
  Entity entity = null;
  if (!entities.isEmpty()) {
    entity = entities.get(0);
  }
  int size = entities.size();
  if (size > 0) {
    logger.severe(
        "Found " + size + " entities for message content " + message + ": " + entities);
  }
  return entity;
}

/**
   * Updates the registration id of a device.
   */
  public static void updateRegistration(String oldId, String newId) {
    logger.info("Updating " + oldId + " to " + newId);
    Transaction txn = datastore.beginTransaction();
    try {
      Entity entity = findDeviceByRegId(oldId);
      if (entity == null) {
        logger.warning("No device for registration id " + oldId);
        return;
      }
      entity.setProperty(DEVICE_REG_ID_PROPERTY, newId);
      datastore.put(entity);
      txn.commit();
    } finally {
      if (txn.isActive()) {
        txn.rollback();
      }
    }
  }

  /**
   * Gets all registered devices.
   */
  public static List<String> getDevices() {
    List<String> devices;
    Transaction txn = datastore.beginTransaction();
    try {
      Query query = new Query(DEVICE_TYPE);
      Iterable<Entity> entities =
          datastore.prepare(query).asIterable(DEFAULT_FETCH_OPTIONS);
      devices = new ArrayList<String>();
      for (Entity entity : entities) {
        String device = (String) entity.getProperty(DEVICE_REG_ID_PROPERTY);
        devices.add(device);
      }
      txn.commit();
    } finally {
      if (txn.isActive()) {
        txn.rollback();
      }
    }
    return devices;
  }

  
  public static List<String> getMessages() {
	    List<String> messages;
	    Transaction txn = datastore.beginTransaction();
	    try {
	      Query query = new Query(MESSAGE_TYPE);
	      Iterable<Entity> entities =
	          datastore.prepare(query).asIterable(DEFAULT_FETCH_OPTIONS);
	      messages = new ArrayList<String>();
	      for (Entity entity : entities) {
	        String message = (String) entity.getProperty(MESSAGE_CONTENT_PROPERTY);
	        messages.add(message);
	      }
	      txn.commit();
	    } finally {
	      if (txn.isActive()) {
	        txn.rollback();
	      }
	    }
	    return messages;
	  }

  public static String getUserId() {
	    List<String> ids;
	    Transaction txn = datastore.beginTransaction();
	    try {
	      Query query = new Query(USERID_TYPE);
	      Iterable<Entity> entities =
	          datastore.prepare(query).asIterable(DEFAULT_FETCH_OPTIONS);
	      ids = new ArrayList<String>();
	      for (Entity entity : entities) {
	        String userId = (String) entity.getProperty(USERID_CONTENT_PROPERTY);
	        ids.add(userId);
	      }
	      txn.commit();
	    } finally {
	      if (txn.isActive()) {
	        txn.rollback();
	      }
	    }
	    return ids.get(0);
	  }
  
  /**
   * Gets the number of total devices.
   */
  public static int getTotalDevices() {
    Transaction txn = datastore.beginTransaction();
    try {
      Query query = new Query(DEVICE_TYPE).setKeysOnly();
      List<Entity> allKeys =
          datastore.prepare(query).asList(DEFAULT_FETCH_OPTIONS);
      int total = allKeys.size();
      logger.fine("Total number of devices: " + total);
      txn.commit();
      return total;
    } finally {
      if (txn.isActive()) {
        txn.rollback();
      }
    }
  }

  private static Entity findDeviceByRegId(String regId) {
    Query query = new Query(DEVICE_TYPE)
        .addFilter(DEVICE_REG_ID_PROPERTY, FilterOperator.EQUAL, regId);
    PreparedQuery preparedQuery = datastore.prepare(query);
    List<Entity> entities = preparedQuery.asList(DEFAULT_FETCH_OPTIONS);
    Entity entity = null;
    if (!entities.isEmpty()) {
      entity = entities.get(0);
    }
    int size = entities.size();
    if (size > 0) {
      logger.severe(
          "Found " + size + " entities for regId " + regId + ": " + entities);
    }
    return entity;
  }
  
  private static Entity findPolygonByRegId(String regId) {
	  Query deviceQuery = new Query(DEVICE_TYPE)
	  .setFilter(new Query.FilterPredicate(DEVICE_REG_ID_PROPERTY,
	                                 FilterOperator.EQUAL,
	                                 regId));

	    PreparedQuery pDeviceQuery = datastore.prepare(deviceQuery);
		Entity device = pDeviceQuery.asSingleEntity();
  
	    Query polygonQuery = new Query(POLYGON_TYPE)
	        .setAncestor(device.getKey());
	    PreparedQuery pPolygonQuery = datastore.prepare(polygonQuery);
	    Entity polygon = pPolygonQuery.asSingleEntity();	
	    return polygon;
	  }

  
  public static JSONObject getPolygon(String regId)
  {
	  Entity polygonEntity = null;
	  JSONObject polygonJson = new JSONObject();
	  	  
	  polygonEntity = findPolygonByRegId(regId);
	  
	  if(polygonEntity != null)
	  {
		  try {
			  Text polygonText = (Text) polygonEntity.getProperty(COORD_PROPERTY);
			  polygonJson = new JSONObject(polygonText.getValue());
		} catch (JSONException e) {
			logger.severe( "json exception , could not read polygon from datastore " + regId);
			e.printStackTrace();
		}
	  }
	  else
	  {
		  logger.severe( "could not find polygon for device " + regId);
	  }
	  	  
	  return polygonJson;
	  
  }
  
  /**
   * Creates a persistent record with the devices to be notified using a
   * multicast message.
   *
   * @param devices registration ids of the devices.
   * @return encoded key for the persistent record.
   */
  public static String createMulticast(List<String> devices) {
    logger.info("Storing multicast for " + devices.size() + " devices");
    String encodedKey;
    Transaction txn = datastore.beginTransaction();
    try {
      Entity entity = new Entity(MULTICAST_TYPE);
      entity.setProperty(MULTICAST_REG_IDS_PROPERTY, devices);
      datastore.put(entity);
      Key key = entity.getKey();
      encodedKey = KeyFactory.keyToString(key);
      logger.fine("multicast key: " + encodedKey);
      txn.commit();
    } finally {
      if (txn.isActive()) {
        txn.rollback();
      }
    }
    return encodedKey;
  }

  /**
   * Gets a persistent record with the devices to be notified using a
   * multicast message.
   *
   * @param encodedKey encoded key for the persistent record.
   */
  public static List<String> getMulticast(String encodedKey) {
    Key key = KeyFactory.stringToKey(encodedKey);
    Entity entity;
    Transaction txn = datastore.beginTransaction();
    try {
      entity = datastore.get(key);
      @SuppressWarnings("unchecked")
      List<String> devices =
          (List<String>) entity.getProperty(MULTICAST_REG_IDS_PROPERTY);
      txn.commit();
      return devices;
    } catch (EntityNotFoundException e) {
      logger.severe("No entity for key " + key);
      return Collections.emptyList();
    } finally {
      if (txn.isActive()) {
        txn.rollback();
      }
    }
  }

  /**
   * Updates a persistent record with the devices to be notified using a
   * multicast message.
   *
   * @param encodedKey encoded key for the persistent record.
   * @param devices new list of registration ids of the devices.
   */
  public static void updateMulticast(String encodedKey, List<String> devices) {
    Key key = KeyFactory.stringToKey(encodedKey);
    Entity entity;
    Transaction txn = datastore.beginTransaction();
    try {
      try {
        entity = datastore.get(key);
      } catch (EntityNotFoundException e) {
        logger.severe("No entity for key " + key);
        return;
      }
      entity.setProperty(MULTICAST_REG_IDS_PROPERTY, devices);
      datastore.put(entity);
      txn.commit();
    } finally {
      if (txn.isActive()) {
        txn.rollback();
      }
    }
  }

  /**
   * Deletes a persistent record with the devices to be notified using a
   * multicast message.
   *
   * @param encodedKey encoded key for the persistent record.
   */
  public static void deleteMulticast(String encodedKey) {
    Transaction txn = datastore.beginTransaction();
    try {
      Key key = KeyFactory.stringToKey(encodedKey);
      datastore.delete(key);
      txn.commit();
    } finally {
      if (txn.isActive()) {
        txn.rollback();
      }
    }
  }

public static Double getMinMagnitude(String aRegId) {
	
	Double minMagnitude = RegisterServlet.MIN_MAGNITUDE_DEFAULT;
	  Query deviceQuery = new Query(DEVICE_TYPE)
	  .setFilter(new Query.FilterPredicate(DEVICE_REG_ID_PROPERTY,
	                                 FilterOperator.EQUAL,
	                                 aRegId));

	    PreparedQuery pDeviceQuery = datastore.prepare(deviceQuery);
		Entity device = pDeviceQuery.asSingleEntity();
		
		if(device != null)
		{
			minMagnitude = (Double) device.getProperty(DEVICE_MIN_MAG_PROPERTY);
		}
		
	return minMagnitude;
}

}
