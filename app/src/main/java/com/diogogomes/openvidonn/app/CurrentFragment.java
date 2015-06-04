/*
Copyright 2014 Diogo Gomes <diogogomes@gmail.com>

This file is part of OpenVidonn.

OpenVidonn is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

OpenVidonn is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with Foobar. If not, see http://www.gnu.org/licenses/.
*/

package com.diogogomes.openvidonn.app;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.numberprogressbar.NumberProgressBar;
import org.bostonandroid.datepreference.DatePreference;
import com.diogogomes.openvidonn.app.model.DayHistory;
import com.diogogomes.openvidonn.app.model.History;
import com.diogogomes.openvidonn.app.model.Movement;
import com.diogogomes.openvidonn.app.model.PersonalInfo;
import com.echo.holographlibrary.Bar;
import com.echo.holographlibrary.BarGraph;
import com.echo.holographlibrary.PieGraph;
import com.echo.holographlibrary.PieSlice;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link com.diogogomes.openvidonn.app.CurrentFragment.OnCurrentFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CurrentFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class CurrentFragment extends Fragment {
    private static final String TAG = CurrentFragment.class.getSimpleName();

    private static final String ARG_DEVICE = "device";
    private static final String ARG_HISTORY = "history";
    private static final String ARG_DAY = "day_of_week";
    private static final String ARG_BATTERY_LEVEL = "batteryLevel";

    private BluetoothDevice device;

    private OnCurrentFragmentInteractionListener mListener;

    private BluetoothLeService mBluetoothLeService = null;
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private int batteryLevel = 0;
    private History history = null;
    private int dayOfWeek;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param dev Bluetooth Device.
     * @return A new instance of fragment CurrentFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CurrentFragment newInstance(BluetoothDevice dev) {
        CurrentFragment fragment = new CurrentFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_DEVICE, dev);
        fragment.setArguments(args);
        return fragment;
    }

    public static CurrentFragment newInstance(int batteryLevel, History history, int day_of_week) {
        CurrentFragment fragment = new CurrentFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_HISTORY, history);
        args.putInt(ARG_DAY, day_of_week);
        args.putInt(ARG_BATTERY_LEVEL, batteryLevel);
        fragment.setArguments(args);
        return fragment;
    }

    public CurrentFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            if(getArguments().containsKey(ARG_DEVICE)) {
                device = getArguments().getParcelable(ARG_DEVICE);
                Calendar c = Calendar.getInstance();
                dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
            } if(getArguments().containsKey(ARG_HISTORY) && getArguments().containsKey(ARG_DAY) && getArguments().containsKey(ARG_BATTERY_LEVEL)) {
                history = getArguments().getParcelable(ARG_HISTORY);
                dayOfWeek = getArguments().getInt(ARG_DAY);
                batteryLevel = getArguments().getInt(ARG_BATTERY_LEVEL);
            }
        }
        if(device != null)
            Log.d(TAG, "onCreate with " + device.getName());
        else if(history != null)
            Log.d(TAG, "onCreate with history for day " + dayOfWeek);
        else
            Log.d(TAG, "onCreate without device set");

        Intent gattServiceIntent = new Intent(getActivity(), BluetoothLeService.class);
        getActivity().getApplicationContext().bindService(gattServiceIntent, mServiceConnection, getActivity().BIND_AUTO_CREATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_current, container, false);
    }



    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnCurrentFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        getActivity().getApplicationContext().registerReceiver(mLeServiceReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null && device != null) {
            final boolean result = mBluetoothLeService.connect(device.getAddress());
            Log.d(TAG, "Connect request result=" + result);
            if(!result) {
                mListener.onDeviceDisconnected();
            } else {
                updateData(true);
            }
        } else if(history != null){
            Log.d(TAG, "Show history for day = " + dayOfWeek);
            DayHistory dh = history.getDay(dayOfWeek);
            drawSteps(dh.getTotalSteps());
            drawDistance(dh.getTotalDistance());
            drawHistory();
            drawBattery(batteryLevel);

            TextView textViewCurrent = (TextView) getActivity().findViewById(R.id.current_time);
            textViewCurrent.setText(dh.getDate("dd-MMM-yyyy "));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        getActivity().getApplicationContext().unregisterReceiver(mLeServiceReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        getActivity().getApplicationContext().unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, BluetoothLeService.REQUEST_ENABLE_BT);
                Log.d(TAG, "requiring user to turn ON bluetooth");
            }
            // Automatically connects to the device upon successful start-up initialization.
            if(device != null)
                mBluetoothLeService.connect(device.getAddress());
            if(mBluetoothLeService.isConnected()) {
                if(history == null) {
                    updateData(true);
                } else {
                    // Get batteryLevel
                    mBluetoothLeService.readBatteryLevel();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected");
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mLeServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "BroadcastReceiver = " + action);
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mBluetoothLeService.close();
                mListener.onDeviceDisconnected();
                mConnected = false;
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                updateData(true);
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                if(intent.hasExtra(BluetoothLeService.BATTERY_LEVEL)) {
                    drawBattery(intent.getIntExtra(BluetoothLeService.BATTERY_LEVEL, 0));
                } else if(intent.hasExtra(BluetoothLeService.VIDONN_MOVEMENT)){


                    Movement m = intent.getParcelableExtra(BluetoothLeService.VIDONN_MOVEMENT);

                    Log.i(TAG, "Current = " + m.toString());

                    // Last Update
                    TextView textViewCurrent = (TextView) getActivity().findViewById(R.id.current_time);
                    textViewCurrent.setText(m.getCurrent("HH:mm:ss dd-MMM-yyyy "));

                    drawSteps(m.getSteps());
                    drawCalories(m.getCalories());

                } else if(intent.hasExtra(BluetoothLeService.VIDONN_HISTORY)){
                    history = intent.getParcelableExtra(BluetoothLeService.VIDONN_HISTORY);

                    drawHistory();
                }
            }
        }
    };

    // Refresh data when clicked
    PieGraph.OnSliceClickedListener refreshData =  new PieGraph.OnSliceClickedListener() {
        @Override
        public void onClick(int index) {
            Log.e(TAG, "index = " + index);

            updateData(false);
        }
    };

    private void drawBattery(int value) {
        this.batteryLevel = value;

        NumberProgressBar batteryLevel = (NumberProgressBar) getActivity().findViewById(R.id.battery_level);
        batteryLevel.setProgress(value);
    }

    private int weekday2int(String day) {
        try
        {
            DateFormat formatter ;
            Date date ;
            formatter = new SimpleDateFormat("E");
            date = formatter.parse(day);
            GregorianCalendar g = new GregorianCalendar();
            g.setTime(date);
            Log.e(TAG, day + " = " + g.get(Calendar.DAY_OF_WEEK));
            return g.get(Calendar.DAY_OF_WEEK);
        }
        catch (ParseException e)
        {
            Log.e(TAG, "Couldn't parse day of the week !?");
        }
        return 0;
    }

    private void drawHistory() {

        final ArrayList<Bar> points = new ArrayList<Bar>();
        for (DayHistory dh : history.getLastWeek()) {
            Bar d = new Bar();
            d.setColor(getActivity().getResources().getColor(R.color.graph_history));
            SimpleDateFormat formatter=new SimpleDateFormat("E");
            if(dh != null) {
                d.setName(formatter.format(dh.getCalendar().getTime()));
                d.setValue(dh.getTotalSteps());

                if(dayOfWeek == dh.getCalendar().get(Calendar.DAY_OF_WEEK)) {
                    d.setColor(getActivity().getResources().getColor(R.color.graph_history_selected));
                } else {
                    d.setSelectedColor(getActivity().getResources().getColor(R.color.graph_history_selected));
                }
            } else {
                d.setName("...");
                d.setValue(0);
            }
            points.add(d);

        }

        final BarGraph g = (BarGraph)getActivity().findViewById(R.id.graph_history);

        g.setOnBarClickedListener(new BarGraph.OnBarClickedListener() {
            @Override
            public void onClick(int index) {
                Bar c = points.get(index);
                mListener.setCurrentDay(batteryLevel, history, weekday2int(c.getName()));
            }
        });

        g.setBars(points);

    }

    private void drawDistance(int distance) {
        // Calories
        TextView textViewDistance = (TextView) getActivity().findViewById(R.id.text_2nd);
        textViewDistance.setText(Integer.toString(distance));

        final PieGraph pg_distance = (PieGraph) getActivity().findViewById(R.id.graph_2nd);
        pg_distance.setInnerCircleRatio(getActivity().getResources().getInteger(R.integer.graph_inner_circle_ratio));
        pg_distance.removeSlices();

        pg_distance.post(new Runnable() {
            @Override
            public void run() {
                Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.distance);
                pg_distance.setBackgroundBitmap(Bitmap.createScaledBitmap(b, pg_distance.getWidth()/2, pg_distance.getHeight()/2, true));
            }
        });

        PieSlice slice = new PieSlice();
        slice.setColor(getActivity().getResources().getColor(R.color.graph_distance_accomplished));
        slice.setValue(distance);
        pg_distance.addSlice(slice);

        slice = new PieSlice();
        slice.setColor(getActivity().getResources().getColor(R.color.graph_missing)); //TODO set color according to objective
        slice.setValue(history.getTotalDistance()-distance);
        pg_distance.addSlice(slice);

    }

    private void drawCalories(int cals) {
        // Calories
        TextView textViewCalories = (TextView) getActivity().findViewById(R.id.text_2nd);
        textViewCalories.setText(Integer.toString(cals));

        final PieGraph pg_cal = (PieGraph) getActivity().findViewById(R.id.graph_2nd);
        pg_cal.setInnerCircleRatio(getActivity().getResources().getInteger(R.integer.graph_inner_circle_ratio));
        pg_cal.removeSlices();


        pg_cal.post(new Runnable() {
            @Override
            public void run() {
                Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.burn);
                pg_cal.setBackgroundBitmap(Bitmap.createScaledBitmap(b, pg_cal.getWidth()/2, pg_cal.getHeight()/2, true));
            }
        });

        /*
            BMR = sleeping
            Little to no exercise	Daily kilocalories needed = BMR x 1.2
            Light exercise (1–3 days per week)	Daily kilocalories needed = BMR x 1.375
            Moderate exercise (3–5 days per week)	Daily kilocalories needed = BMR x 1.55
            Heavy exercise (6–7 days per week)	Daily kilocalories needed = BMR x 1.725
            Very heavy exercise (twice per day, extra heavy workouts)	Daily kilocalories needed = BMR x 1.9
         */
        double limits[] = {0, 0.2, 0.375, 0.55, 0.725, 0.9};
        int limits_color[] = {
            getActivity().getResources().getColor(R.color.BMR_little_to_no_exercise_color),
            getActivity().getResources().getColor(R.color.BMR_light_exercise_color),
            getActivity().getResources().getColor(R.color.BMR_moderate_color),
            getActivity().getResources().getColor(R.color.BMR_heavy_exercise_color),
            getActivity().getResources().getColor(R.color.BMR_very_heavy_exercise_color),
            getActivity().getResources().getColor(R.color.BMR_very_heavy_exercise_color)
        };
        double bmr = calculateBMR();
        if(bmr == 0) return;

        for(int i=0; i<limits.length; i++) {
            limits[i] = bmr * limits[i];
        }

        int calories = (cals == 0 ? 1 : cals); //this overcomes a bug in PieChart where a slice with 0 misbehaves
        int i = 0;
        do {
            PieSlice slice = new PieSlice();

            slice.setColor(limits_color[i]);
            double diff = limits[i+1] - limits[i];
            if(calories > diff) {
                slice.setValue((float) diff);
            } else {
                slice.setValue(calories);
            }
            calories-=diff;
            pg_cal.addSlice(slice);
            i++;
        } while(calories > 0 && i < 5);

        if(calories > 0) {
            // we have an IronMan...
            PieSlice slice = new PieSlice();
            slice.setColor(limits_color[5]);
            slice.setValue(calories);
            pg_cal.addSlice(slice);
        } else {
            // fill in the missing calories...
            PieSlice slice = new PieSlice();
            slice.setColor(getActivity().getResources().getColor(R.color.graph_missing)); //TODO set color according to objective
            slice.setValue((float) (bmr - cals)); //BMR as maximum amount!
            pg_cal.addSlice(slice);
        }

    }

    private void drawSteps(int steps) {
        // Steps
        TextView textViewSteps = (TextView) getActivity().findViewById(R.id.text_steps);
        textViewSteps.setText(Integer.toString(steps));

        final PieGraph pg = (PieGraph) getActivity().findViewById(R.id.graph_steps);
        pg.setInnerCircleRatio(getActivity().getResources().getInteger(R.integer.graph_inner_circle_ratio));
        pg.removeSlices();

        pg.post(new Runnable() {
            @Override
            public void run() {
                Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.pegadas);
                pg.setBackgroundBitmap(Bitmap.createScaledBitmap(b, pg.getWidth()/2, pg.getHeight()/2, true));
            }
        });

        PieSlice slice = new PieSlice();
        slice.setColor(getActivity().getResources().getColor(R.color.graph_steps_accomplished));
        slice.setValue(steps > 0 ? steps : 1);
        pg.addSlice(slice);

        slice = new PieSlice();
        slice.setColor(getActivity().getResources().getColor(R.color.graph_missing));
        int steps_goal = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(SettingsActivity.STEPS_GOAL, getActivity().getResources().getInteger(R.integer.max_steps)+""));

        int max_steps = (int) (steps_goal * 1.10) - steps;//10% slack
        if(max_steps <= 0) //avoid IronMan's who can go offscale
            max_steps = steps;
        slice.setValue(max_steps);
        pg.addSlice(slice);

    }

    public void updateData(boolean readHistory) {
        // Get batteryLevel
        mBluetoothLeService.readBatteryLevel();
        // Go and read movements
        mBluetoothLeService.readVidonnCharacteristic(VidonnGattAttributes.VIDONN_MOVEMENT);

        // Read History too :)
        if(readHistory)
            mBluetoothLeService.readHistory(false);
        // Set Date & Time
        mBluetoothLeService.writeDateTime();
    }

    private double calculateBMR() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String gender = settings.getString(SettingsActivity.GENDER, null);

        int height = Integer.parseInt(settings.getString(SettingsActivity.HEIGHT, "0"));
        int weight = Integer.parseInt(settings.getString(SettingsActivity.WEIGHT, "0"));

        Calendar birthday = DatePreference.getDateFor(settings, SettingsActivity.BIRTHDAY);

        int age = PersonalInfo.calculateAge(birthday);

        double bmr = 0;

        if(gender == null || height == 0 || weight == 0) {
            Log.d(TAG, "We don't have Personal Information to calculate BMR");

            mListener.requestSettings();
            return bmr;
        }

        //The Harris–Benedict equations revised by Roza and Shizgal in 1984.[http://www.ajcn.org/content/40/1/168]
        if(gender.equals(R.string.male)) {
            // Men	BMR = 88.362 + (13.397 x weight in kg) + (4.799 x height in cm) - (5.677 x age in years)
            bmr = 88.362 + (13.397 * weight) + (4.799 * height) - (5.677 * age);
        } else {
            // Women	BMR = 447.593 + (9.247 x weight in kg) + (3.098 x height in cm) - (4.330 x age in years)
            bmr = 447.593 + (9.247 * weight) + (3.098 * height) - (4.330 * age);
        }

        Log.d(TAG, gender + "\tage=" + age + "\theight=" + height + "\tweight="+weight+ "\tBMR="+ bmr);

        return bmr;
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.CANT_CONNECT_VIDONN);
        intentFilter.addAction(BluetoothLeService.NEW_BLUETOOTH_DEVICE_FOUND);
        return intentFilter;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnCurrentFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onDeviceDisconnected();
        public void requestSettings();
        public void setCurrentDay(int batteryLevel, History history, int day_of_week);
    }

}
