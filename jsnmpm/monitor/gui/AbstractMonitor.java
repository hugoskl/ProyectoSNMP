package jsnmpm.monitor.gui;

import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import jsnmpm.control.utilities.*;
import jsnmpm.control.whisper.CtrlWhisper;
import jsnmpm.control.whisper.ProcessWhisper;
import jsnmpm.control.whisper.RequestWhisper;
import jsnmpm.control.whisper.Whisper;

/**
 * This class handles all the asynchronous communication between SNMPController and a given GUI/Terminal Monitor.
 * @author MrStonedDog
 *
 */
public abstract class AbstractMonitor implements Subscriber<Whisper>{

	/** Does nothing **/
	@Override
	public void onSubscribe(Subscription subscription) {}

	@Override
	public void onNext(Whisper item) {
		if(item instanceof ProcessWhisper) 
			this.processProcessWhisper((ProcessWhisper)item);
	
		else if(item instanceof RequestWhisper)
			this.processRequestWhisper((RequestWhisper)item);
		
		else if(item instanceof CtrlWhisper) 
			this.processCtrlWhisper((CtrlWhisper)item);
		
	}
	
	
	/** Does nothing **/
	@Override
	public void onError(Throwable throwable) {}
	/** Does nothing **/
	@Override
	public void onComplete() {}
	
	/**
	 * Processes a ProcessWhisper message send by the SNMPController.<br>
	 * These whispers normally contain data about an executing SNMPProcess.
	 * @param item
	 */
	public abstract void processProcessWhisper(ProcessWhisper processWhisper);
	
	/**
	 * Processes a ProcessWhisper message send by the SNMPController.<br>
	 * These whispers normally contain data from an Syncrhonized SNMP Request
	 * @param item
	 */
	public abstract void processRequestWhisper(RequestWhisper requestWhisper);
	
	/**
	 * Processes a CtrlWhisper message send by the SNMPController. <br>
	 * A ctrlWhisper contains: <br/>
	 * � String: PS(?) <br/>
	 * � int: execTime <br/>
	 * � int: exitCode <br/>
	 * This can be handled accessing the static final constants in CTRLConstants.
	 * @param ctrlWhisper
	 */
	public abstract void processCtrlWhisper(CtrlWhisper ctrlWhisper);
	



}
