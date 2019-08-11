package DiscordBots.TF2PugBot;

import java.util.ArrayList;
import java.util.HashMap;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
/**
 * Hello world!
 *
 */
public class PlayerDatabaseManager {
	public static String databaseUsername = "discordbot";
	public static String databasePassword = "pugrunner";
	public static int DEFAULTELO = 1200;
	
	private String databaseName;
	private HashMap<String, Player> players;
	private Connection connect;
	
	public PlayerDatabaseManager(ArrayList<Player> players, String databaseName) throws SQLException {
		this.databaseName = databaseName;
		connect = DriverManager
                .getConnection("jdbc:mysql://localhost/" + databaseName +
                        "?user=" + databaseUsername + "&password=" + databasePassword);
		readDatabase();
		
	}
	
	private void readDatabase() throws SQLException {
		Statement stmt = connect.createStatement();
		try {
			ResultSet selection = stmt.executeQuery("SELECT * FROM " + databaseName + ".players");
			while(selection.next()) {
				String steamID64 = selection.getString("steamID64");
				float elo = selection.getFloat("elo");
				int wins = selection.getInt("wins");
				int losses = selection.getInt("losses");
				String prefs_ulti = selection.getString("prefs_ulti");
				String prefs_fours = selection.getString("prefs_fours");
				String prefs_sizes = selection.getString("preprefs_sizesfs_ulti");
				players.put(steamID64, new Player(steamID64, elo, wins, losses, prefs_ulti, prefs_fours, prefs_sizes));
			}
		} 
		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		finally {
	        if (stmt != null) { stmt.close(); }
	    }
	}
	
	public void addNewPlayer(String steamID64) throws SQLException {
		Player newPlayer = new Player(steamID64, DEFAULTELO, 0, 0);
		players.put(steamID64, newPlayer);
		Statement stmt = null;
	    try {
	        connect.setAutoCommit(false);
	        stmt = connect.createStatement();
	        String playerValues = newPlayer.getID() + "," + DEFAULTELO + "," + 0 + "," + 0 + "," + newPlayer.getUltiduoClasses() + "," 
	        						+ newPlayer.getFoursClasses() + "," + newPlayer.getSixesClasses();
	        
	        stmt.addBatch("INSERT INTO " + databaseName + ".players" + "VALUES(" + playerValues + ")");

	        int [] updateCounts = stmt.executeBatch();
	        connect.commit();
	    	
	    } catch(SQLException e) {
	    	e.printStackTrace();
	    } finally {
	        if (stmt != null) { stmt.close(); }
	        connect.setAutoCommit(true);
	    }
	}
		
	public HashMap<String, Player> getPlayers() {return players;}
}
