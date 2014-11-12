/*
Copyright 2014 Diogo Gomes <diogogomes@gmail.com>

This file is part of OpenVidonn.

OpenVidonn is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

OpenVidonn is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with Foobar. If not, see http://www.gnu.org/licenses/.
*/

package com.diogogomes.openvidonn.app;

/**
 * Created by dgomes on 25/05/14.
 * http://www.vidonn.com/en/product.html
 */

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class VidonnGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();

    /*
        Services extracted from
        https://developer.bluetooth.org/gatt/services/Pages/ServicesHome.aspx
    */

    public static String GENERIC_ACCESS = "00001800-0000-1000-8000-00805f9b34fb";
    public static String GENERIC_ATTRIBUTE = "00001801-0000-1000-8000-00805f9b34fb";
    public static String DEVICE_INFORMATION = "0000180a-0000-1000-8000-00805f9b34fb";
    public static String BATTERY_SERVICE = "0000180f-0000-1000-8000-00805f9b34fb";
    public static String IMMEDIATE_ALERT = "00001802-0000-1000-8000-00805f9b34fb";

    /*
        Characteristics extracted from:
        https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicsHome.aspx
     */

    public static String DEVICE_NAME = "00002a00-0000-1000-8000-00805f9b34fb";
    public static String APPEARANCE = "00002a01-0000-1000-8000-00805f9b34fb";
    public static String PERIPHERAL_PRIVACY_FLAG = "00002a02-0000-1000-8000-00805f9b34fb";
    public static String RECONNECTION_ADDRESS = "00002a03-0000-1000-8000-00805f9b34fb";
    public static String PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS = "00002a04-0000-1000-8000-00805f9b34fb";

    public static String SERVICE_CHANGED = "00002a05-0000-1000-8000-00805f9b34fb";

    public static String SYSTEM_ID = "00002a23-0000-1000-8000-00805f9b34fb";
    public static String MODEL_NUMBER_STRING = "00002a24-0000-1000-8000-00805f9b34fb";
    public static String SERIAL_NUMBER_STRING = "00002a25-0000-1000-8000-00805f9b34fb";
    public static String FIRMWARE_REVISION_STRING = "00002a26-0000-1000-8000-00805f9b34fb";
    public static String HARDWARE_REVISION_STRING = "00002a27-0000-1000-8000-00805f9b34fb";
    public static String SOFTWARE_REVISION_STRING = "00002a28-0000-1000-8000-00805f9b34fb";
    public static String MANUFACTURER_NAME_STRING = "00002a29-0000-1000-8000-00805f9b34fb";
    public static String IEEE_11073_20601_REGULATORY_CERTIFICATION_DATA_LIST = "00002a2a-0000-1000-8000-00805f9b34fb";
    public static String PNP_ID = "00002a50-0000-1000-8000-00805f9b34fb";

    public static String BATTERY_LEVEL = "00002a19-0000-1000-8000-00805f9b34fb";

    public static String ALERT_LEVEL = "00002a06-0000-1000-8000-00805f9b34fb";

    // VIDONN_SERVICE specific
    public static String VIDONN_SERVICE = "0000ff01-0000-1000-8000-00805f9b34fb";

    public static String VIDONN_DEVICE_NAME = "0000f010-0000-1000-8000-00805f9b34fb";
    public static String VIDONN_PAIR_PIN = "0000f011-0000-1000-8000-00805f9b34fb";
    public static String VIDONN_FIRWMWARE_VERSION = "0000f012-0000-1000-8000-00805f9b34fb";
    public static String VIDONN_PERSONAL_INFO = "0000f013-0000-1000-8000-00805f9b34fb";
    public static String VIDONN_HISTORY = "0000f014-0000-1000-8000-00805f9b34fb";
    public static String VIDONN_MAC_ADDRESS = "0000f015-0000-1000-8000-00805f9b34fb";
    public static String VIDONN_DATETIME = "0000f016-0000-1000-8000-00805f9b34fb";
    public static String VIDONN_ALARM = "0000f017-0000-1000-8000-00805f9b34fb";
    public static String VIDONN_NOTIFICATION = "0000f018-0000-1000-8000-00805f9b34fb";
    public static String VIDONN_MOVEMENT = "0000f019-0000-1000-8000-00805f9b34fb";


    static {
        // SERVICES:
        attributes.put(GENERIC_ACCESS, "Generic Access");
        attributes.put(GENERIC_ATTRIBUTE, "Generic Attribute");
        attributes.put(DEVICE_INFORMATION, "Device Information");
        attributes.put(BATTERY_SERVICE, "Battery Service");
        attributes.put(VIDONN_SERVICE, "Vidonn Service");
        attributes.put(IMMEDIATE_ALERT, "Immediate Alert");

        // Characteristics:
        attributes.put(DEVICE_NAME, "Device Name");
        attributes.put(APPEARANCE, "Appearance");
        attributes.put(PERIPHERAL_PRIVACY_FLAG, "Peripheral privacy flag");
        attributes.put(RECONNECTION_ADDRESS, "Reconnection Address");
        attributes.put(PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS, "Peripheral preferred connection parameters");

        attributes.put(SERVICE_CHANGED, "Service Changed");

        attributes.put(SYSTEM_ID, "System Identification");
        attributes.put(MODEL_NUMBER_STRING, "Model Number");
        attributes.put(SERIAL_NUMBER_STRING, "Serial Number");
        attributes.put(FIRMWARE_REVISION_STRING, "Firmware Revision");
        attributes.put(HARDWARE_REVISION_STRING, "Hardware Revision");
        attributes.put(SOFTWARE_REVISION_STRING, "Software Revision");
        attributes.put(MANUFACTURER_NAME_STRING, "Manufacturer Name");
        attributes.put(IEEE_11073_20601_REGULATORY_CERTIFICATION_DATA_LIST, "IEEE 11073-20601 Regulatory Certification Data List");
        attributes.put(PNP_ID, "PnP ID");

        attributes.put(VIDONN_DEVICE_NAME, "Device Name");
        attributes.put(VIDONN_PAIR_PIN, "Pair Pin");
        attributes.put(VIDONN_FIRWMWARE_VERSION, "Firmware Version");
        attributes.put(VIDONN_PERSONAL_INFO, "Personal Info");
        attributes.put(VIDONN_HISTORY, "History");
        attributes.put(VIDONN_MAC_ADDRESS, "MAC Address");
        attributes.put(VIDONN_DATETIME, "Date and Time");
        attributes.put(VIDONN_ALARM, "Alarm");
        attributes.put(VIDONN_NOTIFICATION, "Call/SMS Notification");
        attributes.put(VIDONN_MOVEMENT, "Movement");

        attributes.put(BATTERY_LEVEL, "Battery Level");

        attributes.put(ALERT_LEVEL, "Alert Level");


    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}