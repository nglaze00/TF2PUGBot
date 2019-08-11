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
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.restaction.MessageAction;


public class PrivateMessageManager extends ListenerAdapter {
	
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
            //The message was sent in a PrivateChannel.
            //In this example we don't directly use the privateChannel, however, be sure, there are uses for it!
        	String[] commands = msg.split(" ");
        	for(int index=0;index<commands.length;index++) {commands[index]=commands[index].toLowerCase();}
        	if(commands[0].equals("!profile"))
        	{
        		if(commands.length==1) //Help on command
        		{
        			sendDM(author,"Command usage: !profile [profile-link]");
        			sendDM(author,"Example: !profile https://steamcommunity.com/id/exeloar/");
        			break done;
        		}
    			String profileID = Player.urlToProfileID(Player.extractURL(commands[1]));
                if (profileID == "error") {
                	String errorMsg = "No valid profile ID found. Make sure your URL is in one of the formats: https://steamcommunity.com/id/(vanity ID) \n"
                 			+ "https://steamcommunity.com/profiles/(ID number)";
                 	System.out.println(errorMsg);
                 	sendDM(author, errorMsg);
                }
                else {
                	sendDM(author, "Profile ID successfully recorded as: " + profileID);
                 	// ADD ID TO database
                }
        	}
        	else if(commands[0].equals("!classes")) //!classes Ultiduo(Soldier,Med) 4s(ANY) 6s()")
        	{
        		String classSelect="";
        		if(commands.length==1) 
        		{
        			sendDM(author,"Command usage: !classes [format](class1,class2,class3)");
        			sendDM(author,"Example: !classes Ultiduo(Soldier,Med) 4s(ALL) 6s(Med,Scout,Roamer,Pocket,Demo)");
        			break done;
        		}
        		if(true/*Database does not contain player*/) {sendDM(author,"We do not have you as a player in our database yet. Please send us your profile (!profile)");}
        		Player player = new Player(null, 0, 0, 0, null, null, null);//FIXME:Should be read from above
        		for(int i=1;i<commands.length;i++) 
        		{
        			String format = commands[i].substring(0,commands[i].indexOf("("));
        			if(format.equals("ultiduo")) 
        			{
        				classSelect+="ultiduo - ";
        				boolean[] ultiduo = {false,false};
        				if(commands[i].contains("all")||commands[i].contains("any")) {ultiduo[0]=true;classSelect+="Soldier ";ultiduo[1]=true;classSelect+="Medic ";}
        				else
        				{
        					if(commands[i].contains("soldier")||commands[i].contains("pocket")) {ultiduo[0]=true;classSelect+="Soldier ";}
        					if(commands[i].contains("med")||commands[i].contains("medic")) {ultiduo[1]=true;classSelect+="Medic ";}
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
        					if(commands[i].contains("scout")) {fours[0]=true;classSelect+="Scout ";}
	        				if(commands[i].contains("soldier")||commands[i].contains("pocket")) {fours[1]=true;classSelect+="Soldier ";}
	        				if(commands[i].contains("demo")||commands[i].contains("demoman")) {fours[2]=true;classSelect+="Demoman ";}
	        				if(commands[i].contains("med")||commands[i].contains("medic")) {fours[3]=true;classSelect+="Medic ";}
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
        					if(commands[i].contains("scout")) {sixes[0]=true;classSelect+="Scout ";}
	        				if(commands[i].contains("pocket")) {sixes[1]=true;classSelect+="Pocket ";}
	        				if(commands[i].contains("roamer")) {sixes[2]=true;classSelect+="Roamer ";}
	        				if(commands[i].contains("demo")||commands[i].contains("demoman")) {sixes[3]=true;classSelect+="Demoman ";}
	        				if(commands[i].contains("med")||commands[i].contains("medic")) {sixes[4]=true;classSelect+="Medic ";}
        				}
        				player.setSixesClassPrefs(sixes);
        				classSelect+="\n";
        			}
        			else {sendDM(author,"Game format misspelling or currently unsupported format detected");break done;}
        		}
        		sendDM(author,"Current Classes Selected: \n"+classSelect);
        	}
        }
    }
}
