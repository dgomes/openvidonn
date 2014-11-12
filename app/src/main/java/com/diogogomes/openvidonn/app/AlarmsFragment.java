/*
Copyright 2014 Diogo Gomes <diogogomes@gmail.com>

This file is part of OpenVidonn.

OpenVidonn is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

OpenVidonn is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with Foobar. If not, see http://www.gnu.org/licenses/.
*/

package com.diogogomes.openvidonn.app;

import android.app.Activity;
import android.app.ListFragment;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;


import com.diogogomes.openvidonn.app.model.Alarm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AlarmsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link AlarmsFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class AlarmsFragment extends ListFragment {
    private static final String TAG = AlarmsFragment.class.getSimpleName();

    private OnFragmentInteractionListener mListener;

    private AbsListView mListView;
    private static AlarmArrayAdapter alarmList = null;

    private BluetoothLeService mBluetoothLeService = null;

    private static ProgressDialog pdialog = null;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment AlarmsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AlarmsFragment newInstance() {
        AlarmsFragment fragment = new AlarmsFragment();
        return fragment;
    }

    public AlarmsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Activity activity = getActivity();

        Intent gattServiceIntent = new Intent(activity, BluetoothLeService.class);
        getActivity().getApplicationContext().bindService(gattServiceIntent, mServiceConnection, activity.BIND_AUTO_CREATE);


        alarmList = new AlarmArrayAdapter(getActivity(), getFragmentManager(), this);

        pdialog = new ProgressDialog(activity);
        pdialog.setCancelable(true);
        pdialog.setMessage(getResources().getString(R.string.loading));
        pdialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume()");
        getActivity().getApplicationContext().registerReceiver(mLeServiceReceiver, makeLeServiceIntentFilter());

    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "onPause()");
        getActivity().getApplicationContext().unregisterReceiver(mLeServiceReceiver);
    }

    private final BroadcastReceiver mLeServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "BroadcastReceiver = " + action);
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

                if(intent.hasExtra(BluetoothLeService.VIDONN_ALARMS)) {
                    ArrayList<Alarm> alarms = intent.getParcelableArrayListExtra(BluetoothLeService.VIDONN_ALARMS);
                    Log.d(TAG, "Received alarms");
                    pdialog.dismiss();
                    for(Object a : alarms.toArray()) {
                        Log.i(TAG, ((Alarm) a).toStringDebug());
                        alarmList.addAlarm((Alarm) a);
                    }

                    alarmList.sort();
                }
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_alarms, container, false);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        ((AdapterView<ListAdapter>) mListView).setAdapter(alarmList);

        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
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
    public void onListItemClick(ListView l, View v, int position, long id) {

        super.onListItemClick(l, v, position, id);

        Log.d(TAG, "onItemClick");
        final Alarm alarm = alarmList.getAlarm(position);
        if (alarm == null) return;
        else {
            Log.i(TAG, alarm.toString());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: " + resultCode);
        super.onActivityResult(requestCode, resultCode, data);
        alarmList.notifyDataSetInvalidated();
    }

    public void saveAlarms(ArrayList<Alarm> alarms) {
        mBluetoothLeService.writeAlarms(alarms);
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
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
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
                return;
            }
            mBluetoothLeService.readAlarms();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected");
            mBluetoothLeService = null;
        }
    };

    private static IntentFilter makeLeServiceIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
