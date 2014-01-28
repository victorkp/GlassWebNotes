package com.victor.kaiser.pendergrast.glass.notes.api;


public class NotesJsonMaker {
	
	public static String makeJson(String notes){
		return "{ \"notes\" : \"" + notes + "\"," +
				"\"lastModified\" : \"" + System.currentTimeMillis() + "\" } ";
	}

}
