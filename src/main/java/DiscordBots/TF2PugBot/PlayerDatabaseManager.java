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
	
	public PlayerDatabaseManager(String databaseName){
		try {
			players = new HashMap<String, Player>();
			
			Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
			
			this.databaseName = databaseName;
			connect = DriverManager
	                .getConnection("jdbc:mysql://localhost/" + databaseName +
	                        "?user=" + databaseUsername + "&password=" + databasePassword);
			readDatabase();
			
		} 
		catch (InstantiationException | IllegalAccessException | SQLException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
				String prefs_sizes = selection.getString("prefs_sixes");
				players.put(steamID64, new Player(steamID64, elo, wins, losses, prefs_ulti, prefs_fours, prefs_sizes));
				System.out.println("Loaded player " + steamID64 + " from database");
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
	
	public void addNewPlayer(String steamID64) throws Exception {
		Player newPlayer = new Player(steamID64, DEFAULTELO, 0, 0);
		if (players.keySet().contains(steamID64)) {
			throw new Exception("Player already in database");
		}
		players.put(steamID64, newPlayer);
		Statement stmt = null;
	    try {

	        stmt = connect.createStatement(); 
	        stmt.executeUpdate("INSERT INTO " + databaseName + ".players " + newPlayer.queryValues());
	        stmt.close();

	        System.out.println("New player " + steamID64 + " added to database");
	    	
	    } catch(SQLException e) {
	    	e.printStackTrace();
	    } 
	}
	public void updateDatabase(ArrayList<Player> players) {
		try {
			Statement stmt = connect.createStatement();
			for (Player player : players) { 
				 stmt.executeUpdate("REPLACE INTO " + databaseName + ".players " + player.queryValues());
			}
		} 
		catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
	public void updateDatabaseAll() {
		updateDatabase(new ArrayList<Player>(players.values()));
	}
	
	public HashMap<String, Player> getPlayers() {return players;}
}
