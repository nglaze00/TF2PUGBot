package DiscordBots.TF2PugBot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;

import DiscordBots.TF2PugBot.PugRunner.Format;
import net.dv8tion.jda.core.entities.Member;

/**
 * Hello world!
 *
 */
public class Player {
	
	
	
	private String discordID;
	private String steamID64;
	private double elo;
	private int wins;
	private int losses;
	private boolean[] ultiClassPrefs;
	private boolean[] foursClassPrefs;
	private boolean[] sixesClassPrefs;
	private int assignedClass; // See Game.getClassName() for values
	private Member member;
	
	public Player(String steamID64, String discordID, double elo, int wins, int losses) {
		ultiClassPrefs = new boolean[] {true, true};
		foursClassPrefs = new boolean[] {true, true, true, true};
		sixesClassPrefs = new boolean[] {true, true, true, true, true};
		assignedClass = -1;
		this.steamID64 = steamID64;
		this.discordID = discordID;
		this.elo = elo;
		this.wins = wins;
		this.losses = losses;
	}
	
	public Player(String steamID64, String discordID, double elo, int wins, int losses, String prefs_ulti, String prefs_fours, String prefs_sizes) {
		this(steamID64, discordID, elo, wins, losses);
		
		this.ultiClassPrefs = stringToBooleanArray(prefs_ulti);
		this.foursClassPrefs = stringToBooleanArray(prefs_fours);
		this.sixesClassPrefs = stringToBooleanArray(prefs_sizes);
	}
	
	
	public String getSteamID() {return steamID64;}
	public String getDiscordID() {return discordID;}
	public double getElo() {return elo;} 
	public int getWins() {return wins;}
	public int getLosses() {return losses;}
	public int getAssignedClass() {return assignedClass;}
	public Member getMember() {
		return member;
	}

	public String getClassPrefsAsString(Format format) 
	{
		boolean[] classPrefs = null;
		String res = "";
		switch (format) {
			case ULTIDUO: {
				classPrefs = ultiClassPrefs;
				break;
			}
			case FOURS: {
				classPrefs = foursClassPrefs;
				break;
			}
			case SIXES: {
				classPrefs = sixesClassPrefs;
				break;
			}
		}
		for (boolean pref : classPrefs) {
			if (pref) {
				res += "1";
			}
			else {
				res += "0";
			}
		}
		return res;
	}

	public String getPreferences() 
	{
		String preferences = "";
		preferences+="Ultiduo - ";
		if(ultiClassPrefs[0]==true) {preferences+="Soldier ";}
		if(ultiClassPrefs[1]==true) {preferences+="Med";}
		preferences+="\n4s - ";
		if(foursClassPrefs[0]==true) {preferences+="Scout ";}
		if(foursClassPrefs[1]==true) {preferences+="Soldier ";}
		if(foursClassPrefs[2]==true) {preferences+="Demo ";}
		if(foursClassPrefs[3]==true) {preferences+="Med";}
		preferences+="\n6s - ";
		if(sixesClassPrefs[0]==true) {preferences+="Scout ";}
		if(sixesClassPrefs[1]==true) {preferences+="Pocket ";}
		if(sixesClassPrefs[2]==true) {preferences+="Roamer ";}
		if(sixesClassPrefs[3]==true) {preferences+="Demo ";}
		if(sixesClassPrefs[4]==true) {preferences+="Med";}
		return preferences;
	}
	public String queryValues() { 
		return "VALUES('" + getSteamID() + "', '" + getDiscordID() + "', " + getElo() + ", " + getWins() + ", " + getLosses() + ", '" + getClassPrefsAsString(Format.ULTIDUO) + "', '" 
				+ getClassPrefsAsString(Format.FOURS) + "', '" + getClassPrefsAsString(Format.SIXES) + "')";
	}
	
	public void setElo(double newElo) {elo=newElo;}
	public void setUltiduoClassPrefs(boolean[] ultiduo) {ultiClassPrefs=ultiduo;}
	public void setFoursClassPrefs(boolean[] fours) {foursClassPrefs=fours;}
	public void setSixesClassPrefs(boolean[] sixes) {sixesClassPrefs=sixes;}
	public void setAssignedClass(int assignment) {
		assignedClass = assignment;
	}
	
	private static boolean[] stringToBooleanArray(String binaryString) {
		boolean[] res = new boolean[binaryString.length()];
		for (int i=0;i<binaryString.length();i++){
			res[i] = binaryString.charAt(i) == '1';			
		}
		return res;
	}
	
	public static String extractURL(String msg) {
		/**
		 * Extracts Steam profile url from msg
		 */
		int urlStartIdx = msg.indexOf("http");
		if (urlStartIdx == -1) {
			return "no https";
		}
		String[] slashSplits = msg.substring(urlStartIdx).split("/", 6);
		return String.join("/", Arrays.copyOfRange(slashSplits, 0, 5));
	}
	public static String urlToProfileID(String url) {
		/**
		 * Accesses Steam API to convert url to profile ID, if necessary
		 */
		try {
            String vanityName = url.split("/", 5)[4];//FIXME unhardcode
            try { 
            	return Long.parseLong(vanityName) + "";
            	}
            catch (Exception e) {}
            URL logsAPI = new URL("http://api.steampowered.com/ISteamUser/ResolveVanityURL/v0001/?key=2692C3FCE161DBFC25A43F005C6236BB&vanityurl=" + vanityName);
			BufferedReader in = new BufferedReader(new InputStreamReader(logsAPI.openStream()));
			String line = in.readLine();
			String profileID = line.split("\"", 7)[5];
			return profileID;
        }
        catch (Exception e) {
        	System.out.println(e.toString() + " Invalid URL");
        	return "error";
        } 
	}

	public void setMember(Member discordMember) {
		member = discordMember;
		
	}

	

}
