package com.victor.kaiser.pendergrast.glass.notes.content;

import java.util.ArrayList;

import android.content.Context;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.glass.app.Card;
import com.google.android.glass.widget.CardScrollAdapter;
import com.victor.kaiser.pendergrast.glass.notes.R;

public class NoteAdapter extends CardScrollAdapter {
	private static final String TAG = "NoteAdapter";

	private Context mContext;
	private ArrayList<Card> mCards;
	private ArrayList<String> mNotes;

	public NoteAdapter(Context context, String notes) {
		mContext = context;

		buildCards(notes);
	}

	private void buildCards(String notes) {
		String[] noteArray = notes.split("|");
		mNotes = new ArrayList<String>(noteArray.length);

		for (String note : noteArray) {
			mNotes.add(note);

			Card card = new Card(mContext);
			card.setText(note);
			card.setFootnote(mContext.getString(R.string.text_tap_for_options));
		}

	}

	@Override
	public int findIdPosition(Object arg0) {
		return -1;
	}

	@Override
	public int findItemPosition(Object arg0) {
		return mCards.indexOf(arg0);
	}

	@Override
	public int getCount() {
		if (mCards != null) {
			return mCards.size();
		} else {
			return 0;
		}
	}

	@Override
	public Object getItem(int index) {
		return mNotes.get(index);
	}

	@Override
	public View getView(int index, View convertView, ViewGroup viewGroup) {
		return mCards.get(index).toView();
	}

}
