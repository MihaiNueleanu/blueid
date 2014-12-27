package com.blueid.blueid.blueid;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

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
    String host = "192.168.0.102";
    int port = 9999;
    int timeout = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Login button
        final Button button = (Button) findViewById(R.id.login);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                //run new thread for the CRUD connection
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            //get input username and password
                            EditText Username = (EditText)findViewById(R.id.Username);
                            EditText Password = (EditText)findViewById(R.id.Password);
                            String usernamevalue = Username.getText().toString();
                            String passwordvalue = Password.getText().toString();

                            //connect to socket
                            Socket socket = new Socket();
                            socket.connect(new InetSocketAddress(host, port), timeout);
                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                            //create JSON from username and password and send
                            JSONObject  command = new JSONObject();
                            JSONObject  params = new JSONObject();
                            params.put ("username" , usernamevalue );
                            params.put ("password" , passwordvalue);
                            command.put ("command" , "login");
                            command.put ("params" , params);

                            out.print(command);

                            //todo receive commands from the python server via BufferedReader in

                            //close socket and connection
                            out.close();
                            in.close();
                            socket.close();

                        } catch (UnknownHostException e) {
                            System.err.println("Unknown Host.");
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
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
