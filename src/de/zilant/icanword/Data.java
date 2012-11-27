package de.zilant.icanword;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.jeremybrooks.knicker.AccountApi;
import net.jeremybrooks.knicker.KnickerException;
import net.jeremybrooks.knicker.WordApi;
import android.app.SearchManager;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class Data extends SQLiteOpenHelper {

	public Data(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		Resources resources = context.getResources();
		System.setProperty(resources.getString(R.string.wordnik_api_key_key),
				resources.getString(R.string.wordnik_api_key_value));
		executor = Executors.newSingleThreadExecutor();
		isOnlineFuture = executor.submit(new IsOnlineCallable());
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(ENABLE_FOREIGN_KEYS);
		db.beginTransaction();
		try
		{
			db.execSQL(CREATE_WORDS_TABLE);
			db.execSQL(CREATE_PARTS_OF_SPEECH_TABLE);
			db.execSQL(CREATE_EXPLANATIONS_TABLE);
			db.execSQL(CREATE_DEFINITIONS_TABLE);
			db.execSQL(CREATE_DEFINITIONS_TABLE_UPDATE_TRIGGER);
			db.execSQL(CREATE_INDEX_FOR_DEFINITIONS_PART_OF_SPEECH_ID);
			db.execSQL(CREATE_INDEX_FOR_DEFINITIONS_EXPLANATION_ID);
			db.execSQL(CREATE_INDEX_FOR_DEFINITIONS_WORD_ID_NEED_TO_MEMORIZE);
			db.execSQL(CREATE_INDEX_FOR_DEFINITIONS_NEED_TO_MEMORIZE);
			db.execSQL(CREATE_INDEX_FOR_EXPLANATIONS_DESCRIPTION);
			db.execSQL(CREATE_INDEX_FOR_WORDS_SPELLING);
			db.execSQL(CREATE_INDEX_FOR_PARTS_OF_SPEECH_SPELLING);
			db.execSQL(CREATE_DEFINITIONS_VIEW);
			db.execSQL(CREATE_DEFINITIONS_VIEW_INSERT_TRIGGER);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}
	
	public Cursor select(String word) {
		try {
			return executor.submit(new SelectDefinitions(word)).get();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Cursor suggest(String query) {
		try {
			return executor.submit(new SelectSuggestions(query)).get();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private boolean isOnline() {
		try {
			return isOnlineFuture.get();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private ExecutorService executor;
	private Future<Boolean> isOnlineFuture;

	private class IsOnlineCallable implements Callable<Boolean> {
		
		@Override
		public Boolean call() throws Exception { return AccountApi.apiTokenStatus().isValid(); }
	
	}
	
	private class SelectDefinitions implements Callable<Cursor> {

		public SelectDefinitions(String word) {
			this.word = word;
		}
		
		@Override
		public Cursor call() throws Exception {
			Cursor selectedDefinitions = null;
			try {
				selectedDefinitions = getWritableDatabase().rawQuery(
						word.isEmpty()
							? SELECT_ALL_DEFINITIONS_FOR_MEMORIZING
							: SELECT_DEFINITIONS_OF_WORD,
						new String[] {word});
				if(selectedDefinitions != null
						&& isOnline() 
						&& selectedDefinitions.getCount() == 0 
						&& !word.isEmpty())
					selectedDefinitions = download(word);
			} catch (Exception e)
			{
				//TODO: if null (went wrong), then notify user
				if(selectedDefinitions == null)
					selectedDefinitions = new MatrixCursor(projection);
			}
			
			return selectedDefinitions;
		}
		
		private Cursor download(String word) throws KnickerException {
			List<net.jeremybrooks.knicker.dto.Definition> definitions =  WordApi.definitions(word);
			MatrixCursor result = new MatrixCursor(projection);
			for(int index = 0; index < definitions.size(); index++) {
				net.jeremybrooks.knicker.dto.Definition definition = definitions.get(index);
				result.addRow(new Object[] {
						index,
						definition.getWord(),
						definition.getPartOfSpeech(),
						definition.getText(),
						0
				});
			}

			return result;
		}
	
		private String word;
		
		private final String[] projection = {
			BaseColumns._ID,
			"word",
			"part_of_speech",
			"explanation",
			"need_to_memorize"
		};
		private static final String SELECT_PREFIX = "SELECT " +
				"id, " +
				"word, " +
				"part_of_speech, " +
				"explanation, " +
				"need_to_memorize " +
				"FROM applicable_definitions WHERE ";
		private static final String SELECT_ALL_DEFINITIONS_FOR_MEMORIZING = SELECT_PREFIX +
				"need_to_memorize = 1 " +
				"ORDER BY word, part_of_speech, explanation;";
		private static final String SELECT_DEFINITIONS_OF_WORD = SELECT_PREFIX +
				"word = ? " +
				"ORDER BY need_to_memorize DESC, explanation;";
		
	}
	
	private class SelectSuggestions implements Callable<Cursor> {

		public SelectSuggestions(String query) {
			this.query = query;
		}
		
		@Override
		public Cursor call() throws Exception {
			MatrixCursor suggestions = new MatrixCursor(
					new String[] {
							BaseColumns._ID,
							SearchManager.SUGGEST_COLUMN_TEXT_1,
							SearchManager.SUGGEST_COLUMN_QUERY});
			if(query.length() > 0 &&isOnline()) {
				try
				{	
					List<String> remoteRawSuggestions = WordApi.lookup(query, false, true).getSuggestions();
					for(int index = -1; index >= -remoteRawSuggestions.size(); index--) {
						Object object = remoteRawSuggestions.get(-(index + 1));
						suggestions.addRow(new Object[] {
								index,
								object,
								object
						});
					}
				}
				catch(KnickerException e)
				{
					e.printStackTrace();
				}				
			}
			Cursor localSuggestions = getWritableDatabase().rawQuery(SELECT_SUGGESTIONS,new String[] {query + "%"});
			int idIndex = localSuggestions.getColumnIndex("id");
			int spellingIndex = localSuggestions.getColumnIndex("spelling");
			for(int index = 0; index < localSuggestions.getCount(); index++) {
				Object object = localSuggestions.getString(spellingIndex);
				suggestions.addRow(new Object[] {
						localSuggestions.getInt(idIndex),
						object,
						object
				});
			}
			return suggestions;
		}
		
		private String query;
		
		private static final String SELECT_SUGGESTIONS = "SELECT " +
				"* " +
				"FROM words " +
				"WHERE spelling LIKE ? " +
				"ORDER BY spelling;";
		
	}

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "definitions";
	private static final String ENABLE_FOREIGN_KEYS = "PRAGMA foreign_keys = ON;";
	
	private static final String CREATE_WORDS_TABLE = "CREATE TABLE IF NOT EXISTS words (" +
			"id INTEGER PRIMARY KEY, " +
			"spelling TEXT NOT NULL UNIQUE" +
			");";
	private static final String CREATE_PARTS_OF_SPEECH_TABLE = "CREATE TABLE IF NOT EXISTS parts_of_speech (" +
			"id INTEGER PRIMARY KEY, " +
			"spelling TEXT NOT NULL UNIQUE" +
			");";
	private static final String CREATE_EXPLANATIONS_TABLE = "CREATE TABLE IF NOT EXISTS explanations (" +
			"id INTEGER PRIMARY KEY, " +
			"description TEXT NOT NULL UNIQUE" +
			");";
	
	private static final String CREATE_DEFINITIONS_TABLE = "CREATE TABLE IF NOT EXISTS definitions (" +
			"id INTEGER PRIMARY KEY, " +
			"need_to_memorize INTEGER NOT NULL DEFAULT 0, " +
			"explanation INTEGER NOT NULL REFERENCES explanations ON UPDATE CASCADE, " +
			"word INTEGER NOT NULL REFERENCES words ON UPDATE CASCADE, " +
			"part_of_speech INTEGER NOT NULL REFERENCES parts_of_speech ON UPDATE CASCADE, " +
			"UNIQUE (explanation, word) ON CONFLICT ABORT);";
	
	private static final String CREATE_DEFINITIONS_TABLE_UPDATE_TRIGGER = 
			"CREATE TRIGGER IF NOT EXISTS definitions_update_trigger " +
			"AFTER UPDATE OF need_to_memorize ON definitions " +
			"FOR EACH ROW WHEN " +
				"new.need_to_memorize == 0 AND NOT EXISTS (" +
					"SELECT * FROM definitions WHERE word = new.word AND need_to_memorize != 0) " +
			"BEGIN " +
				"DELETE FROM definitions WHERE word = new.word; " +
				"DELETE FROM words WHERE id = new.word; " +
				"DELETE FROM parts_of_speech WHERE id NOT IN (SELECT part_of_speech FROM definitions); " +
				"DELETE FROM explanations WHERE id NOT IN (SELECT explanation FROM definitions); " +
			"END;";
	
	private static final String CREATE_DEFINITIONS_VIEW = "CREATE VIEW applicable_definitions AS " +
			"SELECT " +
				"definitions.id AS id, " +
				"words.spelling AS word, " +
				"parts_of_speech.spelling AS part_of_speech, " +
				"explanations.description AS explanation, " +
				"definitions.need_to_memorize AS need_to_memorize " +
				"FROM ((words " +
					"INNER JOIN definitions " +
					"ON words.id = definitions.word) " +
						"INNER JOIN parts_of_speech " +
						"ON parts_of_speech.id = definitions.part_of_speech) " +
							"INNER JOIN explanations " +
							"ON explanations.id = definitions.explanation;";
	
	private static final String CREATE_DEFINITIONS_VIEW_INSERT_TRIGGER = 
			"CREATE TRIGGER IF NOT EXISTS applicable_definitions_insert_trigger " +
			"INSTEAD OF INSERT ON applicable_definitions " +
			"BEGIN " +
				"INSERT OR ROLLBACK INTO definitions (need_to_memorize, explanation, word, part_of_speech) " +
					"SELECT " +
						"new.need_to_memorize AS need_to_memorize, " +
						"explanations.id AS explanation, " +
						"words.id AS word, " +
						"parts_of_speech.id AS part_of_speech " +
							"FROM ((" +
								"SELECT id FROM explanations " +
								"WHERE description = new.explanation) AS explanations " +
								"INNER JOIN (" +
									"SELECT id FROM words " +
									"WHERE spelling = new.word) AS words) " +
									"INNER JOIN (" +
										"SELECT id FROM parts_of_speech " +
										"WHERE spelling = new.part_of_speech) AS parts_of_speech;" +
			"END;";
	
	private static final String CREATE_INDEX_FOR_DEFINITIONS_PART_OF_SPEECH_ID = "CREATE INDEX IF NOT EXISTS definitions_part_of_speech_id_index " +
			"ON definitions (part_of_speech);";
	private static final String CREATE_INDEX_FOR_DEFINITIONS_EXPLANATION_ID = "CREATE INDEX IF NOT EXISTS definitions_explanation_id_index " +
			"ON definitions (explanation);";
	
	private static final String CREATE_INDEX_FOR_DEFINITIONS_WORD_ID_NEED_TO_MEMORIZE = "CREATE INDEX IF NOT EXISTS definitions_word_id_need_to_memorize_index " +
			"ON definitions (word, need_to_memorize);";
	
	private static final String CREATE_INDEX_FOR_DEFINITIONS_NEED_TO_MEMORIZE = "CREATE INDEX IF NOT EXISTS definitions_need_to_memorize " +
			"ON definitions (need_to_memorize);";
	
	private static final String CREATE_INDEX_FOR_EXPLANATIONS_DESCRIPTION = "CREATE INDEX IF NOT EXISTS explanations_description_index " +
			"ON explanations (description);";
	private static final String CREATE_INDEX_FOR_WORDS_SPELLING = "CREATE INDEX IF NOT EXISTS words_spelling_index " +
			"ON words (spelling);";
	private static final String CREATE_INDEX_FOR_PARTS_OF_SPEECH_SPELLING = "CREATE INDEX IF NOT EXISTS parts_of_speech_spelling_index " +
			"ON parts_of_speech (spelling);";
	
}
