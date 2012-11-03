package technion.prime.postprocessing.uploading;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import technion.prime.utils.DocNode;


/**
 * Uploads XML files generated from a HistoryCollection into a database.
 */
public class Uploader {
	// Connection options
	private static final String DBMS = "mysql";
	private static final String SERVER_NAME = "localhost";
	private static final String PORT = "3306";
	private static final String DB_NAME = "primedb";
	private static final String USERNAME = "root";
	private static final String PASSWORD = "";
	
	// Table names
	private static final String TABLE_AUTOMATA = "Automata";
	private static final String TABLE_EDGES = "Edges";
	private static final String TABLE_LABELS = "Labels";
	
	// Active connection
	private Connection conn;
	private PreparedStatement addAutomaton;
	private PreparedStatement addEdge;
	private PreparedStatement addLabel;
	
	private final static boolean debug = true;
	
	public static void main(String args[]) {
		if (args.length == 0) {
			System.err.println("Please provide an XML folder, or \"-init\" if you want to setup the database.");
			return;
		}
		Uploader up = new Uploader();
		if (args[0].equals("-init")) {
			up.setupTables();
		} else {
			File[] files = getFiles(args[0]);
			if (files != null) up.uploadFiles(files);
		}
	}
	
	private static File[] getFiles(String folder) {
		File dir = new File(folder);
	    if (!dir.isDirectory()) {
	    	System.err.println(folder + " is not a valid directory.");
	    	return null;
	    }
	    
	    File[] files = dir.listFiles(new FilenameFilter() {
			@Override public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".xml");
			}
		});
	    
	    return files;
	}
	
	public Uploader() {
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		} catch (Exception e) {
			System.err.println("Unable to find and load driver.");
			e.printStackTrace();
		}
	}

	public void setupTables() {
		try {
			connect();
			createAutomataTable();
			createEdgesTable();
			createLabelsTable();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			disconnect();
		}
	}
	
	public void uploadFiles(File[] files) {
		try {
			connect();
			prepareStatements();
			int i = 1;
			for (File f : files) {
				try {
					if (debug) {
						System.out.println(
								String.format("Uploading file %d/%d: %s...", i++, files.length, f.getName()));
					}
					DocNode doc = DocNode.load(f.getAbsolutePath());
					uploadDoc(doc);
				} catch (SQLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (debug) System.out.println("Done.");
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			disconnect();
		}
	}
	
	private void prepareStatements() throws SQLException {
		addAutomaton = conn.prepareStatement("INSERT INTO " + TABLE_AUTOMATA + " VALUES(?, ?)");
		addEdge = conn.prepareStatement("INSERT INTO " + TABLE_EDGES + " VALUES(?, ?, ?, ?, ?)");
		addLabel = conn.prepareStatement("INSERT INTO " + TABLE_LABELS + " VALUES(?, ?, ?, ?)");
	}

	private void uploadDoc(DocNode doc) throws SQLException {
		String autId = doc.getAttribute("id");
		String type = doc.getChildNamed("type").getValue();
		addToTypeTable(autId, type);
		int edgeId = 0;
		for (DocNode edgeNode : doc.getAllChildrenNamed("edge")) {
			String src = edgeNode.getAttribute("src");
			String dst = edgeNode.getAttribute("dst");
			double weight = Double.valueOf(edgeNode.getAttribute("weight"));
			addToEdgeTable(autId, edgeId, src, dst, weight);
			int labelId = 0;
			for (DocNode labelNode : edgeNode.getAllChildrenNamed("label")) {
				String signature = labelNode.getValue();
				addToLabelTable(autId, edgeId, labelId, signature);
				labelId++;
			}
			edgeId++;
		}
	}

	private void addToTypeTable(String autId, String type) throws SQLException {
		addAutomaton.setString(1, autId);
		addAutomaton.setString(2, type);
		addAutomaton.executeUpdate();
	}

	private void addToEdgeTable(String autId, int edgeId, String src, String dst, double weight) throws SQLException {
		addEdge.setString(1, autId);
		addEdge.setInt(2, edgeId);
		addEdge.setString(3, src);
		addEdge.setString(4, dst);
		addEdge.setDouble(5, weight);
		addEdge.executeUpdate();
	}

	private void addToLabelTable(String autId, int edgeId, int labelId, String signature) throws SQLException {
		addLabel.setString(1, autId);
		addLabel.setInt(2, edgeId);
		addLabel.setInt(3, labelId);
		addLabel.setString(4, signature);
		addLabel.executeUpdate();
	}

	public void connect() throws SQLException {
		if (conn != null) return;
		
	    Properties connectionProps = new Properties();
	    connectionProps.put("user", USERNAME);
	    connectionProps.put("password", PASSWORD);
	    
    	conn = DriverManager.getConnection(
    			String.format("jdbc:%s://%s:%s/%s", DBMS, SERVER_NAME, PORT, DB_NAME),
    			connectionProps);
	}
	
	public void disconnect() {
		if (conn == null) return;
		try {
			conn.close();
		} catch (SQLException e) {
			// Swallow
		}
		conn = null;
	}
	
	private void createAutomataTable() throws SQLException {
		conn.createStatement().execute(String.format(
			"CREATE TABLE `%s`.`%s`(" +
			"	AutID VARCHAR(50) NOT NULL," +
			"	Type TEXT NOT NULL," +
			"	PRIMARY KEY(AutID)" +
			");", DB_NAME, TABLE_AUTOMATA));
	}
	
	private void createEdgesTable() throws SQLException {
		conn.createStatement().execute(String.format(
			"CREATE TABLE `%s`.`%s`(" +
			"	AutID VARCHAR(50) NOT NULL," +
			"	EdgeID INT NOT NULL," +
			"	Src VARCHAR(15) NOT NULL," +
			"	Dst VARCHAR(15) NOT NULL," +
			"	Weight DECIMAL(16,8)," +
			"	PRIMARY KEY(AutID, EdgeID)" +
			");", DB_NAME, TABLE_EDGES));
	}
	
	private void createLabelsTable() throws SQLException {
		conn.createStatement().execute(String.format(
			"CREATE TABLE `%s`.`%s`(" +
			"	AutID VARCHAR(50) NOT NULL," +
			"	EdgeID INT NOT NULL," +
			"	LabelID INT NOT NULL," +
			"	Signature TEXT NOT NULL," +
			"	PRIMARY KEY(AutID, EdgeID, LabelID)" +
			");", DB_NAME, TABLE_LABELS));
	}
	
}
