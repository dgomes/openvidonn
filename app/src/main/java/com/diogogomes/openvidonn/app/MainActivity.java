/*
Copyright 2014 Diogo Gomes <diogogomes@gmail.com>

This file is part of OpenVidonn.

OpenVidonn is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

OpenVidonn is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with Foobar. If not, see http://www.gnu.org/licenses/.
*/

package com.diogogomes.openvidonn.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.diogogomes.openvidonn.app.model.History;


public class MainActivity extends Activity implements BluetoothDevicesFragment.OnBluetoothDevicesFragmentInteractionListener, CurrentFragment.OnCurrentFragmentInteractionListener, AlarmsFragment.OnFragmentInteractionListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String DEVICE = "com.diogogomes.MainActivity.device";

    private BluetoothDevicesFragment bluetoothDevicesFragment = null;
    private BluetoothDevice device = null;

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d(TAG, "onSaveInstanceState");
        // Save the user's current game state
        savedInstanceState.putParcelable(DEVICE, device);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (findViewById(R.id.fragment_container) != null) {

            Log.d(TAG, "onCreate: fragment_container != null");

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                Log.d(TAG, "restoring from savedInstanceState");
                device = savedInstanceState.getParcelable(DEVICE);
                CurrentFragment currentFragment = CurrentFragment.newInstance(device);

                getFragmentManager().beginTransaction().add(R.id.fragment_container, currentFragment).commit();
                return;
            }

            // Create a new Fragment to be placed in the activity layout
            bluetoothDevicesFragment = new BluetoothDevicesFragment();

            // In case this activity was started with special instructions from an
            // Intent, pass the Intent's extras to the fragment as arguments
            bluetoothDevicesFragment.setArguments(getIntent().getExtras());

            // Add the fragment to the 'fragment_container' FrameLayout
            getFragmentManager().beginTransaction().add(R.id.fragment_container, bluetoothDevicesFragment).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);

            startActivity(intent);
            return true;
        } else if ( id == R.id.action_alarms) {
            AlarmsFragment alarmFragment = AlarmsFragment.newInstance();

            Log.d(TAG, "AlarmsFragment...");
            FragmentTransaction ft = getFragmentManager().beginTransaction();

            ft.replace(R.id.fragment_container, alarmFragment).addToBackStack("alarm");
            ft.commit();

            return true;
        }else if (id == R.id.action_find_device) {
            Log.d(TAG, "Find Device...");
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_container, bluetoothDevicesFragment).addToBackStack("find");
            ft.commit();
            return true;

        } else if (id == R.id.action_about) {
            Log.d(TAG, "About...");
            // Inflate the about message contents
            View messageView = getLayoutInflater().inflate(R.layout.about, null, false);

            // When linking text, force to always use default color. This works
            // around a pressed color state bug.
            TextView textView = (TextView) messageView.findViewById(R.id.about_credits);
            int defaultColor = textView.getTextColors().getDefaultColor();
            textView.setTextColor(defaultColor);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setIcon(R.drawable.ic_launcher);
            builder.setTitle(R.string.app_name);
            builder.setView(messageView);
            builder.create();
            builder.show();
            return true;

        } else if (id == R.id.action_refresh) {
            CurrentFragment currentFragment = CurrentFragment.newInstance(device);

            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_container, currentFragment).addToBackStack("refresh");
            ft.commit();
            return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDeviceSelected(BluetoothDevice device) {
        Log.d(TAG, "onDeviceSelected (" + device.getName() + ")");
        this.device = device;

        CurrentFragment currentFragment = CurrentFragment.newInstance(device);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, currentFragment);
        ft.commit();
    }

    @Override
    public void onDeviceDisconnected() {
        Log.d(TAG, "Device disconnected find new device...");
        bluetoothDevicesFragment.setBoot(true);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, bluetoothDevicesFragment);
        ft.commit();
    }

    @Override
    public void requestSettings() {
        Log.d(TAG, "No settings defined");

        Intent intent = new Intent(this, SettingsActivity.class);

        startActivity(intent);
    }

    @Override
    public void setCurrentDay(int batteryLevel, History history, int day_of_week) {
        Log.d(TAG, "setCurrentDay (" + day_of_week + ")");

        CurrentFragment currentFragment = CurrentFragment.newInstance(batteryLevel, history, day_of_week);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, currentFragment);
        ft.addToBackStack(null);
        ft.commit();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        Log.d(TAG, "Got an interaction in AlarmsFragment... don't know what to do...");
    }
}
