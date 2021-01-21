package jsnmpm.terminal;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Erase;
import org.fusesource.jansi.AnsiConsole;

/**
 * 
 * @author MrStonedDog
 *
 */
public class Terminal {
	
	public static enum Prompt{
		NORMAL, SHELL
	}
	// ----------  PARAMETERS / VARIABLES  -----------
	// ·· SYSTEM INFO
	private String os = null;
	private int terminalWidth = 120;
	public Prompt currentStatus = Prompt.NORMAL;
	
	// ### I/O ###
	private BufferedReader br;
	
	// ### MOUSE POSITIONING ####
	// ·· PROMPTS
	// SHELL
	String shellPrompt = "jsnmp> ";
	public final int startShellPromptRow = 30;
	public final int shellpromptCol = 0;
	public int currentShellPromptRow = startShellPromptRow;
	public int currentShellPromptCol = shellpromptCol;
	// OPTION
	String optionPrompt = "Option: ";
	public final int startOptionPromptRow = 25;
	public final int startOptionPromptCol = getBannerStartCol();
	public int currentOptionPrompRow = startOptionPromptRow;
	public int currentOptionPrompCol = startOptionPromptCol;
	
	
	// ·· INFO
	public final int infoRow = 12;
	// ·· MENU
	public final int menuRow = 14;
	
	
	// DATA
	private final int BANNER_WIDTH = 76;
	public Ansi ansiUserinput = Ansi.ansi().fgDefault();
	public Ansi ansiPrompt = Ansi.ansi().fgBrightRed().bgDefault();
	public Ansi ansiMenuOption = Ansi.ansi().bgRed().fgBlack();
	public Ansi ansiMenuHeader = Ansi.ansi().bgRed().fgDefault();
	public Ansi ansiBanner = Ansi.ansi().fgBrightRed();
	
	public Ansi ansiInfo = Ansi.ansi().fgBrightRed();
	public Ansi ansiInfo2 = Ansi.ansi().fgBrightBlue();
	public Ansi ansiDefault = Ansi.ansi().bgDefault().fgDefault();
	public Ansi ansiError = Ansi.ansi().fgBrightMagenta();
	public Ansi ansiWarning = Ansi.ansi().fgYellow();

	
	// ######### 	CONSTRUCTOR 	###########
	public Terminal() throws IOException {
		AnsiConsole.systemInstall();
		this.br = new BufferedReader(new InputStreamReader(System.in));
		this.os = this.getOS();
		//Ansi.ansi().cursor(0, 0);
	}
	
	// ##################### 		TERMINAL I/O 		#########################
	
	public synchronized String readInput() {
		
		try {
			cout((this.currentStatus == Prompt.NORMAL) 
					? Ansi.ansi().a(this.ansiPrompt).a(this.optionPrompt).a(this.ansiUserinput)
					: Ansi.ansi().a(this.ansiPrompt).a(this.shellPrompt).a(this.ansiUserinput));
			String input = br.readLine();
			return input;
		} catch (IOException e) {
			return null;
		}
	}
	public String readInput(Ansi text) {
		try {
			cout((this.currentStatus == Prompt.NORMAL) 
					? Ansi.ansi().a(text).a(this.ansiUserinput)
					: Ansi.ansi().a(text).a(this.ansiUserinput));
			String input = br.readLine();
			return input;
		} catch (IOException e) {
			return null;
		}
	}
	
	
	public void write(Ansi ansi, String text) {
		for(int i = 0; i<text.length(); i++) {
			System.out.print(Ansi.ansi().a(ansi).a(text.charAt(i)));
			try {
				Thread.sleep(3);
			} catch (InterruptedException e) {
			}
		}
	}
	
	/**
	 * Used only for printing text with cursor positioning already specified.
	 * @param text
	 */
	private void print(Ansi text) {
		System.out.print(text);
	}
	
	public synchronized void cout(Ansi text) {
		
		System.out.print((this.currentStatus == Prompt.NORMAL) 
				? Ansi.ansi().cursor(this.currentOptionPrompRow++,this.currentOptionPrompCol).a(text).a(this.ansiUserinput)
				: Ansi.ansi().cursor(this.currentShellPromptRow++,this.currentShellPromptCol).a(text).a(this.ansiUserinput));
	}
	
	
	public void cout(Ansi text, int row, int col) {
		System.out.print(Ansi.ansi().a(text).cursor(row, col));
	}
	
