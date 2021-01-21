package jsnmpm.main;

import java.io.IOException;
import java.net.UnknownHostException;

import jsnmpm.control.SNMPAgent;
import jsnmpm.control.SNMPManager;
import utilities.user.KbCtrl;
import utilities.user.Kbctl;

public class TestSnmp {
	
	static SNMPManager manager = null;
	static String prompt = "\n#snmp>";
	
	
	public static void main(String ...args) throws IOException, InterruptedException {
	
		manager = new SNMPManager();
		int op = 0;
		clearScreen();
		//System.out.println("\n\n"+JSNMPU.createASCIICustomBanner(JSNMPU.banner1, 2));
		while(true) {
			op = execMainMenu();
			switch(op) {
				case 1:
					clearScreen();
					break;
				case 2:
					break;
				case 3:
					break;
				case 4:
					break;
			}
		}
	}
	
	public static void writeText(String text) {
		for(int i = 0; i<text.length(); i++) {
			System.out.print(text.charAt(i));
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
			
			
			
	}
	

	public static void clearScreen() throws InterruptedException, IOException {  
		//final String os = System.getProperty("os.name");
		//if (os.contains("Windows"))
    			new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
		//else
		//	Runtime.getRuntime().exec("clear");
		//Process p = Runtime.getRuntime().exec("mode 80, 25");
	}  

	private static void cls() throws IOException {
		Runtime.getRuntime().exec("cmd.exe /c cls");
	}
	public static String readLine() {
		return Kbctl.askString("");
	}
	
	
	public static int execMainMenu() {
		System.out.println("\n#\t1. ADD AGENT");
		System.out.println("\n#\t2. SHOW AGENTS");
		System.out.println("\n#\t3. SEND SYNC");
		System.out.println("\n#\t4. SEND ASYNC\n#\n#");
		return KbCtrl.askInteger(prompt);
	}

	
	public static void addAgent() {
		writeText("\n# ·········· ADD AGENT");
		String ip = KbCtrl.askString("# IP (required): ");
		String rcom = KbCtrl.askString("# Read Community (required): ");
		SNMPAgent agent = null;
		try {
			agent = new SNMPAgent(ip, rcom);
			manager.addAgent(agent);
			writeText("#\n#Agent Added Successfully!");
		} catch (UnknownHostException e) {
			System.err.println("# Cannot create Agent, Unknown Host Exception");
		}
		if(agent != null) {
			writeText("#\n#Checking agent status: ");
			writeText(agent.isReachable() ? "REACHABLE" : "UNREACHABLE");
			writeText("#\n#SNMP Service is active: ");
			//TODO
			
		}
	
		
	}
}
