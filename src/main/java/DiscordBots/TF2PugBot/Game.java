package DiscordBots.TF2PugBot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import DiscordBots.TF2PugBot.PugRunner.Format;
import net.dv8tion.jda.core.entities.Member;
/**
 * Hello world!
 *
 */

public class Game {
	enum Winner {BLU,RED,TIE;}
	private ArrayList<Player> bluPlayers;
	private ArrayList<Player> redPlayers;
	private Winner winner = null;
	private long startTime;
	private Format format;
	private int teamSize;
	
	public Game(long sTime, Format format){
		startTime=sTime;
		this.format = format;
		switch (format) {
			case ULTIDUO: {
				teamSize = 2;
				break;
			}
			case FOURS: {
				teamSize = 4;
				break;
			}
			case SIXES: {
				teamSize = 6;
				break;
			}
		}
	}
	public void addPlayer(Player p, boolean isRed) 
	{
		if(isRed) 	{ redPlayers.add(p); }
		else 		{ bluPlayers.add(p); }
	}
	
	public float computeTeamElo(ArrayList<Player> teamPlayers) {
		float eloSum = 0;
		for (Player player : teamPlayers) {
			eloSum += player.getElo();
		}
		return eloSum / teamPlayers.size();
	}
	
	
	public void updatePlayerElos() {
		int kFactor = 50;
		
		if (winner == null) {
			System.out.println("Match isn't over yet. updateElos() was called in error.");
			return;
		}
		double redElo = computeTeamElo(this.redPlayers);
		double bluElo = computeTeamElo(this.bluPlayers);
		
		double redTransformed = Math.pow(10, redElo / 400);
		double bluTransformed = Math.pow(10, redElo / 400);
		
		double redExpected = redTransformed / (redTransformed + bluTransformed);
		double bluExpected = bluTransformed / (redTransformed + bluTransformed);
		
		double redScore = 0;
		double bluScore = 0;
		if (this.winner == Winner.RED) {
			redScore = 1;
			bluScore = 0;
		}
		else if (this.winner == Winner.BLU) {
			redScore = 0;
			bluScore = 1;
		}
		
		else if (this.winner == Winner.TIE) {
			redScore = 0.5;
			bluScore = 0.5;
		}
		
		double redEloChange = kFactor * (redScore - redExpected);
		double bluEloChange = kFactor * (redScore - redExpected);
		
		for (Player player : redPlayers) {
			player.setElo(player.getElo() + redEloChange);
		}
		for (Player player : bluPlayers) {
			player.setElo(player.getElo() + bluEloChange);
		}
		

	}
	
	public long getStartTime() {return startTime;}
	public ArrayList<Player> getBluTeam() {return bluPlayers;}
	public ArrayList<Player> getRedTeam() {return redPlayers;}
	
