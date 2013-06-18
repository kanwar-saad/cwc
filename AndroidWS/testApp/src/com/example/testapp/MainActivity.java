package com.example.testapp;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
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
import com.example.testapp.LazyAdapter;
import com.example.testapp.MainActivity;
import com.example.testapp.R;


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
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.Menu;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.util.Log;

public class MainActivity extends Activity {

	private static final String TAG = "MainActivity";
	
	static final String KEY_FILENAME = "name";
    static final String KEY_SIZE = "size";
    static final String KEY_DATE = "downloaded_on";
    static final String KEY_TS = "ts";
	
    
	private static Thread rxThread;
	private static Timer CNINFOTimer;
	private Context ctx;
	private ControllerService cs;
		
	// Drawable items
	
	private ImageView jpgview;
	private ListView loglist;
	
	private ListView fileListView ;  
	private LazyAdapter fileListAdapter ;
	
	private Button roleButton;
	//private TextView onlineStatusText;
	//private TextView roleText;
	//private LinearLayout onlineStatusTextContainer;
	//private LinearLayout roleTextContainer;
	
	private TextProgressBar progressBar;
	
	// Data Items
	
	private ArrayList<String> logsdataarray = new ArrayList<String>();
	
	// Adapters
	//StableArrayAdapter logAdapter;
	
	// Handlers for messages from worker threads
	
	Handler handler = new Handler() {
		  @Override
		  public void handleMessage(Message msg) {
			  Bundle bundle = msg.getData();
				String mtype = bundle.getString("mtype");
				if (mtype == null) return;	// Discard message if "mtype" tag not found in message
				
				if (mtype.equals("FILE_START")){
					long totalBytes = bundle.getLong("TOTAL_BYTES");
					progressBar.setVisibility(progressBar.VISIBLE);
					progressBar.setMax(100);
					progressBar.setProgress(0);
					
				}else if (mtype.equals("FILE_END")){
					//progressBar.setVisibility(progressBar.INVISIBLE);
					progressBar.setMax(100);
					progressBar.setProgress(0);
					// Open Image file
					String fileName = bundle.getString("path");
					fileListAdapter.notifyDataSetChanged();
					Toast.makeText(MainActivity.this, fileName + " Downloaded Successfully", Toast.LENGTH_SHORT).show();
					
					
				}else if (mtype.equals("FILE_PROGRESS")){
					int currBytes = bundle.getInt("CURRENT_BYTES");
					Log.v(TAG, "Current Progress = " + String.valueOf(currBytes));
					progressBar.setProgress(0);
					progressBar.setProgress(currBytes);
					
				}else if (mtype.equals("TOAST_MSG")){
					String tmsg = bundle.getString("msg");
					Toast.makeText(MainActivity.this, tmsg, Toast.LENGTH_SHORT).show();
				
				}else if (mtype.equals("ROLE_CHANGED")){
					String role = bundle.getString("role");
					if (role.equals("CN")){
						roleButton.setText("CN");
						roleButton.setTextColor(Color.parseColor("#CF3013"));	// RED
						String tmsg = "Role Changed To Cloud Node (CN)";
						Toast.makeText(MainActivity.this, tmsg, Toast.LENGTH_SHORT).show();
					}else if (role.equals("CL")){
						roleButton.setText("CL");
						roleButton.setTextColor(Color.parseColor("#0BCB02"));
						String tmsg = "Role Changed To Cloud Leader (CL)";
						Toast.makeText(MainActivity.this, tmsg, Toast.LENGTH_SHORT).show();
						
					}	
					
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
    	// Very initial setup
    	ConfigData.setAppPath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/COIN");
    	//ConfigData.setAppPath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Get all drawable items 
        //progressBar = (ProgressBar) findViewById(R.id.DLProgressBar);
        progressBar = (TextProgressBar) findViewById(R.id.DLProgressBar);
        
        fileListView = (ListView) findViewById( R.id.listView1 );
        roleButton = (Button) findViewById( R.id.roleButton );
        
        if (ConfigData.getFacility().equals("CN")){
			roleButton.setText("CN");
			roleButton.setTextColor(Color.parseColor("#CF3013"));	// RED
			
		}else if (ConfigData.getFacility().equals("CL")){
			roleButton.setText("CL");
			roleButton.setTextColor(Color.parseColor("#0BCB02"));	// GREEN
			
		}	
        
        roleButton.setText("CN");
        roleButton.setTextColor(Color.parseColor("#CF3013"));
        
        View header = getLayoutInflater().inflate(R.layout.list_header, null);
        
        progressBar.setVisibility(progressBar.VISIBLE);
        progressBar.setMax(100);
		progressBar.setProgress(0);
		
        
        ArrayList<HashMap<String, String>> fileList = new ArrayList<HashMap<String, String>>();
        //fileList = getDownloadedItems();
	    fileListView.setEmptyView(findViewById(R.id.empty));
	    //fileListView.addHeaderView(header);
	    fileListAdapter=new LazyAdapter(this, null);
	    // Set the ArrayAdapter as the ListView's adapter.  
	    fileListView.setAdapter( fileListAdapter );    
	    
	    fileListView.setOnItemClickListener(new OnItemClickListener()
	        {
		    	public void onItemClick(AdapterView<?> arg0, View v, int position, long id)
		    	{
		    		
		    		HashMap<String, String> map = new HashMap<String, String>();
		    		map = (HashMap<String, String>) fileListView.getItemAtPosition(position);
		    		String fileName = map.get(KEY_FILENAME);
		    		String fullPath = ConfigData.getAppPath() + "/" + fileName;
					File sdPath = new File(fullPath);
					Intent intent = new Intent();
					intent.setAction(android.content.Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.fromFile(sdPath), getMimeType(fileName));
					ctx.startActivity(intent);    
		    		
//		    		AlertDialog.Builder adb = new AlertDialog.Builder(MainActivity.this);
//		    		adb.setTitle("ListView OnClick");
//		    		adb.setMessage("Selected Item is = " + getMimeType(fileName));
//		    	    adb.setPositiveButton("Ok", null);
//		    	    adb.show();            
	    	    }
	    	});
        
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
    
    
    
    
    public static String getMimeType(String url)
    {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        return type;
    }
    
    
    
     
}
