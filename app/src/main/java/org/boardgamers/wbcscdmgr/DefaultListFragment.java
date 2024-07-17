package org.boardgamers.wbcscdmgr;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Kevin
 */
public class DefaultListFragment extends Fragment {
	//private final String TAG="Default List Fragment";

	protected DefaultListAdapter listAdapter;
	protected ExpandableListView listView;
	private long hoursIntoConvention;

	private boolean allStarred;
	private ImageView star;
	private List<Event> changedEvents;
	private ProgressDialog dialog;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View view = inflater.inflate(getLayoutId(), container, false);

		listView = view.findViewById(R.id.default_list_view);

		star = view.findViewById(R.id.sl_star);
//	Commenting out the below disables clicking the star that changes all displayed
//  events which lead to crashes.  JLF 7/16/24
//		star.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				showSaveDialog();
//			}
//		});

		hoursIntoConvention = Helpers.getHoursIntoConvention();

		return view;
	}

	private void showSaveDialog() {
		changedEvents = listAdapter.getChangedEvents(!allStarred);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle("Are you sure?").setMessage(
				"Do you really want to change the star for " + String.valueOf(changedEvents.size()) +
						" events?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				new SaveEventData().execute();
			}
		}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		}).setCancelable(true);

		builder.create().show();
	}

	/**
	 * set tournamentEventsStarIV image resource
	 */
	private void setGameStarIV() {
		star.setImageResource(allStarred ? R.drawable.star_on : R.drawable.star_off);
	}

	/**
	 * Event star changed - check for change in allStarred boolean and set game star image view. Call
	 * setGameStar before return
	 */
	void setAllStarred() {
		allStarred = true;
		for (List<Event> events : listAdapter.events) {
			for (Event event : events) {
				if (!event.starred) {
					allStarred = false;
					setGameStarIV();
					return;
				}
			}
		}
		setGameStarIV();
	}

	void expandGroups(int group, int count) {
		for (int i = group; i < group + count; i++) {
			listView.expandGroup(i);
		}
	}

	void collapseGroups(int group, int count) {
		for (int i = group; i < group + count; i++) {
			listView.collapseGroup(i);
		}
	}

	int getLayoutId() {
		return R.layout.default_list;
	}

	public void updateEvent(Event event) {
		if (listAdapter != null) {
			listAdapter.updateEvent(event);
			listAdapter.notifyDataSetChanged();

			setAllStarred();
		}
	}

	public void removeEvent(Event event) {
		if (listAdapter != null) {
			listAdapter.removeEvent(event);
			listAdapter.notifyDataSetChanged();

			setAllStarred();
		}
	}

	public void reloadAdapterData() {

	}

	class PopulateAdapterTask extends AsyncTask<Integer, Integer, Integer> {
		Context context;
		List<List<Event>> events;
		int numGroups;


		@Override
		protected void onPostExecute(Integer integer) {
			listView.setAdapter(listAdapter);

			for (int i = 0; i < listAdapter.getGroupCount(); i++) {
				listView.expandGroup(i);
			}

			listAdapter.events = events;

			refreshAdapter();

			setAllStarred();

			super.onPostExecute(integer);
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			events = new ArrayList<>();
			for (int i = 0; i < numGroups; i++) {
				events.add(new ArrayList<Event>());
			}
		}

		@Override
		protected Integer doInBackground(Integer... params) {
			return -1;
		}
	}

	private class SaveEventData extends AsyncTask<Integer, Integer, Void> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			dialog = new ProgressDialog(getActivity());
			dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			dialog.setCancelable(false);
			dialog.setTitle("Saving, please wait...");
			dialog.show();

			allStarred = !allStarred;
			setGameStarIV();

			for (Event event : changedEvents) {
				event.starred = !event.starred;
			}
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			if (dialog.isShowing()) {
				dialog.dismiss();
			}

			refreshAdapter();
			super.onPostExecute(aVoid);
		}

		@Override
		protected Void doInBackground(Integer... params) {
			// save events in DB
			WBCDataDbHelper dbHelper = new WBCDataDbHelper(getActivity());
			dbHelper.getWritableDatabase();
			dbHelper.insertUserEventData(MainActivity.userId, changedEvents);
			dbHelper.close();

			for (Event changedEvent : changedEvents) {
				if (getActivity() instanceof MainActivity) {
					((MainActivity) getActivity()).changeEventInLists(changedEvent);
				}
			}


			return null;
		}
	}

	@Override
	public void onPause() {
		if (dialog != null && dialog.isShowing()) {
			dialog.dismiss();
		}
		super.onPause();
	}


	@Override
	public void onResume() {
		super.onResume();
		refreshAdapter();
	}

	protected void refreshAdapter() {
		hoursIntoConvention = Helpers.getHoursIntoConvention();
		if (listAdapter != null) {
			listAdapter.notifyDataSetChanged();
		}
	}

	class DefaultListAdapter extends BaseExpandableListAdapter {
		//private final String TAG="Default Adapter";

		private final int COLOR_JUNIOR;
		private final int COLOR_SEMINAR;
		private final int COLOR_QUALIFY;
		private final int COLOR_OPEN_TOURNAMENT;
		private final int COLOR_NON_TOURNAMENT;

		final LayoutInflater inflater;
		final String[] dayStrings;
		private final int id;


		List<List<Event>> events;

		DefaultListAdapter(Context c, List<List<Event>> e, int i) {
			inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			events = e;
			id = i;

			dayStrings = c.getResources().getStringArray(R.array.days);

			// get resources
			COLOR_JUNIOR = ContextCompat.getColor(getActivity(), R.color.junior);
			COLOR_SEMINAR = ContextCompat.getColor(getActivity(), R.color.seminar);
			COLOR_QUALIFY = ContextCompat.getColor(getActivity(), R.color.qualify);
			COLOR_NON_TOURNAMENT = ContextCompat.getColor(getActivity(), R.color.non_tournament);
			COLOR_OPEN_TOURNAMENT = ContextCompat.getColor(getActivity(), R.color.open_tournament);
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View view,
								 ViewGroup parent) {

			final Event event = getChild(groupPosition, childPosition);

			if (view == null) {
				view = inflater.inflate(R.layout.list_item, parent, false);
			} else {
				if (this.id == 0) {
					Log.d("LIST", "View for event " + event.title + " is not null");
				}
			}

			int tColor = getTextColor(event);
			int tType = getTextStyle(event);

			TextView title = view.findViewById(R.id.li_title);
			title.setText(event.title);
			title.setTypeface(null, tType);
			title.setTextColor(tColor);

			if (event.title.contains("Junior")) {
				title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.junior_icon, 0, 0, 0);
			} else {
				title.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}

			TextView hour = view.findViewById(R.id.li_hour);
			hour.setText(MainActivity.getDisplayHour(event.hour, 0));
			hour.setTypeface(null, tType);
			hour.setTextColor(tColor);

			TextView duration = view.findViewById(R.id.li_duration);
			duration.setText(String.format(Locale.US, "%.2f", event.duration));
			duration.setTypeface(null, tType);
			duration.setTextColor(tColor);

			if (event.continuous) {
				duration.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.continuous_icon, 0);
			} else {
				duration.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}

			TextView location = view.findViewById(R.id.li_location);
			location.setText(event.location);
			location.setTypeface(null, tType);
			location.setTextColor(tColor);

			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					selectEvent(event);
				}
			});

			ImageView starIV = view.findViewById(R.id.li_star);
			starIV.setImageResource(event.starred ? R.drawable.star_on : R.drawable.star_off);
			starIV.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					changeEventStar(event);
				}
			});

			boolean started = event.day * 24 + event.hour <= hoursIntoConvention;
			boolean ended = event.day * 24 + event.hour + event.duration <= hoursIntoConvention;
			boolean happening = started && !ended;

			if (event.id == MainActivity.selectedEventId) {
				view.setBackgroundResource(R.drawable.selected);
			} else if (childPosition % 2 == 0) {
				if (ended) {
					view.setBackgroundResource(R.drawable.ended_light);
				} else if (happening) {
					view.setBackgroundResource(R.drawable.current_light);
				} else {
					view.setBackgroundResource(R.drawable.future_light);
				}
			} else {
				if (ended) {
					view.setBackgroundResource(R.drawable.ended_dark);
				} else if (happening) {
					view.setBackgroundResource(R.drawable.current_dark);
				} else {
					view.setBackgroundResource(R.drawable.future_dark);
				}
			}
			return view;
		}

		void selectEvent(Event event) {
			setSelectedEvent(event.id);
			notifyDataSetChanged();
		}

		@Override
		public View getGroupView(final int groupPosition, final boolean isExpanded, View view,
								 final ViewGroup parent) {
			view = inflater.inflate(getGroupViewId(groupPosition), parent, false);

			TextView name = view.findViewById(R.id.sg_name);
			name.setText(getGroupTitle(groupPosition));
			name.setSelected(true);

			if (isExpanded) {
				view.setBackgroundResource(R.drawable.group_expanded);
			} else {
				view.setBackgroundResource(R.drawable.group_collapsed);
			}

			return view;
		}

		public int getGroupViewId(int groupPosition) {
			return R.layout.list_group_large;
		}

		public String getGroupTitle(int groupPosition) {
			return null;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return getGroupId(groupPosition) + 1000 + events.get(groupPosition).get(childPosition).id;
		}

		@Override
		public long getGroupId(int groupPosition) {
			return id * 500000 + groupPosition * 5000;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		/**
		 * @param event - event needed for format, title, class, and qualify
		 * @return integer value of color
		 */
		int getTextColor(Event event) {
			if (event.qualify) {
				return COLOR_QUALIFY;
			} else if (event.title.contains("Junior")) {
				return COLOR_JUNIOR;
			} else if (event.format.equalsIgnoreCase("Seminar")) {
				return COLOR_SEMINAR;
			} else if (event.format.equalsIgnoreCase("SOG") || event.format.equalsIgnoreCase("MP Game") ||
					event.title.indexOf("Open Gaming") == 0) {
				return COLOR_OPEN_TOURNAMENT;
			} else if (event.eClass.length() == 0) {
				return COLOR_NON_TOURNAMENT;
			} else {
				return Color.BLACK;
			}
		}

		/**
		 * @param event - event needed for format, title, class, and qualify
		 * @return integer value of typeface
		 */
		int getTextStyle(Event event) {
			if (event.qualify) {
				return Typeface.BOLD;
			} else if (event.title.contains("Junior")) {
				return Typeface.NORMAL;
			} else if (event.eClass.length() == 0 || event.format.equalsIgnoreCase("Demo")) {
				return Typeface.ITALIC;
			} else {
				return Typeface.NORMAL;
			}
		}

		@Override
		public Event getChild(int groupPosition, int childPosition) {
			return events.get(groupPosition).get(childPosition);
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return getGroup(groupPosition).size();
		}

		@Override
		public List<Event> getGroup(int groupPosition) {
			return events.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return events.size();
		}

		void changeEventStar(Event event) {
			event.starred = !event.starred;

			updateEvent(event);
			notifyDataSetChanged();

			setAllStarred();

			// TODO what if not main activity (tournament list?)
			if (getActivity() instanceof MainActivity) {
				((MainActivity) getActivity()).changeEventInLists(event);
			}

			saveEvent(event);
		}

		void updateEvent(Event event) {
		}

		void removeEvent(Event deletedEvent) {
		}

		List<Event> getChangedEvents(boolean changedStar) {
			List<Event> changedEvents = new ArrayList<>();

			for (int i = 0; i < events.size(); i++) {
				if ((id == 1 && i % ScheduleListFragment.GROUPS_PER_DAY == 0) ||
						(id == 2 && i != UserDataListFragment.EVENTS_INDEX)) {
					continue;
				}

				for (Event event : events.get(i)) {
					if (event.starred ^ changedStar) {
						changedEvents.add(event);
					}
				}
			}

			return changedEvents;
		}
	}

	private void saveEvent(Event event) {
		WBCDataDbHelper dbHelper = new WBCDataDbHelper(getActivity());
		dbHelper.getWritableDatabase();
		dbHelper.insertUserEventData(MainActivity.userId, event.id, event.starred, event.note);
		dbHelper.close();
	}

	private void setSelectedEvent(long id) {
		EventFragment eventFragment = (EventFragment) getFragmentManager()
				.findFragmentById(R.id.eventFragment);
		if (eventFragment != null) {
			// don't call setEvent if it's the same event
			if (MainActivity.selectedEventId != id) {
				eventFragment.setEvent(id);
				MainActivity.selectedEventId = id;
			}
		} else {
			MainActivity.selectedEventId = id;
			Intent intent = new Intent(getActivity(), EventActivity.class);
			getActivity().startActivity(intent);
		}
	}
}
