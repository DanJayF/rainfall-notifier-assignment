package com.ubiquitouscomputing.rainfallnotifier.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

/**
 * BroadcastReceiver for any CreateAlarmTask.
 * Acquires a wakelock when alarm triggered, runs the RainCheckService
 * then releases the wakelock.
 */
public class OnScheduleReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        //Acquire a partial WakeLock
        PowerManager mgr = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RainCheckWakelock");
        wakeLock.acquire();

        //Start RainCheckService
        context.startService(new Intent(context, RainCheckService.class));

        //Release the WakeLock
        wakeLock.release();
    }
}
