package com.blueid.blueid.blueid;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class LocalService extends Service {
    private int mStartMode = START_REDELIVER_INTENT;       // indicates how to behave if the service is killed
	private IBinder binder = new MyBinder(); // interface for clients that bind
    private boolean mAllowRebind = false;   // indicates whether onRebind should be used
    private Random mGenerator = new Random();
    public static boolean isRunning = false;

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
        isRunning = true;
        // The service is starting, due to a call to startService()
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        Log.d("Goku", "In ServiceStart received this intent:" + intent.getStringExtra("Username") + intent.getStringExtra("SessionKey"));

        //make foreground service that always displays a notification when started
        startNotification(intent);
        //start the actual Bluetooth scanning & co.
        startBackgroundScanning();


        // If we get killed, after returning from here, restart + give the intent back
        return mStartMode;
        //return super.onStartCommand(intent,flags,startId);
    }



    private void startNotification(Intent intent) {
        Notification notification = new Notification(R.drawable.ic_logo, "TestString",
                System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, LoggedInActivity.class);
        notificationIntent.putExtra("Username",intent.getStringExtra("Username"));
        notificationIntent.putExtra("SessionKey",intent.getStringExtra("SessionKey"));
        Log.d("Goku", "In ServiceStart we create notification intent with extras:" + notificationIntent.getStringExtra("Username") + notificationIntent.getStringExtra("SessionKey"));
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        notification.setLatestEventInfo(this, "TestTitle",
                "TestNotificationMessage", pendingIntent);
        startForeground(ONGOING_NOTIFICATION_ID, notification); //could be any ID
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
        isRunning = false;

        //todo added this
        unregisterReceiver(mReceiver);
        // check if connection is there
        if (ct != null)
            ct.cancel();
    }


    public class MyBinder extends Binder {

    	LocalService getService() {
            // Return this instance of LocalService so clients can call public methods
            return LocalService.this;
        }
    }


    /////////////////////////////////////////////////////////////////////////////////////
    /// Here we start the bluetooth part ///////////
    ////////////////////////////////////////////////////////////////////////////////////
    private void startBackgroundScanning() {
        //todo here we do the autoscanning
        connectToGate();
  }

    private void connectToGate() {
        // register for connected device ACL status
        // Register the BroadcastReceiver
        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister
        // during onDestroy
        registerReceiver(mReceiver, new IntentFilter(
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        IntentFilter filter1 = new IntentFilter(
                BluetoothDevice.ACTION_ACL_CONNECTED);
        IntentFilter filter2 = new IntentFilter(
                BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        IntentFilter filter3 = new IntentFilter(
                BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(mReceiver, filter1);
        this.registerReceiver(mReceiver, filter2);
        this.registerReceiver(mReceiver, filter3);

        // ... /// in place of button listeners

        // Test for adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(),
                    "Nope. No bluetooth. Sorry", Toast.LENGTH_LONG).show();
            stopSelf();// Device does not support Bluetooth. Exit application
        }

        if (!mBluetoothAdapter.isEnabled()) {
            // we can ask the user to turn it on
            // Intent enableBtIntent = new
            // Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            // startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

            // or we can turn it on ourselves MUHAHHA
            mBluetoothAdapter.enable();
            // Wait until the Bluetooth Adapter is enabled before continuing
            while (!mBluetoothAdapter.isEnabled())
                ;

        }

        // Check for already paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
                .getBondedDevices();
        Log.d("Goku", pairedDevices.toString());
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a
                // ListView
                // et.setText(et.getText().toString()+ device.getName() + " - "
                // + device.getAddress() + "\n");
                Log.d("Goku", "For the device " + device.getName()
                        + " we have the UUID:" + device.getUuids());

                if (device.getAddress().equals("00:12:10:23:04:18")) {
                    Log.d("Goku", "got in the connect");
                    new ConnectThread(device).run();
                    Log.d("Goku", "got after the connect");

                }
            }
        }

        // try to manually pair the devices based on the known mac(no prior
        // bonding)
        // This will prompt the user to pair with the gate if he didn't pair
        // before
        BluetoothDevice bd = mBluetoothAdapter
                .getRemoteDevice("00:12:10:23:04:18");
        if (bd.getBondState() == BluetoothDevice.BOND_NONE)
            // Use reflection to get access to hidden method setpin
            try {
                Log.d("Goku", "Try to set the PIN");
                Method m = bd.getClass().getMethod("setPin", byte[].class);
                m.invoke(bd, "1234".getBytes());
                Log.d("Goku", "Success to add the PIN.");
//                et.setText(et.getText().toString()
//                        + "Success in adding the PIN");

                bd.getClass()
                        .getMethod("setPairingConfirmation", boolean.class)
                        .invoke(bd, true);
                // bd.getClass().getMethod("cancelPairingUserInput",
                // boolean.class).invoke(bd);
                // bd.getClass().getMethod("createBond", (Class[])
                // null).invoke(bd, (Object[]) null);
                // et.setText(et.getText().toString() +
                // "Success in creating the bond");
                // this will probably block the thread so watch out
                // while(bd.getBondState()!=BluetoothDevice.BOND_BONDED) ;

            } catch (Exception e) {
                Log.e("setPin()", e.toString());
            }
        new ConnectThread(bd).start();

    }

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 1337;

    private String MACAddress;
    private BluetoothAdapter mBluetoothAdapter;
   // private EditText et;
    private IntentFilter filter;
    private ConnectedThread ct;
    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    // mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    // not synchronized
                    // et.append(readMessage);
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(),
                            msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
                            .show();
                    break;
            }
        }
    };

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent
                    .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.d("Goku SCAN",
                        device.getName()
                                + " - "
                                + device.getAddress()
                                + "; RSSI: "
                                + intent.getShortExtra(
                                BluetoothDevice.EXTRA_RSSI, (short) 0));
                // Device found
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                Log.d("Goku ACL", "Yup, connected to " + device.getName() + " "
                        + device.getAddress()); // Device is now connected
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
                    .equals(action)) {
                Log.d("Goku SCAN", "finished scan");// Done searching

              //todo commented this
              //  findViewById(R.id.discoveryButton).setVisibility(View.VISIBLE);
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED
                    .equals(action)) {
                Log.d("Goku ACL", "Disconnect requested"); // Device is about to
                // disconnect
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                Log.d("Goku ACL", "Disconnected"); // Device has disconnected
            }

        }
    };

    //todo the listeners have not been fully implemented in the service
    //todo they have the methods for writing and reading
