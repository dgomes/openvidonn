package com.diogogomes;

/**
 * Created by dgomes on 28/05/14.
 *
 * Copy & Paste from:
 * https://github.com/wadael/DatePreference
 */
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.DatePicker;

public class DatePreference extends DialogPreference implements
        DatePicker.OnDateChangedListener {
    private String dateString;
    private String changedValueCanBeNull;
    private DatePicker datePicker;

    public DatePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public DatePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Produces a DatePicker set to the date produced by {@link #getDate()}. When
     * overriding be sure to call the super.
     *
     * @return a DatePicker with the date set
     */
    @Override
    protected View onCreateDialogView() {
        this.datePicker = new DatePicker(getContext());
        Calendar calendar = getDate();
        datePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH), this);
        datePicker.setCalendarViewShown(false); /* else shows day + month + (calendarview instead of year) on api v19/4.4 */
        return datePicker;
    }

    /**
     * Produces the date used for the date picker. If the user has not selected a
     * date, produces the default from the XML's android:defaultValue. If the
     * default is not set in the XML or if the XML's default is invalid it uses
     * the value produced by {@link #defaultCalendar()}.
     *
     * @return the Calendar for the date picker
     */
    public Calendar getDate() {
        try {
            Date date = formatter().parse(defaultValue());
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return cal;
        } catch (java.text.ParseException e) {
            return defaultCalendar();
        }
    }

    /**
     * Set the selected date to the specified string.
     *
     * @param dateString
     *          The date, represented as a string, in the format specified by
     *          {@link #formatter()}.
     */
    public void setDate(String dateString) {
        this.dateString = dateString;
    }

    /**
     * Produces the date formatter used for dates in the XML. The default is yyyy.MM.dd.
     * Override this to change that.
     *
     * @return the SimpleDateFormat used for XML dates
     */
    public static SimpleDateFormat formatter() {
        return new SimpleDateFormat("yyyy.MM.dd");
    }

    /**
     * Produces the date formatter used for showing the date in the summary. The default is MMMM dd, yyyy.
     * Override this to change it.
     *
     * @return the SimpleDateFormat used for summary dates
     */
    public static SimpleDateFormat summaryFormatter() {
        return new SimpleDateFormat("MMMM dd, yyyy");
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    /**
     * Called when the date picker is shown or restored. If it's a restore it gets
     * the persisted value, otherwise it persists the value.
     */
    @Override
    protected void onSetInitialValue(boolean restoreValue, Object def) {
        if (restoreValue) {
            this.dateString = getPersistedString(defaultValue());
            setTheDate(this.dateString);
        } else {
            boolean wasNull = this.dateString == null;
            setDate((String) def);
            if (!wasNull)
                persistDate(this.dateString);
        }
    }

    /**
     * Called when Android pauses the activity.
     */
    @Override
    protected Parcelable onSaveInstanceState() {
        if (isPersistent())
            return super.onSaveInstanceState();
        else
            return new SavedState(super.onSaveInstanceState());
    }

    /**
     * Called when Android restores the activity.
     */
    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(state);
            setTheDate(((SavedState) state).dateValue);
        } else {
            SavedState s = (SavedState) state;
            super.onRestoreInstanceState(s.getSuperState());
            setTheDate(s.dateValue);
        }
    }

    /**
     * Called when the user changes the date.
     */
    public void onDateChanged(DatePicker view, int year, int month, int day) {
        Calendar selected = new GregorianCalendar(year, month, day);
        this.changedValueCanBeNull = formatter().format(selected.getTime());
    }

    /**
     * Called when the dialog is closed. If the close was by pressing "OK" it
     * saves the value.
     */
    @Override
    protected void onDialogClosed(boolean shouldSave) {
        if (shouldSave && this.changedValueCanBeNull != null) {
            setTheDate(this.changedValueCanBeNull);
            this.changedValueCanBeNull = null;
        }
    }

    private void setTheDate(String s) {
        setDate(s);
        persistDate(s);
    }

    private void persistDate(String s) {
        persistString(s);
        setSummary(summaryFormatter().format(getDate().getTime()));
    }

    /**
     * The default date to use when the XML does not set it or the XML has an
     * error.
     *
     * @return the Calendar set to the default date
     */
    public static Calendar defaultCalendar() {
        return new GregorianCalendar(1970, 0, 1);
    }

    /**
     * The defaultCalendar() as a string using the {@link #formatter()}.
     *
     * @return a String representation of the default date
     */
    public static String defaultCalendarString() {
        return formatter().format(defaultCalendar().getTime());
    }

    private String defaultValue() {
        if (this.dateString == null)
            setDate(defaultCalendarString());
        return this.dateString;
    }

    /**
     * Called whenever the user clicks on a button. Invokes {@link #onDateChanged(DatePicker, int, int, int)}
     * and {@link #onDialogClosed(boolean)}. Be sure to call the super when overriding.
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        datePicker.clearFocus();
        onDateChanged(datePicker, datePicker.getYear(), datePicker.getMonth(),
                datePicker.getDayOfMonth());
        onDialogClosed(which == DialogInterface.BUTTON1); // OK?
    }

    /**
     * Produces the date the user has selected for the given preference, as a
     * calendar.
     *
     * @param preferences
     *          the SharedPreferences to get the date from
     * @param field
     *          the name of the preference to get the date from
     * @return a Calendar that the user has selected
     */
    public static Calendar getDateFor(SharedPreferences preferences, String field) {
        Date date = stringToDate(preferences.getString(field,
                defaultCalendarString()));
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }

    private static Date stringToDate(String dateString) {
        try {
            return formatter().parse(dateString);
        } catch (ParseException e) {
            return defaultCalendar().getTime();
        }
    }

    private static class SavedState extends BaseSavedState {
        String dateValue;

        public SavedState(Parcel p) {
            super(p);
            dateValue = p.readString();
        }

        public SavedState(Parcelable p) {
            super(p);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeString(dateValue);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}