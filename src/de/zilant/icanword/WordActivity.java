package de.zilant.icanword;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class WordActivity extends MenuActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.general);
		Intent intent = getIntent();
		if(Intent.ACTION_SEARCH.equals(intent.getAction())) {
			query = intent.getStringExtra(SearchManager.QUERY);
			setTitle(query);
			getActionBar().setDisplayHomeAsUpEnabled(true);
			TextView emptyText = (TextView) findViewById(android.R.id.empty);
			emptyText.setText(R.string.no_definitions_found);
			setListAdapter(getAdapter());
			getLoaderManager().initLoader(0, null, this);
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, Main.class)
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		if(!checkboxValues.isEmpty()) {
			List<ContentValues> insertValues = new ArrayList<ContentValues>();
			LinkedList<ContentValues> updateValues = new LinkedList<ContentValues>();
			
			Cursor cursor = getAdapter().getCursor();
			if(cursor.moveToFirst()) {
				do {
					long id = cursor.getLong(ID_INDEX);
					ContentValues values = new ContentValues();
					Boolean isWanted = checkboxValues.get(cursor.getLong(ID_INDEX));
					values.put(String.valueOf(IS_WANTED_INDEX), isWanted == null ? cursor.getInt(IS_WANTED_INDEX) : isWanted ? 1: 0);
					if(id < 0) {
						values.put(String.valueOf(WORD_INDEX), cursor.getString(WORD_INDEX));
						values.put(String.valueOf(PART_OF_SPEECH_INDEX), cursor.getString(PART_OF_SPEECH_INDEX));
						values.put(String.valueOf(DEFINITION_INDEX), cursor.getString(DEFINITION_INDEX));
						insertValues.add(values);
					} else if(checkboxValues.containsKey(id)){
						values.put(String.valueOf(ID_INDEX), id);
						if(checkboxValues.get(id))
							updateValues.addFirst(values);
						else
							updateValues.addLast(values);
					}
				} while(cursor.moveToNext());
				for(ContentValues values : updateValues)
					getContentResolver().update(getUri(), values, null, null);
				if(getContentResolver().bulkInsert(getUri(), insertValues.toArray(new ContentValues[insertValues.size()])) > 0 
						|| updateValues.size() > 0)
					getLoaderManager().restartLoader(0, null, this);
					
			}
			
		}
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		CheckBox checkbox = (CheckBox) v.findViewById(R.id.checkbox_definition);
		checkbox.toggle();
		if(checkboxValues.remove(id) == null)
			checkboxValues.put(id, checkbox.isChecked());
	}
	
	@Override
	protected CursorAdapter getAdapter() {
		if(adapter == null) {
			adapter = new SimpleCursorAdapter(
					this,
					R.layout.definition_layout,
					null,
					new String[] {projection[PART_OF_SPEECH_INDEX], projection[DEFINITION_INDEX], projection[IS_WANTED_INDEX]},
					new int[] {R.id.part_of_speech_text, R.id.definition_text, R.id.checkbox_definition}, 0);
			adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
				@Override
				public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
					if(columnIndex == IS_WANTED_INDEX) {
						CheckBox checkBox = (CheckBox) view;
						Boolean value = checkboxValues.get(cursor.getLong(ID_INDEX));
						if(value == null)
							value = cursor.getInt(columnIndex) != 0;
						checkBox.setChecked(value);
						
						return true;
					}
					else
						return false;
				}
			});
		}
		return adapter;
	}

	@Override
	protected String getSelection() {
		return "word = ?";
	}

	@Override
	protected String[] getSelectionArgs() {
		return new String[] {query};
	}

	@Override
	protected String getSortOrder() {
		return "ORDER BY need_to_memorize DESC, explanation";
	}
	
	private String query;
	private SimpleCursorAdapter adapter;
	private Map<Long, Boolean> checkboxValues = new HashMap<Long, Boolean>();
	
}
