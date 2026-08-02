package com.winlator.cmod.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;

import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.winlator.cmod.R;
import com.winlator.cmod.MainActivity;

public class NotificationService extends Service {
    private static boolean isRunning = false;
	private static boolean isContainerActive = false;
	private BroadcastReceiver screenStateReceiver;
    public static PowerManager.WakeLock wakeLock = null;
	private static volatile SharedPreferences prefs;
	private static final String PREF_USE_WAKELOCK = "enable_background_wakelock";
    
    public static boolean isRunning() {
        return isRunning;
    }
	public static void setContainerActive(boolean isActive) {
		isContainerActive = isActive;
	}
    
	@Override
	public void onCreate() {
		super.onCreate();

		PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NotificationService::KeepAlive");
		wakeLock.setReferenceCounted(false);

		prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		// Screen-lock detection.
		screenStateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (Intent.ACTION_SCREEN_OFF.equals(action)) {
					acquireWakeLock();
				} else if (Intent.ACTION_USER_PRESENT.equals(action)) {
					if (wakeLock != null && wakeLock.isHeld()) {
						wakeLock.release();
					}
				}
			}
		};

		IntentFilter screenFilter = new IntentFilter();
		screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
		screenFilter.addAction(Intent.ACTION_USER_PRESENT);
		registerReceiver(screenStateReceiver, screenFilter);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {	
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            stopSelf();
            return START_NOT_STICKY;
        }
        
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MainActivity.NOTIFICATION_CHANNEL_ID)
			.setSmallIcon(R.drawable.ic_stat_ab_gear_0011)
			.setContentTitle("Winlator")
			.setContentText("Winlator is running, do not kill or swipe this notification")
			.setPriority(NotificationCompat.PRIORITY_LOW)
		 	.setContentIntent(pendingIntent)
		 	.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
		 	.setOngoing(true);
		 
		Notification notification = builder.build();
		startForeground(MainActivity.NOTIFICATION_ID, notification);
        
        isRunning = true;

		return START_NOT_STICKY;
	}

	@Override
	public void onTaskRemoved(Intent rootIntent) {
		stopForeground(STOP_FOREGROUND_REMOVE);
		stopSelf();
        isRunning = false;
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
		android.os.Process.killProcess(android.os.Process.myPid());
	}
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
		if (screenStateReceiver != null) {
			try { unregisterReceiver(screenStateReceiver); } catch (Exception ignored) {}
			screenStateReceiver = null;
		}
    }

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private void acquireWakeLock() {
        if (!isContainerActive || wakeLock == null || prefs == null) return;
        if (!prefs.getBoolean(PREF_USE_WAKELOCK, false)) return;
		if (!wakeLock.isHeld()) {
			wakeLock.acquire();
		}
	}
}
