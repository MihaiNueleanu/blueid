//package service;
//
//
//import android.app.Fragment;
//import android.content.ComponentName;
//import android.content.Context;
//import android.content.Intent;
//import android.content.ServiceConnection;
//import android.os.Bundle;
//import android.os.IBinder;
//import android.provider.AlarmClock;
//import android.support.v7.app.ActionBarActivity;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.Menu;
//import android.view.MenuInflater;
//import android.view.MenuItem;
//import android.view.View;
//import android.view.View.OnClickListener;
//import android.view.ViewGroup;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.Toast;
//
//import com.blueid.blueid.blueid.LocalService;
//
//public class Helloandroid extends ActionBarActivity {
//	public final static String EXTRA_MESSAGE = "com.example.helloandroid.MESSAGE";
//	MyBinder clientBinder;
//	LocalService mLocalService;
//	boolean mBound = false;
//
//
//	@Override
//	protected void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//		setContentView(R.layout.activity_main);
//		Button b = (Button) findViewById(R.id.button);
//		final EditText et = (EditText) findViewById(R.id.edit_message);
//
//		b.setOnClickListener(new OnClickListener() {
//
//			@Override
//			public void onClick(View v) {
//				//This is if i want to start an activity with the text from EditText
//				String text = et.getText().toString();
//				Intent intentToSend = new Intent(getApplicationContext(),LoginActivity.class);
//				intentToSend.putExtra(EXTRA_MESSAGE, text);
//
//				//inflates the fragment in the mainlayout
//				ExampleFragment ef = new ExampleFragment();
//				getFragmentManager()
//				.beginTransaction()
//				//.addToBackStack(null)
//				.add(R.id.mainLayout,ef)
//				//.addToBackStack(null)
//				.commit();
//
//				//here we bind to/start the service on click
////				Intent service = new Intent(getApplicationContext(), LocalService.class);
////				bindService(service, , Context.BIND_AUTO_CREATE);
//				//startService(intent);
//
//				postRandomNumber();
//
////				gives error right now
////				Intent bt = new Intent(getApplicationContext(),BluetoothBaby.class);
////				startActivity(bt);
//				//To set an alarm, no UI
//				//createAlarm("My test alarm", 16, 30);
//			}
//		});
//	}
//
//	protected void postRandomNumber() {
//		Log.d("Goku", String.valueOf(mBound));
//		 if (mBound) {
//	            // Call a method from the LocalService.
//	            // However, if this call were something that might hang, then this request should
//	            // occur in a separate thread to avoid slowing down the activity performance.
//	            int num = mLocalService.getRandomNumber();
//	            Log.d("Goku", String.valueOf(num));
//	            Toast.makeText(this, "number: " + num, Toast.LENGTH_SHORT).show();
//	        }
//	}
//
//	@Override
//	 protected void onStart() {
//	        super.onStart();
//	        //start the service in a new thread so it won't interrupt the UI
//	        Thread t = new Thread(){
//	        	public void run(){
//	        		//here we start the service everytime the activity comes to foreground
//	    	        // Bind to LocalService
//	    	        Intent intent = new Intent(getApplicationContext(), LocalService.class);
//	    	        getApplicationContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
//	        	}
//	        	};
//	        	t.start();
//
//	    }
//
//	protected void onStop() {
//        super.onStop();
//        // Unbind from the service
//        if (mBound) {
//            unbindService(mConnection);
//            mBound = false;
//        }
//    }
//
//	 /** Defines callbacks for service binding, passed to bindService() */
//    private ServiceConnection mConnection = new ServiceConnection() {
//
//                @Override
//                public void onServiceConnected(ComponentName className,
//                                               IBinder service) {
//                    // We've bound to LocalService, cast the IBinder and get LocalService instance
//                    clientBinder = (MyBinder) service;
//                    mLocalService = clientBinder.getService();
//                    mBound = true;
//                }
//
//                @Override
//                public void onServiceDisconnected(ComponentName arg0) {
//                    mBound = false;
//                }
//            };
//
//	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//
//	}
//	public void createAlarm(String message, int hour, int minutes) {
//	    Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM)
//	            .putExtra(AlarmClock.EXTRA_MESSAGE, message)
//	            .putExtra(AlarmClock.EXTRA_HOUR, hour)
//	            .putExtra(AlarmClock.EXTRA_MINUTES, minutes)
//	            .putExtra(AlarmClock.EXTRA_SKIP_UI, true);
//	    if (intent.resolveActivity(getPackageManager()) != null) {
//	        startActivity(intent);
//	    }
//	}
//	public boolean onCreateOptionsMenu(Menu menu) {
//		  // Inflate the menu items for use in the action bar
//	    MenuInflater inflater = getMenuInflater();
//	    inflater.inflate(R.menu.main, menu);
//	    return super.onCreateOptionsMenu(menu);
//	}
//
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//	    // Handle presses on the action bar items
//	    switch (item.getItemId()) {
//	        case R.id.action_search:
//	            openSearch();
//	            return true;
//	        case R.id.action_settings:
//	            openSettings();
//	            return true;
//	        default:
//	            return super.onOptionsItemSelected(item);
//	    }
//	}
//
//	private void openSettings() {
//		// TODO Auto-generated method stub
//			}
//	private void openSearch() {
//		// TODO Auto-generated method stub
//			}
//
//	public static class ExampleFragment extends Fragment {
//		    @Override
//		    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//		                             Bundle savedInstanceState) {
//		    	// Inflate the layout for this fragment
//		        return inflater.inflate(R.layout.fragment_main, container, false);
//		    }
//		}
//}
