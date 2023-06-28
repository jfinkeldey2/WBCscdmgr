package org.boardgamers.wbcscdmgr;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Adapter;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Main Activity class
 */
public class MainActivity extends AppCompatActivity {
	private final static String TAG = "Main Activity";

	public static long selectedEventId = -1;
	public static long userId = -1;

	private boolean fromFilter = false;
	public static boolean opened = false;

	private ViewPager viewPager;
	private TabsPagerAdapter pagerAdapter;

	@Override
	public void finish() {
		opened = false;
		super.finish();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "Init");

		setContentView(R.layout.main_layout);

		opened = true;
		pagerAdapter = new TabsPagerAdapter(getSupportFragmentManager());

		viewPager = findViewById(R.id.pager);
		viewPager.setAdapter(pagerAdapter);
		viewPager.setOffscreenPageLimit(4);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		SlidingTabLayout tabs = findViewById(R.id.sliding_layout);
		tabs.setViewPager(viewPager);

		// get version from last time app was opened
		SharedPreferences sp = getSharedPreferences(getResources().getString(R.string.user_data_file),
				Context.MODE_PRIVATE);
		int latestVersion = sp.getInt("last_app_version", -1);

		// get current app version from version code
		int currentVersion;
		try {
			currentVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			Log.d(TAG, "ERROR: Unable to not find version code");
			Toast.makeText(this,
					"ERROR: Could not find version code, contact " + getResources().getString(R.string.email) +
							" for help.", Toast.LENGTH_LONG).show();
			currentVersion = 20230101; // TODO set as version code here and android manifest and gradle build
			e.printStackTrace();
		}

		showDialogs(latestVersion);

		if (latestVersion < currentVersion) {
			SharedPreferences.Editor editor = sp.edit();
			editor.putInt("last_app_version", currentVersion);
			editor.apply();
		}

		NotificationManager mNotificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		if (mNotificationManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(Constants.channelId,
					"Notify for events",
					NotificationManager.IMPORTANCE_DEFAULT);
			mNotificationManager.createNotificationChannel(channel);
		}

		Helpers.scheduleAlarms(this);

