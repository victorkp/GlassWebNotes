package com.victor.kaiser.pendergrast.glass.notes.api;

import org.json.JSONObject;


public class NotesJsonParser {

	/**
	 * The fields that are expected in the JSON response
	 */
	private static class JSONFields {
		public static final String EMAIL = "email";
		public static final String NOTES = "notes";
		public static final String LAST_MODIFIED = "lastModified";
	}

	private String mEmail;
	private String mNotes;
	private long mLastModified;

	public NotesJsonParser(String json) {

		try {
			JSONObject obj = new JSONObject(json);
			
			mEmail = obj.getString(JSONFields.EMAIL);
			mLastModified = obj.getLong(JSONFields.LAST_MODIFIED);
			
			// If there are no notes, then it won't even be in the JSON
			if(obj.has(JSONFields.NOTES)){
				mNotes = obj.getString(JSONFields.NOTES);
			} else {
				mNotes = "";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String getEmail(){
		return mEmail;
	}

	public String getNotes(){
		return mNotes;
	}

	public long getLastModified(){
		return mLastModified;
	}

}
