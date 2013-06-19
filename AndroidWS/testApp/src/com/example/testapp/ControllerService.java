package com.example.testapp;

import java.util.Dictionary;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import com.example.testapp.ConfigData.AppConstant;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;


public class ControllerService extends Service {

	private static final String TAG = "ControllerService";
	
	
	
	/************ <Controller Local Objects> ***********/
	private final IBinder mBinder = new MyBinder();
	private Timer CNINFOTimer;
	private Handler uiHandler = null;
	
	private int startCount = 0;
	private Thread rxThread;
	private Context ctx;
	private Context appCtx;
	
	
	/***************************************************/
	
	Handler handler = new Handler() {
		  @Override
		  public void handleMessage(Message msg) {
			  
			  Bundle bundle = msg.getData();
			  String mtype = bundle.getString("to");
				
		      if (mtype == null) return;
			
		      
			  if (mtype.equals("UI")){ 		/* Forward Message to UI */
				  if (uiHandler != null){
					  Message uiMsg = uiHandler.obtainMessage();
					  uiMsg.setData(bundle);
					  uiHandler.sendMessage(uiMsg);
				  }
			  }				
		 }
	};
		 
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        
		startCount += 1;
		Log.d(TAG, "Service started = " + String.valueOf(startCount) + " times");
		
		if (startCount == 1){
			/* Do all initializations */
			Log.d(TAG, "Doing Service Initializations");
			ControllerInit();
			Log.d(TAG, "Service Initialization Succesful");
			
		}else{
			Log.d(TAG, "No Service Initialization this time");
		}
		
		
        return Service.START_NOT_STICKY;
    }
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "Activity Binding to Service");
		return mBinder;
	}
	
	@Override
	public boolean onUnbind(Intent intent){
		Log.d(TAG, "Service Unbind called");
		uiHandler = null;
		return true;		
	}
	
	
	@Override
	public void onDestroy(){
		Log.d(TAG, "Destroying Service");
		ControllerClose();
		Log.d(TAG, "Controller Service Closed Succesfully");
		
		
	}
	
	public class MyBinder extends Binder {
	    ControllerService getService() {
	      return ControllerService.this;
	    }
	}
	
	
	/***** Service Initialization/Destroy Methods ******/
	/**************************************************/
	private void ControllerInit() {
		
		// Initialize contexts
		ctx = this;
		appCtx = ctx.getApplicationContext();
		// Register battery Status receiver
		registerReceiver(Utils.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		// Get Wifi Service Handler
		ConfigData.Wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		// Initialize Broadcast Socket and Logger 
		ConfigData.CtrlSock = new UDPConnection("", "", ConfigData.getCtrlPort(), ConfigData.getCtrlPort(), true);
	    ConfigData.NLog = new NetLog();
	    // Start Receiver Thread
	    rxThread = new ControlReceiver(ctx, handler);
	    rxThread.start();
	    // Start Timer
	    CNINFOTimer = new Timer();
	    CNINFOTimer.schedule(ts, 10000, (AppConstant.CN_INFO_TIMEOUT*1000));
	    
		
	}
	
	private void ControllerClose() {
		
		unregisterReceiver(Utils.mBatInfoReceiver);
		CNINFOTimer.cancel();
        rxThread.interrupt();
        ConfigData.CtrlSock.close();
        ConfigData.NLog.close();		
	}
	
	
	/***** Interface Methods ******/
	/******************************/
	
	
	public String getData() {
	    return "Hello World : " + String.valueOf(startCount);
	}
	
	
	public void setHandler(Handler h) {
	    uiHandler = h;
	}
	
	

	/***** Internal Methods *******/
	/******************************/
	
	
	private void CNInfoTimerCallback(){
    	String localIP = UDPConnection.getLocalIP(true);
	    String bcAddr = UDPConnection.getBroadcastAddr(localIP, UDPConnection.getNetMask());
	    
    	// Send CN_DISCOVERY_REQ to peers
    	Map<String, Object> data = new LinkedHashMap();
		data.put("msgType", "CN_DISCOVERY_REQ");
		data.put("cnid", Utils.getDeviceID());
		String jsondata = JSONValue.toJSONString(data);
		ConfigData.CtrlSock.setBroadcast(true);
		ConfigData.CtrlSock.send(jsondata, bcAddr, ConfigData.getCtrlPort());	
		
		//try { Thread.sleep(3000); } catch (Exception e){};

		// Send CN_INFO message to BS
    	ConfigData.updatePeersAge();
    	Map<String, Object> cn_info = new LinkedHashMap();
    	Dictionary<?, ?> batProp = Utils.getBatteryStatus();
    	JSONArray neighbors = new JSONArray();
		for (String id : ConfigData.getPeerIds()){
    		neighbors.add(id);
    	}
		
		// Create CN_INFO Message
		cn_info.put("msgType", "CN_INFO");
		cn_info.put("cnid", Utils.getDeviceID());
		cn_info.put("battery_level", String.valueOf(batProp.get("charge_percent")));
		cn_info.put("is_charging", batProp.get("charging"));
		cn_info.put("neighbors", neighbors);    		
		
		// Send Message to BS
		jsondata = JSONValue.toJSONString(cn_info);
		try{
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
}
