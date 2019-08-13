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
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;

/**
 * Hello world!
 *
 */

// TODO make all this shit not static
public class PugRunner
{
	static String token = "";
    static String discordPugServerId = ""; // Do event.getGuild().getId() to get server ID; update for gausspugs
    static String tf2PugServerIP = "192.223.26.144";
    static int tf2PugServerPort = 27015;
    static String tf2PugServerRCONPassword = ""; 
    static String sixesCfg = "ugc_6v_standard";
    static String ultiCfg = "etf2l_ultiduo";
    static String foursCfg = null; // Not on server
	
	static Guild server;
	static VoiceChannel ultiQueue;
    static VoiceChannel foursQueue;
    static VoiceChannel sixesQueue;
    static TF2Server tf2Server1;
    static PlayerDatabase playerDB;
    
    static JDA jda;
    
    
    public ArrayList<Game> currentGames = new ArrayList<Game>();
	public enum Format{ULTIDUO,FOURS,SIXES}
public static void main(String[] args)
    {
    	PugRunner bot = new PugRunner();
        
        tf2Server1 = new TF2Server(tf2PugServerIP, tf2PugServerPort, tf2PugServerRCONPassword);
        

        try
        {
        	playerDB = new PlayerDatabase("gausspugs_players");
            jda = new JDABuilder(token)         // The token of the account that is logging in.
                    .addEventListener(new PrivateMessageManager(playerDB))  // An instance of a class that will handle events.
                    .addEventListener(bot.new VoiceChannelListener())
                    .build();
            jda.awaitReady(); // Blocking guarantees that JDA will be completely loaded.
            setDiscordServer(jda, discordPugServerId);
            
            System.out.println("Finished Building JDA / Discord server connection!");
            
            System.out.println("Testing server configuration...");
            tf2Server1.configurePUG("etf2l", "ultiduo_baloo");
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
		Game game = new Game(startTime, type);
		currentGames.add(game);
		
		// Sort players into teams
		List<Member> queueMembers = server.getVoiceChannelsByName(f+" Queue",true).get(0).getMembers();
		HashMap<String, Player> playersByDiscordID = playerDB.getPlayersByDiscordID();
		ArrayList<Player> queuePlayers = new ArrayList<Player>();
		for (Member queueMember : queueMembers) {
			Player player = playersByDiscordID.get(queueMember.getUser().getId());
			queuePlayers.add(player);
		}
		Player[][] teams = game.sortIntoTeams(queuePlayers);
				
		//Add Players to VCs
		for(Player p : teams[0])//BLU
		{addUserToVoiceChannel(server.getVoiceChannelsByName(f+" BLU "+gameNum,true).get(0),p.getMember());}
		for(Player p : teams[1])//RED
		{addUserToVoiceChannel(server.getVoiceChannelsByName(f+" RED "+gameNum,true).get(0),p.getMember());}

		
		// Configure TF2 server
		if(type==Format.ULTIDUO) {
			tf2Server1.configurePUG(ultiCfg, "ultiduo_baloo");
		}
    	else if(type==Format.FOURS) {
    		tf2Server1.configurePUG(foursCfg, "koth_product_rcx");
    	}
    	else if(type==Format.SIXES) {
    		tf2Server1.configurePUG(sixesCfg, "cp_process_final");
    	}
		
		// DM each player connect info
		int teamSize = game.getTeamSize();
		
		for (int i = 0; i < 2; i++) { 
			String assignedTeam;
			if (i == 0) {
				assignedTeam = "BLU";
			}
			else {
				assignedTeam = "RED";
			}
			for (int j = 0; j < teamSize; j++) {
				Player player = teams[i][j];
				PrivateMessageManager.sendDM(jda.getUserById(player.getDiscordID()), tf2Server1.getConnectInfo() 
						+ "\nteam: " + assignedTeam + " class: " + Game.getClassName(type, player.getAssignedClass()));
			}
		}
	}
    private void endGame(Game g, PlayerDatabase playerDB, int ID) 
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