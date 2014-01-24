package com.victor.kaiser.pendergrast.glass.server.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Text;

public class UserDatabase {

	// Entity Kind of an Article
	private static final String KIND_USER = "kind_user";

	private static final String EMAIL = "email";
	private static final String NOTES = "notes";
	private static final String LAST_MODIFIED = "time_modified";

	// User Cache
	private static final int USER_CACHE_INITIAL_SIZE = 20;
	private static final int USER_CACHE_LOAD_FACTOR = 4;
	private static HashMap<String, UserData> userCache = null;

	// Write Cache
	private static final int WRITE_CACHE_INITIAL_SIZE = 20;
	private static final int WRITE_CACHE_LOAD_FACTOR = 4;
	private static HashMap<String, UserData> writeCache = null;
	

	/**
	 * Opens the database to lookup the user
	 * 
	 * @param email
	 * @return The users as a UserData, or <code>null</code> if no user has
	 *         that email
	 */
	private static UserData getUserFromDatabaseByEmail(String email) {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		Key keyObject = KeyFactory.createKey(KIND_USER, email);
		Query query = new Query(KIND_USER, keyObject).setFilter(new Query.FilterPredicate(EMAIL, FilterOperator.EQUAL, email));

		List<Entity> resultList = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));

		if (resultList.size() > 0) {
			return entityToUser(resultList.get(0));
		}

		return null;
	}

	/**
	 * Get a user from either the HashMap cache if available, or performs a
	 * database lookup if the user is not currently in the HashMap cache
	 * 
	 * @param key
	 * @return The user as an UserData, or <code>null</code> if no user has
	 *         that email
	 */
	public static UserData getUserByEmail(String email) {
		UserData user = userCache.get(email);
		if (user != null) {
			return user;
		}

		// The user wasn't in the read cache, so he/she has to be
		// looked up in the database
		user = getUserFromDatabaseByEmail(email);

		// If the user exists,
		// add this user to the cache for faster lookup in the future
		if (user != null) {
			addToUserCache(email, user);
			return user;
		}

		return null;
	}

	/**
	 * Determine if the database has the user by the article's email
	 * 
	 * @param key
	 * @return <code>true</code> if the database has the article
	 */
	public static boolean hasUser(String email) {
		if (email == null) {
			return false;
		}

		Key keyObject = KeyFactory.createKey(KIND_USER, email);
		Query query = new Query(KIND_USER, keyObject);

		return DatastoreServiceFactory.getDatastoreService().prepare(query).asList(FetchOptions.Builder.withLimit(1)).size() > 0;
	}

	public static void putUser(UserData user) {
		if (user == null) {
			return;
		}

		addToWriteCache(user.getEmail(), user);
	}

	private static void putUserIntoDatastore(UserData user) {
		if (user == null) {
			return;
		}

		Entity entity = userToEntity(user);
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		datastore.put(entity);
	}

	public static Entity userToEntity(UserData user) {
		if (user == null) {
			return null;
		}

		Key keyObject = KeyFactory.createKey(KIND_USER, user.getEmail());

		Entity entity = new Entity(keyObject);
		entity.setProperty(EMAIL, user.getEmail());

		Text notes = new Text(user.getNotes());
		entity.setProperty(NOTES, notes);

		entity.setProperty(LAST_MODIFIED, user.getLastModified());

		return entity;
	}

	public static UserData entityToUser(Entity entity) {
		if (entity == null) {
			return new UserData();
		}

		UserData user = new UserData();
		user.setEmail((String) entity.getProperty(EMAIL));

		Text notesText = (Text) entity.getProperty(NOTES);
		user.setNotes(notesText.getValue());

		user.setLastModified((Long) entity.getProperty(LAST_MODIFIED));

		return user;
	}

	private static UserData getUserFromCache(String email) {
		return (userCache != null) ? (userCache.get(email)) : (null);
	}

	private static void addToUserCache(String email, UserData user) {
		if (userCache == null) {
			userCache = new HashMap<String, UserData>(USER_CACHE_INITIAL_SIZE, USER_CACHE_LOAD_FACTOR);
		}

		userCache.put(email, user);
	}

	/**
	 * Puts the user into the write cache and the user cache
	 * 
	 * @param email
	 * @param user
	 */
	private static void addToWriteCache(String email, UserData user) {
		if (writeCache == null) {
			writeCache = new HashMap<String, UserData>(USER_CACHE_INITIAL_SIZE, USER_CACHE_LOAD_FACTOR);
		}

		writeCache.put(email, user);

		// Update the corresponding user in the user cache
		// so that the information in the user cache is up to date
		addToUserCache(email, user);
	}

	/**
	 * Enters all of the data in the writeCache into the datastore
	 */
	public static void writeCacheToDatastore() {
		if(writeCache == null){
			//Nothing to do
			return;
		}
		
		Set<String> keys = writeCache.keySet();

		for (String userKey : keys) {
			putUser(writeCache.get(userKey));
		}
	}

	/**
	 * Enters all of the data in the writeCache into the datastore and then
	 * clears the write cache
	 */
	public static void writeAndEmptyWriteCache() {
		if(writeCache == null){
			//Nothing to do 
			return;
		}
		
		writeCacheToDatastore();
		writeCache.clear();
	}

}
