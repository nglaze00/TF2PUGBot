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

        if (event.isFromType(ChannelType.PRIVATE) && !author.isBot()) //If this message was sent to a PrivateChannel
        {
            //The message was sent in a PrivateChannel.
            //In this example we don't directly use the privateChannel, however, be sure, there are uses for it!
        	
            String profileID = Player.urlToProfileID(Player.extractURL(msg));
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
    }

}
