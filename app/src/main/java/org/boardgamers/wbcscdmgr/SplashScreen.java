package org.boardgamers.wbcscdmgr;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SplashScreen extends AppCompatActivity {
	private static final String TAG = "SplashScreen";

	private ProgressBar progressBar;
	private int currentNumEvents;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	//	int TOTAL_EVENTS = 751;
		int TOTAL_EVENTS = 1100;

		WBCDataDbHelper dbHelper = new WBCDataDbHelper(this);
		dbHelper.onUpgrade(dbHelper.getWritableDatabase(), dbHelper.getVersion(),
				WBCDataDbHelper.DATABASE_VERSION);
		dbHelper.getReadableDatabase();
		currentNumEvents = dbHelper.getNumEvents();
		dbHelper.close();

		Log.d(TAG, "There are currently " + String.valueOf(currentNumEvents) + " events in DB");

		if (currentNumEvents >= TOTAL_EVENTS) {
			checkForChanges();
		} else {
			setContentView(R.layout.splash);

			Toolbar toolbar = findViewById(R.id.toolbar);
			setSupportActionBar(toolbar);
			setTitle(getResources().getString(R.string.activity_splash));

			progressBar = findViewById(R.id.splash_progress);
			progressBar.setMax(TOTAL_EVENTS);

			new LoadEventsTask(this).execute(0, 0, 0);
		}
	}

	@Override
	public void onBackPressed() {
		showToast("Please wait until all events have loaded");
	}

	private void checkForChanges() {
		String changes = "";
		WBCDataDbHelper dbHelper = new WBCDataDbHelper(this);
		dbHelper.getWritableDatabase();

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		long lastVersionCode = preferences.getLong("lastVersionCode", -1);

		// TODO add changes to database and string here
		/* Removed for 2023
		if (lastVersionCode < 263000300) {
			changes += "Mega Civilization will now be on Monday At 9AM in Festival Hall.  It was originally scheduled for Monday at 12 in Hemlock.\n\n";
			dbHelper.updateEvent(null, "Mega Civilization 1/1", -1, 9, "Festival Hall");
		}
		*/

		dbHelper.close();

		SharedPreferences.Editor editor = preferences.edit();
		editor.putLong("lastVersionCode", 263000300);
		editor.apply();

		if (changes.equalsIgnoreCase("")) {
			startMainActivity();
		} else {
			AlertDialog.Builder changesBuilder = new AlertDialog.Builder(this);
			changesBuilder.setTitle(R.string.changes_dialog_title).setMessage(changes)
					.setNeutralButton(R.string.close, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.dismiss();
							startMainActivity();
						}
					}).setCancelable(false);
			changesBuilder.create().show();
		}
	}

	private void startMainActivity() {
		startActivity(new Intent(this, MainActivity.class));
		finish();
	}

	private void showToast(String string) {
		Toast.makeText(this, string, Toast.LENGTH_SHORT).show();
	}

	/**
	 * Load Events Task to load schedule
	 */
	private class LoadEventsTask extends AsyncTask<Integer, Integer, Integer> {
		private final static String TAG = "Load Events Task";

		private final Context context;

		private LoadEventsTask(Context c) {
			context = c;
		}

		@Override
		protected void onPostExecute(Integer result) {
			if (result > 0) {
				startMainActivity();
			} else if (result == -1) {
				showToast("ERROR: Could not parse schedule file, contact dev@boardgamers.org for help.");
			} else if (result == -2) {
				showToast("ERROR: Could not find schedule file, contact dev@boardgamers.org for help.");
			} else if (result == -3) {
				showToast("ERROR: Could not open schedule file, contact dev@boardgamers.org for help.");
			}

			super.onPostExecute(result);
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			progressBar.setProgress(values[0] - currentNumEvents);

			super.onProgressUpdate(values);
		}

		@Override
		protected Integer doInBackground(Integer... params) {
			// find schedule file
			InputStream is;
			try {
				is = getAssets().open("schedule2023.txt");
			} catch (IOException e2) {
				e2.printStackTrace();
				return -2;
			}
			// read schedule file
			InputStreamReader isr;
			try {
				isr = new InputStreamReader(is);
			} catch (IllegalStateException e1) {
				e1.printStackTrace();
				return -3;
			}

			WBCDataDbHelper dbHelper = new WBCDataDbHelper(context);
			dbHelper.getWritableDatabase();
			MainActivity.userId = (int) dbHelper.insertUser("My Schedule", "");
			PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit()
					.putLong(getResources().getString(R.string.pref_key_schedule_select), MainActivity.userId).apply();


			// parse schedule file
			BufferedReader reader = new BufferedReader(isr);
			try {
				final String preExtraStrings[] =
						{" AFC", " NFC", " FF", " PC", " Circus", " After Action", " Aftermath"};
				final String postExtraStrings[] = {" Demo"};

				final String daysForParsing[] = getResources().getStringArray(R.array.daysForParsing);

				String eventTitle, eClass, format, gm, tempString, location;
				String[] rowData;
				int day, prize;
				float hour, duration, totalDuration;
				boolean continuous, qualify, isTournamentEvent;

				String tournamentTitle, tournamentLabel, shortEventTitle = "";
//				List<String> tournamentTitles = new ArrayList<>();

				String line;
				int rowCount = 0;
				while ((line = reader.readLine()) != null) {
					rowData = line.split("~");

					// currentDay
					tempString = rowData[0];

					day = -1;
					for (int i = 0; i < daysForParsing.length; i++) {
						if (daysForParsing[i].equalsIgnoreCase(tempString)) {
							day = i;
							break;
						}
					}
					if (day == -1) {
						Log.e(TAG, "Unknown date: " + rowData[2] + " in " + line);
						continue;
//						day = 0;
					}

					// title
					eventTitle = rowData[2];

					// time
					int colonIndex = rowData[1].indexOf(":");
					if (colonIndex == -1) {
						hour = Float.valueOf(rowData[1]);
					} else {
						hour = Float.valueOf(rowData[1].substring(0, colonIndex))
								+ Float.valueOf(rowData[1].substring(colonIndex + 1)) / 60;
					}

					if (rowData.length < 8) {
						prize = 0;
						eClass = "";
						format = "";
						duration = 0;
						continuous = false;
						gm = "";
						location = "";
					} else {
						// prize
						tempString = rowData[3];
						if (tempString.equalsIgnoreCase("") || tempString.equalsIgnoreCase("--")) {
							tempString = "0";
						}
						prize = Integer.valueOf(tempString);

						// class
						eClass = rowData[4];

						// format
						format = rowData[5];

						// duration
						if (rowData[6].equalsIgnoreCase("") || rowData[6].equalsIgnoreCase("--")) {
							duration = 0;
						} else {
							duration = (float) Math.round(Float.valueOf(rowData[6]) * 100) / 100;
						}

						// continuous
						continuous = rowData[7].equalsIgnoreCase("Y");

						// gm
						gm = rowData[8];

						// location
						location = rowData[9];

					}

					// get tournament title and label and event short name
					tempString = eventTitle;

					// remove post pre event extra strings
					for (String eventExtraString : preExtraStrings) {
						tempString = tempString.replace(eventExtraString, "");
					}

					// remove post event extra strings
					for (String eventExtraString : postExtraStrings) {
						tempString = tempString.replace(eventExtraString, "");
					}

					isTournamentEvent = eClass.length() > 0;
					if (isTournamentEvent || format.equalsIgnoreCase("Preview")) {
						int spaceIndex = tempString.lastIndexOf(" ");
						shortEventTitle = tempString.substring(spaceIndex + 1);
						tempString = tempString.substring(0, spaceIndex);
					}

					if (eventTitle.contains("Junior")
							|| eventTitle.indexOf("COIN series") == 0
							|| format.equalsIgnoreCase("SOG")
							|| format.equalsIgnoreCase("Preview")) {
						tournamentTitle = tempString;
						tournamentLabel = "";
					} else if (isTournamentEvent) {
						tournamentLabel = rowData[10];
						tournamentTitle = tempString;
					} else {
						tournamentTitle = tempString;
						tournamentLabel = "";

						if (eventTitle.indexOf("Auction") == 0) {
							tournamentTitle = "AuctionStore";
						}

						// search for non tournament main titles
						String[] nonTournamentStrings =
								{"Open Gaming", "Registration", "Vendors Area", "World at War", "Wits & Wagers",
										"Texas Roadhouse BPA Fundraiser", "Memoir: D-Day"};
						for (String nonTournamentString : nonTournamentStrings) {
							if (tempString.indexOf(nonTournamentString) == 0) {
								tournamentTitle = nonTournamentString;
								break;
							}
						}
					}

					long tID = dbHelper.getTournamentID(tournamentTitle);
					if (tID == -1) {
						tID = dbHelper
								.insertTournament(tournamentTitle, tournamentLabel, isTournamentEvent,
										prize, gm);
					}

					// Log.d(TAG, String.valueOf(tournamentId)+": "+tournamentTitle
					// +";;;E: "+eventTitle);

					totalDuration = duration;
					qualify = false;

					if (isTournamentEvent || format.equalsIgnoreCase("Junior")) {
						if (shortEventTitle.equals("SF")) {
							qualify = true;
						} else if (shortEventTitle.equals("QF")) {
							qualify = true;
						} else if (shortEventTitle.equals("F")) {
							qualify = true;
						} else if (shortEventTitle.equals("QF/SF/F")) {
							qualify = true;
							totalDuration *= 3;
						} else if (shortEventTitle.equals("SF/F")) {
							qualify = true;
							totalDuration *= 2;
						} else if (continuous && shortEventTitle.indexOf("R") == 0 &&
								shortEventTitle.contains("/")) {
							int dividerIndex = shortEventTitle.indexOf("/");
							int startRound = Integer.valueOf(shortEventTitle.substring(1, dividerIndex));
							int endRound = Integer.valueOf(shortEventTitle.substring(dividerIndex + 1));

							float currentTime = hour;
							for (int round = 0; round < endRound - startRound; round++) {
								// if time passes midnight, next round starts at 9 the next currentDay
								if (currentTime > 24) {
									if (currentTime >= 24 + 9) {
										Log.d(TAG, "Event " + eventTitle + " goes past 9");
									}
									totalDuration += 9 - (currentTime - 24);
									currentTime = 9;
								}
								totalDuration += duration;
								currentTime += duration;
							}
						} else if (continuous) {
							Log.d(TAG, "Unknown continuous event: " + eventTitle);
						}
					}

					dbHelper.insertEvent(tID, day, hour, eventTitle, eClass, format, qualify,
							duration, continuous, totalDuration, location);

					publishProgress(rowCount);
					rowCount++;
				}

				// close streams and number of events
				isr.close();
				is.close();
				reader.close();

				// get total count
				int tournamentCount = dbHelper.getAllTournaments(MainActivity.userId).size();
				int eventCount = dbHelper.getAllEvents(MainActivity.userId).size();
				dbHelper.close();

				// log statistics
				Log.d(TAG, "Finished load, "
						+ String.valueOf(tournamentCount) + " total tournaments and "
						+ String.valueOf(eventCount) + " total events");
				return rowCount;
			} catch (IOException e) {
				e.printStackTrace();
				return -1;
			}
		}
	}
}
