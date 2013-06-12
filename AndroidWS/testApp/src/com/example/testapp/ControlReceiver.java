package com.example.testapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import com.example.testapp.ConfigData.AppConstant;
import com.example.testapp.ConfigData.Facility;
import com.example.testapp.ConfigData.Severity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.MimeTypeMap;

public class ControlReceiver extends Thread {
	
	private static final String TAG = "CtrlRxThread";
	private static final int maxCtrlMsgLen = 1024;
	
	private UDPConnection CtrlChannel;
	private NetLog NLog;
	private Context context;
	private Handler handler;
	private static Thread dataThread;
	
	
	ControlReceiver(Context c, Handler h){
		context = c;
		
		handler = h;
	}
	
	private void setupChannel()
	{
		// Get Connections //
    	
    	CtrlChannel = ConfigData.CtrlSock;
    	String localIP = UDPConnection.getLocalIP(true);
    	CtrlChannel.setDstIP(UDPConnection.getBroadcastAddr(localIP, UDPConnection.getNetMask()));	// Dst IP set to broadcast address for now. It may change afterwards.
		
    	NLog = ConfigData.NLog;
		//CtrlChannel.send("Ping");
		///////////////////////////////////	
				
	}
	
	public void run() {
		
        // Create Rx Control Channel //
		
		setupChannel();
		
		// Allocate Resources // 
		byte[] receiveData = new byte[maxCtrlMsgLen];
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		
		
		//try { Thread.sleep(5000); } catch (Exception e){};
		Log.d(TAG, "Rx Control Thread Started");
		
		// Start Listening Loop //
		
		while (true){
			String strData = null;
			// receive packet //
			
			try
			{
			    CtrlChannel.getSocket().receive(receivePacket);
			    strData = new String(receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength());
		    }
			catch(Exception e)
			{
				//CtrlChannel.close();
				Log.e(TAG, "Error Receiving data in RxThread" );
				return;
			}
			
			/* Discard messages from local IP */
			if (receivePacket.getAddress().getHostAddress().equals(UDPConnection.getLocalIP(true))){
				//Log.d(TAG, "Discarding Packets from local address" );
				continue;
			}
			
			Log.d(TAG, "Rx raw data : " + strData);
			
			// Parse JSON //
			
			Object obj = null;
			try
			{
				obj=JSONValue.parse(strData);
			}
			catch(Exception e)
			{
				Log.d(TAG, "Invalid Control Packet Received" );
				continue;
			}		
			
			// Process Received Message //
			
			msgHandler(obj, receivePacket.getAddress().getHostAddress(), receivePacket.getPort());
			
		}       
    }
	
	private void msgHandler(Object cPkt, String Address, int port)
	{
		JSONObject rxdata = null;
		try
		{
			//Log.d(TAG, "Packet received from " + Address + String.valueOf(port));
		
			rxdata = (JSONObject) cPkt;
		
			//Log.d(TAG, "value " + rxdata.get("msgType"));
			
		}
		catch(Exception e){Log.d(TAG, "Error in JSON handling"); return;}
		
		String msgType = (String)rxdata.get("msgType");
		
		Map<String, Method> fMap = new LinkedHashMap();
		Class[] params = new Class[3];
		params[0] = JSONObject.class;
		params[1] = String.class;
		params[2] = Integer.TYPE;		
		
		try{
		// Discovery and CL selection messages
		fMap.put("CN_DISCOVERY_REQ", this.getClass().getDeclaredMethod("handle_disc_req", params));
		fMap.put("CN_DISCOVERY_RESP", this.getClass().getDeclaredMethod("handle_disc_resp", params));
		fMap.put("BS_SELECT_CL_REQ", this.getClass().getDeclaredMethod("handle_cl_select_req", params));
		fMap.put("CN_SET_CL", this.getClass().getDeclaredMethod("handle_cn_set_cl", params));
		
		// Data Transmission messages
		fMap.put("BS_DATA_INIT_REQ", this.getClass().getDeclaredMethod("handle_bs_data_init_req", params));
		fMap.put("CL_DATA_INIT_REQ", this.getClass().getDeclaredMethod("handle_cl_data_init_req", params));
		fMap.put("CL_DATA_INIT_RESP", this.getClass().getDeclaredMethod("handle_cl_data_init_resp", params));
		
		
		}
		catch(Exception e){Log.d(TAG, "No such methods found"); return;}
		
		Method m = fMap.get(msgType);
		if (m == null){
			Log.d(TAG,  "Invalid Message Type Received");
			return ;
		}
		else{
			try{
			m.invoke(this, rxdata, Address, port);
			}
			catch(Exception e){Log.d(TAG, "Error invoking method"); return;}
		}
			
		
		
	}
	
