package DiscordBots.TF2PugBot;

import java.util.ArrayList;
import java.util.HashMap; 

/**
 * Hello world!
 *
 */
public class EloDatabaseManager {
	
	private HashMap<Player, Double> eloMap;
	
	public EloDatabaseManager(ArrayList<Player> players, String databaseURL) {
		eloMap = readElos(databaseURL);
	}
	
	
	
	private HashMap<Player, Double> readElos(String databaseURL) {
		//TODO read all elos from file
	}
	
	public Double getElo(Player player) {
		return eloMap.get(player);
	}
	
	public void updateElo(Player player, Double newElo) {
		eloMap.put(player, newElo);
	}
	
	public boolean saveEloMap() {
		/**
		 * Saves current eloMap to file
		 */ // TODO
		return false;
	}
}
