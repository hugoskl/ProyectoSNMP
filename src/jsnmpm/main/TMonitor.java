package jsnmpm.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import jsnmpm.control.SNMPAgent;
import jsnmpm.control.SNMPManager;
import jsnmpm.control.utilities.JSNMPUtil;
import jsnmpm.terminal.TUtil;
import jsnmpm.terminal.Terminal;

/**
 * 
 * @author MrStonedDog
 *
 */
public class TMonitor{

	
	static SNMPManager manager;
	static Terminal terminal;
	
	static volatile boolean running;
	static String[] managerInfo;
	
	public static void main(String ...args) throws IOException {
		running = true;
		terminal = new Terminal();
		manager = new SNMPManager();
		managerInfo = new String[]{"All",String.valueOf(manager.getAgents().size()),"0"};// TODO
		terminal.writeBanner();
		terminal.printMainInfo(managerInfo);
		terminal.printMainMenu();
		//terminal.setUpStartHeader(new String[]{"All",String.valueOf(manager.getAgents().size()),"0"}); // TODO CREATE VARIABLE managerInfo (String[] or Map<>)?
		
		while(running) {
			processInput(terminal.readInput());
		}
	}
	
	// ##############     BASIC INPUT PROCESSING    ############### 
	private static void processInput(String input) {
		if(input != null) {
			if(terminal.currentStatus == Terminal.Prompt.NORMAL)
				processOption(input);
			else 
				processCommand(input);
		}
	}
	
	private static void processOption(String option) {
		switch(option) {
		case "1": //ADD AGENT
			TMonitor.addAgent();
			break;
		case "5":
			if(manager.getAgents().size() > 0)
				TMonitor.sendUniqueQuery();
			else
				terminal.coutWarning("Warning: You dont have any configured Agents!");
			break;
		case "14": //SHELL
			terminal.changePrompt();
			terminal.clearTerminal();
			terminal.setUpStartHeader(new String[]{"All",String.valueOf(manager.getAgents().size()),"0"});
			break;
		case "15": //EXIT
			System.exit(0);
		default:
			terminal.coutError("Error: Option not found!");
			break;
	}
	}
	private static void processCommand(String command) {

		switch(command.split(" ")[0].toLowerCase()) {
		case "resize": // resize [width]
			try {
				terminal.resize(Integer.parseInt(command.split(" ")[1]), new String[]{"All",String.valueOf(manager.getAgents().size()),"0"} );
			} catch (NumberFormatException nfe){
				terminal.coutError("Error: Wrong argument [width]=\'"+command.split(" ")[1]+"\'. Expect type: int");
			} catch(IOException IOe) {
				terminal.coutError("Error: IO is broken");
			} catch(IndexOutOfBoundsException iobe) {
				terminal.coutError("Error: Missing argument [width]");
			}catch(Exception e) {
				terminal.coutError("Error: Unknown expcetion occurred");
			}
			break;
		case "exit":
			terminal.changePrompt();
			terminal.clearTerminal();
			terminal.setUpStartHeader(new String[]{"All",String.valueOf(manager.getAgents().size()),"0"});
			break;
		default:
			terminal.coutError("Error: Unknown command \'" + command.split("  ")[0] + "\'");
			break;
		}
	}
	
	// #############    TERMINAL MENU OPTIONS HANDLING    #####################
	// ··· (1) ADD AGENT
	private static void addAgent() {
		String ip; String name; String readCommunity;
		int port = JSNMPUtil.DEFAULT_SNMP_PORT1;
		Pattern ipPattern = Pattern.compile(JSNMPUtil.IP_PATTERN);
		terminal.deleteLine(); // TODO THIS METHOD SHOULD BE REDONE / RECONFIGURED
		terminal.cout(Ansi.ansi().a(terminal.ansiMenuOption).a(" - ADD AGENT - "));
		boolean askAgain = false;
		// ASK IP
		do {
		ip = terminal.readInput(Ansi.ansi().a(terminal.ansiPrompt).a("IPv4: "));
		if(ipPattern.matcher(ip).matches())
			askAgain = false;
		else {
			terminal.coutError("Error: \'" + ip + "\' is not a valida IP Address");
			askAgain = true;
		}
		}while(askAgain);
		// ASK PORT
		do {
			String sport = terminal.readInput(Ansi.ansi().a(terminal.ansiPrompt).a("Port (def=161):"));
			if(sport.isEmpty() || sport.isBlank())
				port = JSNMPUtil.DEFAULT_SNMP_PORT1;
			else {
				try {
					port = Integer.parseInt(sport);
					askAgain = false;
					if(port != JSNMPUtil.DEFAULT_SNMP_PORT1)
						terminal.coutWarning("Warning: Port \'" + port + "\' is not SNMP default port.");
				}catch(NumberFormatException nfe) {
					terminal.coutError("Error: Wrong input \'" + port + " - Expected type: int");
					askAgain = true;
				}
			}
		}while(askAgain);
		// ASK NAME
		name = terminal.readInput(Ansi.ansi().a(terminal.ansiPrompt).a("Name: "));
		// ASK READCOMMUNITY
		readCommunity = terminal.readInput(Ansi.ansi().a(terminal.ansiPrompt).a("Read Community: "));
		try {
			manager.addAgent(ip, port, name, readCommunity);
			terminal.cout(Ansi.ansi().a(terminal.ansiInfo).a("Agent added successfully!\n"));
			managerInfo[1] = String.valueOf(manager.getAgents().size());
		} catch (UnknownHostException e) {
			terminal.coutError("Error: Agent could not be created... Host is unknown");
		}
		terminal.readInput(Ansi.ansi().a(terminal.ansiMenuOption).a("  -- PRESS ENTER TO CONTINUE --  ").a(terminal.ansiDefault));
		terminal.reset(managerInfo);
	}
	
