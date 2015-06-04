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

import org.bostonandroid.datepreference.DatePreference;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by dgomes on 01/06/14.
 */
public class PersonalInfo implements Parcelable {
    private static final String TAG = Movement.class.getSimpleName() ;

    public enum GENDER {FEMALE, MALE};

    public int height;
    public int weight;
    private GENDER gender;
    public int age;


    public PersonalInfo(int height, int weight, int gender, int age) {
        this.height = height;
        this.weight = weight;
        this.gender = gender==1 ? GENDER.MALE : GENDER.FEMALE;
        this.age = age;
    }

    public PersonalInfo(String height, String weight, GENDER gender, String birthday_string) {
        this.height = Integer.parseInt(height);
        this.weight = Integer.parseInt(weight);
        this.gender = gender;


        try {
            Calendar birthday = new GregorianCalendar();
            birthday.setTime(DatePreference.formatter().parse(birthday_string));
            this.age = calculateAge(birthday);

        } catch (ParseException e) {
            e.printStackTrace();
        }


    }

    public static int calculateAge(Calendar birthday) {
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - birthday.get(Calendar.YEAR);

        //Adjust age
        birthday.set(Calendar.YEAR, today.get(Calendar.YEAR));
        if(today.before(birthday))
            age--;

        return age;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(height);
        out.writeInt(weight);
        out.writeInt(gender == GENDER.MALE ? 1 : 0);
        out.writeInt(age);
    }

    public static final Parcelable.Creator<PersonalInfo> CREATOR = new Parcelable.Creator<PersonalInfo>() {
        public PersonalInfo createFromParcel(Parcel in) {
            return new PersonalInfo(in);
        }

        public PersonalInfo[] newArray(int size) {
            return new PersonalInfo[size];
        }
    };

    private PersonalInfo(Parcel in) {
        this.height = in.readInt();
        this.weight = in.readInt();
        this.gender = in.readInt()==1 ? GENDER.MALE : GENDER.FEMALE;
        this.age = in.readInt();
    }

    public String toString() {
        return "Height:" + height + "\tWeight:" + weight + "\tGender:" + (gender == GENDER.MALE? "Male" : "Female") + "\tAge:" + age;
    }

    public String getHeight() {
        return Integer.toString(height);
    }
    public String getWeight() {
        return Integer.toString(weight);
    }

    public int getGender() {
        return (gender == GENDER.MALE ? 1 : 0);
    }

    public GENDER getPreferenceGender() {
        return gender;
    }
    public String getApproximateBirthday() {
        Calendar bday = Calendar.getInstance();
        bday.clear();
        bday.set(Calendar.YEAR, bday.get(Calendar.YEAR)-age);

        SimpleDateFormat formatter = DatePreference.formatter();
        return formatter.format(bday.getTime());
    }

    public int getAge() {
        return age;

        /* TODO save Calendar not age
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - birthday.get(Calendar.YEAR);

        //Adjust age
        birthday.set(Calendar.YEAR, today.get(Calendar.YEAR));
        if(today.before(birthday))
            age--; */
    }




}
