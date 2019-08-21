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
		System.out.println("Sending DM to " + recipient.getId().toString() + ": " + msg);
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
        User discordUser = event.getAuthor();                //The user that sent the message
        Message message = event.getMessage();           //The message that was received.

        String msg = message.getContentDisplay();              //This returns a human readable version of the Message. Similar to
                                                        // what you would see in the client.
        done:
        if (event.isFromType(ChannelType.PRIVATE) && !discordUser.isBot()) //If this message was sent to a PrivateChannel
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
    				sendDM(discordUser,"Command usage: !profile [profile-link]");
        			sendDM(discordUser,"Example: !profile https://steamcommunity.com/id/exeloar/");
        			break done;
    			}
    			String discordID = discordUser.getId();
                if (profileID == "error") {
                	String errorMsg = "No valid profile ID found. Make sure your URL is in one of the formats: https://steamcommunity.com/id/(vanity ID) \n"
                 			+ "https://steamcommunity.com/profiles/(ID number)";
                 	System.out.println(errorMsg);
                 	sendDM(discordUser, errorMsg);
                }
                else {

                 	try {
                 		playerDB.addNewPlayer(profileID, discordID);
                 		sendDM(discordUser, "You have been successfully added to the player database under this Steam profile! Use !classes to view / update your class preferences.");
                 	}
                	catch (Exception e) {
                		sendDM(discordUser, "This SteamID is already in the database! Use !classes to view or update your current class preferences.");
                	}
                }
        	}
        	else if(msg.contains("!classes"))
        	{        		
        		if(msg.length()==8) 
        		{
        			sendDM(discordUser,"Command usage: !classes [format](class1,class2,class3)"
        					+ "\n !classes current to see your current preferences");
        			sendDM(discordUser,"Example: !classes Ultiduo(Soldier,Med) 4s(ALL) 6s(Med,Scout,Roamer,Pocket,Demo)");
        			break done;
        		}
        		Player player = playerDB.getPlayersByDiscordID().get(discordUser.getId());
        		if(player == null) {sendDM(discordUser,"We do not have you as a player in our database yet. Please send us your profile (!profile)");break done;}
    			if(msg.contains("current")) {sendDM(discordUser, player.getPreferences());break done;}
    			else 
    			{
    				try
    				{
    					String[] temp = msg.split("\\(");
	        			for(int x=0;x<temp.length;x+=2) 
	        			{
	        				try {setPreferences(discordUser,temp[x],temp[x+1],player);}
	        				catch(Exception e){sendDM(discordUser,"Incorrect format. Check !classes for usage");break done;}
	        			}
	        			this.playerDB.updateDatabase(player);
    				}
    				catch(Exception e) {sendDM(discordUser,"Incorrect format. Check !classes for usage");break done;}
    			}
    			sendDM(discordUser,"Current Classes Selected: \n"+player.getPreferences());
        	}
        }
    }
    private void setPreferences(User discordUser, String format,String classes,Player p) 
    {
    	format = format.toLowerCase();
    	classes = classes.toLowerCase();
    	done:
    	if(format.contains("ultiduo"))
		{
			boolean[] ultiduo = {false,false};
			if(classes.contains("all")||classes.contains("any")) {ultiduo[0]=true;ultiduo[1]=true;}
			else 
			{
				boolean any = false;
				if(classes.contains("soldier")||classes.contains("pocket")) {any= true;ultiduo[0]=true;}
				if(classes.contains("med")||classes.contains("medic")) {any= true;ultiduo[1]=true;}
				if(!any) {break done;}
			}
			p.setUltiduoClassPrefs(ultiduo);
		}
		else if(format.contains("4s")) 
		{
			boolean[] fours = {false,false,false,false};
			if(classes.contains("all")||classes.contains("any")) {fours[0]=true;fours[1]=true;fours[2]=true;fours[3]=true;}
			else
			{
				boolean any = false;
				if(classes.contains("scout")) {any=true;fours[0]=true;}
				if(classes.contains("soldier")||classes.contains("pocket")) {any= true;fours[1]=true;}
				if(classes.contains("demo")||classes.contains("demoman")) {any= true;fours[2]=true;}
				if(classes.contains("med")||classes.contains("medic")) {any= true;fours[3]=true;}
				if(!any) {break done;}
			}
			p.setFoursClassPrefs(fours);
		}
		else if(format.contains("6s")) 
		{
			boolean[] sixes = {false,false,false,false,false};
			if(classes.contains("all")||classes.contains("any")){sixes[0]=true;sixes[1]=true;sixes[2]=true;sixes[2]=true;sixes[3]=true;}
			else
			{
				boolean any= false;
				if(classes.contains("scout")) {any= true;sixes[0]=true;}
				if(classes.contains("pocket")) {any= true;sixes[1]=true;}
				if(classes.contains("roamer")) {any= true;sixes[2]=true;}
				if(classes.contains("demo")||classes.contains("demoman")) {any= true;sixes[3]=true;}
				if(classes.contains("med")||classes.contains("medic")) {any= true;sixes[4]=true;}
				if(!any) {break done;}
			}
			p.setSixesClassPrefs(sixes);
		}
		else{sendDM(discordUser,"Game format misspelling or currently unsupported format detected");break done;}
    }
}
