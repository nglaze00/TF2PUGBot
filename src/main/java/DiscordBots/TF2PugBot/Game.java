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