	// ··· (2) SHOW AGENTS
	
	// ··· (3) CONFIG AGENT
	
	// ··· (4) DELETE AGENT
	
	// ··· (5) SEND (A)/SYNC
	public static void sendUniqueQuery() {
		String oid = null;
		int type; int agentID = 0;
		boolean askAgain = false;
		
		terminal.deleteLine(); // TODO THIS METHOD SHOULD BE REDONE / RECONFIGURED
		terminal.cout(Ansi.ansi().a(terminal.ansiMenuOption).a(" - SEND SYNC/ASYNC REQUEST - "));
		do { // SYNC / ASYNC
			try {
				type = Integer.parseInt(terminal.readInput(Ansi.ansi().a(terminal.ansiPrompt).a("(1)SYNC / (2)ASYNC [def:(1)]: "))); // TODO FIX
				askAgain = false;
			}catch(NumberFormatException nfe) {
				terminal.coutError("Error: Unknown option");
				askAgain = true;
			}
		}while(askAgain);
		// CHOOSE AGENT TODO MAYBE MORE THAN 1 AGENT?
		for(SNMPAgent agent : manager.getAgents()) 
			terminal.cout(Ansi.ansi().a(terminal.ansiInfo2).a("ID:" + agent.getId() + " - IP: " + agent.getIp() + " - NAME: "
					+agent.getName()+ " - State: " + agent.getState() + "\n"));
		do {
			String sID = terminal.readInput(Ansi.ansi().a(terminal.ansiPrompt).a("AgentID: "));
			try {
				agentID = Integer.parseInt(sID);
				if(manager.getAgent(agentID) == null) {
					terminal.coutError("Error: Cannot find SNMPAGent for specified AgentID");
					askAgain = true;
				}else
					askAgain = false;
				
			}catch(NumberFormatException nfe) {
				terminal.coutError("Error: Expected type int for AgentID");
				askAgain = true;
			}
		}while(askAgain);
		JSNMPUtil.TEST_OIDS.forEach((key, value) -> {
			terminal.cout(Ansi.ansi().a(terminal.ansiInfo2).a("("+key+") - "+ value[0] + " - " + value[1] +"\n"));
		});
		do {
			String sOID = terminal.readInput(Ansi.ansi().a(terminal.ansiPrompt).a("ID OID: "));
			try {
				int ioid = Integer.parseInt(sOID);
				oid = JSNMPUtil.TEST_OIDS.get(ioid)[1];
				if(oid == null) {
					terminal.coutError("Error: Invalid OID ID");
					askAgain = true;
				}else
					askAgain = false;
				
			}catch(NumberFormatException nfe) {
				terminal.coutError("Error: Expected type int for OID ID");
				askAgain = true;
			}catch(IndexOutOfBoundsException  iob) {
				terminal.coutError("Error: Inavlid OID ID");
				askAgain = true;
			}
		}while(askAgain);
		
		terminal.cout(Ansi.ansi().a(terminal.ansiInfo2).a("Sending Query...\n"));
		String response = JSNMPUtil.getSimplifiedResponse(manager.sendSyncGET(oid, agentID));
		terminal.cout(Ansi.ansi().a(terminal.ansiInfo2).a("Got response!\n"));
		if(response != null)
			terminal.cout(Ansi.ansi().a(terminal.ansiInfo2).a(response));
		else
			terminal.coutWarning("Warning: Response data is NULL...");
		
		terminal.readInput(Ansi.ansi().a(terminal.ansiMenuOption).a("  -- PRESS ENTER TO CONTINUE --  ").a(terminal.ansiDefault));
		terminal.reset(managerInfo);
		
		
	}
	
	// ··· (6) SEND N SYNC
	
	// ··· (7) SEND NEXT/BULK
	
	// ··· (8) SEE TRAPS
	
	// ··· (9) NEW PROCESS
	
	// ··· (10) START PROCESS
	
	// ··· (11) SHOW PROCESSES
	
	// ··· (12) DEL PROCESS
	
	// ··· (13) SNMPMANAGER
	
	// ··· (14) JSMP-SHELL --> NO NEED 4 EXTRA METHOD
	
	// ··· (15) EXIT --> NO NEED 4 EXTRA METHODD
	
	
	
	
	
	
}
