package DiscordBots.TF2PugBot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import com.lukaspradel.steamapi.webapi.client.SteamWebApiClient;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.restaction.MessageAction;

//TODO !profile https://steamcommunity.com/id/iwyy/ doesn't work because of last slash
//TODO "!classes" should show current prefs; "!profile" should show current steam profile (in URL format)
//TODO sending someone else's profile when you(Discord) is already in the database shouldn't be allowed


public class PrivateMessageManager extends ListenerAdapter {
	
	private PlayerDatabase playerDB;
	
	public PrivateMessageManager(PlayerDatabase playerDB) {
		this.playerDB = playerDB;
	}
	public static void sendDM(User recipient, String msg) {
		/**
		 * Sends a direct message to the given user
		 */
		recipient.openPrivateChannel().queue((channel) ->
        {
            channel.sendMessage(msg).queue();
        });
		
	}
	
	@Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
        //These are provided with every event in JDA
		
        JDA jda = event.getJDA();                       //JDA, the core of the api.
        long responseNumber = event.getResponseNumber();//The amount of discord events that JDA has received since the last reconnect.

        //Event specific information
        User author = event.getAuthor();                //The user that sent the message
        Message message = event.getMessage();           //The message that was received.

        String msg = message.getContentDisplay();              //This returns a human readable version of the Message. Similar to
                                                        // what you would see in the client.
        done:
        if (event.isFromType(ChannelType.PRIVATE) && !author.isBot()) //If this message was sent to a PrivateChannel
        {
        	msg.toLowerCase();
        	if(msg.contains("!profile"))
        	{
        		String profileID = "";
    			try
    			{
    				 profileID = Player.urlToProfileID(Player.extractURL(msg.substring(msg.indexOf("http"))));
    			}
    			catch(Exception e) 
    			{
    				sendDM(author,"Command usage: !profile [profile-link]");
        			sendDM(author,"Example: !profile https://steamcommunity.com/id/exeloar/");
        			break done;
    			}
    			String discordID = author.getId();
                if (profileID == "error") {
                	String errorMsg = "No valid profile ID found. Make sure your URL is in one of the formats: https://steamcommunity.com/id/(vanity ID) \n"
                 			+ "https://steamcommunity.com/profiles/(ID number)";
                 	System.out.println(errorMsg);
                 	sendDM(author, errorMsg);
                }
                else {

                 	try {
                 		playerDB.addNewPlayer(profileID, discordID);
                 		sendDM(author, "You have been successfully added to the player database under this Steam profile! Use !classes to view / update your class preferences.");
                 	}
                	catch (Exception e) {
                		sendDM(author, "This SteamID is already in the database! Use !classes to view or update your current class preferences.");
                	}
                }
        	}
        	else if(msg.contains("!classes"))
        	{
        		String classSelect="";
        		
        		if(msg.length()==8) 
        		{
        			sendDM(author,"Command usage: !classes [format](class1,class2,class3)"
        					+ "\n !classes current to see your current preferences");
        			sendDM(author,"Example: !classes Ultiduo(Soldier,Med) 4s(ALL) 6s(Med,Scout,Roamer,Pocket,Demo)");
        			break done;
        		}
        		Player player = playerDB.getPlayersByDiscordID().get(author.getId());
        		if(player == null) {sendDM(author,"We do not have you as a player in our database yet. Please send us your profile (!profile)");break done;}
    			if(msg.contains("current")) {sendDM(author, player.getPreferences());break done;}
    			else 
    			{
    				String format="";
        			try{format = msg.substring(0,msg.indexOf("("));}
        			catch(Exception e) {sendDM(author,"Incorrect syntax for !classes. Run !classes to see accepted usage");break done;}
        			if(format.equals("ultiduo")) 
        			{
        				classSelect+="Ultiduo - ";
        				boolean[] ultiduo = {false,false};
        				if(commands[i].contains("all")||commands[i].contains("any")) {ultiduo[0]=true;classSelect+="Soldier ";ultiduo[1]=true;classSelect+="Medic ";}
        				else
        				{
        					boolean any = false;
        					if(commands[i].contains("soldier")||commands[i].contains("pocket")) {any= true;ultiduo[0]=true;classSelect+="Soldier ";}
        					if(commands[i].contains("med")||commands[i].contains("medic")) {any= true;ultiduo[1]=true;classSelect+="Medic ";}
        					if(!any) {break done;}
        				}
        				player.setUltiduoClassPrefs(ultiduo);
        				classSelect+="\n";
        			}
        			else if(format.equals("4s")) 
        			{
        				classSelect+="4s - ";
        				boolean[] fours = {false,false,false,false};
        				if(commands[i].contains("all")||commands[i].contains("any")) 
        				{
        					fours[0]=true;classSelect+="Scout ";
        					fours[1]=true;classSelect+="Soldier ";
        					fours[2]=true;classSelect+="Demoman ";
        					fours[3]=true;classSelect+="Medic ";
        				}
        				else
        				{
        					boolean any = false;
        					if(commands[i].contains("scout")) {any= true;fours[0]=true;classSelect+="Scout ";}
	        				if(commands[i].contains("soldier")||commands[i].contains("pocket")) {any= true;fours[1]=true;classSelect+="Soldier ";}
	        				if(commands[i].contains("demo")||commands[i].contains("demoman")) {any= true;fours[2]=true;classSelect+="Demoman ";}
	        				if(commands[i].contains("med")||commands[i].contains("medic")) {any= true;fours[3]=true;classSelect+="Medic ";}
	        				if(!any) {break done;}
        				}
        				player.setFoursClassPrefs(fours);
        				classSelect+="\n";
        			}
        			else if(format.equals("6s"))
        			{
        				classSelect+="6s - ";
        				boolean[] sixes = {false,false,false,false,false};
        				if(commands[i].contains("all")||commands[i].contains("any")) 
        				{
        					sixes[0]=true;classSelect+="Scout ";
        					sixes[1]=true;classSelect+="Pocket ";
        					sixes[2]=true;classSelect+="Roamer ";
        					sixes[2]=true;classSelect+="Demoman ";
        					sixes[3]=true;classSelect+="Medic ";
        				}
        				else
        				{
        					boolean any= false;
        					if(commands[i].contains("scout")) {any= true;sixes[0]=true;classSelect+="Scout ";}
	        				if(commands[i].contains("pocket")) {any= true;sixes[1]=true;classSelect+="Pocket ";}
	        				if(commands[i].contains("roamer")) {any= true;sixes[2]=true;classSelect+="Roamer ";}
	        				if(commands[i].contains("demo")||commands[i].contains("demoman")) {any= true;sixes[3]=true;classSelect+="Demoman ";}
	        				if(commands[i].contains("med")||commands[i].contains("medic")) {any= true;sixes[4]=true;classSelect+="Medic ";}
	        				if(!any) {break done;}
        				}
        				player.setSixesClassPrefs(sixes);
        				
        				classSelect+="\n";
        			}
        			else {sendDM(author,"Game format misspelling or currently unsupported format detected");break done;}
        			this.playerDB.updateDatabase(player);
    			}
    			sendDM(author,"Current Classes Selected: \n"+classSelect);
        	}
        }
    }
}
