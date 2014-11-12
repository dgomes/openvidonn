/*
Copyright 2014 Diogo Gomes <diogogomes@gmail.com>

This file is part of OpenVidonn.

OpenVidonn is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

OpenVidonn is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with Foobar. If not, see http://www.gnu.org/licenses/.
*/
package com.diogogomes.openvidonn.app.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by dgomes on 31/05/14.
 */
public class DayHistory implements Serializable {


    private Calendar date = null;
    private int steps[] = new int[24]; // 0h00 to 23h00
    private int distance[] = new int[24]; // 0h00 to 23h00

    DayHistory(Calendar d) {
        this.date = d;
    }

    public Calendar getCalendar() {
        return date;
    }

    public String getDate(String simpleDateFormat) {
        SimpleDateFormat formatter=new SimpleDateFormat(simpleDateFormat);
        String currentDate = formatter.format(date.getTime());
        return currentDate;
    }

    public int [] getSteps() {
        return steps;
    }

    public int [] getDistance() {
        return distance;
    }

    public int getTotalSteps() {
        int sum = 0;
        for(int i : steps) {
            sum+=i;
        }
        return sum;
    }

    public int getTotalDistance() {
        int sum = 0;
        for(int i : distance) {
            sum+=i;
        }
        return sum;
    }

    public String toString() {

        SimpleDateFormat formatter=new SimpleDateFormat("yyyy-MM-dd");
        String sdate = formatter.format(date.getTime());

        String str =  "[" + sdate + "]\tsteps:[";
        for(int i=0; i<steps.length-1; i++)
            str+=steps[i] + ", ";
        str+=steps[steps.length-1];

        str+="] - distances:[";

        for(int i=0; i<distance.length-1; i++)
            str+=distance[i] + ", ";
        str+=distance[distance.length-1] + "]";
        return str;
    }

    public void setSteps(int page, int[] data) {
        System.arraycopy(data, 0, this.steps, page*6, data.length );//there are 4 pages of 6 entries each (data.length)
    }

    public void setDistance(int page, int[] data) {
        System.arraycopy(data, 0, this.distance, page*6, data.length );
    }
}
