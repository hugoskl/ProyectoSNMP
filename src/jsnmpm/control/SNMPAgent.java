package jsnmpm.control;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;

import jsnmpm.control.utilities.JSNMPUtil;

/**
 * 
 * @author MrStonedDog
 *
 */
public class SNMPAgent implements ResponseListener{
	
	// ## STATIC ##
	public static final boolean UNREACHABLE = false;
	public static final boolean REACHABLE = true;
	
	// ## INSTANCE VARIABLES ##
	private InetAddress ip = null;
	private int port = 161;
	private boolean state = UNREACHABLE;
	private int id = -1;
	private String name = null;
	
	// ·· SNMP UTILS
	private Map<OID,PDU> pduList = null;  
	private CommunityTarget<Address> target = null;
	private OctetString readCommunity = new OctetString("public");
	
	
	// ##############  CONSTRUCTORS  ###############
	public SNMPAgent(String ip, int port, String name, String readCommunity) throws UnknownHostException {
		
		this.ip = InetAddress.getByName(ip);
		this.pduList = new HashMap<OID,PDU>();
		this.name = ((name == null) ? "Unknown" : name);
		this.target = JSNMPUtil.createCommunityTarget(String.format("%s/%d", ip, port), new OctetString(readCommunity));
		this.target.setVersion(SnmpConstants.version2c);
		this.state = this.isReachable();
		
	}
	public SNMPAgent(String ip) throws UnknownHostException {
		
		this.ip = InetAddress.getByName(ip);
		this.pduList = new HashMap<OID,PDU>();
		this.target = JSNMPUtil.createCommunityTarget(String.format("%s/%d", ip, this.port), this.readCommunity);
		this.target.setVersion(SnmpConstants.version2c);
		this.state = this.isReachable();
		
	}
	public SNMPAgent(String ip, int port) throws UnknownHostException {
		
		this.ip = InetAddress.getByName(ip);
		this.port = port;
		this.pduList = new HashMap<OID,PDU>();
		this.target = JSNMPUtil.createCommunityTarget(String.format("%s/%d", ip, port), this.readCommunity);
		this.target.setVersion(SnmpConstants.version2c);
		this.state = this.isReachable();
	}
	
	public SNMPAgent(String ip, String readCommunity) throws UnknownHostException {
		this.ip = InetAddress.getByName(ip);
		this.pduList = new HashMap<OID,PDU>();
		this.readCommunity = new OctetString(readCommunity);
		this.target = JSNMPUtil.createCommunityTarget(String.format("%s/%d", ip, port), this.readCommunity);
		this.target.setVersion(SnmpConstants.version2c);
		this.state = this.isReachable();
	}
	public SNMPAgent(CommunityTarget<Address> target) throws UnknownHostException {
		
		String[] address = target.getAddress().toString().split("/");
		this.ip = InetAddress.getByName(address[0]);
		this.port = Integer.parseInt(address[1]);
		this.pduList =  new HashMap<OID,PDU>();
		this.target = target;
		this.readCommunity = target.getCommunity();
		this.state = this.isReachable();
	}
	
	// ################################# PRIVATE METHODS #######################################
	/**
	 * Appends data to @variable pduList. Data must be an instance of PDU.
	 * @param data
	 */
	protected void insertData(PDU pduData) {
			if(pduData != null)
				for(VariableBinding data : pduData.getVariableBindings()) {
					pduList.put(data.getOid(), pduData);
				}
	}
	// ################################## PUBLIC METHODS #######################################
	
	// ·· Handling implemented ResponseListener
	@Override
	public <A extends Address> void onResponse(ResponseEvent<A> event) {
		((Snmp)event.getSource()).cancel(event.getRequest(), this);
		this.insertData(event.getResponse());
		//System.out.printf("Agent: %s | OID: %s -> Data: %s", this.ip, event.getResponse().getVariableBindings());
	}
	// ·················· HELPFUL METHODS
		
	public boolean isReachable() {
		try {
			return this.ip.isReachable(1000);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean isReachable(SNMPAgent agent) {
		try {
			return agent.ip.isReachable(1000);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	// ··· STATIC METHODS 

			
	// ··· INSTANCE METHODS
	
	// ········· SETTERS AND GETTERS ·············
	// Setters:
	protected void setName(String name) {
		this.name = name;
	}
	
	protected void setId(int id) {
		this.id = id;
	}
	
	protected void setReadCommunity(String community) {
		this.readCommunity = new OctetString(community);
	}
	
	
	
	// Getters:
	public String getIp() {
		return this.ip.getHostAddress();
	}
	
	public int getPort() {
		return this.port;
	}
	
	public boolean getState() {
		return this.state;
	}
	
	public int getId() {
		return this.id;
	}
	
	public String getName() {
		return this.name;
	}
	
	public HashMap<OID,PDU> getData(){
		return (HashMap<OID, PDU>) this.pduList;
	}
	
	public String getReadCommunity() {
		return this.readCommunity.toString();
	}
	
	public CommunityTarget<Address> getCommunityTarget() {
		return this.target;
	}
	


	
	
	
}
