package tests;

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

import jsnmpm.control.DBControl;
import jsnmpm.control.SNMPAgent;
import jsnmpm.control.SNMPController;
import jsnmpm.control.SNMPManager;
import jsnmpm.control.SNMPProcess;
import utilities.user.KbCtrl;
import utilities.user.Kbctl;

public class TestSnmp {
	
	public static void main(String ...args) throws IOException, InterruptedException {
		
		
		SNMPController ctrl = new SNMPController();
		ctrl.setDatabase("jsnmpmonitor","127.0.0.1",3307,"root","");
		try {
			ctrl.loadAgents();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		
		try {
			ctrl.addNewAgent("172.20.3.4", 161, "Nobody", "public");
		} catch (UnknownHostException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for(SNMPAgent agent : ctrl.managerGetAgents()) {
			System.out.println("ID: "+  agent.getId());
			System.out.println("ALIAS: " + agent.getName());
			System.out.println("IP: " + agent.getIp());
			System.out.println("PORT: " + agent.getPort());
			System.out.println("READ_COM: " + agent.getReadCommunity());
			System.out.println("Status: " + (agent.isReachable() ? "Reachable" : "Unreachable"));
		}
		

		
		
	}
}