	public void coutError(String err) {
		cout(Ansi.ansi().a(this.ansiError).a(err+"\n"));
	}
	
	public void coutWarning(String warning) {
		cout(Ansi.ansi().a(this.ansiWarning).a(warning+"\n"));
	}
	
	// ##################### 		SYSTEM DATA 		#################
	
	private String getOS() {
		return System.getProperty("os.name");
	}
	
	// ##################### 		TERMINAL CONFIGURATION 		#####################
	
	public void clearTerminal() {
		if(this.os.split(" ")[0].equals("Windows")) {
			try {
				new ProcessBuilder("cmd","/c","cls").inheritIO().start().waitFor();
			} catch (InterruptedException | IOException e) {
				coutError("Error: Cannot reset Window...");
			}
		}
	}
	
	public void reset(String[] info) {
		this.clearTerminal();
		this.setUpStartHeader(info);
	}
	
	
	public void changePrompt() {
		this.currentStatus = ((this.currentStatus == Prompt.NORMAL) ? Prompt.SHELL : Prompt.NORMAL);
	}
	
	public void deleteLine() {
		System.out.println(Ansi.ansi().cursor(--this.currentOptionPrompRow, --this.currentOptionPrompCol).eraseLine());
	}
	
	/** BROKEN, DONT USE */
	private void findNewTerminalWidth() {
		
		if(this.os.split(" ")[0].equals("Windows")) {
			try {
				ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "mode|find","\"Col\"");
				Process pr = pb.inheritIO().start();
				BufferedReader br2 = new BufferedReader(new InputStreamReader(pr.getInputStream()));
				String result = br2.readLine();
				result = Arrays.asList(result.split(" ")).stream().collect(Collectors.filtering(str -> !str.isEmpty(),
					Collectors.toList())).get(1);
				this.br = br2;
				pr.waitFor();
				this.terminalWidth = Integer.parseInt(result);
				
			} catch (IOException e) {
				this.coutError("Error: I/O for obtaining terminal with is broken");
			} catch (InterruptedException e) {
				this.coutError("Error: Proccess interrupted while getting terminal width");
			}
		}
	}
	
	/** Resize all graphics to given width. Does not resize terminal window.*/
	public void resize(int columns, String[] status) throws IOException {
		
		this.terminalWidth = columns;
		this.clearTerminal();
		this.setUpStartHeader(status);
	}
	
	// #######################      TERMINAL INTERFACE     #############################
	
	// ···· POSITIONING
	private int getBannerStartCol() {
		//coutError(""+Math.round(this.terminalWidth-this.BANNER_WIDTH)/2);
		return Math.round(this.terminalWidth-this.BANNER_WIDTH)/2;
	}
	
	private int getMenuStartCol() {
		return getBannerStartCol();
	}
	
	// ···· PRINTING
	public  void setUpStartHeader(String[] info) {
		printBanner();
		printMainInfo(info);
		printMainMenu();
	
		
	}
	public void printBanner() {
		print(Ansi.ansi().a(this.ansiBanner).a(TUtil.createCustomBanner(TUtil.getRandomBanner(), getBannerStartCol(), TUtil.getRandomEncode(), TUtil.getRandomDispay())).a("\n\n"));
	}
	
	public void writeBanner() {
		write(this.ansiBanner,TUtil.createCustomBanner(TUtil.getRandomBanner(), getBannerStartCol(), TUtil.getRandomEncode(), TUtil.getRandomDispay()));
	}

	public void printMainMenu() {
		int col = getMenuStartCol()-5;
		int row = this.menuRow;
		for(String header : TUtil.MENU_OP_HEADERS) {
			print(Ansi.ansi().cursor(row++,col).a(this.ansiMenuHeader).bold().a(header).boldOff());
			for(String option : TUtil.MENU_OPTIONS.get(header)) {
				print(this.ansiMenuOption);
				print(Ansi.ansi().cursor(row++, col).append(String.format("%-"+header.length()+"s", option)).a(this.ansiDefault));
			}
			row = this.menuRow;
			col +=24;
			
		}
	}
	
	public void printMainInfo(String[] info) {
		int i = 0;
		print(Ansi.ansi().cursor(this.infoRow, getMenuStartCol()+10));
		for(String option : TUtil.MANAGER_INFO) {
			print(this.ansiInfo);
			print(Ansi.ansi().a(option).a(this.ansiDefault).a(info[i]).a("   "));
			i++;
		}
	}
	
}
