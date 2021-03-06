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
	private int ID;
	private int redElo;
	private int bluElo;
	
	public Game(long sTime, Format format){
		bluPlayers = new ArrayList<Player>();
		redPlayers = new ArrayList<Player>();
		startTime=sTime;
		this.format = format;
		switch (format) {
			case ULTIDUO: 	{teamSize = 2; break;}
			case FOURS: 	{teamSize = 4; break;}
			case SIXES: 	{teamSize = 6; break;}
		}
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
		
		double redTransformed = Math.pow(10, this.redElo / 400);
		double bluTransformed = Math.pow(10, this.redElo / 400);
		
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
		double bluEloChange = kFactor * (bluScore - bluExpected);
		
		for (Player player : redPlayers) {player.setElo(player.getElo() + redEloChange);}
		for (Player player : bluPlayers) {player.setElo(player.getElo() + bluEloChange);}
	}
	public void resetPlayerClassAssignments() {
		for (Player player : redPlayers) {player.setAssignedClass(-1);}
		for (Player player : bluPlayers) {player.setAssignedClass(-1);}
	}
	
	public long getStartTime() {return startTime;}
	public Player[] getBluTeam() 
	{
		return bluPlayers.toArray(new Player[bluPlayers.size()]);
	}
	public Player[] getRedTeam() 
	{
		return redPlayers.toArray(new Player[redPlayers.size()]);
	}
	public int getID() {return ID;}
	public void setID(int gn) {ID = gn;}
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
	private Player getPlayerWithHighestElo(ArrayList<Player> players) {
		double maxElo = 0;
		int maxEloIdx = -1;
		for (int i = 0; i < players.size(); i++) {
			double playerElo = players.get(i).getElo(); 
			if (playerElo > maxElo) {
				maxElo = playerElo;
				maxEloIdx = i;
			}
		}
		return players.get(maxEloIdx);
	}
	private Player getPlayerWithLowestElo(ArrayList<Player> players) {
		double minElo = 9999999;
		int minEloIdx = -1;
		for (int i = 0; i < players.size(); i++) {
			double playerElo = players.get(i).getElo(); 
			if (playerElo < minElo) {
				minElo = playerElo;
				minEloIdx = i;
			}
		}
		return players.get(minEloIdx);
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
		
		int bluPlayerCount = 0;
		int redPlayerCount = 0;
		
		
		
		ArrayList<Integer> classesSorted = new ArrayList<Integer>();
		int remainingClassSlots[][] = {Arrays.copyOf(playersPerClass, playersPerClass.length), 
				  				  Arrays.copyOf(playersPerClass, playersPerClass.length)}; // blu, red
		
		
		int leastPreferredCount;
		int leastPreferredUnsortedClass;
		for (int classIdx = 0; classIdx < playersPreferringClass.size(); classIdx++) {
			// Find remaining class with least number of players preferring
			leastPreferredCount = 13;
			leastPreferredUnsortedClass = -1;
			for (int i = 0; i < playersPreferringClass.size(); i++) {
				int numPlayersPreferringClass = playersPreferringClass.get(i).size();
				if (numPlayersPreferringClass < leastPreferredCount && !classesSorted.contains(i)) {
					leastPreferredCount = numPlayersPreferringClass;
					leastPreferredUnsortedClass = i;
				}
			}
			// Sort players preferring this class onto teams
			ArrayList<Player> playersToSortToClass = new ArrayList<Player>();
			for (Player player : playersPreferringClass.get(leastPreferredUnsortedClass)) {
				if (player.getAssignedClass() == -1) {
					playersToSortToClass.add(player);
				}
			}
			
			// FIXME
			while (playersToSortToClass.size() > 0) {
				
				double bluEloAvg;
				double redEloAvg;
				if (bluPlayerCount == 0) {
					bluEloAvg = 0;
				}
				else {
					bluEloAvg = bluElo / bluPlayerCount;
				}
				if (redPlayerCount == 0) {
					redEloAvg = 0;
				}
				else {
					redEloAvg = redElo / redPlayerCount;
				}
				
				Player playerToSort = getPlayerWithHighestElo(playersToSortToClass);
				
				playersToSortToClass.remove(playerToSort);
				unsortedPlayers.remove(playerToSort);
				
				playerToSort.setAssignedClass(leastPreferredUnsortedClass);
				// Put them on team with less elo
				if (remainingClassSlots[0][leastPreferredUnsortedClass] == 0 && remainingClassSlots[1][leastPreferredUnsortedClass] != 0) {
					//No spots of this class left on blu, but spots on red --> put on red
					remainingClassSlots[1][leastPreferredUnsortedClass] -= 1;
					teams[1][getIndexForClass(format, leastPreferredUnsortedClass, remainingClassSlots[0][leastPreferredUnsortedClass])] = playerToSort;
					redElo += playerToSort.getElo();
				}
				else if (remainingClassSlots[1][leastPreferredUnsortedClass] == 0 && remainingClassSlots[0][leastPreferredUnsortedClass] != 0) {
					// No spots on red, but spots on blu --> put on blu
					remainingClassSlots[0][leastPreferredUnsortedClass] -= 1;
					teams[0][getIndexForClass(format, leastPreferredUnsortedClass, remainingClassSlots[0][leastPreferredUnsortedClass])] = playerToSort;
					bluElo += playerToSort.getElo();
				}
				else if (remainingClassSlots[0][leastPreferredUnsortedClass] == 0 && remainingClassSlots[1][leastPreferredUnsortedClass] == 0) {
					// No spots left on this class. Remaining players preferring it don't get to play it haha sucks
					break;
				}
				else {	// Spots on both 
					// Put on team with lower elo
					if (bluEloAvg < redEloAvg) {
						remainingClassSlots[0][leastPreferredUnsortedClass] -= 1;
						teams[0][getIndexForClass(format, leastPreferredUnsortedClass, remainingClassSlots[0][leastPreferredUnsortedClass])] = playerToSort;
						bluElo += playerToSort.getElo();
					}
					else {
						remainingClassSlots[1][leastPreferredUnsortedClass] -= 1;
						teams[1][getIndexForClass(format, leastPreferredUnsortedClass, remainingClassSlots[0][leastPreferredUnsortedClass])] = playerToSort;
						redElo += playerToSort.getElo();
					}
				}
				
				
			}
			
			classesSorted.add(leastPreferredUnsortedClass);			
		}
		// If not enough players preferred a class, choose unselected players
		while (unsortedPlayers.size() > 0) {
			Player playerToSort = getPlayerWithHighestElo(unsortedPlayers);
			unsortedPlayers.remove(playerToSort);
			if (bluElo < redElo) {
				boolean done = false;
				for (int i = 0; i < teams[0].length; i++) {
					if (done) break;
					if (teams[0][i] == null) {
						teams[0][i] = playerToSort;
						bluElo += playerToSort.getElo();
						playerToSort.setAssignedClass(getClassForIndex(format, i));
						done = true;
					}
				}
				if (!done) {
					for (int i = 0; i < teams[1].length; i++) {
						if (done) break;
						if (teams[1][i] == null) {
							teams[1][i] = playerToSort;
							redElo += playerToSort.getElo();
							playerToSort.setAssignedClass(getClassForIndex(format, i));
							done = true;
						}
					}
				}
				
			}
			// bluElo > redElo
			else {
				
				boolean done = false;
				for (int i = 0; i < teams[1].length; i++) {
					if (done) break;
					if (teams[1][i] == null) {
						teams[1][i] = playerToSort;
						redElo += playerToSort.getElo();
						playerToSort.setAssignedClass(getClassForIndex(format, i));
						done = true;
					}
				}
				if (!done) {
					for (int i = 0; i < teams[0].length; i++) {
						if (done) break;
						if (teams[0][i] == null) {
							teams[0][i] = playerToSort;
							redElo += playerToSort.getElo();
							playerToSort.setAssignedClass(getClassForIndex(format, i));
							done = true;
						}
					}
				}
			}
		}
		bluElo /= this.teamSize;
		redElo /= this.teamSize;
		for (Player p : teams[0]) bluPlayers.add(p);
		for (Player p : teams[1]) redPlayers.add(p);
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
	public static int getClassForIndex(Format format, int idx) {
		if (format == Format.SIXES) {
			if (idx == 0) return 0;
			else if (idx == 1) return 0;
			else return idx - 1;
		}
		else return idx;
	}
	
	public static int getIndexForClass(Format format, int classIdx, int remainingSlots) {
		if (format == Format.SIXES) {
			if (classIdx == 0) {
				return 2 - remainingSlots;
			}
			else return classIdx + 1;
		}
		else return classIdx;
	}
	public String getFormat() {
		if(format==Format.ULTIDUO) {return "Ultiduo";}
		else if(format==Format.FOURS) {return "4s";}
		else if(format==Format.SIXES) {return "6s";}
		else {return "Unknown Format";}
	}
}
