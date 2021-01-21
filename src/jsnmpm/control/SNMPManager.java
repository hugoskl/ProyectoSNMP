package jsnmpm.control;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;

import jsnmpm.control.utilities.JSNMPUtil;

/**
 * 
 * @author MrStonedDog
 *
 */
public class SNMPManager{
	
	//STATIC PARAMETERS
	
	// INSTANCE VARIABLES
	private Map<Integer, SNMPAgent> agentsMap = null;
	private InetAddress ip = null;
	private int port = 161;
	private int id_counter = 0;
	public TransportMapping<?> transport= null;
	public Snmp snmpHandler = null;
	
	public SNMPManager() throws IOException {
		this.transport= JSNMPUtil.createDefaultUDPTransport();
		this.snmpHandler = new Snmp(this.transport);
		this.snmpHandler.listen();
		agentsMap = new HashMap<Integer, SNMPAgent>();
	}
	
	public SNMPManager(TransportMapping<?> transport) throws IOException {
		this.transport = transport;
		this.snmpHandler = new Snmp(this.transport);
		this.snmpHandler.listen();
		agentsMap = new HashMap<Integer, SNMPAgent>();
		
	}
	
	// ########################## PUBLIC METHODS ##########################
	
	// ···· GETTERS ····
	public String getTransportMappingInfo() {
		return this.transport.getListenAddress().toString();
	}
	// ········ AGENT CONFIGURATION ·········
	public synchronized void addAgent(String ip, int port, String name, String readCommunity) throws UnknownHostException {
		SNMPAgent agent = new SNMPAgent(ip, port, name, readCommunity);
		agent.setId(id_counter);
		this.agentsMap.put(this.id_counter, agent);
		id_counter++;
	}
	public synchronized void addAgent(SNMPAgent agent) {
		agent.setId(id_counter);
		this.agentsMap.put(this.id_counter, agent);
		id_counter++;
	}
	
	public void deleteAgent(int agent_id) {
		this.agentsMap.remove(agent_id);
	}
	
	public synchronized void modifyAgent(int agent_id) {
		//TODO
	}
	
	// ········ AGENTS HANDLING ·············
	
	/**
	 * Returns all the SNMPAgent instances available in Agent Map
	 * @return
	 */
	public ArrayList<SNMPAgent> getAgents() {
		return new ArrayList<SNMPAgent>(agentsMap.values());
	}
	
	/**
	 * Returns the SNMPAgent instance with key = @param agent_id
	 * @param agent_id
	 * @return
	 */
	public SNMPAgent getAgent(int agent_id) {
		return agentsMap.get(agent_id);
	}
	
	
	// ········ SENDING AND RECIEVING
	public ResponseEvent<Address> sendSyncGET(String oid, int agent_id) {
	
		try {
			ResponseEvent<Address> response = this.snmpHandler.send(JSNMPUtil.createPDU(oid), this.agentsMap.get(agent_id).getCommunityTarget());
			this.agentsMap.get(agent_id).insertData(response.getResponse());
			return response;
		} catch (IOException e) {
			
			return null;
		}
	}
	
	public void sendGETNEXT(int...agent_id) {
		
	}
	
	public void sendGETBULK( int maxRepeat, int...agent_id) {
		
	}
	
	
}
