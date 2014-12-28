package com.blueid.blueid.blueid;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends ActionBarActivity {

    //Host, port, timeout
    String host = "192.168.0.107";
    //String host = "10.0.2.2";
    int port = 9999;
    int timeout = 5000;
    Socket socket;
    PrintWriter out;
    BufferedReader in;

    private Button button;
    private EditText Username,Password;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Login button
        button = (Button) findViewById(R.id.login);
        //other widgets
        Username = (EditText)findViewById(R.id.Username);
        Password = (EditText)findViewById(R.id.Password);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                //run new thread for the CRUD connection
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            //get input username and password
                            String usernamevalue = Username.getText().toString();
                            String passwordvalue = Password.getText().toString();

                            //connect to socket
                            socket = new Socket();
                            socket.connect(new InetSocketAddress(host, port), timeout);
                            out = new PrintWriter(socket.getOutputStream(), true);
                            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                            //create JSON from username and password and send
                            JSONObject  command = new JSONObject();
                            JSONObject  params = new JSONObject();
                            params.put ("username" , usernamevalue );
                            params.put ("password" , passwordvalue);
                            command.put ("command" , "login");
                            command.put ("params" , params);

                            out.print(command);

                            //todo receive commands from the python server via BufferedReader in
//                              String line = null;
//                            while ((line = in.readLine()) != null) {
//                            Log.d("Goku", line);}

                        //close socket and connection
                        out.close();
                        in.close();
                        socket.close();
                        Intent intent = new Intent(getApplicationContext(), MainActivity2.class);
                        //TODO remove comment intent.putExtra("user",usernamevalue);
                        startActivity(intent);


                        } catch (Exception e) {
                            System.err.println("Unknown Host.");
                            e.printStackTrace();
                            final Exception e2 = e;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast toast = Toast.makeText(getApplicationContext(), "Error connecting: " + e2.getMessage(), Toast.LENGTH_LONG);
                                    toast.show();
                                    }
                            });
                        }
                    }
                }).start();
                Log.d("MainActivity","thread has been finished");
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
