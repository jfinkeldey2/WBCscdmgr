package org.boardgamers.wbcscdmgr;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kevin on 4/27/2015. Database used for storing all data
 */
class WBCDataDbHelper extends SQLiteOpenHelper {
	private final String TAG = "WBCDataDbHelper";

	static final int DATABASE_VERSION = 19;

	private static final String DATABASE_NAME = "WBCdata.db";

	static abstract class EventEntry implements BaseColumns {
		static final String TABLE_NAME = "events";
		//		static final String COLUMN_EVENT_ID = "e_id";
		static final String COLUMN_TOURNAMENT_ID = "tournament_id";
		static final String COLUMN_TITLE = "title";
		static final String COLUMN_DAY = "day";
		static final String COLUMN_HOUR = "hour";
		static final String COLUMN_CLASS = "eClass";
		static final String COLUMN_FORMAT = "eFormat";
		static final String COLUMN_QUALIFY = "qualify";
		static final String COLUMN_DURATION = "duration";
		static final String COLUMN_CONTINUOUS = "continuous";
		static final String COLUMN_TOTAL_DURATION = "total_duration";
		static final String COLUMN_LOCATION = "location";
		static final String COLUMN_NULLABLE = "null";
	}

	static abstract class TournamentEntry implements BaseColumns {
		static final String TABLE_NAME = "tournaments";
		//		static final String COLUMN_TOURNAMENT_ID = "t_id";
		static final String COLUMN_TITLE = "title";
		static final String COLUMN_LABEL = "label";
		static final String COLUMN_IS_TOURNAMENT = "is_tournament";
		static final String COLUMN_PRIZE = "prize";
		static final String COLUMN_GM = "gm";
		//		static final String COLUMN_FINAL_EVENT = "final_event";
		static final String COLUMN_VISIBLE = "visible";
		static final String COLUMN_NULLABLE = "null";
	}

	static abstract class UserEntry implements BaseColumns {
		static final String TABLE_NAME = "user";
		//		static final String COLUMN_USER_ID = "u_id";
		static final String COLUMN_NAME = "name";
		static final String COLUMN_EMAIL = "email";
		static final String COLUMN_NULLABLE = "null";
	}

	static abstract class UserEventDataEntry implements BaseColumns {
		static final String TABLE_NAME = "user_event_data";
		static final String COLUMN_USER_ID = "user_id";
		static final String COLUMN_EVENT_ID = "event_id";
		static final String COLUMN_STARRED = "starred";
		static final String COLUMN_NOTE = "note";
		static final String COLUMN_NULLABLE = "null";
	}

	static abstract class UserTournamentDataEntry implements BaseColumns {
		static final String TABLE_NAME = "user_tournament_data";
		static final String COLUMN_USER_ID = "user_id";
		static final String COLUMN_TOURNAMENT_ID = "tournament_id";
		static final String COLUMN_FINISH = "finish";
		static final String COLUMN_NULLABLE = "null";
	}

	static abstract class UserCreatedEventEntry implements BaseColumns {
		static final String TABLE_NAME = "user_event";
		static final String COLUMN_USER_ID = "user_id";
	}

	private static final String SQL_CREATE_EVENT_ENTRIES = "CREATE TABLE " + EventEntry.TABLE_NAME + " (" +
			EventEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
//			EventEntry.COLUMN_EVENT_ID + " INTEGER UNIQUE," +
			EventEntry.COLUMN_TOURNAMENT_ID + " INTEGER NOT NULL," +
			EventEntry.COLUMN_TITLE + " TEXT NOT NULL," +
			EventEntry.COLUMN_DAY + " INTEGER NOT NULL," +
			EventEntry.COLUMN_HOUR + " REAL NOT NULL," +
			EventEntry.COLUMN_CLASS + " TEXT NOT NULL," +
			EventEntry.COLUMN_FORMAT + " TEXT NOT NULL," +
			EventEntry.COLUMN_QUALIFY + " INTEGER NOT NULL," +
			EventEntry.COLUMN_DURATION + " REAL NOT NULL," +
			EventEntry.COLUMN_CONTINUOUS + " INTEGER NOT NULL," +
			EventEntry.COLUMN_TOTAL_DURATION + " REAL NOT NULL," +
			EventEntry.COLUMN_LOCATION + " TEXT NOT NULL," +
			"FOREIGN KEY(" + EventEntry.COLUMN_TOURNAMENT_ID + ") REFERENCES "
			+ TournamentEntry.TABLE_NAME + "(" + TournamentEntry._ID + "));";

