package tests;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import jsnmpm.control.SNMPManager;

public class CommandLine {
	
	//static BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	static SNMPManager manager = new SNMPManager();
	static boolean running = true;
	
	// ·································· TERMINAL COMMANDS ····································
	// READ LINE 
	/*static Function<Void, Instruction> readLine = (x) ->{
		String instr = "";
		try {
			System.out.print("JSNMP-MANAGER>");
			//instr = br.readLine();
			String cmd = instr.split(" ")[0];
			try {
				return new Instruction(cmd, instr.substring(cmd.length()+1).split(" "));
			}catch(IndexOutOfBoundsException iob) {
				return new Instruction(cmd, new String[] {});
			}
		} catch (IOException e) {
			return null;
			
		}
	};*/
	
	// CREATE
	static Function<List<String>, ?> CMDCreate = (args) -> {
		if(args.size() == 0) {
			System.err.println("create: Missing parameter [Object].");
			return null;
		}
		switch(args.get(0).toUpperCase()) {
		case "HELP":
		case "?":
			System.out.println("················  COMMAND: CREATE  ················");
			System.out.println("Creates a Object of the given type and given configuration.");
			System.out.println("\n·Usage: create [Object] [options]\n");
			System.out.println("·Objects: AGENT / THREAD");
			System.out.println("\nAGENT OPTIONS: \n -ip -> Agent IP\n -port -> Agent Port\n -rcom -> Readcommunity String");
			System.out.println("\nTHREAD OPTIONS:");
			break;
			
		case "AGENT":
			boolean expectOption = true;
			String arg; String ip = ""; String rcom = "";
			int port = 0;
			
			if(args.size()%2 == 0) {
				System.err.printf("create: Wrong input number, got %d expected %d\n", args.size(), args.size()+1);
				return null;
			}
			if(args.size() == 1){
				System.err.printf("create: Requiered option [-ip] is missing\n", args.size(), args.size()+1);
				return null;
			}
			Iterator<String> itr = args.listIterator(1);
			while(itr.hasNext()) {
				arg = itr.next();
				switch(arg) {
					case "-ip":
						ip = itr.next();
						break;
					case "-port":
						try {
						port = Integer.parseInt(itr.next());
						}catch(Exception e) {
							System.err.println("create: Port must be a numeric!");
							return null;
						}
						break;
						
					case "-rcom":
						rcom = itr.next();
						break;
					
					default:
						if(expectOption) {
							System.err.println("create: Unknown option" + arg);
							return null;
						}
				}
			}
			System.out.printf("\nCreating Agent:\nIP: %s\nPORT: %s\nREADCOM: %s\n\n", ip, port, rcom);

			break;
		case "THREAD":
			break;
		default:
			System.out.printf("create: Unknown parameter [Object] \'%s\'\n", args.get(0));
			break;
		}
		
		return null;
		
	};
	
	// BIN HASHMAP --> MAPS A STRING KEYWORD TO A FUNCTION
	static HashMap<String, Function<List<String>,?>> bin = new HashMap<String, Function<List<String>,?>>(){{
			put("CREATE", CMDCreate);
			
	}};
	
	
	public static void main(String...args) {
		
		System.out.println("########################  JSNMP TERMINAL  #############################");
		while(running) {
			Instruction ins = null;
		try {
			//ins = readLine.apply(null);
			bin.get(ins.getCommand()).apply(ins.getArgs());
		}catch(Exception e) {
			if(bin.get(ins.getCommand()) == null && !ins.getCommand().isEmpty()){
				
				System.err.printf("jsmp: Unknown command \'%s\'\n", ins.getCommand());
			}
		//	System.out.println("WARNING: An unknown error has occurred!");
		}
		}
	}
}

class Instruction{
	private String command = null;
	private List<String> args = null;
	public Instruction(String command, String[] args) {
		this.command = command.toUpperCase();
		this.args = Arrays.asList(args);
	}
	
	public String getCommand() {
		return this.command;
	}
	
	public List<String> getArgs() {
		return this.args;
	}
}
