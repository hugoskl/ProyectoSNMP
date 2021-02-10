package jsnmpm.control;

import java.io.IOException;
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
import jsnmpm.control.process.SNMPProcessWhisper;
import jsnmpm.control.utilities.JSNMPUtil;
import jsnmpm.control.utilities.Whisper;
import jsnmpm.terminal.Terminal;

/**
 * Main class for controlling the program logic. Has method for handling both database and snmpmanager.<br>
 * Instanciates:<br/>
 * · SNMPManager <br/>
 * · DBController <br/>
 * 
 * @author MrStonedDog
 *
 */
public class SNMPController implements Subscriber<Whisper>{
	
	enum INFO{
		DBACCESS("DBAccess: "),
		INTERFACES("Interfaces: "),
		AGENTS("Agents: "),
		TRAPS("Traps: "),
		PROCESSES("Running Processes: ");
		
		public final String value;
		private INFO(String info) {
			this.value = info;
		}
	}
	
	private SNMPManager snmpManager = null;
	private DBControl dbCtrl = null;
	private Map<String,SNMPProcess> processesSNMP = null;
	private Map<String, String> ctrlInfo = null;
	public int agentIDCounter = 0;
	
	// CONSTRUCTOR
	public SNMPController() throws IOException{
		this.snmpManager = new SNMPManager();
		this.processesSNMP = new HashMap<String, SNMPProcess>();
		
	}
	
