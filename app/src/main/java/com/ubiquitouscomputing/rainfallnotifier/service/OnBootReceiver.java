package com.ubiquitouscomputing.rainfallnotifier.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * BroadcastReceiver for android.intent.action.BOOT_COMPLETED
 * Restarts the repeating notification alarm on boot.
 */
public class OnBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {

            //Retrieve notificationTime preference
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String notifTime = prefs.getString("notificationTime", "06");
            boolean toggleState = prefs.getBoolean("notificationToggle", false);

            //Recreate the alarm if service enabled
            if(toggleState) {
                new CreateAlarmTask(context).startAlarm(notifTime);
            }
        }
    }
}