	private void handle_disc_req(JSONObject rxdata, String ip, int port){
		String cnid = (String)rxdata.get("cnid");
		
		Map<String, Object> data=new LinkedHashMap();
		
		data.put("msgType", "CN_DISCOVERY_RESP");
		data.put("cnid", Utils.getDeviceID());
		
		String jsondata = JSONValue.toJSONString(data);
		CtrlChannel.send(jsondata, ip, port);		
		
	}
	
	private void handle_disc_resp(JSONObject rxdata, String ip, int port){
			
		String cnid = (String)rxdata.get("cnid");
		
		if (cnid == null){
			NLog.Log("Discovery response with no CNID", ConfigData.getFacility(), Severity.INFO, Utils.getDeviceID());
			return;
		}
		try{
		Node peer = ConfigData.getPeer(cnid);
		
		
		if (peer == null){
			peer = new Node();
			peer.setIP(ip);
			peer.setPort(port);
			peer.ID = cnid;
			peer.age = AppConstant.PEER_EXPIRE_AGE;
			ConfigData.putPeer(cnid, peer);
		}else{
			peer.setIP(ip);
			peer.setPort(port);
			peer.age = AppConstant.PEER_EXPIRE_AGE;
			peer.ID = cnid;
			ConfigData.putPeer(cnid, peer);
		}
		}
		catch(Exception e){
			Log.d(TAG, "Exception in Handling peer resp");
		}
		
		return;
	}
	
	
	private void handle_cl_select_req(JSONObject rxdata, String ip, int port){
		Log.d(TAG, "Node Selected as CL");
		String cnid = (String)rxdata.get("cnid");
		String clusterid = (String)rxdata.get("cluster_id");
		
		if (cnid.equals(Utils.getDeviceID())){
			// return BS_SELECT_CL_RESP
			Map<String, Object> data=new LinkedHashMap();
			
			data.put("msgType", "BS_SELECT_CL_RESP");
			data.put("cnid", Utils.getDeviceID());
			data.put("cluster_id", clusterid);
			data.put("status", "ACCEPT");
			
			String jsondata = JSONValue.toJSONString(data);
			CtrlChannel.send(jsondata, ip, port);
			
			// Change Role to CL
			ConfigData.setFacility(Facility.CL);
						
			// Send message to peers
			
			Map<String, Object> set_cl=new LinkedHashMap();
			
			set_cl.put("msgType", "CN_SET_CL");
			set_cl.put("clid", Utils.getDeviceID());
			set_cl.put("cluster_id", clusterid);
			
			jsondata = JSONValue.toJSONString(set_cl);
			
			String localIP = UDPConnection.getLocalIP(true);
			CtrlChannel.send(jsondata, UDPConnection.getBroadcastAddr(localIP, UDPConnection.getNetMask()) , ConfigData.getCtrlPort());		
			
		}
		else{
			Log.d(TAG, "CL Selection ID does not match");
		}
			
	}

	private void handle_cn_set_cl(JSONObject rxdata, String ip, int port){
		Log.d(TAG, "CN_SET_CL msg received");
		String clid = (String)rxdata.get("clid");
		String clusterid = (String)rxdata.get("cluster_id");
		
		//Log.d(TAG, "New clid = " + clid);
		//Log.d(TAG, "New cluster ID = " + clusterid);
		
		ConfigData.setFacility(Facility.CN);	// Set Current role a CN
	
		Node new_cl = new Node();
		new_cl.setIP(ip);
		new_cl.setPort(port);
		new_cl.ID = clid;
		
		ConfigData.setCL(new_cl);	
		
	}

