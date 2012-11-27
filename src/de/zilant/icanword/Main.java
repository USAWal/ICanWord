package de.zilant.icanword;

import android.os.Bundle;
import android.widget.CursorAdapter;
import android.widget.SimpleCursorAdapter;

public class Main extends MenuActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.general);
		setListAdapter(getAdapter());
		getLoaderManager().initLoader(0, null, this);
	}
	
	@Override
	protected CursorAdapter getAdapter() {
		if(adapter == null) {
			adapter = new SimpleCursorAdapter(
					this,
					R.layout.wanted_definition_layout,
					null,
					new String[] {projection[3]},
					new int[] {R.id.wanted_word_text}, 0);			
		}
		return adapter;
	}
	
	@Override
	protected String getSelection() {
		return "need_to_memorize = 1";
	}
	
	@Override
	protected String[] getSelectionArgs() {
		return null;
	}
	
	@Override
	protected String getSortOrder() {
		return "word, part_of_speech, explanation";
	}
	
	private SimpleCursorAdapter adapter;

}
