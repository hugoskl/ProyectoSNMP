package jsnmpm.main;

import java.io.IOException;

import jsnmpm.monitor.terminal.TMonitor;

public class JSNMPMain {

	
	public static void main(String...args) throws IOException {
		TMonitor monitor = new TMonitor();
		monitor.start();
	}
}
