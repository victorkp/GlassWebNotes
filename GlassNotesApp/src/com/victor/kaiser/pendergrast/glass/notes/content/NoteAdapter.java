package com.victor.kaiser.pendergrast.glass.notes.content;

import java.util.ArrayList;

import android.content.Context;

import com.google.android.glass.widget.CardScrollAdapter;

public class NoteAdapter extends CardScrollAdapter {
	private static final String TAG = "NoteAdapter";


	private Context mContext;
	private ArrayList<String> mNotes;


	public NoteAdapter(Context context, String notes){
		mContext = context;
		
		String[] noteArray = notes.split("|");	
		mNotes = new ArrayList<String>(noteArray.length);

		for(String note : noteArray){
			mNotes.add(note);
		}
	}
	



}