//    @SuppressLint("NewApi")
//                    @Override
//                    protected void onCreate(Bundle savedInstanceState) {
//                        super.onCreate(savedInstanceState);
//                        setContentView(R.layout.activity_bluetooth_baby);
////        // register for connected device ACL status
////        // Register the BroadcastReceiver
////        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
////        registerReceiver(mReceiver, filter); // Don't forget to unregister
////        // during onDestroy
////        registerReceiver(mReceiver, new IntentFilter(
////                BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
////        IntentFilter filter1 = new IntentFilter(
////                BluetoothDevice.ACTION_ACL_CONNECTED);
////        IntentFilter filter2 = new IntentFilter(
////                BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
////        IntentFilter filter3 = new IntentFilter(
////                BluetoothDevice.ACTION_ACL_DISCONNECTED);
////        this.registerReceiver(mReceiver, filter1);
////        this.registerReceiver(mReceiver, filter2);
////        this.registerReceiver(mReceiver, filter3);
//
//                        et = (EditText) findViewById(R.id.editText1);
//                        Button tb = (Button) findViewById(R.id.toggleButton1);
//                        tb.setOnClickListener(new View.OnClickListener() {
//
//                            @Override
//                            public void onClick(View v) {
//                                //here we get the MAC Address
//                                WifiManager wifiManager = (WifiManager) v.getContext().getSystemService(Context.WIFI_SERVICE);
//                                if(wifiManager.isWifiEnabled()) {
//                                    // WIFI ALREADY ENABLED. GRAB THE MAC ADDRESS HERE
//                                    WifiInfo info = wifiManager.getConnectionInfo();
//                                    MACAddress = info.getMacAddress();
//                                } else {
//                                    // ENABLE THE WIFI FIRST
//                                    wifiManager.setWifiEnabled(true);
//
//                                    // WIFI IS NOW ENABLED. GRAB THE MAC ADDRESS HERE
//                                    WifiInfo info = wifiManager.getConnectionInfo();
//                    MACAddress = info.getMacAddress();
//                    wifiManager.setWifiEnabled(false);
//                }
//                // check if we have a socket connection first
//                if (ct != null)
//                    ct.write(("DID:" + MACAddress).getBytes());
//
//            }
//        });
//        Button discoveryButton = (Button) findViewById(R.id.discoveryButton);
//        discoveryButton.setOnClickListener(new OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                // If we're already discovering, stop it
//                if (mBluetoothAdapter.isDiscovering()) {
//                    mBluetoothAdapter.cancelDiscovery();
//                }
//                // Request discover from BluetoothAdapter
//                mBluetoothAdapter.startDiscovery();
//                // v.setVisibility(View.INVISIBLE);
//            }
//        });
//
//        // Test for adapter
//        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if (mBluetoothAdapter == null) {
//            Toast.makeText(getApplicationContext(),
//                    "Nope. No bluetooth. Sorry", Toast.LENGTH_LONG).show();
//            finish();// Device does not support Bluetooth. Exit application
//        }
//
//        if (!mBluetoothAdapter.isEnabled()) {
//            // we can ask the user to turn it on
//            // Intent enableBtIntent = new
//            // Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            // startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//
//            // or we can turn it on ourselves MUHAHHA
//            mBluetoothAdapter.enable();
//            // Wait until the Bluetooth Adapter is enabled before continuing
//            while (!mBluetoothAdapter.isEnabled())
//                ;
//
//        }
//
//        // Check for already paired devices
//        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
//                .getBondedDevices();
//        Log.d("Goku", pairedDevices.toString());
//        // If there are paired devices
//        if (pairedDevices.size() > 0) {
//            // Loop through paired devices
//            for (BluetoothDevice device : pairedDevices) {
//                // Add the name and address to an array adapter to show in a
//                // ListView
//                // et.setText(et.getText().toString()+ device.getName() + " - "
//                // + device.getAddress() + "\n");
//                Log.d("Goku", "For the device " + device.getName()
//                        + " we have the UUID:" + device.getUuids());
//
//                if (device.getAddress().equals("00:12:10:23:04:18")) {
//                    et.setText(et.getText().toString() + "got in the connect");
//                    new ConnectThread(device).run();
//                    et.setText(et.getText().toString()
//                            + "got after the connect");
//                }
//            }
//        }
//
//        // try to manually pair the devices based on the known mac(no prior
//        // bonding)
//        // This will prompt the user to pair with the gate if he didn't pair
//        // before
//        BluetoothDevice bd = mBluetoothAdapter
//                .getRemoteDevice("00:12:10:23:04:18");
//        if (bd.getBondState() == BluetoothDevice.BOND_NONE)
//            // Use reflection to get access to hidden method setpin
//            try {
//                Log.d("Goku", "Try to set the PIN");
//                Method m = bd.getClass().getMethod("setPin", byte[].class);
//                m.invoke(bd, "1234".getBytes());
//                Log.d("Goku", "Success to add the PIN.");
//                et.setText(et.getText().toString()
//                        + "Success in adding the PIN");
//
//                bd.getClass()
//                        .getMethod("setPairingConfirmation", boolean.class)
//                        .invoke(bd, true);
//                // bd.getClass().getMethod("cancelPairingUserInput",
//                // boolean.class).invoke(bd);
//                // bd.getClass().getMethod("createBond", (Class[])
//                // null).invoke(bd, (Object[]) null);
//                // et.setText(et.getText().toString() +
//                // "Success in creating the bond");
//                // this will probably block the thread so watch out
//                // while(bd.getBondState()!=BluetoothDevice.BOND_BONDED) ;
//
//            } catch (Exception e) {
//                Log.e("setPin()", e.toString());
//            }
//        new ConnectThread(bd).start();
//
//    }