	// GETTERS
	public Map<String, String> getCtrlInfo(){
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
	public DBControl.DB_ERR setDatabase(String dbName, String dbIP, int dbPort, String dbUser, String dbPass) {
		try {
			this.dbCtrl = new DBControl(dbName,dbIP,dbPort,dbUser,dbPass);
			
			return (this.dbCtrl.hasConnection() ? DBControl.DB_ERR.OK : DBControl.DB_ERR.NO_CONNECTION);
		} catch (ClassNotFoundException e) {
			return DBControl.DB_ERR.UNABLE_TO_LOAD_DRIVER;
			
		} catch (SQLException e) {
			return DBControl.DB_ERR.SQL_EXCEPTION;
		}
	}
	
	
	// ################  I N I T   P R O C E D U R E  ##################
	
	public void loadDatabase(Terminal terminal) {
		terminal.print(Ansi.ansi().a(terminal.ansiDefault).a(String.format("%-60s","Accesing Database... ")));
		switch(this.setDatabase("jsnmpmonitor","127.0.0.1",3306,"root","")) {
		case OK:
			terminal.print(Ansi.ansi().a(terminal.ansiGood).a("[OK]").a(terminal.ansiNewLine));
			terminal.print(Ansi.ansi().a(terminal.ansiDefault).a(String.format("%-60s", "Loading Agents...")));
			try {
				this.loadAgents();
				terminal.print(Ansi.ansi().a(terminal.ansiGood).a("[OK]").a(terminal.ansiNewLine));
			} catch (SQLException e) {
				terminal.print(Ansi.ansi().a(terminal.ansiPrompt).a("[FAIL]").a(terminal.ansiNewLine));
				terminal.print(Ansi.ansi().a(terminal.ansiPrompt).a("Error: Something occured while loading agents"));
			}
			break;
			
		case NO_CONNECTION:
			terminal.print(Ansi.ansi().a(terminal.ansiPrompt).a("[FAIL]").a(terminal.ansiNewLine));
			terminal.print(Ansi.ansi().a(terminal.ansiPrompt).a("Error: Cannot connect to Database").a(terminal.ansiNewLine));
			break;
			
		case UNABLE_TO_LOAD_DRIVER:
			terminal.print(Ansi.ansi().a(terminal.ansiPrompt).a("[FAIL]").a(terminal.ansiNewLine));
			terminal.print(Ansi.ansi().a(terminal.ansiPrompt).a("Error: Unable to load Driver"));
			break;
			
		case SQL_EXCEPTION:
			terminal.print(Ansi.ansi().a(terminal.ansiPrompt).a("[FAIL]").a(terminal.ansiNewLine));
			terminal.print(Ansi.ansi().a(terminal.ansiPrompt).a("Error: SQL Exception Occurred!"));

			break;
		default:
			break;
			
		}
	}
	
	/**
	 * Loads information to be displayed in TMonitor. 
	 * @param terminal
	 */
	public void loadInfo(Terminal terminal) {
		ctrlInfo = new HashMap<String, String>();
		
		ctrlInfo.put(INFO.DBACCESS.value, String.valueOf(this.dbCtrl.hasConnection()));
		terminal.print(Ansi.ansi().a(terminal.ansiDefault).a(String.format("%-60s","Getting interfaces...")));
		ctrlInfo.put(INFO.INTERFACES.value, this.snmpManager.getTransportMappingInfo());
		terminal.print(Ansi.ansi().a(terminal.ansiGood).a("[OK]").a(terminal.ansiNewLine));
		
		terminal.print(Ansi.ansi().a(terminal.ansiDefault).a(String.format("%-60s","Getting Agents... ")));
		ctrlInfo.put(INFO.AGENTS.value, String.valueOf(this.snmpManager.getAgents().size()));
		terminal.print(Ansi.ansi().a(terminal.ansiGood).a("[OK]").a(terminal.ansiNewLine));
		
		terminal.print(Ansi.ansi().a(terminal.ansiDefault).a(String.format("%-60s","Getting Proccesses... ")));
		ctrlInfo.put(INFO.PROCESSES.value, String.valueOf(this.processesSNMP.size()));
		terminal.print(Ansi.ansi().a(terminal.ansiGood).a("[OK]").a(terminal.ansiNewLine));
		
		ctrlInfo.put(INFO.TRAPS.value, "0");
	}
	

	
	
	/**
	 * If database is set, retrieves all SNMPAgents from DB, and sets local @variable agentIDCounter to
	 * the max(agentID)+1.
	 * @throws SQLException
	 */
	public void loadAgents() throws SQLException {
		
		this.snmpManager.addAllAgents(dbCtrl.getSNMPAgents());
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
		
		int numAgents = Integer.parseInt(this.ctrlInfo.get(INFO.AGENTS.value));
		this.ctrlInfo.put(INFO.AGENTS.value, String.valueOf(++numAgents));
		return this.agentIDCounter++;
	}
	
	public void modifyAgent(int agent_id) {
		//TODO
	}
	
	public boolean removeAgent(int agent_id) throws SQLException {
		
		if(this.snmpManager.deleteAgent(agent_id)) {
			dbCtrl.removeSNMPAgent(agent_id);
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
	 * 
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
	public String createSNMPProcess(int agentID, long sleepTime, String...oids) {
		String pid = DateTimeFormatter.ofPattern("yyMMddHHmmss").format(LocalDateTime.now());
		SNMPProcess pr = new SNMPProcess(pid,
				agentID, sleepTime, JSNMPUtil.createVariableBindingList(oids));
		
		pr.subscribe(this);
		this.processesSNMP.put(pr.getProcessID(), pr);
		return pid;
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
	 */
	public void stopProcess(String processID) {
		try {
			this.processesSNMP.get(processID).stop();
		} catch (InterruptedException e) {
			
			e.printStackTrace();
		}
	}
	
	public void getProcessData(String processID) {
		
	}
	
	public Set<String> getAllActiveProcessPID() {
		return this.processesSNMP.keySet().stream().filter((String key) -> this.processesSNMP.get(key).isRunning()).collect(Collectors.toSet());
		
	}
	
	public Set<String> getAllSNMPProcessPID() {
		return this.processesSNMP.keySet();
	}
	
	// ####################    S U B S C R I B E R   M E T H O D S     ######################
	public void onSubscribe(Subscription subscription) {
		if(subscription instanceof SNMPProcess) {
			
		}
	}

	@Override
	public synchronized void onNext(Whisper item) {
		
		if(item instanceof SNMPProcessWhisper) { // ··········· WHISPER IS TYPE SNMPROCESSWHISPER
			
			SNMPProcessWhisper psWhisper = (SNMPProcessWhisper) item;
			if(psWhisper.getAction() == SNMPProcessWhisper.SEND_SNMP) {
				try {
					
					this.snmpManager.sendAsync(psWhisper.getDataPDU(),
							this.snmpManager.getAgent(psWhisper.getAgentID()).getCommunityTarget(),
							this.processesSNMP.get(psWhisper.getProcessID()));
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else if(psWhisper.getAction() == SNMPProcessWhisper.RESPONSE) {
				
				try {
					this.dbCtrl.addPDU(((SNMPProcessWhisper) item).getDataPDU(), psWhisper.getAgentID());
				} catch (SQLException e) {
					
				}
			}
		}
		
	}

	@Override
	public synchronized void onError(Throwable throwable) {
		
		
	}

	@Override
	public synchronized void onComplete() {
		
		
	}
	
	
}
