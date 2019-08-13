package DiscordBots.TF2PugBot;

import java.util.ArrayList;
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
	
	public Game(long sTime){startTime=sTime;}
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

}
