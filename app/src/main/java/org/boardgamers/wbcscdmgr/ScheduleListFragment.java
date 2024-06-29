package org.boardgamers.wbcscdmgr;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.SectionIndexer;

import java.util.List;

public class ScheduleListFragment extends DefaultListFragment {
	private final String TAG = "Schedule List Fragment";

    public static final int GROUPS_PER_DAY = 18 + 1;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			view.findViewById(R.id.sl_hour).setVisibility(View.GONE);
			view.findViewById(R.id.sl_hour_divider).setVisibility(View.GONE);
		}

		listView.setFastScrollEnabled(true);
		listView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
				Log.d("SLF", "group pos " + String.valueOf(groupPosition) + " clicked");

				if (groupPosition % GROUPS_PER_DAY == 0) {
					if (listView.isGroupExpanded(groupPosition)) {
						collapseGroups(groupPosition, GROUPS_PER_DAY);
					} else {
						expandGroups(groupPosition, GROUPS_PER_DAY);
					}

					return true;
				} else {
					return false;
				}
			}
		});
		new PopulateScheduleAdapterTask(getActivity(), GROUPS_PER_DAY * Constants.TOTAL_DAYS).execute(0, 0, 0);

		return view;
	}

	public void reloadAdapterData() {
		listAdapter = null;

		new PopulateScheduleAdapterTask(getActivity(), GROUPS_PER_DAY * Constants.TOTAL_DAYS).execute(0, 0, 0);
	}

	/**
	 * Get the current group, based on currentDay and currentHour. If hour is between 24
	 * and 7, select group 0 of that day
	 *
	 * @return groupNumber
	 */
	private int getCurrentGroup() {
		int hoursIntoConvention = (int) Helpers.getHoursIntoConvention();

		if (hoursIntoConvention == -1) {
			return 0;
		} else {
			return hoursIntoConvention / 24 * GROUPS_PER_DAY + Math.max(0, hoursIntoConvention % 24 - 6);
		}
	}

	private class PopulateScheduleAdapterTask extends PopulateAdapterTask {
		private PopulateScheduleAdapterTask(Context c, int g) {
			context = c;
			numGroups = g;


		}

		@Override
		protected void onPostExecute(Integer integer) {
			super.onPostExecute(integer);

			listView.setSelectedGroup(getCurrentGroup());
		}

		@Override
		protected Integer doInBackground(Integer... params) {
			listAdapter = new ScheduleListAdapter(context, events, 1);

			WBCDataDbHelper dbHelper = new WBCDataDbHelper(getActivity());
			dbHelper.getReadableDatabase();
			List<Tournament> tournaments = dbHelper.getAllTournaments(MainActivity.userId);
			List<Event> tempEvents = dbHelper.getAllEvents(MainActivity.userId);
			dbHelper.close();

			Event event;
			int group;
			A:
			while (tempEvents.size() > 0) {
				event = tempEvents.remove(0);

				for (Tournament tournament : tournaments) {
					if (tournament.id == event.tournamentID && !tournament.visible) {
						continue A;
					}
				}

				group = event.day * GROUPS_PER_DAY + (int) event.hour - 6;
				if (event.hour < 1) {
					group += 24;
				}

				events.get(group).add(event);
//        if (event.starred) {
//          group=event.day*GROUPS_PER_DAY;
//          events.get(group).add(event);
//        }
			}

			listAdapter.events = events;

			return 1;
		}
	}

	private class ScheduleListAdapter extends DefaultListAdapter implements SectionIndexer {
		// 18 hours per day (0700 thru 2400) plus "My Events"
		private final int GROUP_HOUR_OFFSET = 7 - 1;    // first hour is 7, offset 1 hour for "My Events"
		private final String[] hours;
		private final String[] sections;

		private ScheduleListAdapter(Context c, List<List<Event>> e, int i) {
			super(c, e, i);

			hours = c.getResources().getStringArray(R.array.hours_24);

			sections = dayStrings;
		}

		@Override
		public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild,
								 View view, ViewGroup parent) {
			view = super.getChildView(groupPosition, childPosition, isLastChild, null, parent);
			view.findViewById(R.id.li_hour).setVisibility(View.GONE);

			return view;
		}

		@Override
		public int getGroupViewId(final int groupPosition) {
			if (groupPosition % GROUPS_PER_DAY == 0) {
				return R.layout.list_group_large;
			} else {
				return R.layout.list_group_small;
			}
		}

		@Override
		public String getGroupTitle(final int groupPosition) {
			if (groupPosition % GROUPS_PER_DAY == 0) {
				return dayStrings[groupPosition / GROUPS_PER_DAY];
			} else {
				String groupTitle = hours[(groupPosition % GROUPS_PER_DAY) - 1] + ": ";

			//	Log.d(TAG, "Group title is " + groupTitle);
				int groupHoursIntoConvention = groupPosition / GROUPS_PER_DAY * 24
						+ groupPosition % GROUPS_PER_DAY + GROUP_HOUR_OFFSET;
				for (int i = 0; i <= groupPosition; i++) {
					for (Event event : events.get(i)) {
						if (event.starred
								&& event.day * 24 + event.hour <= groupHoursIntoConvention
								&& event.day * 24 + event.hour + event.duration > groupHoursIntoConvention) {
							groupTitle += event.title + ", ";
						}
					}
				}

			//	Log.d(TAG, "Group title with events is " + groupTitle);
				return groupTitle.substring(0, groupTitle.length() - 2);
			}
		}

		@Override
		public int getPositionForSection(int sectionIndex) {
			return sectionIndex * GROUPS_PER_DAY;
		}

		public int getSectionForPosition(int position) {
			return position / GROUPS_PER_DAY;
		}

		public Object[] getSections() {
			return sections;
		}

		@Override
		public void updateEvent(Event updatedEvent) {
			int group = updatedEvent.day * GROUPS_PER_DAY + (int) updatedEvent.hour - GROUP_HOUR_OFFSET;
			Event tempEvent;

			boolean isInList = false;
			for (int i = 0; i < events.get(group).size(); i++) {
				tempEvent = events.get(group).get(i);
				if (tempEvent.id == updatedEvent.id) {
					tempEvent.starred = updatedEvent.starred;
					isInList = true;
					break;
				}
			}

			if (!isInList) {
				events.get(group).add(0, updatedEvent);
			}
		}

		@Override
		public void removeEvent(Event deletedEvent) {
			int group;
			group = deletedEvent.day * GROUPS_PER_DAY + (int) deletedEvent.hour - GROUP_HOUR_OFFSET;
			for (int i = 0; i < events.get(group).size(); i++) {
				if (events.get(group).get(i).id == deletedEvent.id) {
					events.get(group).remove(i);
					break;
				}
			}

			group = deletedEvent.day * GROUPS_PER_DAY;
			for (int i = 0; i < events.get(group).size(); i++) {
				if (events.get(group).get(i).id == deletedEvent.id) {
					events.get(group).remove(i);
					break;
				}
			}
		}
	}

}
