package eu.thedarken.fms;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	private SharedPreferences settings;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		try {
			addPreferencesFromResource(R.xml.preferences);
		} catch (Exception e) {
			settings.edit().clear().commit();
			Log.d(getPackageName(), "Settings were corrupt and have been reset!");
			addPreferencesFromResource(R.xml.preferences);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		settings.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		settings.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, final Preference preference) {

		return false;
	}
}
