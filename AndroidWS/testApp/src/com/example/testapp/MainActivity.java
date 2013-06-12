package com.example.testapp;

import java.io.File;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import com.example.testapp.ConfigData.AppConstant;


import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Menu;
import android.widget.*;
import android.util.Log;

public class MainActivity extends Activity {

	private static final String TAG = "MainActivity";
	private static Thread rxThread;
	private static Timer CNINFOTimer;
	private Context ctx;
	private ControllerService cs;
		
	// Drawable items
	
	private ImageView jpgview;
	private ListView loglist;
	private TextView appTitle;
	private TextView centerText;
	private ProgressBar progressBar;
	
	// Data Items
	
	private ArrayList<String> logsdataarray = new ArrayList<String>();
	
	// Adapters
	StableArrayAdapter logAdapter;
	
	// Handlers for messages from worker threads
	
	Handler handler = new Handler() {
		  @Override
		  public void handleMessage(Message msg) {
			  Bundle bundle = msg.getData();
				String mtype = bundle.getString("mtype");
				if (mtype == null) return;	// Discard message if "mtype" tag not found in message
				
				if (mtype.equals("FILE_START")){
					int totalBytes = bundle.getInt("TOTAL_BYTES");
					progressBar.setVisibility(progressBar.VISIBLE);
					progressBar.setMax(100);
					progressBar.setProgress(0);
					
				}else if (mtype.equals("FILE_END")){
					progressBar.setVisibility(progressBar.INVISIBLE);
					progressBar.setMax(100);
					progressBar.setProgress(0);
					// Open Image file
					String fullPath = bundle.getString("path");
					File sdPath = new File(fullPath);
					Intent intent = new Intent();
					intent.setAction(android.content.Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.fromFile(sdPath), "image/*");
					ctx.startActivity(intent);    
					
					
				}else if (mtype.equals("FILE_PROGRESS")){
					int currBytes = bundle.getInt("CURRENT_BYTES");
					Log.v(TAG, "Current Progress = " + String.valueOf(currBytes));
					progressBar.setProgress(0);
					progressBar.setProgress(currBytes);
					
				}else if (mtype.equals("TOAST_MSG")){
					String tmsg = bundle.getString("msg");
					Toast.makeText(MainActivity.this, tmsg, Toast.LENGTH_SHORT).show();
				}
				
				
		     }
		 };
		 
	 private ServiceConnection mConnection = new ServiceConnection() {
		    
			@Override
			public void onServiceConnected(ComponentName className, IBinder binder) {
				// TODO Auto-generated method stub
				cs = ((ControllerService.MyBinder) binder).getService();
				Log.d(TAG, "Service Connected" );
				cs.setHandler(handler);
			    //Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();				
			}

			@Override
			public void onServiceDisconnected(ComponentName className) {
				// TODO Auto-generated method stub
				Log.d(TAG, "Service disConnected" );
				cs = null;
			}
    };
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Get all drawable items 
        appTitle = (TextView) findViewById(R.id.HelloText);
        centerText = (TextView) findViewById(R.id.CenterText);
        progressBar = (ProgressBar) findViewById(R.id.progressBar1);
        
        
        
        progressBar.setVisibility(progressBar.INVISIBLE);
		progressBar.setMax(0);
		progressBar.setProgress(0);
		
        appTitle.setText("COIN Test App");
        centerText.setText("Waiting For New Content");
        ctx = this;
            
		new AsyncAppInit().execute();
		
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    
    
    class AsyncAppInit extends AsyncTask<Void, Void, Void> {

        private Exception exception;

        protected Void doInBackground(Void... v) {
            Void ret = null;
            
            return ret;
        }
                
        protected void onPostExecute(Void v) {
            // TODO: check this.exception 
            // TODO: do something with the feed
        }
     }
    
    
    
    private class StableArrayAdapter extends ArrayAdapter<String> {

        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public StableArrayAdapter(Context context, int textViewResourceId,
            List<String> objects) {
          super(context, textViewResourceId, objects);
          for (int i = 0; i < objects.size(); ++i) {
            mIdMap.put(objects.get(i), i);
          }
        }

        @Override
        public long getItemId(int position) {
          String item = getItem(position);
          return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
          return true;
        }

      }
    
    /*
    @Override
    public void onBackPressed() {
    	//CNINFOTimer.cancel();
        //rxThread.interrupt();
        //ConfigData.CtrlSock.close();
        
        
        //this.finish();
        //System.exit(0);
        //ConfigData.Log.close();
        
    }
    */
    @Override
    protected void onStart(){
    	super.onStart();
    	Log.d(TAG, "Activity Started");
    }
    
    @Override
    protected void onRestart(){
    	super.onRestart();
    	Log.d(TAG, "Activity Restarted");
    }

    @Override
    protected void onResume(){
    	super.onResume();
    	try{
    		Log.d(TAG, "Activity Resumed");
    		Intent service = new Intent(ctx, ControllerService.class);
    		ctx.bindService(service, mConnection, Context.BIND_AUTO_CREATE);
    	    ctx.startService(service);
    		
    	}
    	catch(Exception e){
    		Log.e(TAG, "Error Binding to service on activity resume");
    		e.printStackTrace();
    	}
    	
    }

    @Override
    protected void onPause(){
    	super.onPause();
    	unbindService(mConnection);
    	Log.d(TAG, "Activity Paused");
    }

    @Override
    protected void onStop(){
    	super.onStop();
    	Log.d(TAG, "Activity Stopped");
    }

    @Override
    protected void onDestroy(){
    	super.onDestroy();
    	Log.d(TAG, "Activity Destroyed");
    }
     
}
