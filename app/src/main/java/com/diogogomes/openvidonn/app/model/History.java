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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TreeMap;

/**
 * Created by dgomes on 29/05/14.
 */
public class History implements Parcelable {
    private static final String TAG = History.class.getSimpleName();



    public enum ENTRY_TYPE {STEPS, DISTANCE};
    TreeMap<Calendar, DayHistory> week = new TreeMap<Calendar, DayHistory>();
    int countEntries = 0;

    public History() {}

    public DayHistory addEntry(Calendar date, ENTRY_TYPE type, int page, int data[]) {
        String str = "";
        for (int i : data) {
            str += i + ", ";
        }
        Log.d("addEntry()", "Type:" + (type == ENTRY_TYPE.STEPS ? "steps" : "distance") + "\tpage:" + page+ "\tdata: "+str);
        DayHistory entry = week.get(date);
        if(entry == null) {
            Log.d("addEntry()", "New entry");
            entry = new DayHistory(date);
        }

        switch (type) {
            case STEPS:
                entry.setSteps(page, data);
                break;
            case DISTANCE:
                entry.setDistance(page, data);
                break;
        }
        week.put(date, entry);

        countEntries++;

        Log.d(TAG, "Entries: " + countEntries);

        return entry;
    }

    public boolean isComplete() {
        Log.d(TAG, "isComplete" + (countEntries >= 56));
        if(countEntries >= 56)
            return true;

        return false;
    }

    public DayHistory [] getLastWeek() {
        DayHistory last[] = new DayHistory[7];
        int i = 0;
        for (Calendar k : week.keySet()) { //TreeMap guarantees everything is sorted
            last[i] = week.get(k);
            i++;
        }
        return last;
    }

    public int getTotalDistance() {
        int total = 0;
        for (Calendar k : week.keySet()) { //TreeMap guarantees everything is sorted
            total += week.get(k).getTotalDistance();
        }
        return total;
    }

    public DayHistory getDay(int day) {
        for( Calendar k : week.keySet()) {
            if(day == k.get(Calendar.DAY_OF_WEEK))
                return week.get(k);
        }
        return null;
    }

    public String toString() {
        String str = "";

        for (Calendar k : week.keySet()) {
            str+=week.get(k)+"\n";
        }

        return str;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeSerializable(week);
    }

    public static final Parcelable.Creator<History> CREATOR = new Parcelable.Creator<History>() {
        public History createFromParcel(Parcel in) {
            return new History(in);
        }

        public History[] newArray(int size) {
            return new History[size];
        }
    };

    private History(Parcel in) {
        week = (TreeMap<Calendar, DayHistory>) in.readSerializable();
    }
}