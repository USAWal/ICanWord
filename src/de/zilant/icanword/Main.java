package de.zilant.icanword;

import android.app.ExpandableListActivity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.SimpleCursorAdapter;
import android.widget.SimpleCursorTreeAdapter;

public class Main extends ExpandableListActivity implements LoaderCallbacks<Cursor> {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wanted_definitions_layout);
		setListAdapter(new SimpleCursorTreeAdapter(
				this,
				null,
				R.layout.word_layout,
				new String[] {projection[1]},
				new int[] {R.id.word},
				R.layout.wanted_definition_layout,
				new String[] {projection[3]},
				new int[] {R.id.complex_definition}) {
			
			@Override
			protected Cursor getChildrenCursor(Cursor groupCursor) {
				// TODO Auto-generated method stub
				return null;
			}
		});
		getLoaderManager().initLoader(0, null, this);
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this, getUri(), projection, getSelection(), getSelectionArgs(), getSortOrder());
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loder, Cursor data) {
		((SimpleCursorTreeAdapter) getExpandableListAdapter()).changeCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		((SimpleCursorTreeAdapter) getExpandableListAdapter()).changeCursor(null);
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		getLoaderManager().restartLoader(0, null, this);
	}

	protected String getSelection() {
		return "need_to_memorize = 1";
	}

	protected String[] getSelectionArgs() {
		return null;
	}
	
	protected String getSortOrder() {
		return "word, part_of_speech, explanation";
	}
	
	protected Uri getUri() {
		if(uri == null) {
			Resources resources = getResources();
			uri = new Uri.Builder()
				.scheme(ContentResolver.SCHEME_CONTENT)
				.authority(resources.getString(R.string.provider))
				.appendPath(resources.getString(R.string.definitions_path))
				.build();			
		}
		return uri;
	}
	
	protected static final String[] projection = {
		"id",
		"word",
		"part_of_speech",
		"explanation",
		"need_to_memorize"
	};
	
	private Uri uri;

}
