/*
Copyright 2014 Diogo Gomes <diogogomes@gmail.com>

This file is part of OpenVidonn.

OpenVidonn is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

OpenVidonn is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with Foobar. If not, see http://www.gnu.org/licenses/.
*/
package com.diogogomes.openvidonn.app.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by dgomes on 19/08/14.
 */
public class Alarm implements Parcelable, Comparable<Alarm> {
    public int orderNumber;
    private int hour;
    private int minutes;
    private byte weekdays;
    private boolean enabled; //TODO implement the ON/OFF
    private byte level;

    public static final byte SUNDAY = (byte) 0x40;
    public static final byte MONDAY = (byte) 0x20;
    public static final byte TUESDAY = (byte) 0x10;
    public static final byte WEDNESDAY = (byte) 0x08;
    public static final byte THURSDAY = (byte) 0x04;
    public static final byte FRIDAY = (byte) 0x02;
    public static final byte SATURDAY = (byte) 0x01;

    public static final byte GENERAL = (byte) 0x01;
    public static final byte MEDICINE = (byte) 0x02;
    public static final byte IMPORTANT = (byte) 0x04;

    public static byte alarmWeekDays[] = {Alarm.SUNDAY, Alarm.MONDAY, Alarm.TUESDAY, Alarm.WEDNESDAY, Alarm.THURSDAY, Alarm.FRIDAY, Alarm.SATURDAY};

    public boolean isEnabled() {
        return enabled;
    }

    public void toggleAlarm() {
        enabled = !enabled;
    }

    public void enableAlarm() {
        enabled = true;
    }

    public void disableAlarm() {
        enabled = false;
    }
    public boolean isGeneral() {
        return level == GENERAL;
    }
    public boolean isMedicine() {
        return level == MEDICINE;
    }
    public boolean isImportantl() {
        return level == IMPORTANT;
    }

    public void setLevel(byte level) {
        this.level = level;
    }

    public byte getLevel() {
        return level;
    }

    public Alarm(int hour, int minutes, byte week, boolean enabled, byte level, int order) {
        this.hour = hour;
        this.minutes = minutes;
        this.weekdays = week;
        this.enabled = enabled;
        this.level = level;
        this.orderNumber = order;
    }

    public String toString() {
        SimpleDateFormat ft = new SimpleDateFormat ("hh:mm a");
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY,hour);
        cal.set(Calendar.MINUTE,minutes);
        Date d = cal.getTime();
        return ft.format(d);
    }

    public String toStringDebug() {
        return "[" + orderNumber + "]" + toString() + " - " + String.format("%02X ", weekdays) + " : " +
                (enabled ? "enabled" : "disabled") + " : " +
                ((level == IMPORTANT) ? "Important" : ((level == MEDICINE) ? "Medicine" : "GENERAL"));
    }

    public boolean isDaySet(byte d) {
        if((d & weekdays) == 0)
            return false;
        return true;
    }

    public void setTime(int hourOfDay, int minute) {
        this.hour = hourOfDay;
        this.minutes = minute;
    }

    public int getHour() {
        return hour;
    }

    public int getMinutes() {
        return minutes;
    }

    public byte getWeekdays() {
        return weekdays;
    }

    public void flipDay(byte day) {
        weekdays = (byte) (weekdays ^ day);
    }


    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(orderNumber);
        out.writeInt(hour);
        out.writeInt(minutes);
        out.writeByte(weekdays);
        out.writeByte(level);
        out.writeByte(enabled ? (byte) 0x1 : (byte) 0x0);
    }

    public static final Parcelable.Creator<Alarm> CREATOR = new Parcelable.Creator<Alarm>() {
        public Alarm createFromParcel(Parcel in) {
            return new Alarm(in);
        }

        public Alarm[] newArray(int size) {
            return new Alarm[size];
        }
    };

    private Alarm(Parcel in) {
        orderNumber = in.readInt();
        hour = in.readInt();
        minutes = in.readInt();
        weekdays = in.readByte();
        level = in.readByte();
        enabled = (in.readByte() == 0x1 ? true : false);
    }

    @Override
    public int compareTo(Alarm another) {
        if(orderNumber < another.orderNumber)
            return -1;
        else if(orderNumber > another.orderNumber)
            return 1;
        return 0;
    }
}
