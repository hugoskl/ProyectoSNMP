package jsnmpm.control;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.snmp4j.PDU;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

/**
 * This class is used for periodically monitoring a given SNMPAgent.
 * @author MrStonedDog
 *
 */
public class SNMPProcess implements Runnable{

	private int type = PDU.GET;
	private int processID;
	private int sleepTime;
	private final int agentID;
	private volatile boolean running = false;
	List<VariableBinding> varBindings = null;
	PDU pdu = null;
	Map<OID, Variable> results = null;
	
	// #############   CONSTRUCTOR   ################
	public SNMPProcess(int agentID,int sleepTime, List<VariableBinding> varBindings) {
		this.agentID = agentID;
		this.sleepTime = sleepTime;
		this.varBindings = varBindings;
		this.results = this.varBindings.stream().collect(Collectors.toMap(VariableBinding::getOid, VariableBinding::getVariable));
		this.pdu = new PDU(this.type, varBindings);
		
	}
	
	// ###################   SETTERS AND GETTERS   #########################
	public void setSleepTime(int sleepTime) {
		this.sleepTime = sleepTime;
	}
	
	public void setVarBindings(List<VariableBinding> varBindings) {
		this.varBindings = varBindings;
	}
	
	public int getSleepTime() {
		return this.sleepTime;
	}
	
	public int getAgendID() {
		return this.agentID;
	}
	
	public List<VariableBinding> getVarbindings() {
		return this.varBindings;
	}
	
	public boolean isRunning() {
		return this.running;
	}
	
	
	// RUN METHOD --> FUN IS HERE
	public void run() {
		
		while(running) {
			
		}

	}
	
	
		
}
