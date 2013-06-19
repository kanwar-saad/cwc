
import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;


//import android.util.Log;

//import android.util.Log;

public class UDPClient {

	/**
	 * @param args
	 */
	
	
	
	
	public static void main(String[] args) throws Exception {
      
		/*
	  BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
	  DatagramSocket clientSocket = new DatagramSocket();
	  InetAddress IPAddress = InetAddress.getByName("10.20.255.255");
	  clientSocket.setBroadcast(true);
	  byte[] sendData = new byte[1024];
	  byte[] receiveData = new byte[1024];
	  System.out.println("Write Something");
	  String sentence = inFromUser.readLine();
	  sendData = sentence.getBytes();
	  DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9877);
	  clientSocket.send(sendPacket);
	  System.out.println("Data Sent");
	  DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
	  clientSocket.receive(receivePacket);
	  String modifiedSentence = new String(receivePacket.getData());
	  System.out.println("FROM SERVER:" + modifiedSentence);
	  clientSocket.close();
	   */
		/*
		MulticastSocket socket = new MulticastSocket(4446);
        InetAddress address = InetAddress.getByName("230.0.1.8");
        socket.joinGroup(address);
        DatagramPacket packet;
        
        // get a few quotes
		for (int i = 0; i < 5; i++) {
		
		    byte[] buf = new byte[256];
	        packet = new DatagramPacket(buf, buf.length);
	        socket.receive(packet);
	
	        String received = new String(packet.getData(), 0, packet.getLength());
	        System.out.println("Data Received: " + received);
		}
		
		socket.leaveGroup(address);
		socket.close();
        */
		
		String jsonStr = "{\"a\":\"a\", \"b\":\"b\", \"c\":{\"msg\":\"Hello World\", \"s\": 1}}";
		
		Object obj = null;
		try{
			obj=JSONValue.parse(jsonStr);
		}
		catch(Exception e){
			System.out.println("Error Parsing JSON");
			//continue;
		}	
		
		if (obj instanceof JSONObject)
			System.out.println("Instance of JSONObject");
		if (obj instanceof JSONArray)
			System.out.println("Instance of JSONOArray");
		
		
		
		JSONObject obj2 = (JSONObject) obj;
		System.out.println("a = "+  obj2.get("a"));
		
		//JSONArray obj3 = (JSONArray) obj2.get("c");
		//for (Object temp : obj3) {
		//	System.out.println(String.valueOf(temp));
		//}
		
		JSONObject obj4 = (JSONObject) obj2.get("c");
		int so = (Integer.valueOf(String.valueOf(obj4.get("s"))));
		System.out.println("so = "+  String.valueOf(so));
		
		System.out.println("msg = "+  obj4.get("msg"));
		
		
		
		Map<Object, Object> logMap=new LinkedHashMap();
		
		logMap.put("id", "1234");
	    logMap.put("facility","Server");
	    logMap.put("severity", "Critical");
	    logMap.put("message", "Hello this is a test message");
	    List<String> list = new ArrayList<String>();
	    list.add("Hello world");
	    list.add("How Are You");
	    list.add("How are you studies going");
	    logMap.put("list", list);
	    String logStr = JSONValue.toJSONString(logMap);
	    
	    System.out.println(logStr );
	    
	    Node N = new Node();
	    N.age = 10 ;
	    
	    Map<String, Node> fMap = new LinkedHashMap();
	    fMap.put("f", N);
	    Node m = fMap.get("f");
	    
	    m.age = 15;
	    fMap.put("f", m);
	    
	    Node o = fMap.get("f");
	    
	    
	    System.out.println("Node val = " + String.valueOf(o.age) );
	    
	    Set<Object> s = logMap.keySet();
	    
	    Object[] sarray = s.toArray();
	    
	    for (int i=0;i < sarray.length; i++){
	    	System.out.println("Node key = " + String.valueOf(sarray[i]) );
		    
	    }
	    
	    
	    
		
        
	}

	public static void strfunc(){
		System.out.println("Function called" );
	    
	}
}
