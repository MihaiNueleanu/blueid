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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class LocalService extends Service {
    private int mStartMode = START_REDELIVER_INTENT;       // indicates how to behave if the service is killed
    private IBinder binder = new MyBinder(); // interface for clients that bind
    private boolean mAllowRebind = false;   // indicates whether onRebind should be used
    private Random mGenerator = new Random();
    public static boolean isRunning = false; //indicates whether the service is running or not
    private boolean foundDevice = false;
    private Intent mIntent;
    public static final int ONGOING_NOTIFICATION_ID = 100; //ID of notification, can be any number
    Socket socket;
    PrintWriter out;
    BufferedReader in;
    long timeleft;
    //The system calls this method when the service is first created, to perform one-time setup
    //procedures (before it calls either onStartCommand() or onBind()).
    //If the service is already running, this method is not called.
    @Override
    public void onCreate() {
        //the service is being created
        timeleft = 32400000;
        CountDownTimer cdt = new CountDownTimer(timeleft, 1000) {

            public void onTick(long millisUntilFinished) {
                timeleft = millisUntilFinished;

            }

            public void onFinish() {
                stopSelf();
            }
        }.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;
        mIntent = intent;
        // The service is starting, due to a call to startService()
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
        notificationIntent.putExtra("Username", intent.getStringExtra("Username"));
        notificationIntent.putExtra("SessionKey", intent.getStringExtra("SessionKey"));
        notificationIntent.putExtra("Timeleft",timeleft);
        Log.d("Goku", "In ServiceStart we create notification intent with extras:" + notificationIntent.getStringExtra("Username") + notificationIntent.getStringExtra("SessionKey"));
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        notification.setLatestEventInfo(this, "Bluetooth background scanner initiated",
                "Touch to see remaining time or logout", pendingIntent);
        startForeground(ONGOING_NOTIFICATION_ID, notification); //could be any ID
    }



    @Override
    public IBinder onBind(Intent arg0) {
        // A client is binding to the service with bindService()
        Log.d("Goku", "In onBind received this intent:" + arg0.getStringExtra("Username") + arg0.getStringExtra("SessionKey"));
        Toast.makeText(this, "Background Service started", Toast.LENGTH_SHORT).show();
        Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        v.vibrate(1000);

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


    /**
     * methods for clients
     */
    public long getTimeLeft() {
        return timeleft;
    }

    // The system calls this method when the service is no longer used and is being destroyed.
    //Your service should implement this to clean up any resources such as threads,
    //registered listeners, receivers, etc. This is the last call the service receives.
    @Override
    public void onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
        isRunning = false;

        //todo for debugging purposes: added this
        unregisterReceiver(mReceiver);
        // check if connection is there
        if (ct != null)
            ct.cancel();
        //check if discovery is being made
        if (mBluetoothAdapter != null)
            if (mBluetoothAdapter.isDiscovering())
                mBluetoothAdapter.cancelDiscovery();
    }


    public class MyBinder extends Binder {

        LocalService getService() {
            // Return this instance of LocalService so clients can call public methods
            return LocalService.this;
        }
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    /////////////////////////////////////////////////////////////////////////////////////
    /// Here we start the bluetooth part ///////////
    ////////////////////////////////////////////////////////////////////////////////////
    private void startBackgroundScanning() {
        //todo here we do the autoscanning
        startAutoScanning();

    }

    private void startAutoScanning() {
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
        //todo here we connect with the gate when it was found
        // If we're already discovering, stop it
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        // Request discover from BluetoothAdapter
        mBluetoothAdapter.startDiscovery();

    }

//    private void connectToGate() {
//        // register for connected device ACL status
//        // Register the BroadcastReceiver
//        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
//        registerReceiver(mReceiver, filter); // Don't forget to unregister
//        // during onDestroy
//        registerReceiver(mReceiver, new IntentFilter(
//                BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
//        IntentFilter filter1 = new IntentFilter(
//                BluetoothDevice.ACTION_ACL_CONNECTED);
//        IntentFilter filter2 = new IntentFilter(
//                BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
//        IntentFilter filter3 = new IntentFilter(
//                BluetoothDevice.ACTION_ACL_DISCONNECTED);
//        this.registerReceiver(mReceiver, filter1);
//        this.registerReceiver(mReceiver, filter2);
//        this.registerReceiver(mReceiver, filter3);
//
//        // ... /// in place of button listeners
//
//        // Test for adapter
//        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if (mBluetoothAdapter == null) {
//            Toast.makeText(getApplicationContext(),
//                    "Nope. No bluetooth. Sorry", Toast.LENGTH_LONG).show();
//            stopSelf();// Device does not support Bluetooth. Exit application
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
//                if (device.getAddress().equals("00:12:10:23:04:18")) {
//                    Log.d("Goku", "got in the connect");
//                    new ConnectThread(device).run();
//                    Log.d("Goku", "got after the connect");
//
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
////                et.setText(et.getText().toString()
////                        + "Success in adding the PIN");
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
//    }

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

    private String MACAddress, GID, Challenge;
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
                    Log.d("Goku", "The handler is receiving this:" + readMessage);
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
                //todo check if found device is the gate you're looking for
                if (device.getAddress().equals("00:12:10:23:04:18")) {
                    foundDevice = true;
                    try {
                        Log.d("Goku", "Try to set the PIN");
                        Method m = device.getClass().getMethod("setPin", byte[].class);
                        m.invoke(device, "1234".getBytes());
                        Log.d("Goku", "Success to add the PIN.");

                        device.getClass()
                                .getMethod("setPairingConfirmation", boolean.class)
                                .invoke(device, true);


                    } catch (Exception e) {
                        Log.e("setPin()", e.toString());
                    }
                    Log.d("Goku", "found device, attempting connection");
                    new ConnectThread(device).start();
                    Log.d("Goku", "Connection succeeded");
//                    ////////////////////////////////////////////////////////////////////////////
//                    //// now we're initiating the authorization phase ////////
//                    //////////////////////////////////////////////////////////////////////////
//
//                    //here we get the MAC Address
//
//                    WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//                    if (wifiManager.isWifiEnabled()) {
//                        // WIFI ALREADY ENABLED. GRAB THE MAC ADDRESS HERE
//                        WifiInfo info = wifiManager.getConnectionInfo();
//                        MACAddress = info.getMacAddress();
//                    } else {
//                        // ENABLE THE WIFI FIRST
//                        wifiManager.setWifiEnabled(true);
//
//                        // WIFI IS NOW ENABLED. GRAB THE MAC ADDRESS HERE
//                        WifiInfo info = wifiManager.getConnectionInfo();
//                        MACAddress = info.getMacAddress();
//                        wifiManager.setWifiEnabled(false);
//                    }
//                    // check if we have a socket connection first
//                    if (ct != null)
//                        ct.write(("DID:" + MACAddress).getBytes());
//
                }


            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                Log.d("Goku ACL", "Yup, connected to " + device.getName() + " "
                        + device.getAddress()); // Device is now connected
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
                    .equals(action)) {

                if (mBluetoothAdapter.isEnabled() && (!foundDevice))
                    mBluetoothAdapter.startDiscovery();
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
                    Log.d("Goku", "connectThread socket close exception:" + closeException.toString());

                }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(mmSocket);
        }

        /**
         * Will cancel an in-progress connection, and close the socket
         */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.d("Goku", "connectThread close exception:" + e.toString());

            }
        }
    }

    public void manageConnectedSocket(BluetoothSocket mmSocket) {
        ct = new ConnectedThread(mmSocket);
        // ct.run();
        Log.d("Goku", "Check if we're after run");
        ct.start();
        ////////////////////////////////////////////////////////////////////////////
        //// now we're initiating the authorization phase ////////
        //////////////////////////////////////////////////////////////////////////

        //here we get the MAC Address

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) {
            // WIFI ALREADY ENABLED. GRAB THE MAC ADDRESS HERE
            WifiInfo info = wifiManager.getConnectionInfo();
            MACAddress = info.getMacAddress();
        } else {
            // ENABLE THE WIFI FIRST
            wifiManager.setWifiEnabled(true);

            // WIFI IS NOW ENABLED. GRAB THE MAC ADDRESS HERE
            WifiInfo info = wifiManager.getConnectionInfo();
            MACAddress = info.getMacAddress();
            wifiManager.setWifiEnabled(false);
        }
        // check if we have a socket connection first
        if (ct != null)
            ct.write(("DID:" + MACAddress).getBytes());


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
                Log.d("Goku", "connectedThread constructor exception:" + e.toString());
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024]; // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            boolean sent = false;
            String payload = "";
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
                    payload = payload + readMessage;
                    Log.d("Goku", "\nreceived:" + readMessage);
                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(LocalService.MESSAGE_READ, bytes,
                            -1, buffer).sendToTarget();
                    if (payload.endsWith(":EOT")) {
                        Log.d("Goku", "Payload is:" + payload);
                        payload = payload.replace("GIDChallenge:", "");
                        Log.d("Goku", "Payload now:" + payload);
                        payload =  payload.replace(":EOT", "");
                        Log.d("Goku", "Payload now:" + payload);

                        String payloadArguments[] = payload.split(" ");
                        Log.d("Goku", "payload argument0:" + payloadArguments[0]);
                        Log.d("Goku", "payload argument1:" + payloadArguments[1]);
                        GID = payloadArguments[0];
                        Challenge = payloadArguments[1];
                        sendToServer("");

                        payload = "";
                    }
                    //todo check if received string is gid and challenge
