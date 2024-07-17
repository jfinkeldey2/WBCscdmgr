package org.boardgamers.wbcscdmgr;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

public class EventFragment extends Fragment {
	private final String TAG = "EventFragment";

	private int initialFinish = -1;

	private final int[] finishIDs =
			{R.id.ef_finish_0, R.id.ef_finish_1, R.id.ef_finish_2, R.id.ef_finish_3, R.id.ef_finish_4,
					R.id.ef_finish_5, R.id.ef_finish_6};

	public Event event;
	private Tournament tournament;

	// event description
	private TextView titleTV;
	private TextView dayTV;
	private TextView timeTV;
	private TextView locationTV;
	private TextView formatTV;
	private TextView classTV;
	private TextView gMTV;
	private TextView previewTV;
	private TextView reportTV;
	private TextView tournamentTV;
	private ImageView boxIV;

	// user data
	private EditText noteET;
	private Button shareButton;
	private Button clearButton;
	private TextView finishTV;
	private RadioGroup finishGroup;
	private RadioButton[] finishButtons;
	// views
	private LinearLayout timeLayout;
	private ToggleButton mapOverlayToggle;
	private ImageView map;
	private ImageView mapOverlay;

	private int roomID;
	private boolean overlayOn;
	private boolean runnableActive;
	private final Handler handler = new Handler();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.event_fragment, container, false);

		timeLayout = view.findViewById(R.id.ef_time_layout);
		titleTV = view.findViewById(R.id.ef_title);
		dayTV = view.findViewById(R.id.ef_day);
		timeTV = view.findViewById(R.id.ef_time);
		formatTV = view.findViewById(R.id.ef_format);
		classTV = view.findViewById(R.id.ef_class);
		locationTV = view.findViewById(R.id.ef_location);
		gMTV = view.findViewById(R.id.ef_gm);

		previewTV = view.findViewById(R.id.ef_preview_link);
		previewTV.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openPreviewLink();
			}
		});

		reportTV = view.findViewById(R.id.ef_report_link);
		reportTV.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openReportLink();
			}
		});

		tournamentTV = view.findViewById(R.id.ef_tournament_link);
		tournamentTV.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startSearchActivity();
			}
		});

		boxIV = view.findViewById(R.id.ef_box_image);

		map = view.findViewById(R.id.ef_map);
		mapOverlay = view.findViewById(R.id.ef_map_overlay);

		runnableActive = false;
		mapOverlayToggle = view.findViewById(R.id.ef_map_toggle_button);
		mapOverlayToggle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mapOverlayToggle.isChecked()) {
					startOverlayUpdate();
				} else {
					stopOverlayUpdate();
				}
			}
		});
		noteET = view.findViewById(R.id.ef_note);

		shareButton = view.findViewById(R.id.ef_share);
		shareButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				share();
			}
		});

		clearButton = view.findViewById(R.id.ef_clear);
		clearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				noteET.getText().clear();
			}
		});

		finishTV = view.findViewById(R.id.ef_finish_text);
		finishGroup = view.findViewById(R.id.ef_finish_group);
		finishButtons = new RadioButton[finishIDs.length];
		for (int i = 0; i < finishIDs.length; i++) {
			finishButtons[i] = view.findViewById(finishIDs[i]);
			finishButtons[i].setText(String.valueOf(i));
		}

		setEvent(MainActivity.selectedEventId);
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();

		if (roomID > -1) {
			startOverlayUpdate();
		}
	}

	public void setEvent(long id) {
		saveEventData();
		stopOverlayUpdate();

		if (id == -1) {
			event = null;

			titleTV.setVisibility(View.VISIBLE);
			titleTV.setText(getResources().getString(R.string.title));
			dayTV.setText(getResources().getString(R.string.day));
			timeTV.setText(getResources().getString(R.string.time));
			timeTV.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			locationTV.setText(getResources().getString(R.string.location));
			formatTV.setText(getResources().getString(R.string.format));
			classTV.setText(getResources().getString(R.string.class_string));
			gMTV.setText(getResources().getString(R.string.gm));

			timeLayout.setBackgroundResource(0);

			setRoom("");
			mapOverlay.setImageResource(0);
			map.setImageResource(0);
			stopOverlayUpdate();

			noteET.setEnabled(false);
			clearButton.setEnabled(false);
			// shareButton.setEnabled(true);
			shareButton.setEnabled(false);

			hideNonTournamentViews();
		} else {
			WBCDataDbHelper dbHelper = new WBCDataDbHelper(getActivity());
			dbHelper.getReadableDatabase();
			event = dbHelper.getEvent(MainActivity.userId, id);

			tournament = dbHelper.getTournament(MainActivity.userId, event.tournamentID);

			dbHelper.close();

			if (getActivity() instanceof EventActivity) {
				titleTV.setVisibility(View.GONE);
				getActivity().setTitle(event.title);
			} else {
				titleTV.setVisibility(View.VISIBLE);
				titleTV.setText(event.title);
			}

			String[] dayStrings = getResources().getStringArray(R.array.days);
			dayTV.setText(dayStrings[event.day]);

			String timeString = MainActivity.getDisplayHour(event.hour, 0) + " to " + MainActivity.getDisplayHour(
					event.hour, event.duration);
			timeTV.setText(timeString);
			if (event.continuous) {
				timeTV.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.continuous_icon, 0);
			} else {
				timeTV.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}

			String locationString = getResources().getString(R.string.location) + ": " + event.location;
			locationTV.setText(locationString);

			if (event.format.equalsIgnoreCase("")) {
				formatTV.setVisibility(View.GONE);
			} else {
				String formatString = getResources().getString(R.string.format) + ": " + event.format;
				formatTV.setVisibility(View.VISIBLE);
				formatTV.setText(formatString);
			}

			if (event.format.equalsIgnoreCase("")) {
				classTV.setVisibility(View.GONE);
			} else {
				String classString = getResources().getString(R.string.class_string) + ": " + event.eClass;
				classTV.setVisibility(View.VISIBLE);
				classTV.setText(classString);
			}

			if (tournament != null) {
				String gmString = getResources().getString(R.string.gm) + ": " + tournament.gm;
				gMTV.setVisibility(View.VISIBLE);
				gMTV.setText(gmString);
			} else {
				gMTV.setVisibility(View.GONE);
			}

			setRoom(event.location);

			noteET.setText(event.note);
			noteET.setEnabled(true);
			clearButton.setEnabled(true);
			//shareButton.setEnabled(true);
			shareButton.setEnabled(false);

			long hoursIntoConvention = Helpers.getHoursIntoConvention();
			boolean started = event.day * 24 + event.hour <= hoursIntoConvention;
			boolean ended = event.day * 24 + event.hour + event.duration <= hoursIntoConvention;
			boolean happening = started && !ended;

			if (ended) {
				timeLayout.setBackgroundResource(R.drawable.ended_light);
			} else if (happening) {
				timeLayout.setBackgroundResource(R.drawable.current_light);
			} else {
				timeLayout.setBackgroundResource(R.drawable.future_light);
			}

			if (tournament != null) {
				tournamentTV.setVisibility(View.VISIBLE);
				//tournamentTV.setText("View all " + tournament.title + " events");
			}

			// check if last event is class (is tournament)
			if (tournament != null && tournament.isTournament) {
				String finishString = getResources().getString(R.string.finish) + " in " + tournament.title;
				finishTV.setText(finishString);
				finishTV.setVisibility(View.VISIBLE);
				finishGroup.setVisibility(View.VISIBLE);
				previewTV.setVisibility(View.VISIBLE);
				reportTV.setVisibility(View.VISIBLE);

				int boxId = Helpers.getBoxIdFromLabel(tournament.label, getContext());
				boxIV.setImageResource(boxId);

				// if last event started, allow finish
				for (int i = 0; i <= 6; i++) {
					finishButtons[i].setTextColor(started ? Color.BLACK : Color.GRAY);

					if (i > 0 && i <= tournament.prize) {
						finishButtons[i].setTypeface(null, Typeface.BOLD);
					} else {
						finishButtons[i].setTypeface(null, Typeface.ITALIC);
					}
				}
				initialFinish = tournament.finish;
				finishGroup.check(finishIDs[initialFinish]);
			} else {
				hideNonTournamentViews();
			}
		}
	}

	private void startSearchActivity() {
		Intent intent = new Intent(getActivity(), SearchResultActivity.class);
		intent.putExtra("query_title", tournament.title);
		intent.putExtra("query_id", tournament.id);
		startActivity(intent);
	}

	private void openPreviewLink() {
		if (tournament != null && tournament.isTournament) {
			getActivity().startActivity(new Intent(Intent.ACTION_VIEW,
					Uri.parse("http://boardgamers.org/wbc24/previews/" + tournament.label + ".html")));
		}
	}

	private void openReportLink() {
		if (tournament != null && tournament.isTournament) {
			getActivity().startActivity(new Intent(Intent.ACTION_VIEW,
					Uri.parse("http://boardgamers.org/yearbook23/" + tournament.label + ".html")));
		}
	}

	private void hideNonTournamentViews() {
		finishGroup.setVisibility(View.GONE);
		finishTV.setVisibility(View.GONE);
		previewTV.setVisibility(View.INVISIBLE);
		reportTV.setVisibility(View.INVISIBLE);
		boxIV.setImageResource(R.drawable.box_iv_no_image_text);
	}

	private void share() {
		String s = event.title + ": " + noteET.getText().toString();

		Intent sharingIntent = new Intent(Intent.ACTION_SEND);
		sharingIntent.setType("text/plain");

		String subjectString = event.title;
		sharingIntent.putExtra(Intent.EXTRA_SUBJECT, subjectString);
		sharingIntent.putExtra(Intent.EXTRA_TITLE, subjectString);

		String contentString = s + " #WBC";
		sharingIntent.putExtra(Intent.EXTRA_TEXT, contentString);

		startActivity(Intent.createChooser(sharingIntent, "Share via"));

	}

	private void saveEventData() {
		if (event != null) {
			String note = noteET.getText().toString();
			int finish;

			if (tournament != null && tournament.isTournament) {
				finish = 0;
				for (int i = 0; i < finishButtons.length; i++) {
					if (finishButtons[i].isChecked()) {
						finish = i;
						break;
					}
				}
			} else {
				finish = -1;
			}

			if (note.equalsIgnoreCase(event.note) && (tournament == null || finish == tournament.finish))
				return;

			// save data
			WBCDataDbHelper dbHelper = new WBCDataDbHelper(getActivity());
			dbHelper.getWritableDatabase();
			// debug insert
			long resultUE;
			resultUE = dbHelper.insertUserEventData(MainActivity.userId, event.id, event.starred, note);
			Log.d(TAG," resultUE " + resultUE);
			if (finish > -1 && finish != initialFinish) {
				dbHelper.insertUserTournamentData(MainActivity.userId, event.tournamentID, finish);
			}
			Log.d(TAG, "There are currently " + String.valueOf(dbHelper.getNumEvents()) + " events in main DB");
			Log.d(TAG, "There are currently " + dbHelper.getNumUserNoncreatedEvents() + " events in user DB");
			dbHelper.close();

			if (getActivity() instanceof MainActivity) {
				try {
					((MainActivity) getActivity()).changeEventInLists(event);
				} catch (Exception e) {
					Log.d(TAG, e.getMessage());
				}
			}
		}
	}

	@Override
	public void onPause() {
		saveEventData();
		stopOverlayUpdate();

		super.onPause();
	}

	private void setRoom(String room) {
		mapOverlayToggle.setVisibility(View.GONE);
		roomID = -1;
		String[] roomsDownstairs = getResources().getStringArray(R.array.rooms_ski_lodge);
		for (int i = 0; i < roomsDownstairs.length; i++) {
			if (roomsDownstairs[i].equalsIgnoreCase(room)) {
				roomID = i;
				setImageViews();
				return;
			}
		}

		String[] roomsUpstairs = getResources().getStringArray(R.array.rooms_convention_center);
		for (int i = 0; i < roomsUpstairs.length; i++) {
			if (roomsUpstairs[i].equalsIgnoreCase(room)) {
				roomID = 50 + i;
				setImageViews();
				return;
			}
		}
	}

	private void setImageViews() {
		mapOverlayToggle.setVisibility(View.VISIBLE);

		if (roomID >= 50) {
			map.setImageResource(R.drawable.convention_center);
			mapOverlay.setImageResource(Constants.conventionCenterIDs[roomID - 50]);
		} else {
			map.setImageResource(R.drawable.ski_lodge);
			mapOverlay.setImageResource(Constants.skiLodgeIDs[roomID]);
		}
		overlayOn = true;

		startOverlayUpdate();

	}

	private void startOverlayUpdate() {
		if (!runnableActive) {
			mapOverlayToggle.setChecked(true);
			runnableActive = true;
			handler.postDelayed(runnable, 500);
		}
	}

	private void stopOverlayUpdate() {
		if (runnableActive) {
			mapOverlay.setVisibility(View.GONE);
			mapOverlayToggle.setChecked(false);
			runnableActive = false;
			handler.removeCallbacks(runnable);
		}
	}

	private final Runnable runnable = new Runnable() {
		@Override
		public void run() {
			changeMapOverlayVisibity();
		}
	};

	private void changeMapOverlayVisibity() {
		runnableActive = false;
		mapOverlay.setVisibility(overlayOn ? View.GONE : View.VISIBLE);
		overlayOn = !overlayOn;
		startOverlayUpdate();
	}
}