	public void setWinner(String victor) 
	{
		switch (victor.toLowerCase()) 
		{
			case "red":  { winner=Winner.RED; break; }
			case "blue": { winner=Winner.BLU; break; }
			case "tie":  { winner=Winner.TIE; break; }
			default:{System.out.println("Match is fucking broken oopsie woopsie");}
		}
	}
	public Player[][] sortIntoTeams(ArrayList<Player> queuePlayers) {
		
		// Starting with class with least players preferring it, add highest Elo player to team with lower total elo. repeat until that class is filled
		// If not enough preferring a class, skip it and add remaining players at the end
		
		// Class orders:
		// Ulti: Soldier, Medic
		// Fours: Scout, Soldier, Demo, Medic
		// Sixes: Scout, Roamer, Pocket, Demo, Medic
		ArrayList<Player> unsortedPlayers = new ArrayList<>(queuePlayers);
		int[] playersPerClass = playersPerClass(format);

		// Partition players by class preference
		ArrayList<ArrayList<Player>> playersPreferringClass = new ArrayList<>();
		for(int i = 0; i < playersPerClass.length; i++) playersPreferringClass.add(new ArrayList<Player>());
		
		for (Player player : queuePlayers) {
			String playerPrefs = player.getClassPrefsAsString(format);
			for (int i = 0; i < playerPrefs.length(); i++) {
				if ( playerPrefs.charAt(i) == '1') {
					playersPreferringClass.get(i).add(player);
				}
			}
		}
		
		
		Player[][] teams = new Player[2][teamSize]; // blu, red
		double bluElo = 0;
		double redElo = 0;
		
		int bluPlayers = 0;
		int redPlayers = 0;
		
		int leastPreferredCount;
		int leastPreferredClass;
		ArrayList<Integer> classesSorted = new ArrayList<Integer>();
		int remainingClassSlots[][] = {Arrays.copyOf(playersPerClass, playersPerClass.length), 
				  				  Arrays.copyOf(playersPerClass, playersPerClass.length)}; // blu, red
		
		
		for (int classIdx = 0; classIdx < playersPreferringClass.size(); classIdx++) {
			// Find remaining class with least number of players preferring
			leastPreferredCount = 13;
			leastPreferredClass = -1;
			for (int i = 0; i < playersPreferringClass.size(); i++) {
				int numPlayersPreferringClass = playersPreferringClass.get(i).size();
				if (numPlayersPreferringClass < leastPreferredCount && !classesSorted.contains(i)) {
					leastPreferredCount = numPlayersPreferringClass;
					leastPreferredClass = i;
				}
			}
			// Sort players preferring this class onto teams
			ArrayList<Player> playersToSortToClass = playersPreferringClass.get(leastPreferredClass);
			while (playersToSortToClass.size() > 0) {
				double bluEloAvg;
				double redEloAvg;
				if (bluPlayers == 0) {
					bluEloAvg = 0;
				}
				else {
					bluEloAvg = bluElo / bluPlayers;
				}
				if (redPlayers == 0) {
					redEloAvg = 0;
				}
				else {
					redEloAvg = redElo / redPlayers;
				}
				// Find player with highest elo
				double maxElo = 0;
				int maxEloIdx = -1;
				for (int i = 0; i < playersToSortToClass.size(); i++) {
					double playerElo = playersToSortToClass.get(i).getElo(); 
					if (playerElo > maxElo) {
						maxElo = playerElo;
						maxEloIdx = i;
					}
				}
				Player playerToSort = playersToSortToClass.get(maxEloIdx);
				playerToSort.setAssignedClass(leastPreferredClass);
				// Put them on team with less elo
				if (remainingClassSlots[0][leastPreferredClass] == 0) {
					//No spots on blu --> put on red
					remainingClassSlots[1][leastPreferredClass] -= 1;
					teams[1][getIndexForClass(format, leastPreferredClass, remainingClassSlots[0][leastPreferredClass])] = playerToSort;
					redElo += playerToSort.getElo();
					redPlayers += 1;
				}
				else if (remainingClassSlots[1][leastPreferredClass] == 0) {
					// No spots on red --> put on blu
					remainingClassSlots[0][leastPreferredClass] -= 1;
					teams[0][getIndexForClass(format, leastPreferredClass, remainingClassSlots[0][leastPreferredClass])] = playerToSort;
					bluElo += playerToSort.getElo();
					bluPlayers += 1;
				}
				else {
					// Put on team with lower elo
					if (bluEloAvg < redEloAvg) {
						remainingClassSlots[0][leastPreferredClass] -= 1;
						teams[0][getIndexForClass(format, leastPreferredClass, remainingClassSlots[0][leastPreferredClass])] = playerToSort;
						bluElo += playerToSort.getElo();
						bluPlayers += 1;
					}
					else {
						remainingClassSlots[1][leastPreferredClass] -= 1;
						teams[1][getIndexForClass(format, leastPreferredClass, remainingClassSlots[0][leastPreferredClass])] = playerToSort;
						redElo += playerToSort.getElo();
						redPlayers += 1;
					}
				}
				
				playersToSortToClass.remove(playerToSort);
				unsortedPlayers.remove(playerToSort);
			}
			
			classesSorted.add(leastPreferredClass);			
		}
		
		return teams;
	}
	
	public int getTeamSize() {return teamSize;}
	
	public static int[] playersPerClass(Format format) {
		switch (format) {
			case ULTIDUO: {
				return new int[] {1, 1};
			}
			case FOURS: {
				return new int[] {1, 1, 1, 1};
			}
			case SIXES: {
				return new int[] {2, 1, 1, 1, 1};
			}
		}
		return null;
	}
	public static String getClassName(Format format, int classIdx) {
		switch (format) {
			case ULTIDUO: {
				switch (classIdx) {
					case 0: return "Soldier";
					case 1: return "Medic";
				}
			}
			case FOURS: {
				switch (classIdx) {
					case 0: return "Scout";
					case 1: return "Soldier";
					case 2: return "Demo";
					case 3: return "Medic";
				}
			}
			case SIXES: {
				switch (classIdx) {
					case 0: return "Scout";
					case 1: return "Roamer";
					case 2: return "Pocket";
					case 3: return "Demo";
					case 4: return "Medic";
				}
			}
		}
		return "";
	}
	public static int getIndexForClass(Format format, int classIdx, int remainingSlots) {
		switch (format) {
			case ULTIDUO: {
				switch (classIdx) {
					case 0: return 0;
					case 1: return 1;
				}
			}
			case FOURS: {
				switch (classIdx) {
					case 0: return 0;
					case 1: return 1;
					case 2: return 2;
					case 3: return 3;
				}
			}
			case SIXES: {
				switch (classIdx) {
					case 0: return 2 - remainingSlots;
					case 1: return 2;
					case 2: return 3;
					case 3: return 4;
					case 4: return 5;
				}
			}
		}
		return -1;
	}
	public String getFormat() {
		if(format==Format.ULTIDUO) {return "Ultiduo";}
		else if(format==Format.FOURS) {return "4s";}
		else if(format==Format.SIXES) {return "6s";}
		else {return "Unknown Format";}
	}
}
