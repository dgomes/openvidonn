/*
Copyright 2014 Diogo Gomes <diogogomes@gmail.com>

This file is part of OpenVidonn.

OpenVidonn is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

OpenVidonn is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with Foobar. If not, see http://www.gnu.org/licenses/.
*/

package com.diogogomes.openvidonn.app;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;


/**
 * A fragment representing a list of Items.
 * <p />
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p />
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class BluetoothDevicesFragment extends ListFragment {
    public void setBoot(boolean boot) {
        BluetoothDevicesFragment.boot = boot;
    }

    private static final String TAG = BluetoothDevicesFragment.class.getSimpleName();


    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    public static final String DEFAULT_DEVICE_ADDRESS = "defaultDeviceAddress";

    private Handler mHandler;

    private BluetoothLeService mBluetoothLeService = null;

    private OnBluetoothDevicesFragmentInteractionListener mListener;
    private static String defaultDeviceAddress = null;
    private static boolean boot = true;

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private static BluetoothDeviceArrayAdapter devicesList = null;
    private static ProgressDialog pdialog = null;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public BluetoothDevicesFragment() {
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume()");
        getActivity().getApplicationContext().registerReceiver(mLeServiceReceiver, makeLeServiceIntentFilter());

        listDiscoveredBluetoothDevices();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate()");

        mHandler = new Handler();
        Activity activity = getActivity();

        Intent gattServiceIntent = new Intent(activity, BluetoothLeService.class);
        getActivity().getApplicationContext().bindService(gattServiceIntent, mServiceConnection, activity.BIND_AUTO_CREATE);


        // Use this check to determine whether BLE is supported on the listitem_device.  Then you can
        // selectively disable BLE-related features.


        if (!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(getActivity(), R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            activity.finish();
        }

        devicesList = new BluetoothDeviceArrayAdapter(activity);

        pdialog = new ProgressDialog(activity);
        pdialog.setCancelable(true);
        pdialog.setMessage(getResources().getString(R.string.search_devices));

        if(defaultDeviceAddress == null) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
            defaultDeviceAddress = settings.getString(DEFAULT_DEVICE_ADDRESS, null);

            pdialog.show();
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
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, BluetoothLeService.REQUEST_ENABLE_BT);
                Log.d(TAG, "requiring user to turn ON bluetooth");
            }
            listDiscoveredBluetoothDevices();

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected");
            mBluetoothLeService = null;
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        View view = inflater.inflate(R.layout.fragment_bluetoothdevices, container, false);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        ((AdapterView<ListAdapter>) mListView).setAdapter(devicesList);

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == BluetoothLeService.REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            getActivity().finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnBluetoothDevicesFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "onPause()");
        getActivity().getApplicationContext().unregisterReceiver(mLeServiceReceiver);

        if(mBluetoothLeService!=null)
            mBluetoothLeService.scanLeDevice(false);
        devicesList.clear();
        pdialog.dismiss();
    }

    private final BroadcastReceiver mLeServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "BroadcastReceiver = " + action);
            if (BluetoothLeService.NEW_BLUETOOTH_DEVICE_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothLeService.BLUETOOTH_DEVICE);

                pdialog.dismiss();

                Log.d(TAG, devicesList.toString());

                devicesList.addDevice(device);
                Log.d(TAG, device.getAddress().toString() + " == " + defaultDeviceAddress + " boot=" + boot);
                if(device.getAddress().equals(defaultDeviceAddress) && boot) {
                    selectDevice(device);
                    boot = false;
                }
            }
        }
    };

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {

        super.onListItemClick(l, v, position, id);

        Log.d(TAG, "onItemClick");
        final BluetoothDevice device = devicesList.getDevice(position);
        if (device == null) return;
        selectDevice(device);
    }

    public void selectDevice(BluetoothDevice device) {
        Log.d(TAG, "selectDevice(" + device.getAddress().toString() + ")");

        mBluetoothLeService.scanLeDevice(false);

        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(DEFAULT_DEVICE_ADDRESS, device.getAddress().toString());
            editor.commit();
            mListener.onDeviceSelected(device);
        }
    }

    public static class BluetoothDevicesHelpFragment extends Fragment {
        static BluetoothDevicesFragment listFragment = null;

        public static Fragment newInstance(BluetoothDevicesFragment l) {
            listFragment = l;
            BluetoothDevicesHelpFragment mFragment = new BluetoothDevicesHelpFragment();
            return mFragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // TODO Auto-generated method stub
            View v = inflater.inflate(R.layout.fragment_bluetoothdevices_help, null);

            ImageView imageView = (ImageView) v.findViewById(R.id.help_image);
            imageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            try
            {
                SVG svg = SVG.getFromResource(getActivity(), R.raw.vi_press);
                Drawable drawable = new PictureDrawable(svg.renderToPicture());
                imageView.setImageDrawable(drawable);
            }
            catch(SVGParseException e)
            {
                Log.e(TAG, "Error presenting help SVG with message:" + e.toString());
            }

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "clicked on BluetoothDevicesHelpFragment");

                    FragmentManager fm = getFragmentManager();
                    if (fm.getBackStackEntryCount() > 0) {
                        Log.i(TAG, "popping backstack");
                        fm.popBackStack();
                    }
                    listFragment.listDiscoveredBluetoothDevices();
                }
            });

            return v;
        }
    }


    public void listDiscoveredBluetoothDevices() {
        if(mBluetoothLeService == null)
            return;

        final BluetoothDevicesFragment myList = this;
        // Stops scanning after a pre-defined scan period.
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!mBluetoothLeService.isScanning()) return;
                mBluetoothLeService.scanLeDevice(false);
                if(devicesList.getCount() == 0) {
                    Fragment help_fragment = BluetoothDevicesHelpFragment.newInstance(myList);
                    getFragmentManager().beginTransaction().add(R.id.fragment_container, help_fragment).addToBackStack("List").commit();
                }
                pdialog.hide();

            }
        }, SCAN_PERIOD);

        devicesList.clear();
        pdialog.dismiss();
        pdialog = new ProgressDialog(getActivity());
        pdialog.setCancelable(true);
        pdialog.setMessage(getResources().getString(R.string.search_devices));
        pdialog.show();
        mBluetoothLeService.scanLeDevice(true);

    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getEmptyView();

        if (emptyText instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    private static IntentFilter makeLeServiceIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

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
    public interface OnBluetoothDevicesFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onDeviceSelected(BluetoothDevice device);
    }

}
