package jsnmpm.control;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Flow.Processor;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

import org.fusesource.jansi.Ansi;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.Address;

import jsnmpm.control.process.SNMPProcess;
import jsnmpm.control.utilities.CTRLConstants;
import jsnmpm.control.utilities.JSNMPUtil;
import jsnmpm.control.whisper.CtrlWhisper;
import jsnmpm.control.whisper.ProcessWhisper;
import jsnmpm.control.whisper.RequestWhisper;
import jsnmpm.control.whisper.Whisper;
import jsnmpm.monitor.terminal.Terminal;

/**
 * Main class for controlling the program logic. Has method for handling both database and snmpmanager.<br>
 * Instanciates:<br/>
 * · SNMPManager <br/>
 * · DBController <br/>
 * 
 * @author MrStonedDog
 *
 */
public class SNMPController implements Processor<Whisper, Whisper>{
	
	// CONTROLER INFO
	public static enum INFO{
		
		DBACCESS,
		INTERFACES,
		AGENTS,
		TRAPS,
		PROCESSES;
	}
	
	// CONTROLER RESOURCES
	private SNMPManager snmpManager = null;
	private DBControl dbCtrl = null;
	private Subscriber<Whisper> monitor = null;
	private ControllerFileHandler ctrlFileHandler = null;
	
