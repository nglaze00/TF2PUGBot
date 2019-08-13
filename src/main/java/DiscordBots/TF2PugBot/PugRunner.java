package DiscordBots.TF2PugBot;

import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;

/**
 * Hello world!
 *
 */
public class PugRunner
{
	static Guild server;//FIXME:Server needs to be the server actually 
    static VoiceChannel ultiQueue;
    static VoiceChannel foursQueue;
    static VoiceChannel sixesQueue;
    
    ArrayList<Game> currentGames = new ArrayList<Game>();
	public enum Format{ULTIDUO,FOURS,SIXES}
    public static void main(String[] args)
    {
    	PugRunner bot = new PugRunner();
        String token = "NjA5MjA2MDU1MDk1ODk0MDIx.XVBsfg.EI4crJuIlXK0Zc4AgT6AtLT3zCo";
        String pugServerId = "609217958950207508"; // Do event.getGuild().getId() to get server ID
        PlayerDatabaseManager playerDB;

        try
        {
        	playerDB = new PlayerDatabaseManager("gausspugs_players");
            JDA jda = new JDABuilder(token)         // The token of the account that is logging in.
                    .addEventListener(new PrivateMessageManager(playerDB))  // An instance of a class that will handle events.
                    .addEventListener(bot.new VoiceChannelListener())
                    .build();
            jda.awaitReady(); // Blocking guarantees that JDA will be completely loaded.
            setDiscordServer(jda, pugServerId);
            
            System.out.println("Finished Building JDA / Discord server connection!");
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
    public static void setDiscordServer(JDA jda, String serverId) {
    	server = jda.getGuildById(serverId);
    	ultiQueue = server.getVoiceChannelsByName("Ultiduo Queue",true).get(0);
    	foursQueue = server.getVoiceChannelsByName("4s Queue",true).get(0);
    	sixesQueue = server.getVoiceChannelsByName("6s Queue",true).get(0);
    }
    
    public static boolean isQueueFull(Format f) 
    {
    	if(f==Format.ULTIDUO && ultiQueue.getMembers().size()==4) 		{return true;}
    	else if(f==Format.FOURS && foursQueue.getMembers().size()==8)	{return true;} 
    	else if(f==Format.SIXES && sixesQueue.getMembers().size()==12)  {return true;}
    	return false;
    }
    private void startGame(Format type) {
    	//Create Voice Servers
    	String f = "";
    	if(type==Format.ULTIDUO) {f="Ultiduo";}
    	else if(type==Format.FOURS) {f="4s";}
    	else if(type==Format.SIXES) {f="6s";}
		List<VoiceChannel> channel = server.getVoiceChannelsByName(f+" BLU",true);
		int gameNum = channel.size()+1;
		createVoiceChannel(f+" BLU "+gameNum);createVoiceChannel(f+" RED "+gameNum);

		//Create Game Instance
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		long startTime = calendar.getTimeInMillis() / 1000L;
		currentGames.add(new Game(startTime));
		
		//Add Players to VCs
		Player[][] teams = currentGames.get(currentGames.size()-1).getTeams(server.getVoiceChannelsByName(f+" Queue",true).get(0).getMembers());
		for(Player p : teams[0])//BLU
		{addUserToVoiceChannel(server.getVoiceChannelsByName(f+" BLU "+gameNum,true).get(0),p.getMember());}
		for(Player p : teams[1])//RED
		{addUserToVoiceChannel(server.getVoiceChannelsByName(f+" RED "+gameNum,true).get(0),p.getMember());}
		currentGames.get(currentGames.size()-1).setFormat(type);
		currentGames.get(currentGames.size()-1).setFormat(type);
		//TF2 Server Setup
		
	}
    private void endGame(Game g, PlayerDatabaseManager playerDB, int ID) 
    {
    	String result = LogParser.getResult(ID);
    	g.setWinner(result);
    	g.updatePlayerElos();
    	playerDB.updateDatabase(g.getRedTeam());
    	playerDB.updateDatabase(g.getBluTeam());
    	kickUsersFromVoiceChannel(,g.getType())
    }
    private void createVoiceChannel(String name) {
    	Channel channel = server.getController().createVoiceChannel(name).complete();
    	//Lock Channel
    }
    private void addUserToVoiceChannel(VoiceChannel c, Member m) {server.getController().moveVoiceMember(m,c);}
    private void kickUsersFromVoiceChannel(VoiceChannel c, Format type) 
    {
    	String f = "";
    	if(type==Format.ULTIDUO) {f="Ultiduo";}
    	else if(type==Format.FOURS) {f="4s";}
    	else if(type==Format.SIXES) {f="6s";}
    	for(Member m : c.getMembers()){server.getController().moveVoiceMember(m,server.getVoiceChannelsByName(f+" Queue",true).get(0));}
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