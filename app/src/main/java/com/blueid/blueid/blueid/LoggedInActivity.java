package com.blueid.blueid.blueid;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import com.blueid.blueid.blueid.LocalService.MyBinder;



public class LoggedInActivity extends ActionBarActivity {
    MyBinder clientBinder;
    LocalService mLocalService;
    boolean mBound = false;

    Socket socket;
    PrintWriter out;
    BufferedReader in;



    private TextView tvh,tvm,tvs,message;
    private Button button;
    private SharedPreferences sharedPreferences;
    private long timeleft;
    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            Log.d("Goku","We've bound to localservice");
            clientBinder = (MyBinder) service;
            mLocalService = clientBinder.getService();
            mBound = true;

            final String FORMAT = "%02d:%02d:%02d";
            if(mBound)
                timeleft = mLocalService.getTimeLeft();
            else timeleft = 32400000;
            CountDownTimer cdt = new CountDownTimer(timeleft, 1000) {

                public void onTick(long millisUntilFinished) {
                    timeleft = millisUntilFinished;
                    tvh.setText(""+String.format(FORMAT,
                            TimeUnit.MILLISECONDS.toHours(millisUntilFinished),
                            TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) - TimeUnit.HOURS.toMinutes(
                                    TimeUnit.MILLISECONDS.toHours(millisUntilFinished)),
                            TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(
                                    TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished))));
                }

                public void onFinish() {
                    tvh.setText("-not activated-");
                    message.setText("Your account has been deactivated !");
                }
            }.start();

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_activity2);

        Log.d("Goku","In LoggedInOnCreate: "+getIntent().getStringExtra("SessionKey"));

//        //here we start the bg service
//        if(!LocalService.isRunning)
//        startBoundService();


        button = (Button) findViewById(R.id.logout);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isOnline()) {
                    //here we would send a logout request to the server
                    //run new thread for the CRUD connection
                    new Thread(new Runnable() {
                        public void run() {
                            try { //get input username and password
                                String usernamevalue = getIntent().getStringExtra("Username");
                                //create JSON from username and password and send
                                JSONObject command = new JSONObject();
                                JSONObject params = new JSONObject();
                                params.put("username", usernamevalue);
                                command.put("command", "logout");
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
                                Log.d("Goku", "received something: " + response);
                                try {
                                    //if(!socket.isInputShutdown())
                                    socket.shutdownInput();
                                } catch (Exception e) {
                                    Log.d("Goku", "we get exception:" + e.toString());
                                }
                                Log.d("Goku", "socket input shut down ");
                                Log.d("Goku", "Response received: " + response);
                                if (response.equals("true")) {

                                    //TODO here we stop the bk service on logout

                                    if(mBound)
                                    unbindService(mConnection);
                                    stopService(new Intent(getApplicationContext(),LocalService.class));
                                    finish();
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                                final Exception e2 = e;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast toast = Toast.makeText(getApplicationContext(), "Error connecting: " + e2.getMessage(), Toast.LENGTH_LONG);
                                        toast.show();
                                    }
                                });
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
                else {
                    Toast toast = Toast.makeText(getApplicationContext(), "No internet access", Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });
        tvh= (TextView) findViewById(R.id.tv_h);
        message = (TextView) findViewById(R.id.Message);
//        final String FORMAT = "%02d:%02d:%02d";
        //sharedPreferences = getSharedPreferences(getIntent().getStringExtra("user"), Context.MODE_PRIVATE);

        //if (sharedPreferences.contains("timeleft"))

      //  timeleft = sharedPreferences.getLong("timeleft", 32400000); // default 9 hours
//        if(mBound)
//        timeleft = mLocalService.getTimeLeft();
//        else timeleft = 32400000;
//        CountDownTimer cdt = new CountDownTimer(timeleft, 1000) {
//
//            public void onTick(long millisUntilFinished) {
//                timeleft = millisUntilFinished;
//                tvh.setText(""+String.format(FORMAT,
//                        TimeUnit.MILLISECONDS.toHours(millisUntilFinished),
//                        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) - TimeUnit.HOURS.toMinutes(
//                                TimeUnit.MILLISECONDS.toHours(millisUntilFinished)),
//                        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(
//                                TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished))));
//            }
//
//            public void onFinish() {
//                tvh.setText("-not activated-");
//                message.setText("Your account has been deactivated !");
//            }
//        }.start();


    }
    @Override
    protected void onStart() {
        super.onStart();
        //here we start the bg service
        //TODO need to check here
        //if(!LocalService.isRunning)
            startBoundService();
    }
    //helper method that starts and binds the service
   /*TODO important ! If you only need to interact with the service while your activity
   is visible, you should bind during onStart() and unbind during onStop(). */
    private void startBoundService() {
        //start the service in a new thread so it won't interrupt the UI
        Thread t = new Thread(){
            public void run(){
                Log.d("GokuContext","in StartBoundService" + getApplicationContext());
            // create the intent
                Intent intent = new Intent(getApplicationContext(), LocalService.class);
                intent.putExtra("Username", getIntent().getStringExtra("Username"));//add userID and SessionKey
                intent.putExtra("SessionKey", getIntent().getStringExtra("SessionKey"));
                //start the LocalService

                //this is more like a hack, if the activity has been started by the service, you
                //know the service is already on, so you don't start it again, just bind to it
                if(getIntent().getLongExtra("Timeleft",0)==0)
                startService(intent);
                // Bind to LocalService
                bindService(intent, mConnection, Context.BIND_AUTO_CREATE);//BIND_AUTO_CREATE creates the service if not already there


                Log.d("Goku", "Started and bound the service !");
            }
        };
        t.start();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.putLong("timeleft", timeleft);
//        editor.commit();
    }
    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_activity2, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
