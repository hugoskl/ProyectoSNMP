package jsnmpm.control;

import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.snmp4j.PDU;
import org.snmp4j.smi.VariableBinding;

import com.mysql.cj.protocol.Resultset;


/**
 * 
 * @author MrStonedDog
 *
 */
public class DBControl {

	public static enum DB_ERR{
		OK, 
		NO_CONNECTION, 
		UNABLE_TO_LOAD_DRIVER,
		SQL_EXCEPTION;

	}
	// STATIC 
	private final String PREFIX_URL = "jdbc:mysql://";

	
	// INSTANCE
	/** IP from host executing MySQL **/
	private String host = null;
	
	private int port = 3306;
	private String dbName = null;
	private String user = null;
	private String password = null;
	
	public DBControl(String dbName, String host, int port, String user, String password) throws ClassNotFoundException, SQLException {
		//Class.forName("com.mysql.cj.jdbc.Driver");
        DriverManager.registerDriver(new com.mysql.cj.jdbc.Driver());
		this.dbName = dbName;
		this.port = port;
		this.user = user;
		this.password = password;
		this.host = host;
	}
	
	
	// #########################     H E L P F U L    M E T H O D S      ##########################
	
	/**
	 * Return true if the program is able to connect to the given url, with given user and password. False otherwise.
	 * @return boolean
	 */
	public boolean hasConnection() {
		boolean hasConnection = false;
		try {
			Connection con = this.getConnection();
			hasConnection = con.isValid(0);
			con.close();
		} catch (SQLException e) {}
		
		return hasConnection;
	}
	
	private Connection getConnection() throws SQLException {
		return DriverManager.getConnection(this.PREFIX_URL+this.host+":"+this.port+"/"+this.dbName, this.user, this.password);
	}
	
	
	
	// ###########################     A G E N T S   H A N D L I N G      #############################
	//  --> GET / ADD / MODIFY / DEL
	
	/**
	 * Gets all SNMPAgents on the database.
	 * @return List<SNMPAgent>
	 * @throws SQLException 
	 */
	public List<SNMPAgent> getSNMPAgents() throws SQLException{ 
		// TODO DATABASE AGENT HAS MORE FIELDS THAN SNMPAGENT
		List<SNMPAgent> snmpAgents = new ArrayList<SNMPAgent>();
		Connection con = this.getConnection();
		Statement s = con.createStatement();
		ResultSet rs = s.executeQuery("SELECT * FROM snmpagent");
		while(rs.next()) {
			try {
				SNMPAgent agent = new SNMPAgent(rs.getString("ipv4"), rs.getInt("port"),
						rs.getString("alias"), rs.getString("read_com"));
				
				agent.setId(rs.getInt("agent_id"));
				snmpAgents.add(agent);
				
			} catch (UnknownHostException e) {
			}
		}
		con.close();
		return snmpAgents;
	}
	
	
	public void addSNMPAgent(SNMPAgent agent) throws SQLException {
		Connection con = this.getConnection();
		con.createStatement()
		.executeUpdate(String.format("INSERT INTO SNMPAGENT VALUES (%d, '%s', '%s', '%s', '%s', '%d', '%s', '%s')",
				agent.getId(), null, agent.getName(), agent.getIp(), null, agent.getPort(), agent.getReadCommunity(), null));
		
		con.close();
		
	}
	
	public void removeSNMPAgent(int agent_id) throws SQLException {
		Connection con = this.getConnection();
		con.createStatement().executeUpdate(String.format("DELETE FROM SNMPAGENT WHERE agent_id = %d", agent_id));
		con.close();
	}
	
	// #######################################   P R O C E S S  H A N D L I N G   #################################
	
	// #######################################    P D U   H A N D L I N G    #####################################
	
	// ················  PDU  ·····················
	public synchronized void addPDU(PDU pdu, int agent_id) throws SQLException {
		String date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").format(LocalDateTime.now());
		Connection con = this.getConnection();
		con.createStatement().executeUpdate(String.format("INSERT INTO PDU VALUES (%d, %d, '%s',%d)", pdu.getRequestID().toInt(), pdu.getType(), date, agent_id));
		for(VariableBinding vb : pdu.getVariableBindings())
			this.addPDUVar(pdu.getRequestID().toInt(), date, vb.getOid().toDottedString(), vb.getVariable().toString());
		
		con.close();
	}
	
	// ··············  PDU VAR  ·····················
	private void addPDUVar(int pduID, String datetime, String oid, String variable) throws SQLException {
		Connection con = this.getConnection();
		con.createStatement().executeUpdate(String.format("INSERT INTO VARBINDING VALUES ('%s', '%s', %d, '%s')", oid, variable, pduID, datetime));
		con.close();
	}
	
	
}
