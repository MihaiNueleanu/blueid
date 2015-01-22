package com.blueid.blueid.blueid;

import java.util.Random;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class LocalService extends Service {
    private int mStartMode = START_REDELIVER_INTENT;       // indicates how to behave if the service is killed
	private IBinder binder = new MyBinder(); // interface for clients that bind
    private boolean mAllowRebind = false;   // indicates whether onRebind should be used
    private Random mGenerator = new Random();

    public static final int ONGOING_NOTIFICATION_ID = 100; //ID of notification, can be any number



    //The system calls this method when the service is first created, to perform one-time setup
    //procedures (before it calls either onStartCommand() or onBind()).
    //If the service is already running, this method is not called.
    @Override
    public void onCreate() {
        //the service is being created

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The service is starting, due to a call to startService()
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        Log.d("Goku", "In ServiceStart received this intent:" + intent.getStringExtra("Username") + intent.getStringExtra("SessionKey"));
        //make foreground service that always displays a notification when started
        Notification notification = new Notification(R.drawable.ic_logo, "TestString",
                System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, LoggedInActivity.class);
        notificationIntent.putExtra("Username",intent.getStringExtra("Username"));
        notificationIntent.putExtra("SessionKey",intent.getStringExtra("SessionKey"));

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, "TestTitle",
                "TestNotificationMessage", pendingIntent);
        startForeground(ONGOING_NOTIFICATION_ID, notification); //could be any ID


        // If we get killed, after returning from here, restart + give the intent back
        return mStartMode;
        //return super.onStartCommand(intent,flags,startId);
    }

    //TODO include stopSelf() method when timer expires (8 hours pass)

	@Override
	public IBinder onBind(Intent arg0) {
        // A client is binding to the service with bindService()
        Log.d("Goku", "In onBind received this intent:" + arg0.getStringExtra("Username") + arg0.getStringExtra("SessionKey"));

        return binder;
	}
    @Override
    public boolean onUnbind(Intent intent) {
        // All clients have unbound with unbindService()
        return mAllowRebind;
    }

    @Override
    public void onRebind(Intent intent) {
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called
      // onRebind() returns void, but the client still receives the IBinder in its onServiceConnected()
      // callback.
    }




	 /** methods for clients */
    public  int getRandomNumber() {
      return mGenerator.nextInt(100);
    }

    // The system calls this method when the service is no longer used and is being destroyed.
    //Your service should implement this to clean up any resources such as threads,
    //registered listeners, receivers, etc. This is the last call the service receives.
    @Override
    public void onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();


    }


    public class MyBinder extends Binder {

    	LocalService getService() {
            // Return this instance of LocalService so clients can call public methods
            return LocalService.this;
        }
    }

}
