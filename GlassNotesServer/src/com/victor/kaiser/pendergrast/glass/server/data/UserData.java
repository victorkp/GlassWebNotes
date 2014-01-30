package com.victor.kaiser.pendergrast.glass.server.data;

public class UserData {
	
	public String email;
	public String notes;
	public long lastModified;
	
	public UserData(){
		email = "";
		notes = "";
		lastModified = 0;
	}
	
	public UserData(String email, String notes){
		this.email = email;
		this.notes = notes;
		this.lastModified = 0;
	}
	
	public UserData(String email, String notes, long lastModified){
		this.email = email;
		this.notes = notes;
		this.lastModified = lastModified;
	}

	public void setEmail(String email){
		this.email = email;
	}
	
	public void setNotes(String notes){
		this.notes = notes;
	}
	
	public void setLastModified(long lastModified){
		this.lastModified = lastModified;
	}
	
	public String getEmail(){
		return email;
	}
	
	public String getNotes(){
		return notes;
	}
	
	public long getLastModified(){
		return lastModified;
	}

}
