package com.victor.kaiser.pendergrast.glass.notes.api;


public class NotesJsonMaker {
	
	public static String makeJson(String notes, String email){
		return "{ \"email\" : \"" + email + "\"," +
			"\"notes\" : \"" + notes + "\"," +
			"\"lastModified\" : \"" + System.currentTimeMillis() + "\" } ";
	}

}
