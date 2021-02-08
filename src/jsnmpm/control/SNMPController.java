package jsnmpm.control;

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.IntConsumer;

import org.fusesource.jansi.Ansi;
import org.snmp4j.PDU;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.Address;

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
public class SNMPController {
	
	enum INFO{
		DBACCESS("DBAccess: "),
		INTERFACES("Interfaces: "),
		AGENTS("Agents: "),
		TRAPS("Traps: "),
		PROCESSES("Processes: ");
		
		public final String value;
		private INFO(String info) {
			this.value = info;
		}
	}
	private SNMPManager snmpManager = null;
	private DBControl dbCtrl = null;
	private Map<String, String> ctrlInfo = null;
	public int agentIDCounter = 0;
	
	// CONSTRUCTOR
	public SNMPController() throws IOException{
		this.snmpManager = new SNMPManager();
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
		ctrlInfo.put(INFO.PROCESSES.value, "0");
		terminal.print(Ansi.ansi().a(terminal.ansiGood).a("[OK]").a(terminal.ansiNewLine));
		
		ctrlInfo.put(INFO.TRAPS.value, "0");
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
	
	// #############################  I N I T   M E T H O D S  ##############################
	
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
	
	public ArrayList<SNMPAgent> managerGetAgents(){
		return this.snmpManager.getAgents();
	}
	
	public SNMPAgent managerGetAgent(int agent_id) {
		return this.snmpManager.getAgent(agent_id);
	}
	
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
	
	
}
