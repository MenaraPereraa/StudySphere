package lk.menara.studysphere.util;

import android.text.format.DateFormat;

import androidx.fragment.app.Fragment;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class DateTimePickerHelper {

    public interface DateTimeCallback {
        void onDateTimeSelected(Timestamp timestamp, String formattedString);
    }

    public static void showDateTimePicker(Fragment fragment, Timestamp initialTimestamp, DateTimeCallback callback) {
        Calendar local = Calendar.getInstance();
        if (initialTimestamp != null) {
            local.setTime(initialTimestamp.toDate());
        }

        Calendar utcInitial = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcInitial.clear();
        utcInitial.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH));

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Tracking Date")
                .setSelection(utcInitial.getTimeInMillis())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar picked = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            picked.setTimeInMillis(selection);

            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(
                    picked.get(Calendar.YEAR),
                    picked.get(Calendar.MONTH),
                    picked.get(Calendar.DAY_OF_MONTH),
                    local.get(Calendar.HOUR_OF_DAY),
                    local.get(Calendar.MINUTE),
                    0);
            selectedDate.set(Calendar.MILLISECOND, 0);

            showTimePicker(fragment, selectedDate, callback);
        });

        datePicker.show(fragment.getParentFragmentManager(), "DATE_PICKER");
    }

    private static void showTimePicker(Fragment fragment, Calendar selectedDate, DateTimeCallback callback) {
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTitleText("Select Tracking Time")
                .setHour(selectedDate.get(Calendar.HOUR_OF_DAY))
                .setMinute(selectedDate.get(Calendar.MINUTE))
                .setTimeFormat(DateFormat.is24HourFormat(fragment.requireContext())
                        ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H)
                .build();

        timePicker.addOnPositiveButtonClickListener(v -> {
            selectedDate.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
            selectedDate.set(Calendar.MINUTE, timePicker.getMinute());

            Timestamp timestamp = new Timestamp(selectedDate.getTime());
            callback.onDateTimeSelected(timestamp, formatDateTime(timestamp));
        });

        timePicker.show(fragment.getParentFragmentManager(), "TIME_PICKER");
    }

    public static String formatDateTime(Timestamp timestamp) {
        if (timestamp == null) return "Not set";
        return new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                .format(timestamp.toDate());
    }
}