//                    if () {
//                    //payload = UID|DID|GID|CHALLENGE|SESSIONKEY
//                    }
//                        sendToServer(payload);
//
//                    }


                    // if (i == 42)
                    // break;
                    // Log.d("Goku", "We're in the fucking buffer, with data:" +
                    // bytes);
                } catch (IOException e) {
                    Log.d("Goku", "connectedThread exception:" + e.toString());
                    //todo apparently this is where the connection drops
                    foundDevice = false;
                    //this is where we start the discovery mode again
                    if (mBluetoothAdapter != null)
                        mBluetoothAdapter.startDiscovery();
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
                Log.d("Goku", "connectedThread canceling exception:" + e.toString());
            }
        }
    }

    public void sendToServer(String payload) {
        if (isOnline()) {
            //here we would send a logout request to the server
            //run new thread for the CRUD connection
            new Thread(new Runnable() {
                public void run() {
                    try { //get input username and password
                        String usernameValue = mIntent.getStringExtra("Username");
                        String sessionKey = mIntent.getStringExtra("SessionKey");
                        //create JSON from username and password and send
                        JSONObject command = new JSONObject();
                        JSONObject params = new JSONObject();
                        params.put("uid", usernameValue);
                        params.put("did", MACAddress);
                        params.put("gid", GID);
                        params.put("cha", Challenge);
                        params.put("skey", sessionKey);
                        command.put("command", "authorize");
                        command.put("params", params);

                        socket = new Socket(Utility.ServerIP, Utility.ServerPort);
                        PrintStream out = new PrintStream(socket.getOutputStream(), true);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        //out.write(("How are you?").getBytes());
                        out.write(command.toString().getBytes());
                        //out.close();
                        if (!socket.isOutputShutdown())
                            socket.shutdownOutput();
                        Log.d("Goku", "socket output shut down ");
                        String response = in.readLine();
                        Log.d("Goku", "service received something: " + response);
                        try {
                            //if(!socket.isInputShutdown())
                            socket.shutdownInput();
                        } catch (Exception e) {
                            Log.d("Goku", "we get exception:" + e.toString());
                        }
                        Log.d("Goku", "socket input shut down ");
                        Log.d("Goku", "Response received: " + response);
                        if (response != null)
                            if (!response.equals("false")) {
//                            // check if we have a socket connection first
                                if (ct != null)
                                    ct.write(("ServerResponse:" + response).getBytes());
//
                                //TODO here we stop the bk service on logout
//                            if(mBound)
//                                unbindService(mConnection);
//                            stopService(new Intent(getApplicationContext(),LocalService.class));
//                            finish();
                            }

                    } catch (Exception e) {
                        e.printStackTrace();
                        final Exception e2 = e;
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                Toast toast = Toast.makeText(getApplicationContext(), "Error connecting: " + e2.getMessage(), Toast.LENGTH_LONG);
//                                toast.show();
//                            }
//                        });
                    } finally {
                        if (socket != null) {
                            try {
                                socket.close();
                            } catch (IOException e) {

                                e.printStackTrace();
                            }
                        }
                    }
                }
            }).start();

        }
    }
}