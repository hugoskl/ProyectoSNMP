package jsnmpm.main;

import java.io.Console;
import java.io.IOException;

import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.fusesource.jansi.Ansi;
import org.snmp4j.PDU;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.Address;

import jsnmpm.control.DBControl;
import jsnmpm.control.DBControl.DB_ERR;
import jsnmpm.control.SNMPAgent;
import jsnmpm.control.SNMPController;
import jsnmpm.control.SNMPManager;
import jsnmpm.control.utilities.JSNMPUtil;
import jsnmpm.terminal.Terminal;


/**
 * This class is the main class for JSNMP Monitor APP executed in Terminal Mode.
 * @author MrStonedDog
 *
 */
public class TMonitor{

	
	static SNMPController ctrl;
	static Terminal terminal;
	
	static volatile boolean running;
	
	public static void main(String ...args) throws IOException{
		
		terminal = new Terminal();
		ctrl = new SNMPController();
		ctrl.loadDatabase(terminal);
		ctrl.loadInfo(terminal);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
		
		terminal.clearTerminal();
		terminal.writeBanner();
		terminal.printMainInfo(ctrl.getCtrlInfo());
		terminal.printMainMenu();
		terminal.setInitialCursor();
		
		running = true;
		while(running) {
			processInput(terminal.readInput(Ansi.ansi().a(terminal.ansiPrompt).a((terminal.currentStatus == Terminal.Prompt.NORMAL) 
					? Ansi.ansi().a(terminal.optionPrompt)
					: Ansi.ansi().a(terminal.shellPrompt))));
		}
	}
	

	
	// ##############     BASIC INPUT PROCESSING     ############### 
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
			
		case "2": // SHOW AGETS
			TMonitor.showAgents();
			break;
			
		case "5":
			if(ctrl.managerGetAgents().size() > 0)
				TMonitor.sendUniqueQuery();
			else
				terminal.coutWarning("Warning: You dont have any configured Agents!");
			break;
			
		case "14": //SHELL
			terminal.changePrompt();
			terminal.clearTerminal();
			terminal.setUpStartHeader(ctrl.getCtrlInfo());
			break;
			
		case "15": //EXIT
			System.exit(0);
			break;
			
