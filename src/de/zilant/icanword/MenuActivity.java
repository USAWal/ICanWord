package de.zilant.icanword;

import android.app.ListActivity;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.widget.CursorAdapter;
import android.widget.SearchView;

abstract public class MenuActivity extends ListActivity implements LoaderCallbacks<Cursor>{
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.definitions_menu, menu);
		
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		SearchView searchView = (SearchView) menu.findItem(R.id.search_menu).getActionView();
		searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		
		return true;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loder, Cursor data) {
		getAdapter().swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		getAdapter().swapCursor(null);
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Resources resources = getResources();
		Uri uri = new Uri.Builder()
			.scheme(ContentResolver.SCHEME_CONTENT)
			.authority(resources.getString(R.string.provider))
			.appendPath(resources.getString(R.string.definitions_path))
			.build();
		return new CursorLoader(this, uri, projection, getSelection(), getSelectionArgs(), getSortOrder());
	}
	
	abstract protected CursorAdapter getAdapter();
	abstract protected String getSelection();
	abstract protected String[] getSelectionArgs();
	abstract protected String getSortOrder();
	
	
	protected static final String[] projection = {
		"id",
		"word",
		"part_of_speech",
		"explanation",
		"need_to_memorize"
	};
	protected static final int PART_OF_SPEECH_INDEX = 2;
	protected static final int DEFINITION_INDEX = 3;
	protected static final int IS_WANTED_INDEX = 4;
}