		loadUserData();
	}

	private void showDialogs(int latestVersion) {
		if (latestVersion < 17) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.continuous_dialog_title)
					.setMessage(R.string.continuous_dialog_message)
					.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							showDialogs(17);
						}
					});
			builder.create().show();
		}
	}

	private void loadUserData() {
		Log.d(TAG, "Loading");
		userId = PreferenceManager.getDefaultSharedPreferences(this)
				.getLong(getResources().getString(R.string.pref_key_schedule_select), MainActivity.userId);

		WBCDataDbHelper dbHelper = new WBCDataDbHelper(this);
		dbHelper.getReadableDatabase();
		int numStarredEvents = dbHelper.getStarredEvents(userId).size();
		String scheduleName = dbHelper.getUser(userId).name;
		setTitle("WBC: " + scheduleName);



		// new stuff 6/27
		Log.d(TAG, "There are currently " + String.valueOf(dbHelper.getNumUserNoncreatedEvents()) + " events in User DB");
		List<Event> events = dbHelper.getUserEvents(1);
		for (Event event : events) {
			Log.d(TAG, "User event loading " + event.id + " " + event.title);
		}
		Log.d(TAG, "There are currently " + String.valueOf(numStarredEvents) + " starred events in User DB");
		List<Event> events2 = dbHelper.getStarredEvents(1);
		for (Event event : events2) {
			Log.d(TAG, "User event with star loading " + event.id + " " + event.title);
		}
		Log.d(TAG, "There are currently " + String.valueOf(dbHelper.getEventsWithNotes(1).stream().count()) + " events with notes in User DB");
		List<Event> events3 = dbHelper.getEventsWithNotes(1);
		for (Event event : events3) {
			Log.d(TAG, "User event with notes loading " + event.id + " " + event.title);
		}
		Log.d(TAG, "There are currently " + String.valueOf(dbHelper.getTournamentsWithFinishes(1).stream().count()) + " tournaments with finishes in User DB");
		List<Tournament> tourns = dbHelper.getTournamentsWithFinishes(1);
		for (Tournament tourn : tourns ) {
			Log.d(TAG, "User event with notes loading " + tourn.id + " " + tourn.title);
		}

		dbHelper.close();

		if (numStarredEvents == 0) {
			// sets default tab to schedule if no starred events
			viewPager.setCurrentItem(1);
		}
		Log.d(TAG, "Loading complete for schedule " + scheduleName);
	}

	@Override
	protected void onResume() {

		if (fromFilter) {
			fromFilter = false;
			pagerAdapter.getItem(1).reloadAdapterData();
		}

		if (selectedEventId > -1) {
			WBCDataDbHelper dbHelper = new WBCDataDbHelper(this);
			dbHelper.getReadableDatabase();
			Event event = dbHelper.getEvent(userId, selectedEventId);
			Tournament tournament = dbHelper.getTournament(userId, event.tournamentID);
			dbHelper.close();

			changeEventInLists(event);

			if (pagerAdapter.getItem(2) != null) {
				((UserDataListFragment) pagerAdapter.getItem(2))
						.updateUserEventData(event, tournament.finish);
			}
		}
		Log.d(TAG, "in resume");


		super.onResume();
	}

	public void removeEvent(Event removedEvent) {
		removedEvent.starred = false;

		if (pagerAdapter.getItem(1) != null) {
			pagerAdapter.getItem(1).removeEvent(removedEvent);
		}

		if (pagerAdapter.getItem(0) != null) {
			pagerAdapter.getItem(0).updateEvent(removedEvent);
		}
	}

	public void changeEventInLists(Event event) {
		for (int j = 0; j < 3; j++) {
			if (pagerAdapter.getItem(j) != null) {
				pagerAdapter.getItem(j).updateEvent(event);
			}
		}
	}


	public static String getDisplayHour(float startHour, float duration) {
		int hour = (int) (startHour + duration) % 24;
		float minute = (startHour + duration) % 1;
		float time = hour * 100 + minute * 60;

		return String.format(Locale.US, "%04d", (int) time);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_main, menu);

		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		final SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
		searchView.setSearchableInfo(
				searchManager.getSearchableInfo(new ComponentName(this, SearchResultActivity.class)));
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				searchView.clearFocus();
				return false;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				return false;
			}
		});
		searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
			@Override
			public boolean onSuggestionSelect(int position) {
				return false;
			}

			@Override
			public boolean onSuggestionClick(int position) {
				searchView.clearFocus();
				Cursor cursor = (Cursor) searchView.getSuggestionsAdapter().getItem(position);
				int id = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID));
				String title = cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1));
				startSearchActivity(id, title);
				return true;
			}
		});

		return true;

	}

	private void startSearchActivity(int id, String title) {
		Intent intent = new Intent(this, SearchResultActivity.class);
		intent.putExtra("query_title", title);
		intent.putExtra("query_id", id);
		startActivity(intent);
	}

	private void share() {
		//    WBCDataDbHelper dbHelper=new WBCDataDbHelper(this);
		//    dbHelper.getReadableDatabase();
		//    List<Event> starred=dbHelper.getStarredEvents();
		//    List<Event> notes=dbHelper.getEventsWithNotes();
		//    List<Tournament> tFinishes=dbHelper.getTournamentsWithFinishes();
		//    dbHelper.close();
		//    Log.d(TAG, "Received from DB");

		UserDataListFragment userDataListFragment = (UserDataListFragment) pagerAdapter.getItem(2);
		List<Event> notes = userDataListFragment.listAdapter.events.get(UserDataListFragment.NOTES_INDEX);
		List<Event> eFinishes =
				userDataListFragment.listAdapter.events.get(UserDataListFragment.FINISHES_INDEX);
		List<Event> userEvents =
				userDataListFragment.listAdapter.events.get(UserDataListFragment.EVENTS_INDEX);
		SummaryListFragment summaryListFragment = (SummaryListFragment) pagerAdapter.getItem(0);

		List<List<Event>> starredGroups = summaryListFragment.listAdapter.events;
		List<Event> starred = new ArrayList<>();
		for (List<Event> events : starredGroups) {
			starred.addAll(events);
		}


		String contentBreak = "~~~";
		String delimitter = ";;;";

		String outputString =
				getResources().getString(R.string.settings_schedule_name_check) + contentBreak;

		String email = "Unknown user";
		Pattern emailPattern = Patterns.EMAIL_ADDRESS;
		Account[] accounts = AccountManager.get(this).getAccounts();
		for (Account account : accounts) {
			if (emailPattern.matcher(account.name).matches()) {
				email = account.name;
				break;
			}
		}
		outputString += email + contentBreak + "0";
		for (Event event : userEvents) {
			outputString += String.valueOf(event.id) + delimitter + event.title + delimitter + event.day + delimitter +
					event.hour + delimitter + event.duration + delimitter + event.location + delimitter;
		}
		outputString += contentBreak + "0";
		for (Event event : starred) {
			outputString += String.valueOf(event.id) + delimitter;
		}

		outputString += contentBreak + "0";
		for (Event event : notes) {
			outputString += String.valueOf(event.id) + delimitter + event.note + delimitter;
		}

		outputString += contentBreak + "0";
		for (Event event : eFinishes) {
			outputString += String.valueOf(event.tournamentID) + delimitter + event.note + delimitter;
		}

		File file;
		String fileName = "Schedule.wbc";
		if (isExternalStorageWritable()) {
			Log.d(TAG, "Saving in external");
			File sdCard = Environment.getExternalStorageDirectory();
			if (!sdCard.canWrite()) {
				Log.e(TAG, "Can't write to storage");
				return;
			}

			File dir = new File(sdCard.getAbsolutePath() + "/WBC/");
			if (!dir.mkdirs()) {
				Log.e(TAG, "Directory not created: " + dir.getPath());
				return;
			}

			file = new File(dir, fileName);
		} else {
			Log.d(TAG, "Saving in internal");
			file = new File(getCacheDir(), fileName);
		}

		Log.d(TAG, "File location: " + file.getAbsolutePath());

		FileOutputStream fileOutputStream;
		try {
			fileOutputStream = new FileOutputStream(file);
			fileOutputStream.write(outputString.getBytes());
			fileOutputStream.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND);
		shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
		shareIntent.setType("application/wbc");
		startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share)));
	}

	/* Checks if external storage is available for read and write */

	private boolean isExternalStorageWritable() {
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			return true;
		} else {
			Log.d(TAG, "Error: external storage not writable");
			return false;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_map) {
			startActivity(new Intent(this, MapActivity.class));
		} else if (item.getItemId() == R.id.menu_share) {
			share();
		} else if (item.getItemId() == R.id.menu_help) {
			startActivity(new Intent(this, HelpActivity.class));
		} else if (item.getItemId() == R.id.menu_about) {
			startActivity(new Intent(this, AboutActivity.class));
		} else if (item.getItemId() == R.id.menu_filter) {
			fromFilter = true;
			startActivity(new Intent(this, FilterActivity.class));
		} else if (item.getItemId() == R.id.menu_settings) {
			startActivity(new Intent(this, SettingsActivity.class));
		} else {
			return super.onOptionsItemSelected(item);
		}

		return true;
	}
}
