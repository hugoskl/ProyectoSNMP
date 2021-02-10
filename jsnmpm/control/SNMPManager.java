package jsnmpm.control;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;

import jsnmpm.control.process.SNMPProcess;
import jsnmpm.control.utilities.JSNMPUtil;

/**
 * @SNMPVersion SNMPv2c
 * @author MrStonedDog
 *
 */
public class SNMPManager{
	
	//STATIC PARAMETERS
	
	// INSTANCE VARIABLES
	private Map<Integer, SNMPAgent> agentsMap = null;
	private InetAddress ip = null;
	private int port = 161;
	private TransportMapping<?> transport= null;
	private Snmp snmpHandler = null; 
	
	public SNMPManager() throws IOException {
		this.transport= JSNMPUtil.createDefaultUDPTransport();
		this.snmpHandler = new Snmp(this.transport);
		this.snmpHandler.listen();
		this.agentsMap = new HashMap<Integer, SNMPAgent>();
		
	}
	
	public SNMPManager(TransportMapping<?> transport) throws IOException {
		this.transport = transport;
		this.snmpHandler = new Snmp(this.transport);
		this.snmpHandler.listen();
		this.agentsMap = new HashMap<Integer, SNMPAgent>();

	}
	
	// ########################## PUBLIC METHODS ##########################
	
	// ·········  NETWORK AND HARDWARE SETTINGS  ········
	protected String getTransportMappingInfo() {
		return this.transport.getListenAddress().toString();
	}
	
	
	
	// ········ AGENT CONFIGURATION ·········
	
	/**
	 * Adds a SNMPAgent to the Manager Agent List.
	 * @param agent
	 */
	protected synchronized void addAgent(SNMPAgent agent) {
		this.agentsMap.put(agent.getId(), agent);
	}
	
	protected synchronized void addAllAgents(List<SNMPAgent> agents) {
		this.agentsMap.putAll(agents.stream().collect(Collectors.toMap(SNMPAgent::getId, SNMPAgent::getMyself)));
	}
	
	protected boolean deleteAgent(int agent_id) {
		return (agentsMap.remove(agent_id) != null ?  true : false);
	}
	
	protected synchronized void modifyAgent(int agent_id) {
		//TODO
	}
	
	// ········ AGENTS HANDLING ·············
	
	
	/**
	 * Returns all the SNMPAgent instances available in Agent Map
	 * @return
	 */
	protected ArrayList<SNMPAgent> getAgents() {
		return new ArrayList<SNMPAgent>(agentsMap.values());
	}
	
	/**
	 * Returns the SNMPAgent instance with key = @param agent_id
	 * @param agent_id
	 * @return
	 */
	protected SNMPAgent getAgent(int agent_id) {
		return agentsMap.get(agent_id);
	}
	
	
	// ········ SENDING AND RECIEVING
	/** Synchronized method to send a SNMP-REQUEST to a SNMPAgent with the given OIDs.
	 * 
	 * @param agent_id
	 * @param oid
	 * @return
	 */
	protected ResponseEvent<Address> sendSyncGET(int agent_id, String...oid) {
	
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
	
	/**
	 * Sends asynchronous SNMP-REQUEST.
	 * @param pdu
	 * @param target
	 * @param listener
	 * @throws IOException
	 */
	public void sendAsync(PDU pdu, CommunityTarget<Address> target, SNMPProcess listener) throws IOException {
		this.snmpHandler.send(pdu, target, null, listener);
	}
	
	
}