	private static final String SQL_CREATE_TOURNAMENT_ENTRIES =
			"CREATE TABLE " + TournamentEntry.TABLE_NAME + " (" +
					TournamentEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
					TournamentEntry.COLUMN_TITLE + " TEXT UNIQUE NOT NULL," +
					TournamentEntry.COLUMN_LABEL + " TEXT NOT NULL," +
					TournamentEntry.COLUMN_IS_TOURNAMENT + " INTEGER NOT NULL," +
					TournamentEntry.COLUMN_PRIZE + " INTEGER NOT NULL," +
					TournamentEntry.COLUMN_GM + " TEXT NOT NULL," +
					TournamentEntry.COLUMN_VISIBLE + " INTEGER NOT NULL)";

	private static final String SQL_CREATE_USER_ENTRIES = "CREATE TABLE " + UserEntry.TABLE_NAME + " (" +
			UserEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
			UserEntry.COLUMN_NAME + " TEXT NOT NULL," +
			UserEntry.COLUMN_EMAIL + " TEXT NOT NULL)";

	private static final String SQL_CREATE_USER_TOURNAMENT_DATA_ENTRIES =
			"CREATE TABLE " + UserTournamentDataEntry.TABLE_NAME + " (" +
					UserTournamentDataEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
					UserTournamentDataEntry.COLUMN_USER_ID + " INTEGER," +
					UserTournamentDataEntry.COLUMN_TOURNAMENT_ID + " INTEGER," +
					UserTournamentDataEntry.COLUMN_FINISH + " INTEGER," +
					"UNIQUE(" + UserTournamentDataEntry.COLUMN_USER_ID + "," +
					UserTournamentDataEntry.COLUMN_TOURNAMENT_ID + ") ON CONFLICT REPLACE)";

	private static final String SQL_CREATE_USER_EVENT_DATA_ENTRIES =
			"CREATE TABLE " + UserEventDataEntry.TABLE_NAME + " (" +
					UserEventDataEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
					UserEventDataEntry.COLUMN_USER_ID + " INTEGER," +
					UserEventDataEntry.COLUMN_EVENT_ID + " INTEGER," +
					UserEventDataEntry.COLUMN_STARRED + " INTEGER," +
					UserEventDataEntry.COLUMN_NOTE + " TEXT, " +
					"UNIQUE(" + UserEventDataEntry.COLUMN_USER_ID + "," +
					UserEventDataEntry.COLUMN_EVENT_ID + ") ON CONFLICT REPLACE)";
//
//	private static final String SQL_CREATE_USER_EVENT_ENTRIES =
//			"CREATE TABLE " + UserCreatedEventEntry.TABLE_NAME + " (" +
//					UserCreatedEventEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
//					UserCreatedEventEntry.COLUMN_USER_ID + " INTEGER," +
//					UserCreatedEventEntry.COLUMN_EVENT_ID + " INTEGER," +
//					UserCreatedEventEntry.COLUMN_TITLE + " TEXT," +
//					UserCreatedEventEntry.COLUMN_DAY + " INTEGER," +
//					UserCreatedEventEntry.COLUMN_HOUR + " INTEGER," +
//					UserCreatedEventEntry.COLUMN_DURATION + " REAL," +
//					UserCreatedEventEntry.COLUMN_LOCATION + " TEXT)";

	private static final String SQL_DELETE_EVENT_ENTRIES =
			"DROP TABLE IF EXISTS " + EventEntry.TABLE_NAME;

	private static final String SQL_DELETE_TOURNAMENT_ENTRIES =
			"DROP TABLE IF EXISTS " + TournamentEntry.TABLE_NAME;

	private static final String SQL_DELETE_USER_ENTRIES =
			"DROP TABLE IF EXISTS " + UserEntry.TABLE_NAME;

	private static final String SQL_DELETE_USER_EVENT_DATA_ENTRIES =
			"DROP TABLE IF EXISTS " + UserEventDataEntry.TABLE_NAME;

	private static final String SQL_DELETE_USER_TOURNAMENT_DATA_ENTRIES =
			"DROP TABLE IF EXISTS " + UserTournamentDataEntry.TABLE_NAME;

	private static final String SQL_DELETE_USER_EVENT_ENTRIES =
			"DROP TABLE IF EXISTS " + UserCreatedEventEntry.TABLE_NAME;

	private SQLiteDatabase sqLiteDatabase;

	WBCDataDbHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public void onCreate(SQLiteDatabase db) {
		db.execSQL(SQL_CREATE_EVENT_ENTRIES);
		db.execSQL(SQL_CREATE_TOURNAMENT_ENTRIES);
		db.execSQL(SQL_CREATE_USER_ENTRIES);
		db.execSQL(SQL_CREATE_USER_EVENT_DATA_ENTRIES);
		db.execSQL(SQL_CREATE_USER_TOURNAMENT_DATA_ENTRIES);

		String SQL_CREATE_USER_EVENT_ENTRIES = SQL_CREATE_EVENT_ENTRIES.replace(
				EventEntry.TABLE_NAME, UserCreatedEventEntry.TABLE_NAME);
		SQL_CREATE_USER_EVENT_ENTRIES = SQL_CREATE_USER_EVENT_ENTRIES.replace(
				UserCreatedEventEntry.TABLE_NAME + " (",
				UserCreatedEventEntry.TABLE_NAME + " (" + UserCreatedEventEntry.COLUMN_USER_ID + " INTEGER, ");

		Log.d(TAG, "User created event table code: " + SQL_CREATE_USER_EVENT_ENTRIES);

		db.execSQL(SQL_CREATE_USER_EVENT_ENTRIES);
	}

	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < newVersion) {
			db.execSQL(SQL_DELETE_EVENT_ENTRIES);
			db.execSQL(SQL_DELETE_TOURNAMENT_ENTRIES);
			db.execSQL(SQL_DELETE_USER_ENTRIES);
			db.execSQL(SQL_DELETE_USER_EVENT_DATA_ENTRIES);
			db.execSQL(SQL_DELETE_USER_TOURNAMENT_DATA_ENTRIES);
			db.execSQL(SQL_DELETE_USER_EVENT_ENTRIES);

			onCreate(db);
		}
	}

	int getVersion() {
		sqLiteDatabase = super.getReadableDatabase();
		int version = sqLiteDatabase.getVersion();
		sqLiteDatabase.close();
		return version;
	}

	@Override
	public SQLiteDatabase getWritableDatabase() {
		sqLiteDatabase = super.getWritableDatabase();
		return sqLiteDatabase;
	}

	@Override
	public SQLiteDatabase getReadableDatabase() {
		sqLiteDatabase = super.getReadableDatabase();
		return sqLiteDatabase;
	}

	@Override
	public synchronized void close() {
		sqLiteDatabase.close();
		super.close();
	}

	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onUpgrade(db, oldVersion, newVersion);
	}

	long insertEvent(long tId, int day, float hour, String title, String eClass,
					 String eFormat, boolean qualify, float duration, boolean continuous,
					 float totalDuration, String location) {
		ContentValues values = new ContentValues();
//		values.put(EventEntry.COLUMN_EVENT_ID, eId);
		values.put(EventEntry.COLUMN_TOURNAMENT_ID, tId);
		values.put(EventEntry.COLUMN_TITLE, title);
		values.put(EventEntry.COLUMN_DAY, day);
		values.put(EventEntry.COLUMN_HOUR, hour);
		values.put(EventEntry.COLUMN_CLASS, eClass);
		values.put(EventEntry.COLUMN_FORMAT, eFormat);
		values.put(EventEntry.COLUMN_QUALIFY, qualify ? 1 : 0);
		values.put(EventEntry.COLUMN_DURATION, duration);
		values.put(EventEntry.COLUMN_CONTINUOUS, continuous ? 1 : 0);
		values.put(EventEntry.COLUMN_TOTAL_DURATION, totalDuration);
		values.put(EventEntry.COLUMN_LOCATION, location);

		// Insert the new row, returning the primary key value of the new row
		return sqLiteDatabase.insert(EventEntry.TABLE_NAME, EventEntry.COLUMN_NULLABLE, values);
	}

	long updateEvent(String tournamentTitle, String eventTitle, int day, float hour, String location) {
		ContentValues values = new ContentValues();
		if (day > -1)
			values.put(EventEntry.COLUMN_DAY, day);
		if (hour > -1)
			values.put(EventEntry.COLUMN_HOUR, hour);
		if (location != null)
			values.put(EventEntry.COLUMN_LOCATION, location);


		String whereClause;
		if (tournamentTitle != null) {
			long tournamentId = getTournamentID(tournamentTitle);
			whereClause = EventEntry.COLUMN_TOURNAMENT_ID + "=" + String.valueOf(tournamentId)
					+ " and " + EventEntry.COLUMN_TITLE + "='" + eventTitle + "'";
		} else {
			whereClause = EventEntry.COLUMN_TITLE + "='" + eventTitle + "'";
		}

		Log.d(TAG, whereClause);
		// Insert the new row, returning the primary key value of the new row
		return sqLiteDatabase.update(EventEntry.TABLE_NAME, values, whereClause, null);
	}

	long insertTournament(String title, String label, boolean isTournament, int prize, String gm) {
		ContentValues values = new ContentValues();
		values.put(TournamentEntry.COLUMN_TITLE, title);
		values.put(TournamentEntry.COLUMN_LABEL, label);
		values.put(TournamentEntry.COLUMN_IS_TOURNAMENT, isTournament ? 1 : 0);
		values.put(TournamentEntry.COLUMN_PRIZE, prize);
		values.put(TournamentEntry.COLUMN_GM, gm);
		values.put(TournamentEntry.COLUMN_VISIBLE, 1);

		// Insert the new row, returning the primary key value of the new row
		return sqLiteDatabase
				.insert(TournamentEntry.TABLE_NAME, TournamentEntry.COLUMN_NULLABLE, values);
	}

	int getNumEvents() {
		Cursor cursor = sqLiteDatabase.rawQuery("SELECT COUNT (*) FROM " + EventEntry.TABLE_NAME, null);
		cursor.moveToFirst();
		int count = cursor.getInt(0);
		cursor.close();

		return count;
	}

	List<Event> getUserEvents(long userId) {
		return getEvents(userId, true, false, "", null);
	}

	List<Event> getAllEvents(long userId) {
		return getEvents(userId, true, true, "", null);
	}

	List<Event> getStarredEvents(long userId) {
		return getEvents(userId, true, true,
				UserEventDataEntry.TABLE_NAME + "." + UserEventDataEntry.COLUMN_STARRED + "=1", null);
	}

	List<Event> getEventsWithNotes(long userId) {
		return getEvents(userId, true, true,
				"ifnull(" + UserEventDataEntry.TABLE_NAME + "." + UserEventDataEntry.COLUMN_NOTE + ", '') !='' ", null);
	}

	List<Event> getEventsFromSearchString(long userId, String searchString) {
		return getEvents(userId, true, true,
				"(" + EventEntry.TABLE_NAME + "." + EventEntry.COLUMN_TITLE + " LIKE '%" + searchString + "%' OR " +
						EventEntry.TABLE_NAME + "." + EventEntry.COLUMN_FORMAT + " LIKE '%" + searchString + "%')", null);
	}

	List<Event> getTournamentEvents(long userId, long tournamentId) {
		String where = EventEntry.TABLE_NAME + "." + EventEntry.COLUMN_TOURNAMENT_ID + "=" +
				String.valueOf(tournamentId);
		return getEvents(userId, true, true, where, null);
	}

	Event getFinalEvent(long userId, long tournamentId) {
		String where = EventEntry.TABLE_NAME + "." + EventEntry.COLUMN_TOURNAMENT_ID + "=" + String.valueOf(tournamentId);
		String orderBy = EventEntry.TABLE_NAME + "." + EventEntry.COLUMN_DAY + " DESC, "
				+ EventEntry.TABLE_NAME + "." + EventEntry.COLUMN_HOUR + " DESC";
		List<Event> events = getEvents(userId, true, true, where, orderBy);

		if (events != null && events.size() > 0) {
			//return events.get(events.size() - 1);
			//updated above to return first item (final event) not last
			return events.get(0);
		} else {
			return null;
		}
	}

	Event getEvent(long userId, long eventId) {
		String where = EventEntry.TABLE_NAME + "." + EventEntry._ID + "=" + String.valueOf(eventId);
		List<Event> events = getEvents(userId, true, true, where, null);

		if (events != null && events.size() > 0) {
			return events.get(0);
		} else {
			return null;
		}
	}

	private List<Event> getEvents(long uId, boolean includeUserEvents, boolean includeTournaments, String whereClause,
								  String sortOrder) {
		if (!whereClause.equalsIgnoreCase("")) {
			whereClause += " AND ";
		}

		whereClause += " ifnull(" + UserEventDataEntry.TABLE_NAME + "." + UserEventDataEntry.COLUMN_USER_ID
				+ ", " + String.valueOf(uId) + ") = " + String.valueOf(uId) + " ";

		if (sortOrder == null) {
			sortOrder = EventEntry.COLUMN_DAY + " ASC, " + EventEntry.COLUMN_HOUR + " ASC, " +
					EventEntry.COLUMN_QUALIFY + " ASC, " + EventEntry.COLUMN_TITLE + " ASC";
		}

		String columnsQuery = "SELECT "
				+ EventEntry.TABLE_NAME + "." + EventEntry._ID + " as " + EventEntry._ID + ", "
				+ EventEntry.COLUMN_TOURNAMENT_ID + ", "
				+ EventEntry.COLUMN_DAY + ", "
				+ EventEntry.COLUMN_HOUR + ", "
				+ EventEntry.COLUMN_TITLE + ", "
				+ EventEntry.COLUMN_CLASS + ", "
				+ EventEntry.COLUMN_FORMAT + ", "
				+ EventEntry.COLUMN_QUALIFY + ", "
				+ EventEntry.COLUMN_DURATION + ", "
				+ EventEntry.COLUMN_CONTINUOUS + ", "
				+ EventEntry.COLUMN_TOTAL_DURATION + ", "
				+ EventEntry.COLUMN_LOCATION + " FROM ";


		String query;
		if (includeTournaments && includeUserEvents) {
			query = columnsQuery + " (" + columnsQuery + EventEntry.TABLE_NAME +
					" UNION ALL " + columnsQuery + UserCreatedEventEntry.TABLE_NAME
					+ " AS " + EventEntry.TABLE_NAME + " WHERE " + EventEntry.TABLE_NAME + "."
					+ UserCreatedEventEntry.COLUMN_USER_ID + "=" + String.valueOf(uId) + " ) as "
					+ EventEntry.TABLE_NAME;
		} else if (includeTournaments) {
			query = columnsQuery + EventEntry.TABLE_NAME;
		} else if (includeUserEvents) {
			query = columnsQuery + " ( " + columnsQuery + UserCreatedEventEntry.TABLE_NAME + " AS " + EventEntry.TABLE_NAME
					+ " WHERE " + EventEntry.TABLE_NAME + "."
					+ UserCreatedEventEntry.COLUMN_USER_ID + "=" + String.valueOf(uId) + " ) as "
					+ EventEntry.TABLE_NAME;
		} else {
			return null;
		}


		query = "SELECT "
				+ UserEventDataEntry.COLUMN_STARRED + ", "
				+ UserEventDataEntry.COLUMN_NOTE + ", " + query.substring(6)
				+ " LEFT JOIN " + UserEventDataEntry.TABLE_NAME
				+ " ON " + EventEntry.TABLE_NAME + "." + EventEntry._ID
				+ "=" + UserEventDataEntry.TABLE_NAME + "." + UserEventDataEntry.COLUMN_EVENT_ID
				+ " WHERE " + whereClause
				+ " ORDER BY " + sortOrder;

		Log.d(TAG, "Full event query: " + query);

		String title, eClass, format, location, note;
		int id, tournamentId, day;
		float hour, duration, totalDuration;
		boolean qualify, continuous, starred;

		Event event;
		List<Event> events = new ArrayList<>();

		Log.d(TAG, "getEvents query " + query);

		Cursor cursor = sqLiteDatabase.rawQuery(query, null);
		cursor.moveToFirst();
		cursor.moveToPrevious();
		while (cursor.moveToNext()) {
			id = cursor.getInt(cursor.getColumnIndexOrThrow(EventEntry._ID));
			tournamentId = cursor.getInt(cursor.getColumnIndexOrThrow(EventEntry.COLUMN_TOURNAMENT_ID));
			day = cursor.getInt(cursor.getColumnIndexOrThrow(EventEntry.COLUMN_DAY));
			hour = cursor.getFloat(cursor.getColumnIndexOrThrow(EventEntry.COLUMN_HOUR));
			title = cursor.getString(cursor.getColumnIndexOrThrow(EventEntry.COLUMN_TITLE));
			eClass = cursor.getString(cursor.getColumnIndexOrThrow(EventEntry.COLUMN_CLASS));
			format = cursor.getString(cursor.getColumnIndexOrThrow(EventEntry.COLUMN_FORMAT));
			qualify = cursor.getInt(cursor.getColumnIndexOrThrow(EventEntry.COLUMN_QUALIFY)) == 1;
			duration = cursor.getFloat(cursor.getColumnIndexOrThrow(EventEntry.COLUMN_DURATION));
			continuous = cursor.getInt(cursor.getColumnIndexOrThrow(EventEntry.COLUMN_CONTINUOUS)) == 1;
			totalDuration =
					cursor.getFloat(cursor.getColumnIndexOrThrow(EventEntry.COLUMN_TOTAL_DURATION));
			location = cursor.getString(cursor.getColumnIndexOrThrow(EventEntry.COLUMN_LOCATION));

			starred = cursor.getInt(cursor.getColumnIndexOrThrow(UserEventDataEntry.COLUMN_STARRED)) == 1;
			note = cursor.getString(cursor.getColumnIndexOrThrow(UserEventDataEntry.COLUMN_NOTE));

			event = new Event(id, tournamentId, day, hour, title, eClass, format, qualify, duration,
					continuous, totalDuration, location);

			event.starred = starred;
			event.note = note;

			events.add(event);

		}

		cursor.close();

		return events;
	}

	long getTournamentID(String title) {
		String where = TournamentEntry.TABLE_NAME + "." + TournamentEntry.COLUMN_TITLE + "='" + title.replace("'", "''") + "'";
		String query = "SELECT * FROM " + TournamentEntry.TABLE_NAME + " WHERE " + where;

		Cursor cursor = sqLiteDatabase.rawQuery(query, null);
		long returnValue;
		if (cursor.moveToFirst()) {
			returnValue = cursor.getLong(cursor.getColumnIndex(TournamentEntry._ID));
		} else {
			returnValue = -1;
		}
		cursor.close();
		return returnValue;
	}

	Cursor getSearchCursor(String query) {
		String sortOrder = TournamentEntry.COLUMN_TITLE + " ASC";
		String selection = TournamentEntry.COLUMN_TITLE + " LIKE '%" + query + "%'";
		return sqLiteDatabase
				.query(TournamentEntry.TABLE_NAME, null, selection, null, null, null, sortOrder);
	}

	Tournament getTournament(long userId, long tournamentId) {
		String where = TournamentEntry.TABLE_NAME + "." + TournamentEntry._ID + "=" +
				String.valueOf(tournamentId);
		List<Tournament> tournaments = getTournaments(userId, where);

		if (tournaments != null && tournaments.size() > 0) {
			return tournaments.get(0);
		} else {
			return null;
		}
	}

	List<Tournament> getTournamentsWithFinishes(long userId) {
		String where = UserTournamentDataEntry.TABLE_NAME + "." + UserTournamentDataEntry.COLUMN_FINISH +
				"!=0 AND " + UserTournamentDataEntry.TABLE_NAME + "." +
				UserTournamentDataEntry.COLUMN_FINISH + " IS NOT NULL";
		return getTournaments(userId, where);
	}

	List<Tournament> getAllTournaments(long userId) {
		String where = "";
		return getTournaments(userId, where);
	}

	private List<Tournament> getTournaments(long uId, String where) {
		String sortOrder = TournamentEntry.COLUMN_TITLE + " ASC";

		if (!where.equalsIgnoreCase("")) {
			where += " AND ";
		}
		where += "(" + UserTournamentDataEntry.TABLE_NAME + "." + UserTournamentDataEntry.COLUMN_USER_ID + "=" +
				String.valueOf(uId) + " OR " + UserTournamentDataEntry.TABLE_NAME + "." +
				UserTournamentDataEntry.COLUMN_USER_ID + " IS NULL)";

		String query = "SELECT * FROM " + TournamentEntry.TABLE_NAME + " LEFT JOIN " +
				UserTournamentDataEntry.TABLE_NAME + " ON " + TournamentEntry.TABLE_NAME + "." +
				TournamentEntry._ID + "=" + UserTournamentDataEntry.TABLE_NAME + "." +
				UserTournamentDataEntry.COLUMN_TOURNAMENT_ID + " WHERE " + where + " ORDER BY " + sortOrder;

		Log.d(TAG, "Full tournament query: " + query);

		Cursor cursor = sqLiteDatabase.rawQuery(query, null);

		String title, label, gm;
		long id;
		int prize, finish;
		boolean isTournament, visible;

		List<Tournament> tournaments = new ArrayList<>();
		Tournament tournament;
		if (cursor.moveToFirst()) {
			do {
				id = cursor.getLong(cursor.getColumnIndexOrThrow(TournamentEntry._ID));
				title = cursor.getString(cursor.getColumnIndexOrThrow(TournamentEntry.COLUMN_TITLE));
				label = cursor.getString(cursor.getColumnIndexOrThrow(TournamentEntry.COLUMN_LABEL));
				isTournament =
						cursor.getInt(cursor.getColumnIndexOrThrow(TournamentEntry.COLUMN_IS_TOURNAMENT)) == 1;
				prize = cursor.getInt(cursor.getColumnIndexOrThrow(TournamentEntry.COLUMN_PRIZE));
				gm = cursor.getString(cursor.getColumnIndexOrThrow(TournamentEntry.COLUMN_GM));
//				finalEventId = cursor.getInt(cursor.getColumnIndexOrThrow(TournamentEntry.COLUMN_FINAL_EVENT));
				visible = cursor.getInt(cursor.getColumnIndexOrThrow(TournamentEntry.COLUMN_VISIBLE)) == 1;
				finish = cursor.getInt(cursor.getColumnIndexOrThrow(UserTournamentDataEntry.COLUMN_FINISH));

				Log.d(TAG, "Id is " + String.valueOf(id));

				tournament = new Tournament(id, title, label, isTournament, prize, gm);
				tournament.finish = finish;
				tournament.visible = visible;

				tournaments.add(tournament);
			} while (cursor.moveToNext());
		}
		cursor.close();
		return tournaments;
	}

	int getNumUserEvents() {
		Cursor cursor =
				sqLiteDatabase.rawQuery("SELECT COUNT (*) FROM " + UserCreatedEventEntry.TABLE_NAME, null);
		cursor.moveToFirst();
		int count = cursor.getInt(0);
		cursor.close();

		return count;

	}

	int getNumUserNoncreatedEvents() {
		Cursor cursor =
				sqLiteDatabase.rawQuery("SELECT COUNT (*) FROM " + UserEventDataEntry.TABLE_NAME, null);
		cursor.moveToFirst();
		int count = cursor.getInt(0);
		cursor.close();

		return count;

	}


	long insertUserEvent(long uId, String title, int day, int hour, float duration, String location) {
		ContentValues values = new ContentValues();
		values.put(UserCreatedEventEntry.COLUMN_USER_ID, uId);
		values.put(EventEntry.COLUMN_TITLE, title);
		values.put(EventEntry.COLUMN_DAY, day);
		values.put(EventEntry.COLUMN_HOUR, hour);
		values.put(EventEntry.COLUMN_DURATION, duration);
		values.put(EventEntry.COLUMN_LOCATION, location);

		values.put(EventEntry.COLUMN_TOURNAMENT_ID, -1);
		values.put(EventEntry.COLUMN_CLASS, "");
		values.put(EventEntry.COLUMN_FORMAT, "");
		values.put(EventEntry.COLUMN_QUALIFY, 0);
		values.put(EventEntry.COLUMN_CONTINUOUS, 0);
		values.put(EventEntry.COLUMN_TOTAL_DURATION, duration);

		// Insert the new row, returning the primary key value of the new row
		return sqLiteDatabase
				.insert(UserCreatedEventEntry.TABLE_NAME, EventEntry.COLUMN_NULLABLE, values);
	}

	long deleteUserEvent(long eId) {
		String where = UserCreatedEventEntry._ID + "=" + String.valueOf(eId);
		return sqLiteDatabase.delete(UserCreatedEventEntry.TABLE_NAME, where, null);
	}

	long deleteAllUserEvents(long uID) {
		String where = UserCreatedEventEntry.COLUMN_USER_ID + "=" + String.valueOf(uID);
		return sqLiteDatabase.delete(UserCreatedEventEntry.TABLE_NAME, where, null);
	}

	long insertUser(String name, String email) {
		ContentValues values = new ContentValues();
//		values.put(UserEntry.COLUMN_USER_ID, uId);
		values.put(UserEntry.COLUMN_NAME, name);
		values.put(UserEntry.COLUMN_EMAIL, email);

		// Insert the new row, returning the primary key value of the new row
		return sqLiteDatabase.insert(UserEntry.TABLE_NAME, UserEntry.COLUMN_NULLABLE, values);
	}

	User getUser(long id) {
		List<User> users = getUsers(UserEntry._ID + "=" + String.valueOf(id));

		if (users.size() == 0) {
			users = getUsers("1=1");
		}
		return users.get(0);
	}

	List<User> getUsers(String where) {
		Cursor cursor = sqLiteDatabase.query(UserEntry.TABLE_NAME, null, where, null, null, null, null);

		List<User> users = new ArrayList<>();
		int id;
		String name, email;
		if (cursor.moveToFirst()) {
			do {
				id = cursor.getInt(cursor.getColumnIndex(UserEntry._ID));
				name = cursor.getString(cursor.getColumnIndex(UserEntry.COLUMN_NAME));
				email = cursor.getString(cursor.getColumnIndex(UserEntry.COLUMN_EMAIL));

				users.add(new User(id, name, email));
			} while (cursor.moveToNext());
		}
		cursor.close();
		return users;
	}

	long deleteUser(long userId) {
		String where;
		if (userId == -1) {
			where = "";
		} else {
			where = UserEntry._ID + "=" + String.valueOf(userId);
		}
		return sqLiteDatabase.delete(UserEntry.TABLE_NAME, where, null);
	}

	long deleteUserData(long userId) {
		String where;

		if (userId == -1) {
			where = "";
		} else {
			where = UserEventDataEntry.COLUMN_USER_ID + "=" + String.valueOf(userId);
		}
		int result = sqLiteDatabase.delete(UserEventDataEntry.TABLE_NAME, where, null);

		if (userId == -1) {
			where = "";
		} else {
			where = UserTournamentDataEntry.COLUMN_USER_ID + "=" + String.valueOf(userId);
		}
		result += sqLiteDatabase.delete(UserTournamentDataEntry.TABLE_NAME, where, null);

		if (userId == -1) {
			where = "";
		} else {
			where = UserCreatedEventEntry.COLUMN_USER_ID + "=" + String.valueOf(userId);
		}
		result += sqLiteDatabase.delete(UserCreatedEventEntry.TABLE_NAME, where, null);

		return result;
	}

	long mergeUserData(long userId) {
		ContentValues values = new ContentValues();

		values.put("user_id", MainActivity.userId);

		String where = UserEventDataEntry.COLUMN_USER_ID + "=" + String.valueOf(userId);
		int result = sqLiteDatabase
				.updateWithOnConflict(UserEventDataEntry.TABLE_NAME, values, where, null,
						SQLiteDatabase.CONFLICT_REPLACE);

		where = UserTournamentDataEntry.COLUMN_USER_ID + "=" + String.valueOf(userId);
		result += sqLiteDatabase
				.updateWithOnConflict(UserTournamentDataEntry.TABLE_NAME, values, where, null,
						SQLiteDatabase.CONFLICT_REPLACE);

		where = UserCreatedEventEntry.COLUMN_USER_ID + "=" + String.valueOf(userId);
		result += sqLiteDatabase.update(UserCreatedEventEntry.TABLE_NAME, values, where, null);

		return result;
	}

	long updateTournamentsVisible(List<Tournament> tournaments) {
		ContentValues values;
		String where;
		long result = 0;
		for (Tournament tournament : tournaments) {

			values = new ContentValues();
			values.put(TournamentEntry.COLUMN_VISIBLE, tournament.visible ? 1 : 0);

			where = TournamentEntry._ID + "=" + String.valueOf(tournament.id);

			result += sqLiteDatabase.update(TournamentEntry.TABLE_NAME, values, where, null);
		}
		return result;
	}

	long insertUserTournamentData(long userId, long tournamentId, int finish) {
		ContentValues values = new ContentValues();
		values.put(UserTournamentDataEntry.COLUMN_FINISH, finish);

		String where =
				UserTournamentDataEntry.COLUMN_TOURNAMENT_ID + "=" + String.valueOf(tournamentId) + " AND " +
						UserTournamentDataEntry.COLUMN_USER_ID + "=" + String.valueOf(userId);


		long result = sqLiteDatabase.update(UserTournamentDataEntry.TABLE_NAME, values, where, null);
		Log.d(TAG, "update UTourn " + values + " " + where + " " + result);

		if (result == 0) {
			values.put(UserTournamentDataEntry.COLUMN_USER_ID, userId);
			values.put(UserTournamentDataEntry.COLUMN_TOURNAMENT_ID, tournamentId);

			result = sqLiteDatabase
					.insert(UserTournamentDataEntry.TABLE_NAME, UserTournamentDataEntry.COLUMN_NULLABLE,
							values);
			Log.d(TAG, "insert UTourn " + values + " " + where + " " + result);
		}
		return result;
	}

	long insertUserEventData(long userId, long eventId, boolean starred, String note) {
		Log.d(TAG, "In insert#1 " );
		ContentValues values = new ContentValues();
			values.put(UserEventDataEntry.COLUMN_STARRED, starred ? 1 : 0);
			Log.d(TAG, "note " + note);
			values.put(UserEventDataEntry.COLUMN_NOTE, note);

			String where = UserEventDataEntry.COLUMN_EVENT_ID + "=" + String.valueOf(eventId) + " AND " +
					UserEventDataEntry.COLUMN_USER_ID + "=" + String.valueOf(userId);
			Log.d(TAG, "Event " + String.valueOf(eventId) + " values " + values + " where " + where);
			long result = sqLiteDatabase.update(UserEventDataEntry.TABLE_NAME, values, where, null);
			Log.d(TAG, "update1 result " + result);

			if (result == 0) {
				values.put(UserEventDataEntry.COLUMN_USER_ID, userId);
				values.put(UserEventDataEntry.COLUMN_EVENT_ID, eventId);

				result = sqLiteDatabase
						.insert(UserEventDataEntry.TABLE_NAME, UserEventDataEntry.COLUMN_NULLABLE, values);
				Log.d(TAG, "insert1 result " + result);
			}
			Log.d(TAG,"There are " + getNumUserNoncreatedEvents() + " in the user database");
			return result;
	}

	long insertUserEventData(long userId, List<Event> changedEvents) {
		Log.d(TAG, "In insert#2 " );
		Event event;
		long result = 0;
		for (int i = 0; i < changedEvents.size(); i++) {
			event = changedEvents.get(i);

			result += insertUserEventData(userId, event.id, event.starred, event.note);
		}
		Log.d(TAG, "insert2 result " + result);
		return result;

	}
}