	private void handle_bs_data_init_req(JSONObject rxdata, String ip, int port){
		Log.d(TAG, "BS_DATA_INIT_REQ msg received");
		Socket clientSocket;
		JSONObject metadata = (JSONObject) rxdata.get("metadata");
		int chunk_size = (Integer.valueOf((String.valueOf(rxdata.get("chunk_size")))));
		String mime = (String)metadata.get("mime");
		File sdPath = null;
		String fullPath = null;
		//Log.d(TAG, "MIME = " + mime);
		
		if ("file".equals(mime)){
			
			int totalBytes = Integer.valueOf((String) metadata.get("file_size"));
			Message msg = handler.obtainMessage();
			Bundle bundle = new Bundle();
			bundle.putString("to", "UI");
			bundle.putString("mtype", "FILE_START");
			bundle.putInt("TOTAL_BYTES", totalBytes);
            msg.setData(bundle);
            handler.sendMessage(msg);
            
			// Open dst file
			FileOutputStream out;
			try {
				fullPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) +  "/" + (String)metadata.get("name");
				sdPath = new File(fullPath);
				//out = context.openFileOutput((String)metadata.get("name"), Context.MODE_WORLD_READABLE);
				out = new FileOutputStream(sdPath);
			} catch (FileNotFoundException e1) {
				Log.d(TAG, "Error Opening file for writing at CL");
				e1.printStackTrace();
				return;
			}  
			//Log.d(TAG, "chk 1");  
		    byte[] chunk = new byte[chunk_size];
		    //Log.d(TAG, "chk 2");
			// Create a TCP client connection with BS
			try {
				int data_port = (Integer.valueOf((String.valueOf(rxdata.get("data_port")))));
				clientSocket = new Socket(ip, data_port);
			} catch (UnknownHostException e) {
				Log.e(TAG, "Error in Data init connection to BS");
				e.printStackTrace();
				return;
			} catch (IOException e) {
				Log.e(TAG, "Error in Data init connection to BS");
				e.printStackTrace();
				return;
			}
			
			
			int nBytes = 0;
			int tBytes = 0;
			int curr_perc = 0;
			int prev_perc = 0;
			try {
				InputStream is = clientSocket.getInputStream();
								
				while ((nBytes = is.read(chunk)) != -1){
					out.write(chunk, 0, nBytes);
					tBytes += nBytes;
					
					curr_perc = ((tBytes * 100) / totalBytes);
					if (curr_perc != prev_perc){
						msg = handler.obtainMessage();
						bundle = new Bundle();
						bundle.putString("to", "UI");
						bundle.putString("mtype", "FILE_PROGRESS");
						bundle.putInt("CURRENT_BYTES", curr_perc);
						msg.setData(bundle);
				        handler.sendMessage(msg);
				        
				        prev_perc = curr_perc;
					}
			        
				}
				is.close();
				out.close();
			} catch (IOException e) {
				Log.e(TAG, "Error in Data receiving at CL");
				e.printStackTrace();
			}
			
			try {
				clientSocket.close();
				
			} catch (IOException e) {
				Log.e(TAG, "Error closing clientsocket");
				e.printStackTrace();
			}
			Log.d(TAG, "Bytes Received = " + String.valueOf(tBytes));
			
			
			//Now broadcast message to CN's
			Map<String, Object> init_data_msg=new LinkedHashMap();
			
			init_data_msg.put("msgType", "CL_DATA_INIT_REQ");
			init_data_msg.put("clid", Utils.getDeviceID());
			init_data_msg.put("chunk_size", String.valueOf(chunk_size));
			init_data_msg.put("metadata", metadata);
			String jsondata = JSONValue.toJSONString(init_data_msg);
					
			String localIP = UDPConnection.getLocalIP(true);
			CtrlChannel.send(jsondata, UDPConnection.getBroadcastAddr(localIP, UDPConnection.getNetMask()) , ConfigData.getCtrlPort());		
			
			msg = handler.obtainMessage();
			bundle = new Bundle();
			bundle.putString("to", "UI");
			bundle.putString("mtype", "FILE_END");
			bundle.putString("path", fullPath);
			msg.setData(bundle);
	        handler.sendMessage(msg);
			
		}
		else{
			Log.d(TAG, "Unknown MIME type received");
			
		}
		
		
	}
			
	private void handle_cl_data_init_resp(JSONObject rxdata, String ip, int port){
		Log.d(TAG, "CL_DATA_INIT_RESP msg received");
		try{
		String fname = (String) rxdata.get("filename");
		int r_port = Integer.valueOf((String) rxdata.get("port"));
		int chunk_size = Integer.valueOf((String) rxdata.get("chunk_size"));
		File sdPath;
		
		sdPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) +  "/" + fname);
		
		Thread t = new FileSenderTCP(context, handler, ip, r_port, sdPath, chunk_size);
		t.start();
		}
		catch(Exception e){
			Log.d(TAG, "Error in handling CL_DATA_INIT_RESP");
			e.printStackTrace();
		}
	}
	
	
	private void handle_cl_data_init_req(JSONObject rxdata, String ip, int port){
		JSONObject metadata = (JSONObject) rxdata.get("metadata");
		int chunk_size = Integer.valueOf((String) rxdata.get("chunk_size"));
		String mime = (String)metadata.get("mime");
		String file_name = (String)metadata.get("name");
				
		if ("file".equals(mime)){
			// Send message to UI
			int totalBytes = Integer.valueOf((String) metadata.get("file_size"));
			Message msg = handler.obtainMessage();
			Bundle bundle = new Bundle();
			bundle.putString("to", "UI");
			bundle.putString("mtype", "FILE_START");
			bundle.putInt("TOTAL_BYTES", totalBytes);
            msg.setData(bundle);
            handler.sendMessage(msg);
			
			
			FileOutputStream out;
			ServerSocket serverSocket = null;
			Socket connectionSocket = null;
			InputStream is = null;
			OutputStream os = null;
			byte[] chunk = new byte[chunk_size];
			int nBytes = 0;
			int tBytes = 0;
			File sdPath = null;
			String fullPath = null;
			try {
				fullPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) +  "/" + file_name;
				sdPath = new File(fullPath);
				//out = context.openFileOutput((String)metadata.get("name"), Context.MODE_WORLD_READABLE);
				out = new FileOutputStream(sdPath);
			} catch (FileNotFoundException e1) {
				Log.d(TAG, "Error Opening file for writing at CN");
				e1.printStackTrace();
				return;
			}  
			
			try {
				
				
				//Now Send message to CL to send file
				Map<String, Object> init_data_msg=new LinkedHashMap();
				
				init_data_msg.put("msgType", "CL_DATA_INIT_RESP");
				init_data_msg.put("cnid", Utils.getDeviceID());
				init_data_msg.put("chunk_size", String.valueOf(chunk_size));
				init_data_msg.put("filename", file_name);
				init_data_msg.put("port", String.valueOf(ConfigData.getDataPort()));
				String jsondata = JSONValue.toJSONString(init_data_msg);
						
				String localIP = UDPConnection.getLocalIP(true);
				CtrlChannel.send(jsondata, ip , port);		

				
				// Accept Connection
				serverSocket = new ServerSocket(ConfigData.getDataPort());
				connectionSocket = serverSocket.accept();
				is = connectionSocket.getInputStream();
				os = connectionSocket.getOutputStream();
				
				
				int prev_perc = 0;
				int curr_perc = 0;
				// Receive data
				Log.d(TAG, "Starting Transfer : " + connectionSocket.toString());
				while ((nBytes = is.read(chunk)) != -1){
					
					out.write(chunk, 0, nBytes);
					tBytes += nBytes;
					//Log.d(TAG, "Bytes Received = " + String.valueOf(tBytes));
					
					curr_perc = ((tBytes * 100) / totalBytes);
					//Log.d(TAG, "Download % = " + String.valueOf(curr_perc));
					if (curr_perc != prev_perc){
						msg = handler.obtainMessage();
						bundle = new Bundle();
						bundle.putString("to", "UI");
						bundle.putString("mtype", "FILE_PROGRESS");
						bundle.putInt("CURRENT_BYTES", curr_perc);
						msg.setData(bundle);
				        handler.sendMessage(msg);
				        
				        prev_perc = curr_perc;
					}
				}				
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally{
				try {
					is.close();
					out.close();
					connectionSocket.close();
					serverSocket.close();
					
				} catch (IOException e) {
					
					e.printStackTrace();
				}
				
			}
			
			Log.d(TAG, "Total Bytes Received = " + String.valueOf(tBytes));
			
			msg = handler.obtainMessage();
			bundle = new Bundle();
			bundle.putString("to", "UI");
			bundle.putString("mtype", "FILE_END");
			bundle.putString("path", fullPath);
			
			msg.setData(bundle);
	        handler.sendMessage(msg);
					
		}
		
	}

}	// End Class
