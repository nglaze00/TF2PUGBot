package DiscordBots.TF2PugBot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;


public class LogParser {
	public static final String[] uploaders = {"U:1:351789566"}; //The uploader IDs of all servers used by the pugbot
	public static int matchEndID(Game g)
	{
		long matchStartTime = 500; //g.getStartTime(); FIXME: HARDCODED EPOCH FOR TEST
		long matchEndTime = -1;
		String player = "76561198312055294"; //g.getPlayers()[0].getID(); //FIXME: HARDCODED PLAYER FOR TEST
		for(String upload : uploaders)
		{
			try 
			{
				URL logsAPI = new URL("http://logs.tf/api/v1/log?uploader="+upload+"&player="+player+"&limit=1");
				BufferedReader in = new BufferedReader(new InputStreamReader(logsAPI.openStream()));
				String line;
				int ID = -1;
				while((line = in.readLine())!=null) 
				{
					System.out.println(line);
					if(line.contains("\"id\": ")) {
						String temp = line.split("\"id\": ")[1];
						ID = Integer.parseInt(temp.substring(0,temp.indexOf(",")));						
					}
					else if(line.contains("\"date\": ")) 
					{
						String temp = line.split("\"date\": ")[1];
						matchEndTime = Long.parseLong(temp.substring(0,temp.indexOf(",")));
						if(matchEndTime < matchStartTime) {return -1;}
					}
					if(ID!=-1 && matchEndTime!=-1) {return ID;} 
				}
				in.close();//NOHACK
			} 
			catch (MalformedURLException e) {System.out.println("LOGS.TF API/Match Link is Incorrect");} 
			catch (IOException e) {System.out.println("Cannot connect to logs.tf");}
		}
		return -1;
	}
	public static String getResult(int ID) 
	{
		try
		{
			URL logsAPI = new URL("http://logs.tf/api/v1/log/"+ID);//ID
			BufferedReader in = new BufferedReader(new InputStreamReader(logsAPI.openStream()));
			String line = in.readLine();
			int redScore = Integer.parseInt(line.split("score\": ")[1].substring(0,line.split("score\": ")[1].indexOf(",")));
			int blueScore = Integer.parseInt(line.split("score\": ")[2].substring(0,line.split("score\": ")[2].indexOf(",")));
			if(redScore == blueScore) {return "tie";}
			else if(redScore>blueScore) {return "red";}
			else {return "blue";}
		}
		catch(Exception e) {System.out.println("Cannot recieve match stats");}
		return null;
	}
}
