package DiscordBots.TF2PugBot;

import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;

/**
 * Hello world!
 *
 */
public class PugRunner
{
	static Guild server = null;//FIXME:Server needs to be the server actually 
    static VoiceChannel ultiQueue = server.getVoiceChannelsByName("Ultiduo Queue",true).get(0);
    static VoiceChannel foursQueue = server.getVoiceChannelsByName("4s Queue",true).get(0);
    static VoiceChannel sixesQueue = server.getVoiceChannelsByName("6s Queue",true).get(0);
	public enum Format{ULTIDUO,FOURS,SIXES}
    public static void main(String[] args)
    {
    	PugRunner bot = new PugRunner();
        //We construct a builder for a BOT account. If we wanted to use a CLIENT account
        // we would use AccountType.CLIENT
        try
        {
            JDA jda = new JDABuilder("NjA5MjA2MDU1MDk1ODk0MDIx.XU-iGA.hz2C4bE03vAjKqcVch5v1ZwJVMs")         // The token of the account that is logging in.
                    .addEventListener(new PrivateMessageManager())  // An instance of a class that will handle events.
                    .addEventListener(bot.new VoiceChannelListener())
                    .build();
            jda.awaitReady(); // Blocking guarantees that JDA will be completely loaded.
            System.out.println("Finished Building JDA!");
        }
        catch (LoginException e)
        {
            //If anything goes wrong in terms of authentication, this is the exception that will represent it
            e.printStackTrace();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
    public static boolean isQueueFull(Format f) 
    {
    	if(f==Format.ULTIDUO && ultiQueue.getMembers().size()==4) 		{return true;}
    	else if(f==Format.FOURS && foursQueue.getMembers().size()==8)	{return true;} 
    	else if(f==Format.SIXES && sixesQueue.getMembers().size()==12)  {return true;}
    	return false;
    }
    private void startGame(Format type) {
		
	}
    
    public class VoiceChannelListener extends ListenerAdapter{
    	@Override
        public void onGuildVoiceJoin(GuildVoiceJoinEvent join) {
        	String f = join.getChannelJoined().getName();
        	System.out.println(f);
        	Format type = null;
        	if(f.equals("Ultiduo Queue")) {type=Format.ULTIDUO;}
        	else if(f.equals("4s Queue")) {type=Format.FOURS;}
        	else if(f.equals("6s Queue")) {type=Format.SIXES;}
    		if(type!=null) {
    			if(isQueueFull(type)) {startGame(type);};
    		}
    	}
    }
}