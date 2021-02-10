package jsnmpm.control.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.stream.Collectors;

import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

import jsnmpm.control.utilities.Whisper;

/**
 * This class is used for periodically monitoring a given SNMPAgent.
 * @author MrStonedDog
 *
 */

public class SNMPProcess implements ResponseListener, Runnable, Publisher<SNMPProcessWhisper>, Subscription{
// TODO SHOULD THIS CLASS EXTEND THREAD INSTEAD OF IMPLEMENTING RUNNABLE?
	
	private int type = PDU.GET;
	private String processID;
	private String name;
	private String description;
	private long sleepTime;
	private final int agentID;
	private Thread executer;
	
	private volatile boolean running = false;
	private List<VariableBinding> varBindings = null;
	private PDU pdu = null;
	private Map<OID, Variable> results = null;
	private Subscriber<Whisper> subscriber = null;
	
	// #############   CONSTRUCTOR   ################
	public SNMPProcess(String processID, int agentID, long sleepTime, List<VariableBinding> varBindings) {
		this.processID = processID;
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
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setDescription(String descr) {
		this.description = descr;
	}
	
	public void setVarBindings(List<VariableBinding> varBindings) {
		this.varBindings = varBindings;
		this.pdu = new PDU(this.type, this.varBindings);
	}
	
	public String getProcessID() {
		return this.processID;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getDescription() {
		return this.description;
	}
	
	public long getSleepTime() {
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
	
	
	// #######################   P R O C E S S   M E T H O D S   ############################
	
	/**
	 * Creates a new Thread and runs this instance of SNMPProcess. 
	 */
	public void start() {
		this.executer = new Thread(this);
		this.executer.start();
	}
	
	/**
	 * Tries to kill the Thread that is being executed by this instance.
	 * @throws InterruptedException 
	 */
	public void stop() throws InterruptedException { 
		if(this.executer != null) {
			this.running = false;
			this.executer.join();
		}
			
	}
	
	// ····  RESPONSELISTENER ·········
	@Override
	public <A extends Address> void onResponse(ResponseEvent<A> event) {
		
		  ((Snmp)event.getSource()).cancel(event.getRequest(), this); // CANCELING THE ASYNC RESPONSE WHEN RECIEVED
		  this.subscriber.onNext(new SNMPProcessWhisper(this.processID, this.agentID, SNMPProcessWhisper.RESPONSE, event.getResponse()));
	}
	
	@Override
	public void run() {
		this.running = true;
		while(this.running) {
			
			try {
				Thread.sleep(this.sleepTime);
				this.subscriber.onNext(new SNMPProcessWhisper(this.processID, this.agentID, SNMPProcessWhisper.SEND_SNMP, this.pdu));
			} catch (InterruptedException e) {
				
			}
		}
		
	}

	@Override
	public void subscribe(Subscriber subscriber) {
		this.subscriber = subscriber;
		this.subscriber.onSubscribe(this);
		
	}




	@Override
	public void request(long n) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub
		
	}





	


	
		
}