	// CONTROLER VARIABLES
	private Map<INFO, String> ctrlInfo = null;
	private int agentIDCounter = 0;
	
	
	// ############    CONSTRUCTOR    ###########
	/**
	 * Throws a FileNotFoundException if no configuration file is found.
	 * @param monitor
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public SNMPController(Subscriber<Whisper> monitor) throws FileNotFoundException, IOException{
		this.ctrlFileHandler = new ControllerFileHandler();
		
		this.monitor = monitor;
		this.ctrlInfo = new HashMap<INFO, String>();
		this.init();
	}
	
	// GETTERS
	public Map<INFO, String> getCtrlInfo(){
		return this.ctrlInfo;
	}
	
	// SETTERS
	/**
	 * Sets database with given parameters. Returns an int depending on the result.<br/>
	 * @param dbName
	 * @param dbIP
	 * @param dbPort
	 * @param dbUser
	 * @param dbPass
	 * @return 0 = Good <br/> 1 = No Connection <br/> 2 = Cannot load driver </br> 3 = Unknown SQLException
	 */
	private boolean setDatabase(String dbName, String dbIP, int dbPort, String dbUser, String dbPass) {
		try {
			this.dbCtrl = new DBControl(dbName,dbIP,dbPort,dbUser,dbPass);
			
			return (this.dbCtrl.hasConnection());
		} catch (ClassNotFoundException e) {
			this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "ERROR: Unable to load Database handler");
			
		} catch (SQLException e) {
			this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "ERROR: Database query returned an unknown SQLException");
		}
		return false;
	}
	
	
	
	
	// ################  I N I T   P R O C E D U R E  ##################
	private void init() {
		
		this.ctrlFileHandler.writeEmptyLineToLogFile(this.ctrlFileHandler.getCtrlLogFilePath());
		// INITIALIZING SNMPMANAGER
		try {
			this.snmpManager = new SNMPManager(this);
		} catch (IOException e) {
			this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getSNMPLogFilePath(), "ERROR: Cannot load SNMPManager - Cause:" + e.getMessage());
		}
		
		// LOAD DATABASE CONFIGURATION
		try {
	
		this.ctrlInfo.put(INFO.DBACCESS,
				this.setDatabase(this.ctrlFileHandler.getConfProperty("dbname"),
						this.ctrlFileHandler.getConfProperty("host"),
						Integer.parseInt(this.ctrlFileHandler.getConfProperty("port")),
						this.ctrlFileHandler.getConfProperty("user"),
						this.ctrlFileHandler.getConfProperty("pass")) ? "True" : "False");
		
		this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(),
				"Database configuration established successfully");
		this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(),
				(this.ctrlInfo.get(INFO.DBACCESS) == "True") ? "Connection with database established!" : "WARNING: Cannot establish connection with database");
		
		} catch(NumberFormatException nfe) {
			this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "ERROR: Wrong format por property 'PORT' in configuration file");
			this.ctrlInfo.put(INFO.DBACCESS, "False");
		}
				
		// LOADING AGENTS TO SNMPMANAGER
		try {
			this.loadAgents();
			this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(),
					"Success loading database agents to SNMPManager");
		} catch (SQLException e) {
			this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "ERROR: Cannot load agents from database\n"+
												this.ctrlFileHandler.indentation + "Cause: "+ e.getMessage().split("\n")[0] + "\n" +
												this.ctrlFileHandler.indentation + "ErrorCode: " + e.getErrorCode());
		}
		
		// LOADING CONFIGURATION TO MONITOR
		this.loadInfo();
		this.ctrlFileHandler.writeEmptyLineToLogFile(this.ctrlFileHandler.getCtrlLogFilePath());
		this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "Display Information loaded successfully");
		
		// TODO LOAD DEFAULT COLORS FOR TERMINAL???
		
		// TODO LOAD DEFAULT PARAMETERS FOR A GUI IMPLEMENTATION???
	}
	
	/**
	 * Loads information to be displayed in TMonitor. 
	 * @param terminal
	 */
	public void loadInfo() {
		ctrlInfo = new HashMap<INFO, String>();
		
		ctrlInfo.put(INFO.DBACCESS, String.valueOf(this.dbCtrl.hasConnection()));
		ctrlInfo.put(INFO.INTERFACES, this.snmpManager.getTransportMappingInfo());
		ctrlInfo.put(INFO.AGENTS, String.valueOf(this.snmpManager.getAgents().size()));
		ctrlInfo.put(INFO.PROCESSES, String.valueOf(this.snmpManager.getAllActiveProcessPID().size()));
		ctrlInfo.put(INFO.TRAPS, "0");
	}
	
	/**
	 * If database is set, retrieves all SNMPAgents from DB, and sets local @variable agentIDCounter to
	 * the max(agentID)+1.
	 * @throws SQLException
	 */
	public void loadAgents() throws SQLException {
		
		this.snmpManager.addAllAgents(this.dbCtrl.getSNMPAgents());
		this.agentIDCounter = this.snmpManager.getAgents().stream().mapToInt(SNMPAgent::getId).max().orElse(0);
		++this.agentIDCounter;
	}

	
	// ############################  H A N D L I N G   S N M P   A G E N T S  ###########################
	// ---> AFFECTS BOTH SNMPMANAGER AND DBCONTROLLER
	public synchronized int addNewAgent(String ip, int port, String name, String readCom) throws UnknownHostException, SQLException {
		SNMPAgent agent = new SNMPAgent(ip, port, name, readCom);
		agent.setId(this.agentIDCounter);
		this.snmpManager.addAgent(agent);
		
		if(this.dbCtrl.hasConnection())
			dbCtrl.addSNMPAgent(agent);
		
		int numAgents = Integer.parseInt(this.ctrlInfo.get(INFO.AGENTS));
		this.ctrlInfo.put(INFO.AGENTS, String.valueOf(++numAgents));
		return this.agentIDCounter++;
	}
	
	public void modifyAgent(int agent_id) {
		
	}
	
	public boolean removeAgent(int agent_id) throws SQLException {
		if(this.snmpManager.deleteAgent(agent_id)) {
			dbCtrl.removeSNMPAgent(agent_id);
			
			this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "Agent '" + agent_id + "was deleted. HostIP: " +
												this.snmpManager.getAgent(agent_id).getIp());
			return true;
		}else {
			return false;
		}
	}
	
	// ###################################   M A N A G E R   M E T H O D S   ####################################
	public String managerGetTransportMappingInfo() {
		return this.snmpManager.getTransportMappingInfo();
	}
	
	public void managerAddTransport() {
		
	}
	
	/** Return an ArrayList with all the exitsting instances of SNMPAgent
	 * @return
	 */
	public ArrayList<SNMPAgent> managerGetAgents(){
		return this.snmpManager.getAgents();
	}
	
	/**
	 * Return the an SNMPAgent instance for the given agent_id or null if no SNMPAgent exists for given agent_id.
	 * @param agent_id
	 * @return
	 */
	public SNMPAgent managerGetAgent(int agent_id) {
		return this.snmpManager.getAgent(agent_id);
	}
	
	/**
	 * Sends a sync GET REQUEST to the given agent with the given OIDs.
	 * @param agent_id
	 * @param oid
	 * @return
	 */
	public ResponseEvent<Address> managerSendSyncGET(int agent_id, String...oid) {
		return this.snmpManager.sendSyncGET(agent_id, oid);
	}
	
	/** Sends an async REQUEST to the given agent_id, with the given oids and an object "handler" to 
	 * choose how to handle the request response.
	 * 
	 * @param agent_id
	 * @param handler
	 * @param oid
	 * @throws IOException
	 */
	public void managerSendAsync(int agent_id, Object handler, String...oid) throws IOException {
		this.snmpManager.sendAsync(JSNMPUtil.createPDU(oid), this.snmpManager.getAgent(agent_id).getCommunityTarget(), handler);
	}
	

	// ##################################   D A T A B A S E    M E T H O D S   #######################################
	
	public boolean dbHasConnection() {
		return this.dbCtrl.hasConnection();
	}
	
	public List<SNMPAgent> dbGetSNMPAgents() throws SQLException{
		return this.dbCtrl.getSNMPAgents();
	}
	
	public void dbAddPDU(PDU pdu, int agentID) throws SQLException {
		this.dbCtrl.addPDU(pdu, agentID);
	}
	
	public void dbAddProcess(int agentID, long sleepTime, String...oids) {
		
	}

	
	
