package jsnmpm.control.whisper;

import jsnmpm.control.utilities.CTRLConstants;

public class CtrlWhisper implements Whisper{

	
	private String process;
	private int exitCode;
	
	public CtrlWhisper(String process,int execTime, int exitCode) {
		this.process = process;
		this.exitCode = exitCode;

	}
	
	public String getProcess() {
		return this.process;
	}
	
	public int getExitCode() {
		return this.exitCode;
	}
}
