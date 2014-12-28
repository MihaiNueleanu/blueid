package com.blueid.blueid.blueid;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;


public class MainActivity2 extends ActionBarActivity {
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
                finish();
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
