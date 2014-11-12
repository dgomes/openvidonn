/*
Copyright 2014 Diogo Gomes <diogogomes@gmail.com>

This file is part of OpenVidonn.

OpenVidonn is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

OpenVidonn is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with Foobar. If not, see http://www.gnu.org/licenses/.
*/

package com.diogogomes.openvidonn.app;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;

import com.diogogomes.openvidonn.app.model.Alarm;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

/**
 * Created by dgomes on 19/08/14.
 */
public class AlarmArrayAdapter extends BaseAdapter {
    private static String TAG = AlarmArrayAdapter.class.getSimpleName();
    private final AlarmsFragment alarmsFragment;

    private ArrayList<Alarm> alarms;
    private LayoutInflater mInflator;
    private FragmentManager fragmentManager;



    public AlarmArrayAdapter(Context context, FragmentManager fm, AlarmsFragment parent) {
        super();
        mInflator = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        fragmentManager = fm;
        alarmsFragment = parent;
        alarms = new ArrayList<Alarm>();
    }

    public void addAlarm(Alarm alarm) {
        if(!alarms.contains(alarm)) {
            alarms.add(alarm);
        }
        this.notifyDataSetChanged();
    }

    public void sort() {
        Collections.sort(alarms);
        this.notifyDataSetChanged();
    }

    public Alarm getAlarm(int position) {
        return alarms.get(position);
    }

    public String toString() {
        String s = "";
        for(Alarm a : alarms)
            s += a.toString() + "\n";
        return s;
    }

    @Override
    public int getCount() {
        return alarms.size();
    }