//  ############################   P R O C E S S O R    M E T H O D S   ########################
	// TODO HANDLE DATABASE FOR PROCESSES
	/**
	 * Creates a new Process. This process is accessible from the Map(snmpPSList). Creating a process does not start it.
	 * To do so, call @method startProcess(processID). To stop it, call stopProcess(processID). Once stopped, the Thread executing will be killed.
	 * To start a new Thread call @method startProcess(processID). One SNMPProcess must be executed only by one Thread at a time.
	 * 
	 * Returns the processID for this new SNMPProcess.
	 * @param agentI	 * @param sleepTime
	 * @param oids
	 * @return 
	 */
	public String createSNMPProcess(int agentID, long sleepTime, String...oids) {
		String processID = this.snmpManager.createSNMPProcess(agentID, sleepTime, this, oids);
		this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "New SNMPProcess created. ProcessID = " + processID);
		return processID;
		
	}
	
	/**
	 * Deletes a process. If the process is being executed when this method is called, pray to God he helps you.
	 * @param processID
	 */
	public void deleteSNMPProcess(String processID) {
		this.snmpManager.deleteSNMPProcess(processID);
		this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "SNMPProcess '" + processID + "' has been deleted!");
	}
	
	/**
	 * Starts a new Thread for the given process. If the process is being executed when this method is called, pray to God he helps you.
	 * @param processID
	 */
	public void startProcess(String processID) {
		this.snmpManager.startProcess(processID);
		 ctrlInfo.put(INFO.PROCESSES, String.valueOf(this.getAllActiveProcessPID().size()));
		 this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "Starting SNMPProcess '" + processID + "'");
	}
	
	/**
	 * Stops the Thread executed by a process. 
	 * @param processID
	 */
	public void stopProcess(String processID) {
		try {
			this.snmpManager.stopProcess(processID);
			 this.ctrlFileHandler.writeToLogFile(this.ctrlFileHandler.getCtrlLogFilePath(), "SNMPProcess '" + processID + "' has been stopped");
			ctrlInfo.put(INFO.PROCESSES, String.valueOf(this.getAllActiveProcessPID()));
		} catch (InterruptedException e) {
			

		}
	}
	
	public void setProcessSaveDataInDB(boolean status, String processID) {
		this.snmpManager.getProcess(processID).setSaveInDB(status);
	}
	
	public void setProcessVerbose(boolean status, String processID) {
		this.snmpManager.getProcess(processID).setShowResponse(status);
	}
	
	public void getProcessData(String processID) {
		
	}
	
	public Set<String> getAllActiveProcessPID() {
		return this.snmpManager.getAllActiveProcessPID();	
	}
	
	public Set<String> getAllSNMPProcessPID() {
		return this.snmpManager.getAllSNMPProcessPID();
	}
	
	// ####################    S U B S C R I B E R   M E T H O D S     ######################
	public void onSubscribe(Subscription subscription) {

	}

	@Override
	public synchronized void onNext(Whisper item) {
		
		 // ··········· WHISPER IS TYPE SNMPROCESSWHISPER
		if(item instanceof ProcessWhisper) {
			
			ProcessWhisper psWhisper = (ProcessWhisper) item;
			if(psWhisper.getAction() == ProcessWhisper.SEND_SNMP) {
				try {
					
					this.snmpManager.sendAsync(psWhisper.getResponsePDU(),
							this.snmpManager.getAgent(psWhisper.getAgentID()).getCommunityTarget(),
							this.snmpManager.getProcess(psWhisper.getProcessID()));
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else if(psWhisper.getAction() == ProcessWhisper.RESPONSE) {
				
				try { //TODO IF DB IS NOT ACTIVE THIS BREAKS
					
					if(this.snmpManager.getProcess(psWhisper.getProcessID()).getSaveInDB())
						this.dbCtrl.addPDU(((ProcessWhisper)item).getResponsePDU(), psWhisper.getAgentID());
					if(this.snmpManager.getProcess(psWhisper.getProcessID()).getShowResponse())
						this.monitor.onNext(psWhisper);
				} catch (SQLException e) {
					
				}
			}
		return;}
		
		 // ··········· WHISPER IS TYPE SNMPWHISPER
		if(item instanceof RequestWhisper) {
			
			this.monitor.onNext(item);
		}
		
	}

	/** Does nothing! */
	@Override
	public synchronized void onError(Throwable throwable) {}

	/** Does nothing! */
	@Override
	public synchronized void onComplete() {}

	/** Does nothing! */
	@Override
	public void subscribe(Subscriber<? super Whisper> subscriber) {}
	
	
}
