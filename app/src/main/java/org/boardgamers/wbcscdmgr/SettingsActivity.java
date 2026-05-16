package org.boardgamers.wbcscdmgr;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;

public class SettingsActivity extends AppCompatActivity {
	private static final String TAG = "Settings";

	private static final int GET_FILE_REQUEST_CODE = 0;

	//	private static List<User> users;
//	public static long currentUserId;
	private static final String folder = Environment.getExternalStorageDirectory().getAbsolutePath() + "/WBC/";

	private String scheduleName;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.settings);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayShowHomeEnabled(true);
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}
		setTitle(R.string.activity_settings);

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		getFragmentManager().beginTransaction().replace(R.id.setings_content, new SettingsFragment())
				.commit();

		// Get intent, action and MIME type
		Intent intent = getIntent();
		String action = intent.getAction();
		String type = intent.getType();

		if (Intent.ACTION_VIEW.equals(action) && type != null) {
			if ("application/wbc".equals(type)) {
				handleSendText(intent); // Handle text being sent
			} else {
				Log.d(TAG, type);
			}
		}
	}

	private void getFile() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("file/*");
		startActivityForResult(intent, GET_FILE_REQUEST_CODE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == GET_FILE_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				Log.d(TAG, "Good result");
				handleSendText(data);
			} else {
				Log.d(TAG, "Bad result");
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	private void handleSendText(Intent intent) {
		InputStream inputStream;
		try {
			Log.d(TAG, "Getting content");
			StringBuilder fileContent = new StringBuilder("");

			inputStream = getContentResolver().openInputStream(intent.getData());
			byte[] buffer = new byte[1024];
			int n;
			assert inputStream != null;
			while ((n = inputStream.read(buffer)) != -1) {
				fileContent.append(new String(buffer, 0, n));
			}

			new SaveScheduleTask(this).execute(fileContent.toString());
		} catch (IOException e) {
			Log.d(TAG, "IO exception");
			e.printStackTrace();
		}
	}

	private class SaveScheduleTask extends AsyncTask<String, Void, Integer> {
		private String scheduleSource;
//		private int userId;

		private final Context context;

		private SaveScheduleTask(Context c) {
			context = c;
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (result > 0) {
				AlertDialog.Builder builder = new AlertDialog.Builder(context);

				View dialogView = View.inflate(context, R.layout.dialog_import_schedule, null);

				final EditText editText = dialogView.findViewById(R.id.schedule_import_name_et);
				editText.getText().clear();

				final RadioGroup radioGroup =
						dialogView.findViewById(R.id.schedule_import_merge_rg);
				radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						boolean noMerging = checkedId == R.id.schedule_import_neither;

						editText.setEnabled(noMerging);
					}
				});

				builder.setView(dialogView);
				builder.setTitle("Schedule successfully imported!")
						.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								scheduleName = editText.getText().toString();

								if (radioGroup.getCheckedRadioButtonId() == R.id.schedule_import_replace) {
									mergeSchedule(true);
								} else if (radioGroup.getCheckedRadioButtonId() == R.id.schedule_import_merge) {
									mergeSchedule(false);
								}

								dialog.dismiss();
							}
						}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).setCancelable(false);
				builder.create().show();
			} else {
				Toast.makeText(context, "This is not an authentic WBC schedule file!", Toast.LENGTH_SHORT)
						.show();
			}

			super.onPostExecute(result);
		}

		@Override
		protected Integer doInBackground(String... params) {
			String data = params[0];

			Log.d(TAG, "INPUT:\n" + data);

			String contentBreak = "~~~";
			String delimitter = ";;;";

			String[] splitData = data.split(contentBreak);

			if (!splitData[0]
					.equalsIgnoreCase(getResources().getString(R.string.settings_schedule_name_check))) {
				return -1;
			}

			scheduleSource = splitData[1];
			String[] createdEvents = splitData[2].split(delimitter);
			String[] starredEvents = splitData[3].split(delimitter);
			String[] eventNotes = splitData[4].split(delimitter);
			String[] tournamentFinishes = splitData[5].split(delimitter);

			WBCDataDbHelper dbHelper = new WBCDataDbHelper(context);
			dbHelper.getWritableDatabase();

			// insert new user
			long userId = dbHelper.insertUser(scheduleName, scheduleSource);
			dbHelper.close();
			PreferenceManager.getDefaultSharedPreferences(context).edit()
					.putLong(getResources().getString(R.string.pref_key_schedule_select), userId)
					.apply();

			MainActivity.userId = userId;

			// insert created events
			String title, location;
			int eId, day, hour;
			float duration;
			int newEId = dbHelper.getNumUserEvents();

			int[] oldIds = new int[createdEvents.length / 6];
			int[] newEIds = new int[createdEvents.length / 6];

			for (int i = 0; i + 6 <= createdEvents.length; i += 6) {
				if (createdEvents[i].equalsIgnoreCase("0")) {
					break;
				}
				eId = Integer.valueOf(createdEvents[i]);
				title = createdEvents[i + 1];
				day = Integer.valueOf(createdEvents[i + 2]);
				hour = Integer.valueOf(createdEvents[i + 3]);
				duration = Float.valueOf(createdEvents[i + 4]);
				location = createdEvents[i + 5];

				dbHelper.insertUserEvent(userId, title, day, hour, duration, location);

				oldIds[i / 6] = eId;
				newEIds[i / 6] = newEId;

				newEId++;
			}

			// insert user event data (notes)
			String note;
			boolean starred;
			for (int i = 0; i + 2 <= eventNotes.length; i += 2) {
				if (eventNotes[i].equalsIgnoreCase("0")) {
					break;
				}
				eId = Integer.valueOf(eventNotes[i]);
				note = eventNotes[i + 1];

				newEId = -1;
					for (int j = 0; j < oldIds.length; j++) {
						if (oldIds[j] == eId) {
							newEId = newEIds[j];
							break;
						}
					}

				// check if event is also starred
				starred = false;
				for (int j = 0; j < starredEvents.length; j++) {
					if (starredEvents[j].equalsIgnoreCase(eventNotes[i])) {
						starred = true;
						starredEvents[j] = "-1";
						break;
					}
				}

				// insert user event data with note and starred
				dbHelper.insertUserEventData(userId, newEId, starred, note);
			}

			// insert user event data (starred)
			for (String eIdString : starredEvents) {
				if (eIdString.equalsIgnoreCase("0")) {
					break;
				}
				eId = Integer.valueOf(eIdString);
				newEId = -1;
				for (int i = 0; i < oldIds.length; i++) {
					if (oldIds[i] == eId) {
						newEId = newEIds[i];
						break;
					}
				}


				// if star added in notes, don't edit event and erase note
				if (newEId > -1) {
					dbHelper.insertUserEventData(userId, newEId, true, "");
				}
			}

			int finish, tId;
			for (int i = 0; i + 2 <= tournamentFinishes.length; i += 2) {
				if (tournamentFinishes[i].equalsIgnoreCase("0")) {
					break;
				}
				tId = Integer.valueOf(tournamentFinishes[i]);
				finish = Integer.valueOf(tournamentFinishes[i + 1]);

				dbHelper.insertUserTournamentData(userId, tId, finish);

			}

			dbHelper.close();

			Log.d(TAG, "Finish");

			return 1;
		}
	}

	private void showMergeScheduleDialog() {
		WBCDataDbHelper dbHelper = new WBCDataDbHelper(this);
		dbHelper.getReadableDatabase();
		User defaultUser = dbHelper.getUsers("").get(0);
		User currentUser = dbHelper.getUser(MainActivity.userId);
		dbHelper.close();

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getResources().getString(R.string.settings_schedule_merge)).setMessage(
				"How do you want to save " + currentUser.name + "? "
						+ "Do you want to merge with " + defaultUser.name
						+ " or replace it? This operation cannot be undone.")
				.setPositiveButton("Replace", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mergeSchedule(true);
						dialog.dismiss();
					}
				}).setNegativeButton("Merge", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mergeSchedule(false);
				dialog.dismiss();
			}
		}).setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.create().show();
	}

	private void mergeSchedule(boolean overwrite) {
		new MergeScheduleTask(this).execute(overwrite);
	}

	private class MergeScheduleTask extends AsyncTask<Boolean, Void, Void> {
		private final Context context;

		private MergeScheduleTask(Context c) {
			context = c;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			PreferenceManager.getDefaultSharedPreferences(context).edit()
					.putLong(getResources().getString(R.string.pref_key_schedule_select), MainActivity.userId)
					.apply();

//			SettingsFragment.updatePreferences();

			super.onPostExecute(aVoid);
		}

		@Override
		protected Void doInBackground(Boolean... params) {
			boolean overwrite = params[0];

			WBCDataDbHelper dbHelper = new WBCDataDbHelper(context);
			dbHelper.getWritableDatabase();
			if (overwrite) {
				dbHelper.deleteUserData(MainActivity.userId);
			}
			dbHelper.mergeUserData(MainActivity.userId);
//			dbHelper.deleteUser(MainActivity.userId);
			dbHelper.close();

			return null;
		}
	}

	public static class SettingsFragment extends PreferenceFragment {
		private Preference scheduleSelect;
		private Preference scheduleMerge;
		private Preference scheduleDelete;
		private Preference scheduleExport;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			addPreferencesFromResource(R.xml.preferences);

			SharedPreferences sp = getPreferenceManager().getSharedPreferences();

			Preference notify = findPreference(getResources().getString(R.string.pref_key_notify));
			notify.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					return true;
				}
			});

			Preference notifyTime = findPreference(getResources().getString(R.string.pref_key_notify_time));
			notifyTime.setSummary(
					String.valueOf(sp.getInt(getResources().getString(R.string.pref_key_notify_time), 5)));

			final String[] notifyTypeEntries =
					getResources().getStringArray(R.array.settings_notify_type_entries);

			Preference notifyType = findPreference(getResources().getString(R.string.pref_key_notify_type));
			notifyType.setSummary(notifyTypeEntries[Integer
					.valueOf(sp.getString(getResources().getString(R.string.pref_key_notify_type), "2"))]);
			notifyType.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					preference.setSummary(notifyTypeEntries[Integer.valueOf((String) newValue)]);
					return true;
				}
			});

			Preference scheduleImport =
					findPreference(getResources().getString(R.string.pref_key_schedule_import));
			scheduleImport.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					((SettingsActivity) getActivity()).getFile();
					return true;
				}
			});

			scheduleSelect = findPreference(getResources().getString(R.string.pref_key_schedule_select));

			scheduleMerge = findPreference(getResources().getString(R.string.pref_key_schedule_merge));
			scheduleMerge.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					((SettingsActivity) getActivity()).showMergeScheduleDialog();
					return true;
				}
			});

			scheduleDelete = findPreference(getResources().getString(R.string.pref_key_schedule_delete));
			scheduleDelete.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					showDeleteScheduleDialog();
					return true;
				}
			});
            /* Removed share JLF 5/16/26

			scheduleExport = findPreference(getResources().getString(R.string.pref_key_schedule_export));
			scheduleExport.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
				public boolean onPreferenceClick(Preference preference) {
					((SettingsActivity) getActivity()).share();
					return true;
				}
			});
                */
