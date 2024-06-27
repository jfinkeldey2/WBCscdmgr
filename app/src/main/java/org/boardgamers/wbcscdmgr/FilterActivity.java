package org.boardgamers.wbcscdmgr;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class FilterActivity extends AppCompatActivity {
	private final String TAG = "Filter Activity";

	private final int SECTION_TOURNAMENT_ID = 1000;

	private List<Tournament> tournaments;
	private List<Tournament> changedTournaments;

	private Character[] sections;
	private Integer[] sectionIndices;

	private ProgressDialog dialog;
	private WBCDataDbHelper dbHelper;

	private FilterListAdapter listAdapter;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.filter);

		dialog = new ProgressDialog(this);
		dialog.setCancelable(false);
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.setTitle("Saving, please wait...");

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayShowHomeEnabled(true);
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

		dbHelper = new WBCDataDbHelper(this);
		dbHelper.getWritableDatabase();
		tournaments = dbHelper.getAllTournaments(MainActivity.userId);
		dbHelper.close();

		List<Character> sectionsList = new ArrayList<>();
		List<Integer> sectionIndicesList = new ArrayList<>();

		// add first char so list is not empty
		Character letter = tournaments.get(0).title.charAt(0);
		sectionsList.add(letter);
		sectionIndicesList.add(0);
		tournaments.add(0, new Tournament(SECTION_TOURNAMENT_ID, "" + letter, "", false, 0, ""));

		int numSections = 1;
		for (int i = 1; i < tournaments.size(); i++) {
			letter = tournaments.get(i).title.charAt(0);

			if (sectionsList.get(numSections - 1) != letter) {
				sectionsList.add(letter);
				sectionIndicesList.add(i);
				tournaments.add(i,
						new Tournament(SECTION_TOURNAMENT_ID + numSections, "" + letter, "", false, 0, ""));

				numSections++;
				i++;
			}
		}

		sections = new Character[sectionsList.size()];
		sectionsList.toArray(sections);

		sectionIndices = new Integer[sectionIndicesList.size()];
		sectionIndicesList.toArray(sectionIndices);

		ListView listView = findViewById(R.id.filter_list_view);

		//listView.setFastScrollEnabled(true);
		listAdapter = new FilterListAdapter();

		listView.setAdapter(listAdapter);

		FilterSideSelector sideSelector = findViewById(R.id.filter_side_selector);
		sideSelector.setListView(listView);

		// select all image button
		Button selectAll = findViewById(R.id.filter_select_all);
		selectAll.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				selectAll();
			}
		});

		// select non tournament image button
		Button selectTournaments = findViewById(R.id.filter_select_tournament);
		selectTournaments.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				selectTournaments();
			}
		});

		// deselect all image button
		Button deselectAll = findViewById(R.id.filter_deselect_all);
		deselectAll.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				deselectAll();
			}
		});

		Button deselectTournaments = findViewById(R.id.filter_deselect_tournament);
		deselectTournaments.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				deselectTournaments();
			}
		});
	}

	@Override
	protected void onResume() {
		dbHelper.getWritableDatabase();
		super.onResume();
	}

	private void selectAll() {
		changedTournaments = new ArrayList<>();

		for (Tournament tournament : tournaments) {
			if (!tournament.visible) {
				tournament.visible = true;
				changedTournaments.add(tournament);
			}
		}

		listAdapter.notifyDataSetChanged();
		save();
	}

	private void selectTournaments() {
		changedTournaments = new ArrayList<>();

		for (Tournament tournament : tournaments) {
			if (tournament.isTournament && !tournament.visible) {
				tournament.visible = true;
				changedTournaments.add(tournament);
			}
		}

		listAdapter.notifyDataSetChanged();
		save();
	}

	private void deselectAll() {
		changedTournaments = new ArrayList<>();

		for (Tournament tournament : tournaments) {
			if (tournament.visible) {
				tournament.visible = false;
				changedTournaments.add(tournament);
				}
		}

		listAdapter.notifyDataSetChanged();
		save();
	}

	private void deselectTournaments() {
		changedTournaments = new ArrayList<>();

		for (Tournament tournament : tournaments) {
			if (tournament.isTournament && tournament.visible) {
				tournament.visible = false;
				changedTournaments.add(tournament);
			}
		}

		listAdapter.notifyDataSetChanged();
		save();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_filter, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		if (id == android.R.id.home) {
			finish();
			return true;
		} else if (id == R.id.filter_select_all) {
			selectAll();
			return true;
		} else if (id == R.id.filter_select_tournament) {
			selectTournaments();
			return true;
		} else if (id == R.id.filter_deselect_all) {
			deselectAll();
			return true;
		} else if (id == R.id.filter_deselect_tournament) {
			deselectTournaments();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onPause() {
		dbHelper.close();

		super.onPause();
	}

	private void save() {
		new SaveTask().execute();
	}

	private class SaveTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected void onPreExecute() {
			if (changedTournaments.size() > 1) {
				dialog.show();
			}
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(Void result) {
			if (changedTournaments.size() > 1) {
				dialog.dismiss();
			}
			super.onPostExecute(result);
		}

		@Override
		protected Void doInBackground(Void... params) {
			dbHelper.updateTournamentsVisible(changedTournaments);
			Log.d(TAG, "doInBackground changed");
			return null;
		}
	}

	public class FilterListAdapter extends BaseAdapter implements SectionIndexer {
		private final LayoutInflater inflater;

		public FilterListAdapter() {
			inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return tournaments.size();
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public Object getItem(int position) {
			return tournaments.get(position);
		}

		@Override
		public View getView(final int position, View view, ViewGroup parent) {
			int sectionIndex = -1;
			for (int i = 0; i < sectionIndices.length; i++) {
				if (sectionIndices[i] == position) {
					sectionIndex = i;
					break;
				}
			}

			if (sectionIndex == -1) {
				view = inflater.inflate(R.layout.filter_list_item, parent, false);
				Tournament tournament = tournaments.get(position);

				TextView titleTV = view.findViewById(R.id.filter_title);
				titleTV.setText(tournament.title);

				TextView labelTV = view.findViewById(R.id.filter_label);
				labelTV.setText(tournament.label);

				ImageView boxIV = view.findViewById(R.id.filter_image_view);
				boxIV.setImageResource(Helpers.getBoxIdFromLabel(tournament.label, FilterActivity.this));

				final CheckBox checkBox = view.findViewById(R.id.filter_checkbox);
				checkBox.setChecked(tournament.visible);
				checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						tournaments.get(position).visible = isChecked;

						changedTournaments = new ArrayList<>();
						changedTournaments.add(tournaments.get(position));
						save();
						// added to fix filtering 6/22/24 JLF
						Log.d(TAG, "checkbox changed for tournament " + tournaments.get(position) + String.valueOf(tournaments.get(position).label) + String.valueOf(tournaments.get(position).id) + tournaments.get(position).visible);
						//listAdapter.notifyDataSetChanged();
					}
				});

				view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						checkBox.setChecked(!checkBox.isChecked());
					}
				});
			} else {
				view = inflater.inflate(R.layout.list_group_small, parent, false);
				String letter = sections[sectionIndex].toString();

				TextView textView = view.findViewById(R.id.sg_name);
				textView.setText(letter);

				view.setBackgroundResource(R.drawable.group_collapsed);
			}

			return view;
		}

		public Object[] getSections() {
			return sections;
		}

		public int getPositionForSection(int i) {
			int fixedIndex = Math.max(Math.min(sectionIndices.length - 1, i), 0);
			return sectionIndices[fixedIndex];
		}

		public int getSectionForPosition(int position) {
			for (int i = 0; i < sectionIndices.length; i++) {
				if (sectionIndices[i] == position) {
					return i;
				}
			}
			return 0;
		}
	}

}
