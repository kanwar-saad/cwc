

public class Node {

	private String IPAddr;
	private int port;
	public String ID;
	public int age;
	
	Node(){
		IPAddr = "0.0.0.0";
		port = 0;
	}
	
	Node(String ip, int ePort){
		IPAddr = ip;
		port = ePort;		
	}
	
	void setPort(int eport){
		port = eport;
		
	}
	
	void setIP(String ip){
		IPAddr = ip;		
	}
	
	int getPort(){
		return port;
		
	}
	
	String getIP(){
		return IPAddr;		
	}
}