    @Override
    public Object getItem(int position) {
        return alarms.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        final ViewHolder viewHolder;
        // General ListView optimization code.
        if (convertView == null) {
            convertView = mInflator.inflate(R.layout.listitem_alarm, null);
            viewHolder = new ViewHolder();
            viewHolder.textClock = (TextView) convertView.findViewById(R.id.textClock);
            viewHolder.week[0] = (ToggleButton) convertView.findViewById(R.id.tbSun);
            viewHolder.week[1] = (ToggleButton) convertView.findViewById(R.id.tbMon);
            viewHolder.week[2] = (ToggleButton) convertView.findViewById(R.id.tbTue);
            viewHolder.week[3] = (ToggleButton) convertView.findViewById(R.id.tbWed);
            viewHolder.week[4] = (ToggleButton) convertView.findViewById(R.id.tbThu);
            viewHolder.week[5] = (ToggleButton) convertView.findViewById(R.id.tbFri);
            viewHolder.week[6] = (ToggleButton) convertView.findViewById(R.id.tbSat);
            viewHolder.alarmSwitch = (SeekBar) convertView.findViewById(R.id.alarmSwitch);
            viewHolder.textDescription = (TextView) convertView.findViewById(R.id.textDescription);
            viewHolder.weekList = (LinearLayout) convertView.findViewById(R.id.weeklist);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final Alarm alarm = alarms.get(position);
        final AlarmArrayAdapter isto = this;  //hack to pass this into onCheckedChanged

        viewHolder.textClock.setText(alarm.toString());
        viewHolder.textClock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "I clicked!!!");
                DialogFragment newFragment = new TimePickerFragment();
                Bundle args = new Bundle();
                args.putParcelable(TimePickerFragment.TIMEPICKER_ALARM, alarm);
                newFragment.setTargetFragment(alarmsFragment, 1);
                newFragment.setArguments(args);
                newFragment.show(fragmentManager, "timePicker");
            }
        });

        viewHolder.alarmSwitch.setOnSeekBarChangeListener(null); //disable listener so that next line doesn't restore everything back
        switch (alarm.getLevel()) {
            case 0:
                viewHolder.textDescription.setText(convertView.getResources().getString(R.string.descrition_off));
                viewHolder.alarmSwitch.setProgress(0);
                viewHolder.weekList.setVisibility(View.GONE);
                break;
            case Alarm.GENERAL:
                viewHolder.textDescription.setText(convertView.getResources().getString(R.string.descrition_general));
                viewHolder.alarmSwitch.setProgress(1);
                viewHolder.weekList.setVisibility(View.VISIBLE);
                break;
            case Alarm.MEDICINE:
                viewHolder.textDescription.setText(convertView.getResources().getString(R.string.descrition_medicine));
                viewHolder.alarmSwitch.setProgress(2);
                viewHolder.weekList.setVisibility(View.VISIBLE);
                break;
            case Alarm.IMPORTANT:
                viewHolder.textDescription.setText(convertView.getResources().getString(R.string.descrition_important));
                viewHolder.alarmSwitch.setProgress(3);
                viewHolder.weekList.setVisibility(View.VISIBLE);
                break;
        }
        viewHolder.alarmSwitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
              @Override
              public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                  if (progress > 0) {
                      alarm.enableAlarm();
                      viewHolder.weekList.setVisibility(View.VISIBLE);
                  } else {
                      alarm.disableAlarm();
                      viewHolder.weekList.setVisibility(View.GONE);
                  }
                  switch (progress) {
                    case 0:
                        alarm.setLevel((byte) 0);
                        viewHolder.textDescription.setText(seekBar.getResources().getString(R.string.descrition_off));
                        break;
                    case 1:
                        alarm.setLevel(Alarm.GENERAL);
                        viewHolder.textDescription.setText(seekBar.getResources().getString(R.string.descrition_general));
                        break;
                    case 2:
                        alarm.setLevel(Alarm.MEDICINE);
                        viewHolder.textDescription.setText(seekBar.getResources().getString(R.string.descrition_medicine));
                        break;
                    case 3:
                        alarm.setLevel(Alarm.IMPORTANT);
                        viewHolder.textDescription.setText(seekBar.getResources().getString(R.string.descrition_important));
                        break;
                }
              }

              @Override
              public void onStartTrackingTouch(SeekBar seekBar) {

              }

              @Override
              public void onStopTrackingTouch(SeekBar seekBar) {
                //TODO save to bracelet
                alarmsFragment.saveAlarms(alarms);
              }
        });

        int i = 0;
        for(ToggleButton dayOfWeek : viewHolder.week) {
            dayOfWeek.setOnCheckedChangeListener(null); //disable listener so that next line doesn't restore everything back
            dayOfWeek.setChecked(alarm.isDaySet(Alarm.alarmWeekDays[i]));
            dayOfWeek.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    switch(buttonView.getId()) {
                        case R.id.tbSun:
                            alarm.flipDay(Alarm.SUNDAY);
                            break;
                        case R.id.tbMon:
                            alarm.flipDay(Alarm.MONDAY);
                            break;
                        case R.id.tbTue:
                            alarm.flipDay(Alarm.TUESDAY);
                            break;
                        case R.id.tbWed:
                            alarm.flipDay(Alarm.WEDNESDAY);
                            break;
                        case R.id.tbThu:
                            alarm.flipDay(Alarm.THURSDAY);
                            break;
                        case R.id.tbFri:
                            alarm.flipDay(Alarm.FRIDAY);
                            break;
                        case R.id.tbSat:
                            alarm.flipDay(Alarm.SATURDAY);
                            break;
                    }
                    isto.notifyDataSetChanged();
                }
            });
            i++;
        }

        return convertView;
    }


    static class ViewHolder {
        TextView textClock;
        TextView textDescription;
        ToggleButton week[] = new ToggleButton[7];
        SeekBar alarmSwitch;
        LinearLayout weekList;
    }


    public static class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {
        private static final java.lang.String TIMEPICKER_ALARM = "Alarm";

        private Alarm alarm;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Bundle bundle = this.getArguments();
            if(bundle.containsKey(TIMEPICKER_ALARM)) {
                this.alarm = (Alarm) bundle.getParcelable(TIMEPICKER_ALARM);
            } else {
                final Calendar c = Calendar.getInstance();
                int hour = c.get(Calendar.HOUR_OF_DAY);
                int minute = c.get(Calendar.MINUTE);
                this.alarm = new Alarm(hour, minute, (byte) 0, false, (byte) 0, 0);
            }

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, alarm.getHour(), alarm.getMinutes(),
                    DateFormat.is24HourFormat(getActivity()));
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            // Do something with the time chosen by the user
            alarm.setTime(hourOfDay, minute);
            Fragment targetFragment = getTargetFragment();
            if (targetFragment != null) {
                targetFragment.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, null);
            }
        }
    }


}