		default:
			terminal.coutError("Error: Option not found!");
			break;
	}
	}
	private static void processCommand(String command) {

		switch(command.split(" ")[0].toLowerCase()) {
		case "resize": // resize [width]
			try {
				terminal.resize(Integer.parseInt(command.split(" ")[1]), ctrl.getCtrlInfo());
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
			terminal.setUpStartHeader(ctrl.getCtrlInfo());
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
		boolean askAgain = false;
		
		String title = "              -  ADD AGENT  -              ";
		terminal.deleteLastLines(terminal.currentPromptRow - terminal.startOptionPromptRow);
		terminal.currentPromptCol = terminal.getTerminalWidth()/2 - title.length()/2;
		terminal.cout(Ansi.ansi().a(terminal.ansiMenuOption).a(title));
		terminal.currentPromptCol = terminal.startOptionPromptCol;
		terminal.coutNewLine();
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
			String sPort = terminal.readInput(Ansi.ansi().a(terminal.ansiPrompt).a("Port (def=161):"));
			if(sPort.isEmpty() || sPort.isBlank())
				port = JSNMPUtil.DEFAULT_SNMP_PORT1;
			else {
				try {
					port = Integer.parseInt(sPort);
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
			terminal.coutNewLine();
			int agentID = ctrl.addNewAgent(ip, port, name, readCommunity);
			// CHECK IF AGENT IS REACHABLE
			terminal.print(Ansi.ansi().cursor(terminal.currentPromptRow, terminal.currentPromptCol).a(terminal.ansiInfo2).a("Checking if agent is reachable..."));
			if(ctrl.managerGetAgent(agentID).isReachable()) {
				terminal.print(Ansi.ansi().a(terminal.ansiInfo2).a(" OK\n"));
				terminal.currentPromptRow++;
				terminal.coutNewLine();
			}
			else {
				terminal.print(Ansi.ansi().a(terminal.ansiInfo2).a(" NO\n"));
				terminal.currentPromptRow++;
				terminal.coutNewLine();
				terminal.coutWarning("Warning: Cannot reach target (PING)/(TCP PORT 7). Make sure the machine is up.");
			}
			
			
			
			if(name.isEmpty() || name.isBlank())
				terminal.coutWarning("Warning: Agent has no name!");
			if(readCommunity.isBlank() || readCommunity.isEmpty())
				terminal.coutWarning("Warning: ReadCommunity is empty!");
			
			terminal.cout(Ansi.ansi().a(terminal.ansiInfo).a("--> Agent added successfully!"));
			
		} catch (UnknownHostException e) {
			terminal.coutError("Error: Agent could not be created... Host is unknown");
		} catch (SQLException e) {
			terminal.coutError("Error: Agent could not be created... There was a problem with the Database");
			e.printStackTrace();
		}
		terminal.coutNewLine();
		terminal.currentPromptCol = 1 + (terminal.getTerminalWidth() - "  -- PRESS ENTER TO CONTINUE --  ".length()) / 2;
		terminal.readInput(Ansi.ansi().a(terminal.ansiMenuOption).a("  -- PRESS ENTER TO CONTINUE --  ").a(terminal.ansiDefault));
		terminal.reset(ctrl.getCtrlInfo());
	}
	
	
	// ··· (2) SHOW AGENTS TODO FIIIX
	public static void showAgents() {
		String title = "              -  SHOW AGENTS  -              ";
		terminal.deleteLastLines(terminal.currentPromptRow - terminal.startOptionPromptRow);
		terminal.currentPromptCol = terminal.getTerminalWidth()/2 - title.length()/2;
		terminal.cout(Ansi.ansi().a(terminal.ansiMenuOption).a(title));
		terminal.coutNewLine();
		
		// AGENTS WILL DE DISPLAYED ACCORDING TO THE GIVEN SPACE (X Lines / 40 Col (width)
		List<SNMPAgent> agentList = ctrl.managerGetAgents();
		if(agentList.size() == 0) {
			terminal.currentPromptCol = (terminal.getTerminalWidth() - "NO AGENTS ARE CONFIGURED".length()) / 2;
			terminal.coutWarning("NO AGENTS ARE CONFIGURED");
			terminal.coutNewLine();
			terminal.currentPromptCol = (terminal.getTerminalWidth() - "  -- PRESS ENTER TO CONTINUE --  ".length()) / 2;
			terminal.readInput(Ansi.ansi().a(terminal.ansiMenuOption).a("  -- PRESS ENTER TO CONTINUE --  ").a(terminal.ansiDefault));
			terminal.reset(ctrl.getCtrlInfo());
			return;
		}
		
		terminal.currentPromptCol = terminal.startOptionPromptCol;
		int agentXRow = terminal.getTerminalWidth()/40;
		boolean hasNextAgent = true;
		int agentDescriptionRows = 5; int jumpLines = 2;
		for(int i = 0, col = terminal.currentPromptCol;
				i<agentList.size(); i+=agentXRow,  col = terminal.startOptionPromptCol) {
			Ansi headers = Ansi.ansi(); Ansi names = Ansi.ansi(); Ansi addresses = Ansi.ansi();
			Ansi states = Ansi.ansi(); Ansi readComms = Ansi.ansi();
			//  HEADERS
			try {
				for(int j = i, width=35, ag = 0; j < agentXRow + i; j++, ag++){
					headers.a(Ansi.ansi().cursor(terminal.currentPromptRow, terminal.currentPromptCol + width * ag)
							.a(terminal.ansiMenuOption).a(" - AGENT " + agentList.get(j).getId() + " - "));
				
					names.a(Ansi.ansi().cursor(terminal.currentPromptRow + 1, terminal.currentPromptCol + width * ag)
							.a(terminal.ansiPrompt).a("Name: ").a(terminal.ansiDefault).a(agentList.get(j).getName()));
				
					addresses.a(Ansi.ansi().cursor(terminal.currentPromptRow + 2, terminal.currentPromptCol + width * ag)
							.a(terminal.ansiPrompt).a("Address: ").a(terminal.ansiDefault).a(agentList.get(j).getCommunityTarget().getAddress()));
				
					states.a(Ansi.ansi().cursor(terminal.currentPromptRow + 3, terminal.currentPromptCol + width * ag)
							.a(terminal.ansiPrompt).a("State: ").a(terminal.ansiDefault).a((agentList.get(j).getState()) ? "Reachable" : "Unreachable"));
				
					readComms.a(Ansi.ansi().cursor(terminal.currentPromptRow + 4, terminal.currentPromptCol + width * ag)
							.a(terminal.ansiPrompt).a("Read Comm: ").a(terminal.ansiDefault).a(agentList.get(j).getReadCommunity()));
				}
			}catch(IndexOutOfBoundsException exc) {hasNextAgent = false;}
			
			terminal.print(headers.a(terminal.ansiDefault).a("\n"));
			terminal.print(names.a("\n"));
			terminal.print(addresses.a("\n"));
			terminal.print(states.a("\n"));
			terminal.print(readComms.a("\n"));
			
			terminal.currentPromptRow += agentDescriptionRows;
			terminal.jumpLines(jumpLines);
			
			if(!hasNextAgent)
				break;
	
		}

		terminal.currentPromptCol = (terminal.getTerminalWidth() - "  -- PRESS ENTER TO CONTINUE --  ".length()) / 2;
		terminal.readInput(Ansi.ansi().a(terminal.ansiMenuOption).a("  -- PRESS ENTER TO CONTINUE --  ").a(terminal.ansiDefault));
		terminal.reset(ctrl.getCtrlInfo());
	}
	
	// ··· (3) CONFIG AGENT
	
	// ··· (4) DELETE AGENT
	
	// ··· (5) SEND (A)/SYNC
	public static void sendUniqueQuery() {
		//String oid = null;
		String[] oids = null;
		int type; int[] agentIDs;
		boolean askAgain = false;
		String title = "            - SEND SYNC REQUEST -          ";
		
		terminal.deleteLastLines(terminal.currentPromptRow - terminal.startOptionPromptRow);
		terminal.currentPromptCol = terminal.getTerminalWidth()/2 - title.length()/2;
		terminal.cout(Ansi.ansi().a(terminal.ansiMenuOption).a(title));
		terminal.currentPromptCol = terminal.startOptionPromptCol;
		// CHOOSE AGENT(S)
		terminal.cout(Ansi.ansi().a(terminal.ansiInfo2).a("Choose one ore more agents (separated by \",\")"));
		terminal.coutNewLine();
		
		terminal.print(Ansi.ansi().cursor(terminal.currentPromptRow, (terminal.getTerminalWidth() - "AGENTS".length()) / 2).a(terminal.ansiPrompt).a("AGENTS"));
		terminal.coutNewLine();
		
		for(SNMPAgent agent : ctrl.managerGetAgents()) 
			terminal.cout(Ansi.ansi().a(terminal.ansiInfo2).a("ID:" + agent.getId() + " - IP: " + agent.getIp() + " - NAME: "
					+agent.getName()+ " - State: " + agent.getState()));
		
		do {
			String[] strIDs = terminal.readInput(Ansi.ansi().a(terminal.ansiPrompt).a("Agents ID: ")).split(",");
			agentIDs = new int[strIDs.length];
			for(int i = 0; i < strIDs.length; i++) {
				try {
					int agentID = Integer.parseInt(strIDs[i].trim());
					if(ctrl.managerGetAgent(agentID) == null) {
						terminal.coutError("Error: Cannot find SNMPAGent with ID=\'" + strIDs[i] + "\'.");
						askAgain = true;
						break;
					}else {
						agentIDs[i] = agentID;
						askAgain = false;
					}	
				
				}catch(NumberFormatException nfe) {
					terminal.coutError("Error: Expected type int for AgentID");
					askAgain = true;
					break;
				}
			}
		}while(askAgain);
		
		// SHOW TEST OIDS TODO HOW DO WE DO THIS FOR ALL OIDS?? XD
		JSNMPUtil.TEST_OIDS.forEach((key, value) -> {
			terminal.cout(Ansi.ansi().a(terminal.ansiInfo2).a("("+key+") - "+ value[0] + " - " + value[1]));
		});
		do {
			String sOID = terminal.readInput(Ansi.ansi().a(terminal.ansiPrompt).a("ID OIDs: "));
			try {
				oids = Arrays.asList(sOID.split(",")).stream().map(String::trim).map(Integer::parseInt).map((Integer index) -> {return JSNMPUtil.TEST_OIDS.get(index)[1];}).collect(Collectors.toList()).toArray(new String[0]);
				
				/*int ioid = Integer.parseInt(sOID);
				oid = JSNMPUtil.TEST_OIDS.get(ioid)[1];*/
				for(String oid : oids) {
					
					if(oid == null) {
						terminal.coutError("Error: Invalid OID ID");
						askAgain = true;
					}else
						askAgain = false;
				}
			}catch(NumberFormatException nfe) {
				terminal.coutError("Error: Expected type int for OID ID");
				askAgain = true;
			}catch(IndexOutOfBoundsException  iob) {
				terminal.coutError("Error: Inavlid OID ID");
				askAgain = true;
			}catch(NullPointerException npo) {
				terminal.coutError("Error: Cannot find select OID index");
				askAgain = true;
			}
		}while(askAgain);
		
		
		HashMap<Integer, PDU> pdus = new HashMap<Integer,PDU>();
		terminal.cout(Ansi.ansi().a(terminal.ansiInfo2).a("Sending..."));
		
		for(int agentID : agentIDs) {
			
			ResponseEvent<Address> ans = ctrl.managerSendSyncGET(agentID, oids);
			pdus.put(agentID, ans.getResponse());
			
			for(String[] response : JSNMPUtil.getSimplifiedUniqueResponse(ans)) {
				try {
					terminal.cout(Ansi.ansi().a(terminal.ansiInfo2).a("Response from Agent \'"+ agentID +"\' (" + response[0] + "):"));
					terminal.cout(Ansi.ansi().a(terminal.ansiInfo2).a(String.format("OID: %s -> %s", response[1], response[2])));
				}catch(IndexOutOfBoundsException | NullPointerException exc) {
					terminal.coutError("Error: Response from Agent \'"+ agentID + "\' is NULL");
					}
				catch(Exception e){
					terminal.coutError("Error: Unknown Error reading response from Agent \'"+ agentID + "\'");
				}
			}
			terminal.coutNewLine();
			
		}
		
		askAgain = true;
		boolean saved = true;
		
		do {
		
		switch(terminal.readInput(Ansi.ansi().a(terminal.ansiPrompt).a("Do you wish to save recieved PDUs in DB? (y/n) :")).toLowerCase()) {
		case "y":
		case "yes":
			for(int agentID : pdus.keySet()) {
				try {
					ctrl.dbAddPDU(pdus.get(agentID), agentID);
				} catch (SQLException e) {
					saved = false;
				}
			}
			askAgain = false;
			terminal.coutNewLine();
			
			if(saved)
				terminal.cout(Ansi.ansi().a(terminal.ansiInfo).a("PDU(s) saved successfuly!"));
			else
				terminal.coutError("Error: Cannot save PDU(s)");
			
			break;
			
		case "n":
		case "no":
			terminal.coutNewLine();
			terminal.coutWarning("PDU(s) were not saved.");
			askAgain = false;
			break;
		
		default:
			askAgain = true;
			break;
		}
		}while(askAgain);
		
		terminal.coutNewLine();
		terminal.currentPromptCol = (terminal.getTerminalWidth() - "  -- PRESS ENTER TO CONTINUE --  ".length()) / 2;
		terminal.readInput(Ansi.ansi().a(terminal.ansiMenuOption).a("  -- PRESS ENTER TO CONTINUE --  ").a(terminal.ansiDefault));
		terminal.reset(ctrl.getCtrlInfo());
		
		
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
