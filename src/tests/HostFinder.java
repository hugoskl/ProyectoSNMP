package tests;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class HostFinder {
	
	
	public static void main(String [] args) {
		
		discoverNetworkHosts();
		
	}
	
	public static ArrayList<String> discoverNetworkHosts(){
		String ip = "192.168.1.";
		for(int i = 20; i < 255 ; i++) {
			
			sendPingRequest(ip+i);
		}
		return null;
	}
	
	public static void sendPingRequest(String ipAddress) {
		
		InetAddress host;
		try {
			host = InetAddress.getByName(ipAddress);
			if(host.isReachable(1000))
			System.out.println(host+" is reachable!");
			else
				System.out.println(host+" is NOT reachable!");
		
		} catch (UnknownHostException ue) {
			// TODO Auto-generated catch block
			ue.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
