package de.zilant.icanword;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;

public class DataProvider extends ContentProvider {
	
	@Override
	public int delete(Uri arg0, String arg1, String[] arg2) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean onCreate() {
		data = new Data(getContext());
		
		Resources resources = getContext().getResources();
		uriMatcher.addURI(
				resources.getString(R.string.provider),
				resources.getString(R.string.search_suggest_path) + "/" + SearchManager.SUGGEST_URI_PATH_QUERY,
				SUGGESTIONS_ID);
		uriMatcher.addURI(
				resources.getString(R.string.provider),
				resources.getString(R.string.definitions_path),
				WANTED_DEFINITIONS_ID);
		
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		
		switch(uriMatcher.match(uri)) {
			case SUGGESTIONS_ID:
					return data.suggest(selectionArgs[0]);
			case WANTED_DEFINITIONS_ID:
				return data.select(selectionArgs == null ? "" : selectionArgs[0]);
			default:
				return null;
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	private Data data;
	

	private final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	
	private static final int SUGGESTIONS_ID = 1;
	private static final int WANTED_DEFINITIONS_ID = 2;
	
}
