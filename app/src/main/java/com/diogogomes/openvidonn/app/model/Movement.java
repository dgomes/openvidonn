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

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by dgomes on 26/05/14.
 */
public class Movement implements Parcelable {
    private static final String TAG = Movement.class.getSimpleName() ;
    private int distance;
    private int steps;
    private Calendar currentTime;
    private int calories;

    public int getSteps() {
        return steps;
    }

    public Movement(Calendar currentTime, int steps, int distance, int calories) {
        this.currentTime = currentTime;
        this.steps = steps;
        this.distance = distance;
        this.calories = calories;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeSerializable(currentTime);
        out.writeInt(steps);
        out.writeInt(distance);
        out.writeInt(calories);
    }

    public static final Parcelable.Creator<Movement> CREATOR = new Parcelable.Creator<Movement>() {
        public Movement createFromParcel(Parcel in) {
            return new Movement(in);
        }

        public Movement[] newArray(int size) {
            return new Movement[size];
        }
    };

    private Movement(Parcel in) {
        currentTime = (Calendar) in.readSerializable();
        steps = in.readInt();
        distance = in.readInt();
        calories = in.readInt();
    }

    public Calendar getCurrentTime() {
        return currentTime;
    }

    public String toString() {
        return "[" + getCurrent("yyyy-MM-dd") + "]\tsteps:" + steps + "\tdistance:" + distance + "\tcalories:" + calories;
    }

    public String getCurrent(String simpleDateFormat) {
        SimpleDateFormat formatter=new SimpleDateFormat(simpleDateFormat);
        String currentDate = formatter.format(currentTime.getTime());
        return currentDate;
    }

    public int getCalories() {
        return calories;
    }
}
