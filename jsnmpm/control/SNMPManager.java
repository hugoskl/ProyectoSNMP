package jsnmpm.control;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.stream.Collectors;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;

import jsnmpm.control.SNMPController.INFO;
import jsnmpm.control.process.SNMPProcess;
import jsnmpm.control.utilities.JSNMPUtil;
import jsnmpm.control.whisper.ProcessWhisper;
import jsnmpm.control.whisper.RequestWhisper;
import jsnmpm.control.whisper.Whisper;

/**
 * @SNMPVersion SNMPv2c
 * @author MrStonedDog
 *
 */
public class SNMPManager implements ResponseListener, Publisher<Whisper>{
	
	//STATIC PARAMETERS
	
	// INSTANCE VARIABLES
	private Map<Integer, SNMPAgent> agentsMap = null;
	private InetAddress ip = null;
	private int port = 161;
	private TransportMapping<?> transport= null;
	private Snmp snmpHandler = null; 
	
	private Map<String,SNMPProcess> processesSNMP = null;
	private Subscriber<Whisper> control = null;
	
	public SNMPManager(Subscriber<Whisper> control) throws IOException {
		this.transport= JSNMPUtil.createDefaultUDPTransport();
		this.snmpHandler = new Snmp(this.transport);
		this.snmpHandler.listen();
		this.agentsMap = new HashMap<Integer, SNMPAgent>();
		this.processesSNMP = new HashMap<String, SNMPProcess>();
		this.control = control;
	}
	
	public SNMPManager(TransportMapping<?> transport) throws IOException {
		this.transport = transport;
		this.snmpHandler = new Snmp(this.transport);
		this.snmpHandler.listen();
		this.agentsMap = new HashMap<Integer, SNMPAgent>();
		this.processesSNMP = new HashMap<String, SNMPProcess>();

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
	protected synchronized ResponseEvent<Address> sendSyncGET(int agent_id, String...oid) {
	
		try {
			ResponseEvent<Address> response = this.snmpHandler.send(JSNMPUtil.createPDU(oid), this.agentsMap.get(agent_id).getCommunityTarget());
			this.agentsMap.get(agent_id).insertData(response.getResponse());
			return response;
	
		} catch (IOException e) {
			
			return null;
		}
	}
	
	
	/**
	 * Sends asynchronous SNMP-REQUEST.
	 * @param pdu
	 * @param target
	 * @param listener
	 * @throws IOException
	 */
	public void sendAsync(PDU pdu, CommunityTarget<Address> target, Object handler) throws IOException {
		this.snmpHandler.send(pdu, target, handler, this);
		
	}
	
	
	// ##########################    R E S P O N S E   L I S T E N E R     #################################
	@Override
	public synchronized <A extends Address> void onResponse(ResponseEvent<A> event) { //TODO
		
		((Snmp)event.getSource()).cancel(event.getRequest(), this); // CANCELING THE ASYNC RESPONSE WHEN RECIEVED
		  
		// ······  HANDLING PROCESS RESPONSE
		if(event.getUserObject() != null && event.getUserObject() instanceof SNMPProcess) { 
			  
			  this.control.onNext(new ProcessWhisper(((SNMPProcess)event.getUserObject()).getProcessID(),
					  ((SNMPProcess)event.getUserObject()).getAgendID(),
					  ProcessWhisper.RESPONSE, event.getResponse()));
			  
		// ······  HANDLING SIMPLE REQUEST RESPONSE
		}else {
			
			this.control.onNext(new RequestWhisper(((RequestWhisper)event.getUserObject()).getAgentID(),event.getResponse()));
			 
		}
	}

	@Override
	public void subscribe(Subscriber<? super Whisper> subscriber) {
		//subscriber.onSubscribe();
		
	}
	
//  ############################   P R O C E S S O R    M E T H O D S   ########################
	
	/**
	 * Creates a new Process. This process is accessible from the Map(snmpPSList). Creating a process does not start it.
	 * To do so, call @method startProcess(processID). To stop it, call stopProcess(processID). Once stopped, the Thread executing will be killed.
	 * To start a new Thread call @method startProcess(processID). One SNMPProcess must be executed only by one Thread at a time.
	 * 
	 * Returns the processID for this new SNMPProcess.
	 * @param agentID
	 * @param sleepTime
	 * @param oids
	 * @return 
	 */
	public String createSNMPProcess(int agentID, long sleepTime, Subscriber<Whisper> sub, String...oids) {
		String pid = DateTimeFormatter.ofPattern("yyMMddHHmmss").format(LocalDateTime.now());
		SNMPProcess pr = new SNMPProcess(pid,
				agentID, sleepTime, JSNMPUtil.createVariableBindingList(oids), sub);
		
		this.processesSNMP.put(pr.getProcessID(), pr);
		return pr.getProcessID();
	}
	
	/**
	 * Deletes a process. If the process is being executed when this method is called, pray to God he helps you.
	 * @param processID
	 */
	public void deleteSNMPProcess(String processID) {
		this.processesSNMP.remove(processID);
	}
	
	/**
	 * Starts a new Thread for the given process. If the process is being executed when this method is called, pray to God he helps you.
	 * @param processID
	 */
	public void startProcess(String processID) {
		this.processesSNMP.get(processID).start();
	}
	
	/**
	 * Stops the Thread executed by a process. 
	 * @param processID
	 * @throws InterruptedException 
	 */
	public void stopProcess(String processID) throws InterruptedException {
		
			this.processesSNMP.get(processID).stop();
	}
	
	public void getProcessData(String processID) {
		
	}
	
	public Set<String> getAllActiveProcessPID() {
		return this.processesSNMP.keySet().stream().filter((String key) -> this.processesSNMP.get(key).isRunning()).collect(Collectors.toSet());
	}
	
	public Set<String> getAllSNMPProcessPID() {
		return this.processesSNMP.keySet();
	}
	
	public SNMPProcess getProcess(String processID) {
		return this.processesSNMP.get(processID);
	}
	
}
