package com.blueid.blueid.blueid;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.CountDownTimer;
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


public class LoggedInActivity extends ActionBarActivity {

    //Host, port, timeout
    String host = "192.168.43.164";
    //String host = "10.0.2.2";
    int port = 9999;
    int timeout = 5000;
    Socket socket;
    PrintWriter out;
    BufferedReader in;



    private TextView tvh,tvm,tvs,message;
    private Button button;
    private SharedPreferences sharedPreferences;
    private long timeleft;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_activity2);

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
                                String usernamevalue = getIntent().getStringExtra("user");
                                //create JSON from username and password and send
                                JSONObject command = new JSONObject();
                                JSONObject params = new JSONObject();
                                params.put("username", usernamevalue);
                                command.put("command", "logout");
                                command.put("params", params);

                                socket = new Socket(host, port);
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
                                        // TODO Auto-generated catch block
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
        final String FORMAT = "%02d:%02d:%02d";
        sharedPreferences = getSharedPreferences(getIntent().getStringExtra("user"), Context.MODE_PRIVATE);

        //if (sharedPreferences.contains("timeleft"))

        timeleft = sharedPreferences.getLong("timeleft", 32400000); // default 9 hours
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
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong("timeleft", timeleft);
        editor.commit();
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
