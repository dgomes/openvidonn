/*
Copyright 2014 Diogo Gomes <diogogomes@gmail.com>

This file is part of OpenVidonn.

OpenVidonn is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

OpenVidonn is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with Foobar. If not, see http://www.gnu.org/licenses/.
*/

package com.diogogomes.openvidonn.app;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import android.widget.Toast;

import org.bostonandroid.datepreference.DatePreference;
import com.diogogomes.openvidonn.app.model.PersonalInfo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity {
    public static final String TAG = SettingsActivity.class.getSimpleName();

    public static final String GENDER = "gender";
    public static final String HEIGHT = "height";
    public static final String WEIGHT = "weight";
    public static final String BIRTHDAY = "birthday";
    public static final String STEPS_GOAL = "steps_goal";

    private BluetoothLeService mBluetoothLeService = null;

    private GeneralPreferenceFragment general;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        general = new GeneralPreferenceFragment();
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, general)
                .commit();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        getApplicationContext().bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            // TODO: If Settings has multiple levels, Up should navigate up
            // that hierarchy.
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        getApplicationContext().registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        getApplicationContext().unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        PersonalInfo pinfo = new PersonalInfo(
                settings.getString(HEIGHT, "0"),
                settings.getString(WEIGHT, "0"),
                settings.getString(GENDER, getString(R.string.male)).equalsIgnoreCase(getString(R.string.male)) ? PersonalInfo.GENDER.MALE : PersonalInfo.GENDER.FEMALE,
                settings.getString(BIRTHDAY, "1970.01.01")
        );

        if(mBluetoothLeService.isConnected())
            mBluetoothLeService.writePersonalInformation(pinfo);
        else {
            Toast.makeText(getApplicationContext(), "Warning! Not connected to a bracelet!", Toast.LENGTH_LONG).show();
        }

        getApplicationContext().unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            Log.d(TAG, "onPreferenceChange(" + preference.toString() + ", " + value.toString() + ")");
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);

            } else if(preference instanceof DatePreference) {
                DatePreference date = (DatePreference) preference;

                String d = DatePreference.summaryFormatter().format(date.getDate().getTime());
                date.setSummary(d);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }

            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
    }

    @Override
    protected boolean isValidFragment (String fragmentName)
    {
        if(GeneralPreferenceFragment.class.getName().equals(fragmentName))
            return true;

        if(NotificationPreferenceFragment.class.getName().equals(fragmentName))
            return true;

        if(DataSyncPreferenceFragment.class.getName().equals(fragmentName))
            return true;
        return false;

    }
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference(GENDER));
            bindPreferenceSummaryToValue(findPreference(WEIGHT));
            bindPreferenceSummaryToValue(findPreference(HEIGHT));
            bindPreferenceSummaryToValue(findPreference(BIRTHDAY));

            bindPreferenceSummaryToValue(findPreference(STEPS_GOAL));
        }

        public void updateEditTextPreferenceValue(String key, String val) {
            EditTextPreference k = (EditTextPreference) findPreference(key);
            //k.setText(val);
            sBindPreferenceSummaryToValueListener.onPreferenceChange(k, val);
        }

        public void updateListPreferenceValue(String key, String val) {
            ListPreference k = (ListPreference) findPreference(key);
            //k.setValueIndex(k.findIndexOfValue(val));
            sBindPreferenceSummaryToValueListener.onPreferenceChange(k, val);

        }

        public void updateDatePreferenceValue(String key, String dateString) {
            DatePreference k = (DatePreference) findPreference(key);
            //k.setDate(dateString);
            sBindPreferenceSummaryToValueListener.onPreferenceChange(k, dateString);
        }
    }

    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);

            bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
        }
    }

    public static class DataSyncPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);

            bindPreferenceSummaryToValue(findPreference("sync_frequency"));
        }
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            if(mBluetoothLeService.isConnected())
                mBluetoothLeService.readVidonnCharacteristic(VidonnGattAttributes.VIDONN_PERSONAL_INFO);
            else {
                Toast.makeText(getApplicationContext(), "Warning! Not connected to a bracelet!", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d(TAG, "Received ACTION_DATA_AVAILABLE");
                if (intent.hasExtra(BluetoothLeService.VIDONN_PERSONAL_INFO)) {

                    PersonalInfo pinfo = intent.getParcelableExtra(BluetoothLeService.VIDONN_PERSONAL_INFO);
                    Log.d(TAG, "Received Personal Info: " + pinfo);

                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                    SharedPreferences.Editor ed = settings.edit();

                    String height = pinfo.getHeight();
                    if(!settings.getString(HEIGHT, height).equals(height)) {
                        ed.putString(HEIGHT, height);
                        general.updateEditTextPreferenceValue(HEIGHT, height);
                    }

                    String weight = pinfo.getWeight();
                    if(!settings.getString(WEIGHT, weight).equals(weight)) {
                        ed.putString(WEIGHT, weight);
                        general.updateEditTextPreferenceValue(WEIGHT, weight);
                    }

                    String gender = (pinfo.getPreferenceGender() == PersonalInfo.GENDER.MALE ? getString(R.string.male) : getString(R.string.female));
                    if(!settings.getString(GENDER, gender).equals(gender)) {
                        ed.putString(GENDER, gender);
                        general.updateListPreferenceValue(GENDER, gender);
                    }

                    try {
                        Calendar today = Calendar.getInstance();
                        int birthYear = today.get(Calendar.YEAR) - pinfo.getAge();
                        SimpleDateFormat formatter = DatePreference.formatter();
                        String birthday = settings.getString(BIRTHDAY, formatter.format(today.getTime())); // default is today

                        Calendar bday = Calendar.getInstance();
                        bday.setTime(formatter.parse(birthday));
                        if(bday.get(Calendar.YEAR) == birthYear) {
                            today.set(Calendar.YEAR, birthYear);
                            ed.putString(BIRTHDAY, formatter.format(today.getTime()));
                            general.updateDatePreferenceValue(BIRTHDAY, formatter.format(today.getTime()));
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    ed.commit();

                }
            }
        }
    };

}
