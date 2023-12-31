package org.boardgamers.wbcscdmgr;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.List;

public class DialogPreferenceSchedulePicker extends DialogPreference {
	private RadioGroup group;
	private long value;

	public DialogPreferenceSchedulePicker(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected View onCreateDialogView() {
		View view = View.inflate(getContext(), R.layout.dialog_radio, null);

		group = view.findViewById(R.id.dialog_radio_group);

		WBCDataDbHelper dbHelper = new WBCDataDbHelper(getContext());
		dbHelper.getReadableDatabase();

		List<User> users = dbHelper.getUsers(null);
		dbHelper.close();

		RadioButton button;
		for (int i = 0; i < users.size(); i++) {
			button = new RadioButton(getContext());
			button.setId((int) users.get(i).id);
			button.setText(users.get(i).name);
			//button.setTextSize(textSize);
			group.addView(button);
		}

		group.check((int) MainActivity.userId);

		return view;
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
		if (restorePersistedValue) {
			value = getPersistedLong(MainActivity.userId);
		} else {
			value = (Long) defaultValue;
			if (persistLong(value)) {
				Log.d("", "Success");
			}
		}
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getInteger(index, (int) MainActivity.userId);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			value = group.getCheckedRadioButtonId();
			persistLong(value);

			MainActivity.userId = value;
//			SettingsActivity.SettingsFragment.updatePreferences();
		}
	}
}
