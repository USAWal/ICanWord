package de.zilant.icanword;

import java.util.LinkedList;

import android.app.ExpandableListActivity;
import android.app.SearchManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SearchView;
import android.widget.SimpleCursorTreeAdapter;

public class Main extends ExpandableListActivity implements LoaderCallbacks<Cursor> {
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.definitions_menu, menu);
		
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		SearchView searchView = (SearchView) menu.findItem(R.id.search_menu).getActionView();
		searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
		
		return true;
	}
	
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
				if(childrenCursors == null)
					return null;
				return childrenCursors.get(groupCursor.getPosition());
			}
		});
		final Context context = this;
		getExpandableListView().setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
			
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
				MatrixCursor cursor = (MatrixCursor) getExpandableListAdapter().getGroup(groupPosition);
				String value = cursor.getString(1);
				Intent intent = new Intent(context, WordActivity.class);
				intent.setAction(Intent.ACTION_SEARCH);
				intent.putExtra(SearchManager.QUERY, value);
				startActivity(intent);
				return true;
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
		SimpleCursorTreeAdapter adapter = (SimpleCursorTreeAdapter) getExpandableListAdapter();
		MatrixCursor groupCursor = new MatrixCursor(new String[] {
				data.getColumnName(ID_INDEX),
				data.getColumnName(WORD_INDEX)
		});
		childrenCursors = new LinkedList<MatrixCursor>();
		String word = null;
		if(data.moveToFirst())
		{
			do {
				String dataWord = data.getString(WORD_INDEX);
				if(!dataWord.equals(word)) {
					word = dataWord;
					groupCursor.addRow(new Object[] {groupCursor.getCount(), word});
					childrenCursors.add(new MatrixCursor(new String[] {
							data.getColumnName(ID_INDEX),
							data.getColumnName(PART_OF_SPEECH_INDEX),
							data.getColumnName(DEFINITION_INDEX)
					}));
				}
				childrenCursors.getLast().addRow(new Object[] {
						data.getLong(ID_INDEX),
						data.getString(PART_OF_SPEECH_INDEX),
						data.getString(DEFINITION_INDEX)
				});
			} while(data.moveToNext());
		}
		adapter.setGroupCursor(groupCursor);
		for(int cursorIndex = 0; cursorIndex < childrenCursors.size(); cursorIndex++) {
			adapter.setChildrenCursor(cursorIndex, childrenCursors.get(cursorIndex));
			getExpandableListView().expandGroup(cursorIndex);
		}
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
	protected static final int ID_INDEX = 0;
	protected static final int WORD_INDEX = 1;
	protected static final int PART_OF_SPEECH_INDEX = 2;
	protected static final int DEFINITION_INDEX = 3;
	protected static final int IS_WANTED_INDEX = 4;
	
	private Uri uri;
	LinkedList<MatrixCursor> childrenCursors;

}