//			updatePreferences();

		}

//		public static void updatePreferences() {
//			if (currentUserId == MainActivity.userId) {
//				scheduleMerge.setEnabled(false);
//				scheduleDelete.setEnabled(false);
//			} else {
//				scheduleMerge.setEnabled(true);
//				scheduleDelete.setEnabled(true);
//			}
//
//			scheduleMerge.setSummary("Merge " + users.get(currentUserId).name + " with " +
//					users.get(MainActivity.userId).name + ".");
//			scheduleDelete.setSummary("Remove " + users.get(currentUserId).name + " from schedules");
//			scheduleExport
//					.setSummary("Export " + users.get(currentUserId).name + " to device storage (" + folder + ") and share");
//			scheduleSelect.setSummary("Current: " + users.get(currentUserId).name);
//		}

		public void showDeleteScheduleDialog() {
			WBCDataDbHelper dbHelper = new WBCDataDbHelper(getActivity());
			dbHelper.getReadableDatabase();
			User currentUser = dbHelper.getUsers("").get(0);
			dbHelper.close();

			String scheduleName = currentUser.name;

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(getResources().getString(R.string.settings_schedule_delete))
					.setMessage("Are you sure you want to delete the schedule " + scheduleName +
							"?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					deleteSchedule();
					dialog.dismiss();
				}
			}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			builder.create().show();
		}

		public void deleteSchedule() {
			WBCDataDbHelper dbHelper = new WBCDataDbHelper(getActivity());
			dbHelper.getWritableDatabase();
			dbHelper.deleteUser(MainActivity.userId);
			dbHelper.deleteUserData(MainActivity.userId);

			User currentUser = dbHelper.getUsers("").get(0);
			dbHelper.close();


			MainActivity.userId = currentUser.id;

			PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
					.putLong(getResources().getString(R.string.pref_key_schedule_select), MainActivity.userId).apply();

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_settings, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		if (id == android.R.id.home) {
			finish();
			return true;
		} else if (id == R.id.menu_reset) {
			showResetDialog();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

    /* Removed share icon JLF 5/16/26
	private void share() {
		long uId = MainActivity.userId;
		WBCDataDbHelper dbHelper = new WBCDataDbHelper(this);
		dbHelper.getReadableDatabase();
		List<Event> starred = dbHelper.getStarredEvents(uId);
		List<Event> notes = dbHelper.getEventsWithNotes(uId);
		List<Tournament> tFinishes = dbHelper.getTournamentsWithFinishes(uId);
		List<Event> userEvents = dbHelper.getUserEvents(uId);
		dbHelper.close();
		Log.d(TAG, "Received from DB");

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
		for (Tournament tournament : tFinishes) {
			outputString +=
					String.valueOf(tournament.id) + delimitter + String.valueOf(tournament.finish) + delimitter;
		}

		File file;
		String fileName = "Schedule.wbc";
		if (isExternalStorageWritable()) {
			Log.d(TAG, "Saving in external");
			File dir = new File(folder);

			if (!dir.mkdirs()) {
				Log.d(TAG, "Directory not created");
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
*/

  /* Checks if external storage is available for read and write */

	private boolean isExternalStorageWritable() {
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			return true;
		} else {
			Log.d(TAG, "Error: external storage not writable");
			return false;
		}
	}

	private void showResetDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.reset).setMessage(R.string.reset_message)
				.setPositiveButton("Yes, reset", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						resetPrefs();
					}
				}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		builder.create().show();
	}

	private void resetPrefs() {
		PreferenceManager.getDefaultSharedPreferences(this).edit().clear().apply();
		recreate();
	}

	@Override
	protected void onPause() {
		Helpers.scheduleAlarms(this);

		super.onPause();
	}

	@Override
	public void finish() {
		super.finish();
		if (!MainActivity.opened) {
			startActivity(new Intent(this, MainActivity.class));
		}
	}
}
