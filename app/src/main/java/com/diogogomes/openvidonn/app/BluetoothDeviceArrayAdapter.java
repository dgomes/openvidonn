/*
Copyright 2014 Diogo Gomes <diogogomes@gmail.com>

This file is part of OpenVidonn.

OpenVidonn is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

OpenVidonn is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with Foobar. If not, see http://www.gnu.org/licenses/.
*/

package com.diogogomes.openvidonn.app;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by dgomes on 24/05/14.
 */
public class BluetoothDeviceArrayAdapter extends BaseAdapter {
    private static String TAG = BluetoothDeviceArrayAdapter.class.getSimpleName();

    private ArrayList<BluetoothDevice> mLeDevices;
    private LayoutInflater mInflator;

    public BluetoothDeviceArrayAdapter(Context context) {
        super();
        mInflator = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mLeDevices = new ArrayList<BluetoothDevice>();
    }

    public void addDevice(BluetoothDevice device) {
        if(!mLeDevices.contains(device)) {
            mLeDevices.add(device);
        }
        this.notifyDataSetChanged();
    }

    public String toString() {
        String s = "[";
        for(BluetoothDevice d : mLeDevices)
            s += d.getName() + ", ";
        return s + "]";
    }

    public BluetoothDevice getDevice(int position) {

        return mLeDevices.get(position);
    }

    public void clear() {
        mLeDevices.clear();
        this.notifyDataSetInvalidated();
    }

    @Override
    public int getCount() {
        return mLeDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return mLeDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        // General ListView optimization code.
        if (view == null) {
            view = mInflator.inflate(R.layout.listitem_device, null);
            viewHolder = new ViewHolder();
            viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        BluetoothDevice device = mLeDevices.get(i);
        final String deviceName = device.getName();
        if (deviceName != null && deviceName.length() > 0)
            viewHolder.deviceName.setText(deviceName);
        else
            viewHolder.deviceName.setText("Unknown");

        return view;
    }

    static class ViewHolder {
        TextView deviceName;
    }

}
