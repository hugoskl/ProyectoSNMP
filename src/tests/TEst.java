package tests;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.snmp4j.smi.OID;

import jsnmpm.control.utilities.JSNMPUtil;

public class TEst {

	
	public static void main(String...args) {
		String sOID = "1,2";
		//String[] oids = Arrays.asList(sOID.split(",")).stream().map(String::trim).map(Integer::parseInt).map((Integer index) -> {return JSNMPUtil.TEST_OIDS.get(index)[1];}).collect(Collectors.toList()).toArray(new String[0]);
		try {
		String [] oids = Arrays.asList(sOID.split(",")).stream().map(String::trim).map(Integer::parseInt)
				.map((Integer index) -> {return JSNMPUtil.TEST_OIDS.get(index)[1];})
				.collect(Collectors.toList()).toArray(new String[0]);
		System.out.println(Arrays.toString(oids));
		}catch(NullPointerException npe) {
			System.out.println("Problem");
		}

		
		System.out.println("\007");
		long s, e;
		
		System.out.println("1:");
		s = System.nanoTime();
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");  
		LocalDateTime now = LocalDateTime.now();  
		System.out.println(dtf.format(now));  
		e = System.nanoTime();
		System.out.println("Time: "+(e-s));
		 
		System.out.println("1.1:");
		s = System.nanoTime();
		System.out.println(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").format(LocalDateTime.now()));  
		e = System.nanoTime();
		System.out.println("Time: "+(e-s));
		
		System.out.println("1.1:");
		s = System.nanoTime();
		System.out.println(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").format(LocalDateTime.now()));  
		e = System.nanoTime();
		System.out.println("Time: "+(e-s));
		
		System.out.println("1.1:");
		s = System.nanoTime();
		System.out.println(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").format(LocalDateTime.now()));  
		e = System.nanoTime();
		System.out.println("Time: "+(e-s));
		
		System.out.println("2:");
		s = System.nanoTime();
		SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS");  
		Date date = new Date();  
		System.out.println(formatter.format(date));  
		e = System.nanoTime();
		System.out.println("Time: "+(e-s));
		 
		System.out.println("2.1:");
		s = System.nanoTime();
		System.out.println( new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS").format(new Date())); 
		e = System.nanoTime();
		System.out.println("Time: "+(e-s));

		System.out.println("3:");
		s = System.nanoTime();
		System.out.println(java.time.LocalDate.now().toString()+" "+java.time.LocalTime.now().toString());  
		e = System.nanoTime();
		System.out.println("Time: "+(e-s));
		
		System.out.println("3:");
		s = System.nanoTime();
		System.out.println(java.time.LocalDate.now().toString()+" "+java.time.LocalTime.now().toString());  
		e = System.nanoTime();
		System.out.println("Time: "+(e-s));

	}

}

