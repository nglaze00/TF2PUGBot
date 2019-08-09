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
