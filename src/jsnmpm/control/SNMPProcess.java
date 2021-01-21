package jsnmpm.control;

import java.util.ArrayList;
import java.util.List;

import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

/**
 * This class is used for periodically monitoring a given SNMPAgent.
 * It implements asynchronous sending and receiving of PDUs.
 * 
 * The data collected from the snmp queries is saved in SNMPAgents @variable pduList (HashMap).
 * 
 * This process  may be started and stopped as many times as the user wishes as long as the Thread
 * used for running this instance is not reused. (Threads don't resurrect from death).
 * 
 * The same SNMPAgent may be used for different Processes, in order to implement a different @param sleepTime, 
 * but ONLY if the all the @param VariableBindings are different in each process.
 * Otherwise behavior is unknown.
 * @author MrStonedDog
 *
 */
public class SNMPProcess implements Runnable{

	
	
	private int sleepTime;
	private final int agentID;
	List<VariableBinding> varBindings = null;
	
	// #############   CONSTRUCTOR   ################
	public SNMPProcess(int agentID,int sleepTime, List<VariableBinding> varBindings) {
		this.agentID = agentID;
		this.sleepTime = sleepTime;
		this.varBindings = varBindings;
		
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
	
	
	// RUN METHOD --> FUN IS HERE
	public void run() {
		
	}
	
	
		
}
