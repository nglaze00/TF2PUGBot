package DiscordBots.TF2PugBot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;

/**
 * Hello world!
 *
 */
public class Player {
	
	
	
	
	private String steamID64;
	private float elo;
	private int wins;
	private int losses;
	private boolean[] ultiClassPrefs = new boolean[2];
	private boolean[] foursClassPrefs = new boolean[4];
	private boolean[] sixesClassPrefs = new boolean[5];
	
	public Player(String steamID64, float elo, int wins, int losses) {
		this.steamID64 = steamID64;
		this.elo = elo;
		this.wins = wins;
		this.losses = losses;
	}
	
	public Player(String steamID64, float elo, int wins, int losses, String prefs_ulti, String prefs_fours, String prefs_sizes) {
		this(steamID64, elo, wins, losses);
		
		this.ultiClassPrefs = stringToBooleanArray(prefs_ulti);
		this.foursClassPrefs = stringToBooleanArray(prefs_fours);
		this.sixesClassPrefs = stringToBooleanArray(prefs_sizes);
	}
	
	
	public String getID() {return steamID64;}
	public float getElo() {return elo;} 
	public boolean[] getUltiduoClasses() {return ultiClassPrefs;}
	public boolean[] getFoursClasses() {return foursClassPrefs;}
	public boolean[] getSixesClasses() {return sixesClassPrefs;}
	
	public void setElo(float newElo) {elo=newElo;}
	public void setUltiduoClassPrefs(boolean[] ultiduo) {ultiClassPrefs=ultiduo;}
	public void setFoursClassPrefs(boolean[] fours) {foursClassPrefs=fours;}
	public void setSixesClassPrefs(boolean[] sixes) {sixesClassPrefs=sixes;}
	
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
}