//
//    protected void onDestroy() {
//        super.onDestroy();
//        unregisterReceiver(mReceiver);
//        // check if connection is there
//        if (ct != null)
//            ct.cancel();
//    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server
                // code
                tmp = device.createInsecureRfcommSocketToServiceRecord(UUID
                        .fromString("00001101-0000-1000-8000-00805F9B34FB"));

                // Method m;
                // try {
                // m = device.getClass().getMethod("createRfcommSocket", new
                // Class[] {int.class});
                // tmp = (BluetoothSocket) m.invoke(device, 1);
                // } catch (NoSuchMethodException | IllegalAccessException |
                // IllegalArgumentException | InvocationTargetException e) {
                //
                // e.printStackTrace();
                // }

            } catch (IOException e) {

//                et.setText(et.getText().toString() + "got exception "
//                        + e.toString());
                Log.d("Goku connection problem", e.toString());
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            if (mBluetoothAdapter.isDiscovering())
                mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    Log.d("Goku connection problem",
                            connectException.toString());
                    mmSocket.close();
                } catch (IOException closeException) {
                }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(mmSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    public void manageConnectedSocket(BluetoothSocket mmSocket) {
        ct = new ConnectedThread(mmSocket);
        // ct.run();
        Log.d("Goku", "Check if we're after run");
        ct.start();

    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024]; // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            int i = 0;
            while (true) {
                try {
                    // i++;
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // // Send the obtained bytes to the UI activity
                    // mHandler.ob
                    // mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)

                    // .sendToTarget();
                    byte[] readBuf = (byte[]) buffer;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, bytes);
                    Log.d("Goku", "\nreceived:" + readMessage);
                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(LocalService.MESSAGE_READ, bytes,
                            -1, buffer).sendToTarget();

                    // if (i == 42)
                    // break;
                    // Log.d("Goku", "We're in the fucking buffer, with data:" +
                    // bytes);
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(final byte[] bytes) {
            try {
//                et.post(new Runnable() {
//                    public void run() {
//                        et.setText(et.getText().toString()
//                                + "The UI thread accessed in the ConnectThread is writing this:"
//                                + new String(bytes));
//                    }
//                });
                Log.d("Goku", "The UI thread accessed in the ConnectThread is writing this:" + new String(bytes));
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.d("Goku writing problem", e.toString());
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }


}
