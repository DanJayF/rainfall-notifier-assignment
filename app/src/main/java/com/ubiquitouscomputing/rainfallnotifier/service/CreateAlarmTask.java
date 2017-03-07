package com.ubiquitouscomputing.rainfallnotifier.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

public class CreateAlarmTask {

    private AlarmManager manager;
    private PendingIntent alarmIntent;

    //Constructor to populate AlarmManager and PendingIntent objects
    public CreateAlarmTask(Context context) {
        manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent receiverIntent = new Intent(context, OnScheduleReceiver.class);
        alarmIntent = PendingIntent.getBroadcast(context, 1, receiverIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    //Setup a new alarm
    public void startAlarm(String hour) {

        //Cancel any existing alarms
        cancelAlarms();

        //Convert hour String to int
        int hourInt = Integer.parseInt(hour);

        //Set up alarm time
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hourInt);

        //Schedule the repeating alarm
        manager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                alarmIntent);  //Set repeating every 24 hours
    }

    //Cancel any existing alarms
    public void cancelAlarms() {
        if (manager != null) {
            manager.cancel(alarmIntent);
        }
    }
}
