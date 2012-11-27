package de.zilant.icanword;

import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
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
						checkBox.setChecked(cursor.getInt(columnIndex) != 0);
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
	
}
