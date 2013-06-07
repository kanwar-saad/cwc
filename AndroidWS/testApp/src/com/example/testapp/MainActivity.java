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


import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
				
				if (mtype.equals("FILE_START")){
					int totalBytes = bundle.getInt("TOTAL_BYTES");
					progressBar.setVisibility(progressBar.VISIBLE);
					progressBar.setMax(100);
					progressBar.setProgress(0);
					
				}else if (mtype.equals("FILE_END")){
					progressBar.setVisibility(progressBar.INVISIBLE);
					progressBar.setMax(100);
					progressBar.setProgress(0);
					
				}else if (mtype.equals("FILE_PROGRESS")){
					int currBytes = bundle.getInt("CURRENT_BYTES");
					Log.v(TAG, "Current Progress = " + String.valueOf(currBytes));
					progressBar.setProgress(0);
					progressBar.setProgress(currBytes);					
				}
				
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
        
        //jpgview = (ImageView) findViewById(R.id.jpgview);
        //loglist = (ListView) findViewById(R.id.loglist);
        
        // Set adapter for log list
        //logsdataarray.add("This is a test list item");
        //logAdapter = new StableArrayAdapter(this, android.R.layout.simple_list_item_1, logsdataarray);
        //loglist.setAdapter(logAdapter);
        
        progressBar.setVisibility(progressBar.INVISIBLE);
		progressBar.setMax(0);
		progressBar.setProgress(0);
		
        appTitle.setText("COIN Test App");
        centerText.setText("Waiting For New Content");
        // Insert dummy item in list
        
        
        //Show Picture
        /*String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) +  "/" + "Stream1.jpg";
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        Bitmap bm = BitmapFactory.decodeFile(filePath, options);
        jpgview.setImageBitmap(bm); 
        */
        
        registerReceiver(Utils.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        
        //while(Utils.isBatteryDataValid()==false);
        
        //Log.v(TAG, "Reading values" );
        //Dictionary<?, ?> batProp = Utils.getBatteryStatus();
        //Log.v(TAG, "values read" );
        //Log.v(TAG, String.valueOf(batProp.get("charge_percent")) );
	    //Log.v(TAG, String.valueOf(batProp.get("charging")) );
	    //Log.v(TAG, String.valueOf(batProp.get("charge_ac")) );
	    //Log.v(TAG, String.valueOf(batProp.get("charge_usb")) );
	    
	    // Get Wifi Manager
	    ConfigData.Wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
	    ctx = this;
	    
	    new AsyncAppInit().execute();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    @Override
    public void onBackPressed() {
    	CNINFOTimer.cancel();
        rxThread.interrupt();
        ConfigData.CtrlSock.close();
        System.exit(0);
        //ConfigData.Log.close();
        
    }
    
    
    class AsyncAppInit extends AsyncTask<Void, Void, Void> {

        private Exception exception;

        protected Void doInBackground(Void... v) {
            Void ret = null;
            //String localIP = UDPConnection.getLocalIP(true);
    	    //Log.d(TAG, "Local IP = " + localIP + " " +String.valueOf(UDPConnection.getNetMask()));
    	    //String bcAddr = UDPConnection.getBroadcastAddr(localIP, UDPConnection.getNetMask());
    	    
    	    //NetLog netLog = new NetLog();
    	    //netLog.setServer("192.168.137.90", 36963);
    	    //netLog.Log("Testing Net Logging API", Facility.UNDEFINED, Severity.DEBUG, Utils.getDeviceID());
    	    
    	    // Start new control channel and Logging channel
    	    ConfigData.CtrlSock = new UDPConnection("", "", ConfigData.getCtrlPort(), ConfigData.getCtrlPort(), true);
    	    ConfigData.NLog = new NetLog();
    	    
    	    //File sdPath;
    		//sdPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) +  "/" + "1.jpg");
    	    //Thread t = new FileSenderTCP(ctx, "192.168.1.100", 4546, sdPath, 1024);
    		//t.start();
    	    rxThread = new ControlReceiver(ctx, handler);
    	    rxThread.start();
    	    
    	    CNINFOTimer = new Timer();
    	    CNINFOTimer.schedule(ts, 10000, (AppConstant.CN_INFO_TIMEOUT*1000));
    	    
            return ret;
        }
        
        void CNInfoTimerCallback(){
        	//Log.v(TAG, "Timer Called" );
        	
        	String localIP = UDPConnection.getLocalIP(true);
    	    String bcAddr = UDPConnection.getBroadcastAddr(localIP, UDPConnection.getNetMask());
    	    //Log.d(TAG, "Local IP = " + localIP + " " + bcAddr);
    	    
        	// Send CN_DISCOVERY_REQ to peers
        	
        	Map<String, Object> data = new LinkedHashMap();
    		
    		data.put("msgType", "CN_DISCOVERY_REQ");
    		data.put("cnid", Utils.getDeviceID());
    		
    		String jsondata = JSONValue.toJSONString(data);
    		ConfigData.CtrlSock.setBroadcast(true);
    		ConfigData.CtrlSock.send(jsondata, bcAddr, ConfigData.getCtrlPort());	
    		
    		try { Thread.sleep(3000); } catch (Exception e){};
  
    		// Send CN_INFO message to BS
        	
        	ConfigData.updatePeersAge();
        	
        	Map<String, Object> cn_info = new LinkedHashMap();
    		
    		cn_info.put("msgType", "CN_INFO");
    		cn_info.put("cnid", Utils.getDeviceID());
    		
    		Dictionary<?, ?> batProp = Utils.getBatteryStatus();
    		cn_info.put("battery_level", String.valueOf(batProp.get("charge_percent")));
    		//Log.v(TAG, String.valueOf(batProp.get("charge_percent")) );
    		//cn_info.put("battery_level", String.valueOf(65));
    		
    		cn_info.put("is_charging", batProp.get("charging"));
    		
    		JSONArray neighbors = new JSONArray();
    		for (String id : ConfigData.getPeerIds()){
        		neighbors.add(id);
        	}
    		
    		cn_info.put("neighbors", neighbors);    		
    		
    		jsondata = JSONValue.toJSONString(cn_info);
    		//Log.v(TAG, "CN_INFO Message : " + jsondata );
    		try{
    			//Log.v(TAG, "sending CN_INFO Message" );
    			ConfigData.CtrlSock.send(jsondata, ConfigData.getBSAddress(), ConfigData.getBSPort());
    		}
    		catch(Exception e){
    			Log.v(TAG, "Error in sending CN_INFO Message" );
    		}    	    
        }
        
        public TimerTask ts = new TimerTask(){
        
        	@Override
            public void run() {
        		CNInfoTimerCallback();
            }
        };
        
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
     
}
