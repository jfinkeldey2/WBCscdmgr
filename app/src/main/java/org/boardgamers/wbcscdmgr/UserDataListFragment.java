package org.boardgamers.wbcscdmgr;

import android.content.Context;
import android.content.DialogInterface;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment containing user's WBC data, including tournament finishes, event notes, and created
 * events
 */
public class UserDataListFragment extends DefaultListFragment {
	private final String TAG="My WBC Data Activity";
	// Note - above commented out in original

	public static final int EVENTS_INDEX = 0;
	public static final int NOTES_INDEX = 1;
	public static final int FINISHES_INDEX = 2;

	private String[] finishStrings;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		Log.d(TAG, " View in onCreateView " + view);

		if (view != null) {

			Button addEvent = view.findViewById(R.id.add_event);
			addEvent.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showCreateDialog();
				}
			});

			addEvent.setEnabled(false);

			Button deleteAll = view.findViewById(R.id.delete_all);
			deleteAll.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showDeleteDialog();
				}
			});
		}

		finishStrings = getResources().getStringArray(R.array.finish_strings);
		Log.d(TAG, "in dbhelper on create " + R.array.finish_strings);

		new PopulateUserDataAdapterTask(getActivity(), 3).execute(0, 0, 0);
		return view;
	}

	public void updateUserEventData(Event e, int finish) {
		if (listAdapter == null || listAdapter.events == null) {
			return;
		}

		boolean noteInList = false;
		for (Event event : listAdapter.events.get(NOTES_INDEX)) {
			if (e.id == event.id) {
				noteInList = true;
				if (e.note.equalsIgnoreCase("") || e.note.isEmpty()) {
					listAdapter.events.get(NOTES_INDEX).remove(event);
				} else {
					event.title = event.title.substring(0, event.title.length() - event.note.length());
					event.note = e.note;
					event.title += e.note;
				}
				//below added 6/27
				listAdapter.notifyDataSetChanged();

				break;
			}
		}

		boolean finishInList = false;
		for (Event event : listAdapter.events.get(FINISHES_INDEX)) {
			if (event.tournamentID == e.tournamentID) {
				finishInList = true;
				if (finish == 0) {
					listAdapter.events.get(FINISHES_INDEX).remove(event);
				} else {
					event.title = event.title.substring(0, event.title.length() - 3);
					event.note = String.valueOf(finish);
					event.title += finishStrings[finish - 1];
				}
				break;
			}
		}

		WBCDataDbHelper dbHelper = new WBCDataDbHelper(getActivity());
		dbHelper.getReadableDatabase();

		if (!noteInList && e.note != null && e.note.equalsIgnoreCase("")) {
			Event eventNote = dbHelper.getEvent(MainActivity.userId, e.id);
			eventNote.title += ": " + eventNote.note;

			int noteIndex;
			List<Event> notesSearchList = listAdapter.events.get(NOTES_INDEX);
			noteIndex = 0;
			for (; noteIndex < notesSearchList.size(); noteIndex++) {
				if (eventNote.title.compareToIgnoreCase(notesSearchList.get(noteIndex).title) == 1) {
					break;
				}
			}
			notesSearchList.add(noteIndex, eventNote);
		}

		if (!finishInList && finish > 0) {
			Tournament tournamentFinish = dbHelper.getTournament(MainActivity.userId, e.tournamentID);
			//Log.d(TAG, "finish debug tournamentID " + e.tournamentID + " tournamentFinishID " + tournamentFinish.id);
			//Event eventFinish = dbHelper.getFinalEvent(MainActivity.userId, tournamentFinish.id);
			//Above updated to select correct tournament   6/26/2023
			Event eventFinish = dbHelper.getEvent(MainActivity.userId, e.id);

			eventFinish.title += ": " + finishStrings[tournamentFinish.finish - 1];
			eventFinish.note = String.valueOf(tournamentFinish.finish);

			List<Event> finishesSearchList = listAdapter.events.get(FINISHES_INDEX);
			int finishIndex = 0;
			//Log.d(TAG,"indexes " + finishIndex + " " + finishesSearchList.size());
			for (; finishIndex < finishesSearchList.size(); finishIndex++) {
			//	Log.d(TAG, "for list in add finish tf " + tournamentFinish.title + " fs " + finishesSearchList.get(finishIndex).title);
				if (tournamentFinish.title.compareToIgnoreCase(finishesSearchList.get(finishIndex).title) ==
						1) {
					break;
				}
			}
			finishesSearchList.add(finishIndex, eventFinish);
		}

		dbHelper.close();
	}

	@Override
	protected int getLayoutId() {
		return R.layout.user_data;
	}

	//  public void editEvent(int index) {
	//    MainActivity.selectedEventId=index;
	//    DialogEditEvent editNameDialog=new DialogEditEvent();
	//    editNameDialog.show(getFragmentManager(), "edit_event_dialog");
	//  }

	private void showCreateDialog() {
		DialogCreateEvent dialog = new DialogCreateEvent();
		dialog.setTargetFragment(this, 0);
		dialog.show(getFragmentManager(), "create_event_dialog");
	}

	private void showDeleteDialog() {
		String title = "Confirm";
		String message = "Are you sure you want to delete all your events";

		AlertDialog.Builder deleteDialog = new AlertDialog.Builder(getActivity());
		deleteDialog.setTitle(title).setMessage(message)
				.setNegativeButton(getResources().getString(R.string.cancel),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.dismiss();
							}
						}).setPositiveButton("Yes, " + getResources().getString(R.string.delete),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						deleteAllEvents();
						dialog.dismiss();
					}
				});
		deleteDialog.create().show();

	}

	private void deleteAllEvents() {
		WBCDataDbHelper dbHelper = new WBCDataDbHelper(getActivity());
		dbHelper.getWritableDatabase();
		dbHelper.deleteAllUserEvents(MainActivity.userId);
		dbHelper.close();

		int count = listAdapter.events.get(EVENTS_INDEX).size();
		Event[] events = new Event[count];
		while (count > 0) {
			events[count - 1] = listAdapter.events.get(EVENTS_INDEX).remove(count - 1);
			count--;
		}
		refreshAdapter();
		deleteEvents(events);
	}

	private void deleteEvents(Event[] events) {
		for (Event event : events) {
			((MainActivity) getActivity()).removeEvent(event);

		}

	}

	private class PopulateUserDataAdapterTask extends PopulateAdapterTask {
		private PopulateUserDataAdapterTask(Context c, int g) {
			context = c;
			numGroups = g;
		}

		@Override
		protected Integer doInBackground(Integer... params) {
			listAdapter = new UserDataListAdapter(context, events, 2);

			WBCDataDbHelper dbHelper = new WBCDataDbHelper(getActivity());
			dbHelper.getReadableDatabase();

			events = new ArrayList<>();
			events.add(dbHelper.getUserEvents(MainActivity.userId));
			/*experiment 6/27
			List<Event> eventjf = dbHelper.getUserEvents(MainActivity.userId);

			for (Event event : eventjf) {
				event.title += " jf";
			}
			events.add(eventjf);
			end experiment
			*/

			List<Event> eventNotes = dbHelper.getEventsWithNotes(MainActivity.userId);
			List<Tournament> tournamentFinishes = dbHelper.getTournamentsWithFinishes(MainActivity.userId);

			for (Event event : eventNotes) {
				event.title += ": " + event.note;
			}
			events.add(eventNotes);

			List<Event> eventFinishes = new ArrayList<>();
			long temptourney;
			for (Tournament tournament : tournamentFinishes) {
				Log.d(TAG, "pop userid " + MainActivity.userId + " tourney id " + tournament.id);
				temptourney = dbHelper.getTournamentID(tournament.title);
				Tournament tournamentFinish = dbHelper.getTournament(MainActivity.userId, temptourney);
				Log.d(TAG, "temptourney " + temptourney);
				Event eventFinish = dbHelper.getFinalEvent(MainActivity.userId, temptourney);
				Log.d(TAG, "in populateuserdataadapter " +  tournamentFinish.finish);

				if (eventFinish != null) {
					eventFinish.title += ": " + finishStrings[tournamentFinish.finish - 1];
					eventFinish.note = String.valueOf(tournamentFinish.finish);

					eventFinishes.add(eventFinish);

				}
			}

			dbHelper.close();

			events.add(eventFinishes);

			listAdapter.events = events;

			return 1;
		}
	}

	private class UserDataListAdapter extends DefaultListAdapter {
		private UserDataListAdapter(Context c, List<List<Event>> e, int i) {
			super(c, e, i);
		}

		@Override
		public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild,
								 View view, ViewGroup parent) {
			view = super.getChildView(groupPosition, childPosition, isLastChild, null, parent);

			Event event = events.get(groupPosition).get(childPosition);

			if (groupPosition == UserDataListFragment.EVENTS_INDEX) {
				TextView hourTV = view.findViewById(R.id.li_hour);
				String hourString = dayStrings[event.day] + "\n" + String.valueOf(event.hour * 100);
				hourTV.setText(hourString);

				view.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View view) {
						showDeleteDialog(childPosition);
						return true;
					}
				});
			} else {
				view.findViewById(R.id.li_star).setVisibility(View.INVISIBLE);
				view.findViewById(R.id.li_hour).setVisibility(View.GONE);
				view.findViewById(R.id.li_duration).setVisibility(View.GONE);
				view.findViewById(R.id.li_location).setVisibility(View.GONE);
			}
			return view;
		}

		@Override
		public String getGroupTitle(int groupPosition) {
			switch (groupPosition) {
				case UserDataListFragment.EVENTS_INDEX:
					return getResources().getString(R.string.user_events);
				case UserDataListFragment.NOTES_INDEX:
					return getResources().getString(R.string.user_notes);
				case UserDataListFragment.FINISHES_INDEX:
					return getResources().getString(R.string.user_finishes);
				default:
					return null;
			}
		}

		@Override
		public void updateEvent(Event updatedEvent) {
			boolean inList;
			Event tempEvent;

			if (updatedEvent.tournamentID == 0) {
				return;
			}

			inList = false;
			for (int i = 0; i < events.get(UserDataListFragment.EVENTS_INDEX).size(); i++) {
				tempEvent = events.get(UserDataListFragment.EVENTS_INDEX).get(i);
				if (tempEvent.id == updatedEvent.id) {
					tempEvent.starred = updatedEvent.starred;
					inList = true;
					break;
				}
			}

			if (!inList) {
				int index;
				for (index = 0; index < events.get(UserDataListFragment.EVENTS_INDEX).size(); index++) {
					tempEvent = events.get(0).get(index);
					if (tempEvent.day * 24 + tempEvent.hour > updatedEvent.day * 24 + updatedEvent.hour ||
							(tempEvent.day * 24 + tempEvent.hour == updatedEvent.day * 24 + updatedEvent.hour &&
									tempEvent.title.compareToIgnoreCase(updatedEvent.title) == -1)) {
						tempEvent.starred = updatedEvent.starred;
						break;
					}
				}
				events.get(UserDataListFragment.EVENTS_INDEX).add(index, updatedEvent);
			}
		}
	}

	private void showDeleteDialog(final int index) {
		Event event = listAdapter.events.get(UserDataListFragment.EVENTS_INDEX).get(index);

		String dayString = getResources().getStringArray(R.array.days)[event.day];
		String title = "Confirm";
		String message = "Are you sure you want to delete " + event.title + " on " + dayString + " at " +
				String.valueOf(event.hour) + "?";

		android.app.AlertDialog.Builder deleteDialog = new android.app.AlertDialog.Builder(getActivity());
		deleteDialog.setTitle(title).setMessage(message)
				.setNegativeButton(getResources().getString(R.string.cancel),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.dismiss();
							}
						}).setPositiveButton("Yes, " + getResources().getString(R.string.delete),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						deleteEvent(index);
						dialog.dismiss();
					}
				});
		deleteDialog.create().show();

	}

	private void deleteEvent(int index) {
		WBCDataDbHelper dbHelper = new WBCDataDbHelper(getActivity());
		dbHelper.getWritableDatabase();
		dbHelper.deleteUserEvent(listAdapter.events.get(UserDataListFragment.EVENTS_INDEX).get(index).id);
		dbHelper.close();

		Event[] deletedEvents = new Event[]{listAdapter.events.get(UserDataListFragment.EVENTS_INDEX).remove(index)};
		listAdapter.notifyDataSetChanged();
		deleteEvents(deletedEvents);
	}

}
