package org.boardgamers.wbcscdmgr;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

class Helpers {
	private static final String TAG = "Helpers";

	/**
	 * Get hours elapsed since midnight on the first day
	 *
	 * @return hours elapsed since midnight of the first day
	 */
	static long getHoursIntoConvention() {

		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT-4"));
		long currentHour = calendar.get(Calendar.HOUR_OF_DAY);
		long currentDay = (calendar.getTimeInMillis() - getFirstDayCalendar().getTimeInMillis()) / (Constants.milliHour * 24);
		if (currentDay <= 0) {
			currentDay--;
		}

		Log.d("US", "Day is " + String.valueOf(currentDay) + " and hour is " + String.valueOf(currentHour));
		if (currentDay < 0) {
			return -1;
		} else {
			int day = (int) currentDay;
			return day * 24 + currentHour;
		}
	}

	private static Calendar getFirstDayCalendar() {
		SimpleDateFormat dateFormatter = new SimpleDateFormat("M/dd/yyyy HH:mm:ss z", Locale.ENGLISH);
		String firstDayString = "7/25/2025 00:00:00 EST";
		Calendar firstDayCalendar = Calendar.getInstance();
		try {
			Date firstDayDate = dateFormatter.parse(firstDayString);
			firstDayCalendar.setTime(firstDayDate);
		} catch (java.text.ParseException e) {
			Log.d(TAG, "Unable to parse firstDayString: " + firstDayString);
		}

		return firstDayCalendar;
	}


	public static void scheduleAlarms(Context context) {
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		if (alarmManager == null) {
			Log.e(TAG, "Alarm manager is null");
			return;
		}

		// GET CURRENT TIME IN EST
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT-4"));

		// TESTING: set currentDay to day of week
		//MainActivity.currentDay=(calendar.get(Calendar.DAY_OF_WEEK));

		// SET DELAY TO NEXT HOUR, IN MILLIS, FOR UPDATE
		calendar.setTimeInMillis(calendar.getTimeInMillis() / Constants.milliHour * Constants.milliHour + Constants.milliHour);

		// CHECK IF NOTIFICATION ACTIVE
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		if (sharedPref.getBoolean(context.getResources().getString(R.string.pref_key_notify), false)) {

			int notificationTime =
					sharedPref.getInt(context.getResources().getString(R.string.pref_key_notify_time), 5);

			if (Helpers.getHoursIntoConvention() < 0) {
				calendar = Helpers.getFirstDayCalendar();
			} else {
				calendar = Calendar.getInstance();
				int hour = calendar.get(Calendar.HOUR_OF_DAY);
				if (60 - notificationTime <= calendar.get(Calendar.MINUTE)) {
					hour++;
				}
				calendar.set(Calendar.HOUR_OF_DAY, hour);
				calendar.set(Calendar.MINUTE, 60 - notificationTime);
				calendar.set(Calendar.SECOND, 0);
			}


			Intent checkEventServiceIntent = new Intent(context, CheckEventService.class);
			PendingIntent serviceScheduleIntent = PendingIntent.getService(context,
					1324,
					checkEventServiceIntent,
 					PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_MUTABLE);
//					PendingIntent.FLAG_CANCEL_CURRENT);  Replaced 2023

			alarmManager.setRepeating(AlarmManager.RTC_WAKEUP
					, calendar.getTimeInMillis()
					, Constants.milliHour
					, serviceScheduleIntent);

		}
	}


	public static int getBoxIdFromLabel(String label, Context context) {
		String fixedLabel = label.toLowerCase();
		fixedLabel = fixedLabel.replace("&", "and");
		fixedLabel = fixedLabel.replace("!", "exc");
		fixedLabel = fixedLabel.replace("-", "dash");

		fixedLabel = "drawable/box_" + fixedLabel;

		int id = context.getResources().getIdentifier(fixedLabel, null, context.getPackageName());

		return id == 0 ? R.drawable.box_iv_no_image_text : id;
	}
}
