/*
Copyright 2014 Diogo Gomes <diogogomes@gmail.com>

This file is part of OpenVidonn.

OpenVidonn is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

OpenVidonn is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with Foobar. If not, see http://www.gnu.org/licenses/.
*/

package com.diogogomes.openvidonn.app;

import android.app.Fragment;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.diogogomes.openvidonn.app.model.Alarm;
import com.diogogomes.openvidonn.app.model.DayHistory;
import com.diogogomes.openvidonn.app.model.History;
import com.diogogomes.openvidonn.app.model.Movement;
import com.diogogomes.openvidonn.app.model.PersonalInfo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private History hist = new History();

    private HashMap<String, HashMap<String, BluetoothGattCharacteristic>> mGattCharacteristics = new HashMap<String, HashMap<String, BluetoothGattCharacteristic>>();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private boolean mScanning = false;
    private Handler mHandler = new Handler();

    private static final long RECONNECT_PERIOD = 10000;

    enum BluetoothCommunicationType {READ, WRITE};
    private BlockingQueue<Pair<BluetoothCommunicationType, BluetoothGattCharacteristic>> bluetoothCommunicationQueue = new LinkedBlockingQueue<Pair<BluetoothCommunicationType, BluetoothGattCharacteristic>>();

    public final static int REQUEST_ENABLE_BT = 1;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String CANT_CONNECT_VIDONN = "com.diogogomes.openvidonn.cant_connect_vidonn";
    public final static String NEW_BLUETOOTH_DEVICE_FOUND = "com.diogogomes.openvidonn.new_bluetooth_device_found";
    public final static String BLUETOOTH_DEVICE = "com.diogogomes.openvidonn.args.bluetooth_device";

    public final static String ACTION_GATT_CONNECTED = "com.diogogomes.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.diogogomes.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.diogogomes.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.diogogomes.bluetooth.le.ACTION_DATA_AVAILABLE";

    public final static String VIDONN_MOVEMENT = "com.diogogomes.openvidonn.MOVEMENT";
    public final static String VIDONN_HISTORY = "com.diogogomes.openvidonn.HISTORY";
    public final static String VIDONN_PERSONAL_INFO = "com.diogogomes.openvidonn.PERSONAL_INFO";
    public final static String VIDONN_ALARMS = "com.diogogomes.openvidonn.ALARMS";

    public static final String BATTERY_LEVEL = "com.diogogomes.openvidonn.BATTERY_LEVEL";

    public boolean isScanning() {
        return mScanning;
    }

    public boolean isConnected() {
        Log.d(TAG, "mConnectionState = " + mConnectionState);
        return mConnectionState == STATE_CONNECTED;
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "onConnectionStateChange(" + gatt.getDevice().getName() + ", " + status + ", "+ newState+")");
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                bluetoothCommunicationQueue.clear();
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                bluetoothCommunicationQueue.clear();
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                List<BluetoothGattService> gattServices = getSupportedGattServices();
                if (gattServices == null) return;
                mGattCharacteristics = new HashMap<String, HashMap<String, BluetoothGattCharacteristic>>();

                // Loops through available GATT Services.
                for (BluetoothGattService gattService : gattServices) {

                    List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                    HashMap<String,BluetoothGattCharacteristic> charas = new HashMap<String, BluetoothGattCharacteristic>();

                    // Loops through available Characteristics.
                    for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                        charas.put(gattCharacteristic.getUuid().toString(), gattCharacteristic);
                        //Log.e(TAG, charas.toString() + " - Properties: " + gattCharacteristic.getProperties() + "Permissions: "+ gattCharacteristic.getPermissions());
                    }
                    mGattCharacteristics.put(gattService.getUuid().toString(), charas);
                }

                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged()");
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }

            bluetoothCommunicationQueue.remove();
            processNextInQueue();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            Log.d(TAG, "onCharacteristicWrite()");
            bluetoothCommunicationQueue.remove();
            processNextInQueue();
        }

        private void processNextInQueue() {
            if(bluetoothCommunicationQueue.isEmpty()) return;

            Pair<BluetoothCommunicationType, BluetoothGattCharacteristic> e = bluetoothCommunicationQueue.element();

            switch (e.first) {
                case READ:
                    if(!mBluetoothGatt.readCharacteristic(e.second)) {// Skiping characteristics that fail (usually due to permissions...)
                        Log.e(TAG, "error reading " + e.second.getUuid());
                        bluetoothCommunicationQueue.remove();
                        processNextInQueue();
                    }
                    break;
                case WRITE:
                    if(!mBluetoothGatt.writeCharacteristic(e.second)) {
                        Log.e(TAG, "error writing " + e.second.getUuid());
                        bluetoothCommunicationQueue.remove();
                        processNextInQueue();
                    }
                    break;
            }
        }

    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {

        if(characteristic.getService().getUuid().toString().equals(VidonnGattAttributes.VIDONN_SERVICE)) {
            broadcastUpdateVidonn(action, characteristic);
        } else if(characteristic.getService().getUuid().toString().equals(VidonnGattAttributes.BATTERY_SERVICE)) {
            final Intent intent = new Intent(action);

            if(VidonnGattAttributes.BATTERY_LEVEL.equals(characteristic.getUuid().toString())) {
                int batt_level = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                Log.i(TAG, "Battery Level = " + batt_level);
                intent.putExtra(BATTERY_LEVEL, batt_level);
            }
            sendBroadcast(intent);
        }
    }

    private Calendar decodeDate(final int date ) {
        int year = ((date & 0x7e00) >> 9) + 2000;
        int month = ((date & 0x01e0) >> 5);
        int day = (date & 0x001f) + 1;

        return new GregorianCalendar(year, month, day);
    }

    private byte calculateChecksum(byte [] packet) {
        byte chksum = 0;
        for(int i=1; i<packet.length -1; i++) {
            chksum += (i ^ packet[i]);
        }
        return chksum;
    }

    private boolean verifyChecksum(byte [] packet) {
        byte chksum = calculateChecksum(packet);
        if(packet[packet.length-1] == chksum)
            return true;
        return false;
    }

    private void broadcastUpdateVidonn(String action, BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        if(!verifyChecksum(characteristic.getValue())) return;

        if (VidonnGattAttributes.VIDONN_MOVEMENT.equals(characteristic.getUuid().toString())) {
            final int date = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1);
            final Calendar cal = decodeDate(date);

            final int hour = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 3);
            final int minute = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 4);
            final int second = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 5);
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, second);

            final int steps = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 6);

            final int distance = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 10) / 10;

            final int calories = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 14) / 100;


            Movement move = new Movement(cal, steps, distance, calories);
            Log.i(TAG, "Movement = "+ move);

            intent.putExtra(VIDONN_MOVEMENT,move );

        } else if(VidonnGattAttributes.VIDONN_HISTORY.equals(characteristic.getUuid().toString())) {
            final int index = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
            final int page = (index & 0xF); // 0 to 7           i
            final int day = (index >> 4 & 0xF); // 0 to 6       j

            final int date = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 2);
            final Calendar cal = decodeDate(date);

            int data[] = new int[6];
            for(int i=0; i<data.length; i++)
                data[i] = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 4+i*2);

            if( page < 4 ) {
                DayHistory dh = hist.addEntry(cal, History.ENTRY_TYPE.STEPS, page, data);
                //intent.putExtra(VIDONN_HISTORY_DAY, dh);
            } else {
                hist.addEntry(cal, History.ENTRY_TYPE.DISTANCE, page-4, data); // -4 to reset page to 0
                if(!hist.isComplete()) {
                    return; //lets avoid redrawing the interface if there is nothing new to show
                }
            }

            intent.putExtra(VIDONN_HISTORY, hist);
        } else if(VidonnGattAttributes.VIDONN_PERSONAL_INFO.equals(characteristic.getUuid().toString())) {
            final int height = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
            final int weight = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 2);
            final int gender = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 3);
            final int age = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 4);

            PersonalInfo pinfo = new PersonalInfo(height, weight, gender, age);
            Log.i(TAG, "PersonalInfo = " + pinfo.toString());
            intent.putExtra(VIDONN_PERSONAL_INFO, pinfo);
        } else if(VidonnGattAttributes.VIDONN_ALARM.equals(characteristic.getUuid().toString())) {
            int record = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
            Log.d(TAG, "Got alarms for record = " + record);

            ArrayList<Alarm> alarms = new ArrayList<Alarm>();
            for(int i=0; i<4; i++) {
                byte level = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 2+i*4).byteValue();
                byte week = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 3+i*4).byteValue();
                boolean enabled = (week & 0x80) == 0 ? false : true;
                int hour = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 4+i*4);
                int minutes = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 5+i*4);
                alarms.add(new Alarm(hour, minutes, week, enabled, level, record * 100 + i));
            }

            intent.putParcelableArrayListExtra(VIDONN_ALARMS, alarms);
        }

        sendBroadcast(intent);
    }


    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind()");

        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        if(!mBluetoothAdapter.isEnabled()) {
            Log.e(TAG, "BluetoothAdapter is not enabled");
            return false;
        }

        return true;
    }

    public void scanLeDevice(final boolean start) {
        if (start) {
            if(mScanning) return;

            if(isConnected())
                disconnect(); //first release previous device if any
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            if(!mScanning) return;
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        Log.d(TAG, (start ? "start" : "stop") + " scan for LE device => " + (mScanning ? "scanning" : "not scanning"));
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    final Intent intent = new Intent(NEW_BLUETOOTH_DEVICE_FOUND);
                    intent.putExtra(BLUETOOTH_DEVICE, device);
                    sendBroadcast(intent);
                }
            };

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if((mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT) != BluetoothProfile.STATE_CONNECTED)) {
                Log.d(TAG, "ConnectionState = " + mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT));
                if (mBluetoothGatt.connect()) {
                    Log.d(TAG, "Reconnecting...");
                    mConnectionState = STATE_CONNECTING;
                    mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if(mConnectionState == STATE_CONNECTING) {
                                    disconnect();
                                    broadcastUpdate(CANT_CONNECT_VIDONN);
                                }
                            }
                        }, RECONNECT_PERIOD);
                    return false;
                } else {
                    return false;
                }
            } else
                return true;
        }

        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public void readVidonnCharacteristic(String characteristic) {
        if(mGattCharacteristics == null)
            return;
        BluetoothGattCharacteristic c = mGattCharacteristics.get(VidonnGattAttributes.VIDONN_SERVICE).get(characteristic);
        readCharacteristic(c);
    }

    public void readHistory(boolean force) {
        if(!hist.isComplete() || force)
            for(int i=0; i<56; i++) {
                readVidonnCharacteristic(VidonnGattAttributes.VIDONN_HISTORY);
            }
        else
            readVidonnCharacteristic(VidonnGattAttributes.VIDONN_HISTORY);
    }

    public void readAlarms() {
        readVidonnCharacteristic(VidonnGattAttributes.VIDONN_ALARM);
        readVidonnCharacteristic(VidonnGattAttributes.VIDONN_ALARM);
    }

    public void readBatteryLevel() {
        if(mGattCharacteristics == null)
            return;
        BluetoothGattCharacteristic b = mGattCharacteristics.get(VidonnGattAttributes.BATTERY_SERVICE).get(VidonnGattAttributes.BATTERY_LEVEL);
        readCharacteristic(b);
    }

    public void writeAlarms(ArrayList<Alarm> alarms) {
        if(mGattCharacteristics == null)
            return;
        BluetoothGattCharacteristic c = mGattCharacteristics.get(VidonnGattAttributes.VIDONN_SERVICE).get(VidonnGattAttributes.VIDONN_ALARM);

        Log.e(TAG, "writeAlarms:");

        byte packet[] = new byte[19];
        packet[0] = (byte) 0xF5;

        int i = 0;
        for(Object a : alarms) {
            Alarm alarm = (Alarm) a;
            Log.e(TAG, alarm.toStringDebug());
            packet[1] = (i<4) ? (byte) 0x00 : (byte) 0x01;

            packet[2+(i%4)*4] = alarm.getLevel();
            packet[3+(i%4)*4] = alarm.getWeekdays();
            packet[3+(i%4)*4] = (byte) (packet[3+(i%4)*4] | (alarm.isEnabled() ? (byte) 0x80 : (byte) 0x00));
            packet[4+(i%4)*4] = (byte) alarm.getHour();
            packet[5+(i%4)*4] = (byte) alarm.getMinutes();

            i++;
            if(i%4==0) {
                packet[packet.length - 1] = calculateChecksum(packet);
                c.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                c.setValue(packet);
                writeCharacteristic(c);
            }
        }

    }

    public void writeDateTime() {
        if(mGattCharacteristics == null)
            return;
        BluetoothGattCharacteristic c = mGattCharacteristics.get(VidonnGattAttributes.VIDONN_SERVICE).get(VidonnGattAttributes.VIDONN_DATETIME);

        Calendar today = Calendar.getInstance();

        byte packet[] = new byte[8];
        packet[0] = (byte) 0xF5;
        packet[1] = (byte) (today.get(Calendar.YEAR) - 2000);
        packet[2] = (byte) (today.get(Calendar.MONTH));
        packet[3] = (byte) (today.get(Calendar.DAY_OF_MONTH)-1);
        packet[4] = (byte) (today.get(Calendar.HOUR_OF_DAY));
        packet[5] = (byte) (today.get(Calendar.MINUTE));
        packet[6] = (byte) (today.get(Calendar.SECOND));
        packet[packet.length-1] = calculateChecksum(packet);
        c.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        c.setValue(packet);
        writeCharacteristic(c);
    }

    public void writePersonalInformation(PersonalInfo pinfo) {
        if(mGattCharacteristics == null)
            return;
        BluetoothGattCharacteristic c = mGattCharacteristics.get(VidonnGattAttributes.VIDONN_SERVICE).get(VidonnGattAttributes.VIDONN_PERSONAL_INFO);
        if(c == null) {
            Log.e(TAG, "Could not access VIDONN_PERSONAL_INFO");
            Toast.makeText(getApplication(), R.string.error_writing_to_bracelet, Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "write PersonalInfo = " + pinfo);
        byte packet[] = c.getValue();
        if(packet == null || packet.length < 5) {
            Log.e(TAG, "Invalid characteristic in VIDONN_PERSONAL_INFO ");
            Toast.makeText(getApplication(), R.string.error_writing_to_bracelet, Toast.LENGTH_SHORT).show();
            return;
        }
        packet[1] = (byte) pinfo.height;
        packet[2] = (byte) pinfo.weight;
        packet[3] = (byte) pinfo.getGender();
        packet[4] = (byte) pinfo.age;
        packet[packet.length-1] = calculateChecksum(packet);
        c.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        c.setValue(packet);
        writeCharacteristic(c);
    }

    private boolean communicate(BluetoothCommunicationType type, BluetoothGattCharacteristic characteristic) {
        try {
            bluetoothCommunicationQueue.put(new Pair<BluetoothCommunicationType, BluetoothGattCharacteristic>(type, characteristic));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(bluetoothCommunicationQueue.size() > 1) return true;

        Log.d(TAG, "Last BT communication on the queue");
        boolean status =false;

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return status;
        }

        if(characteristic == null) {
            Log.e(TAG, "Null characteristic !?");
            return status;
        }

        switch (type) {
            case WRITE:
                status= mBluetoothGatt.writeCharacteristic(characteristic);
                break;
            case READ:
                status = mBluetoothGatt.readCharacteristic(characteristic);
                break;

        }

        return status;
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        String log = "0x";
        for (byte b: characteristic.getValue()){
            log+=String.format("%3x", b);
        }
        Log.i(TAG, "write: " + log);

        boolean status = communicate(BluetoothCommunicationType.WRITE, characteristic);

        Log.d(TAG, "writeCharacteristic status = " + status);
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        boolean status = communicate(BluetoothCommunicationType.READ, characteristic);

        if(!status)
            Log.e(TAG, "readCharacteristic status = " + status);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to Heart Rate Measurement.
/*        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        } */
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
}