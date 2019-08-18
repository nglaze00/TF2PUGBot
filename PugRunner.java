package DiscordBots.TF2PugBot;

import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.GuildController;

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
    
    static String tf2PugServerIP = "192.223.26.144"; //FIXME: Store in TF2Server.java
    static int tf2PugServerPort = 27015; //FIXME: Store in TF2Server.java
    static String tf2PugServerRCONPassword = ""; //FIXME: Store in TF2Server.java
    static String sixesCfg = "ugc_6v_standard"; //FIXME: Store in TF2Server.java
    static String ultiCfg = "etf2l_ultiduo"; //FIXME: Store in TF2Server.java
    static String foursCfg = null; // Not on server //FIXME: Store in TF2Server.java
	
	static Guild server;
	static GuildController controller;
	static VoiceChannel ultiQueue;
    static VoiceChannel foursQueue;
    static VoiceChannel sixesQueue;
    static TF2Server tf2Server1;
    static PlayerDatabase playerDB;
    
    static JDA jda;
   
    public ArrayList<Game> currentGames = new ArrayList<Game>();
	public enum Format{ULTIDUO,FOURS,SIXES}
	
	public PugRunner() {
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		long timestamp = calendar.getTimeInMillis() / 1000L;
		while(true)
		{
			if(((calendar.getTimeInMillis() / 1000L)-timestamp)>=30) //~30s between checks
			{
				timestamp = calendar.getTimeInMillis() / 1000L;
				for(Game g : currentGames) 
				{
					int ID = LogParser.matchEndID(g);
					if(ID!=-1) {endGame(g,playerDB,ID);}
				}
			}
		}
	}
	public static void main(String[] args)
    {
    	
        PugRunner bot = new PugRunner();//Sets up EndGame Checker and Voice Listener
        //tf2Server1 = new TF2Server(tf2PugServerIP, tf2PugServerPort, tf2PugServerRCONPassword);
        

        try //Setup Listeners and JDA
        {
        	playerDB = new PlayerDatabase("gausspugs_players");
            jda = new JDABuilder(token)         // The token of the account that is logging in.
                    .addEventListener(new PrivateMessageManager(playerDB))  //DM Channel Listener
                    .addEventListener(bot.new VoiceChannelListener()) //Voice Channel Listener
                    .build();
            jda.awaitReady(); // Blocking guarantees that JDA will be completely loaded.
            setDiscordServer(jda, discordPugServerId);
            
            System.out.println("Finished Building JDA / Discord server connection!");
            
        }
        catch (LoginException e){e.printStackTrace();}
        catch (InterruptedException e){e.printStackTrace();}
        
    }
    public static void setDiscordServer(JDA jda, String serverId) {
    	server = jda.getGuildById(serverId);
    	ultiQueue = server.getVoiceChannelsByName("Ultiduo Queue",true).get(0);
    	foursQueue = server.getVoiceChannelsByName("4s Queue",true).get(0);
    	sixesQueue = server.getVoiceChannelsByName("6s Queue",true).get(0);
    	controller = new GuildController(server);
    }
    
    public static boolean isQueueFull(Format f) 
    {
    	if(f==Format.ULTIDUO && ultiQueue.getMembers().size()==4) 		{return true;}
    	else if(f==Format.FOURS && foursQueue.getMembers().size()==8)	{return true;} 
    	else if(f==Format.SIXES && sixesQueue.getMembers().size()==12)  {return true;}
    	return false;
    }
    private void startGame(Format type) {
    	//Get Format String
    	String f = "";
    	if(type==Format.ULTIDUO) {f="Ultiduo";}
    	else if(type==Format.FOURS) {f="4s";}
    	else if(type==Format.SIXES) {f="6s";}
    	
    	//Create Voice Channels
		List<VoiceChannel> channel = server.getVoiceChannelsByName(f+" BLU",true);
		int gameNum = channel.size()+1;
		createVoiceChannel(f+" BLU "+gameNum);createVoiceChannel(f+" RED "+gameNum);
		
		//Create Game Instance
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		long startTime = calendar.getTimeInMillis() / 1000L;
		Game game = new Game(startTime, type);
		game.setID(gameNum);
		currentGames.add(game);
		
		// Form Teams
		List<Member> queueMembers = server.getVoiceChannelsByName(f+" Queue",true).get(0).getMembers();
		HashMap<String, Player> playersByDiscordID = playerDB.getPlayersByDiscordID();
		ArrayList<Player> queuePlayers = new ArrayList<Player>();
		for (Member queueMember : queueMembers) {
			Player player = playersByDiscordID.get(queueMember.getUser().getId());
			queuePlayers.add(player);
		}
		Player[][] teams = game.sortIntoTeams(queuePlayers);
		//Player[][] teams = {{queuePlayers.get(0), queuePlayers.get(1)}, {queuePlayers.get(2), queuePlayers.get(3)}};

		//Add Players to VCs
		VoiceChannel bluVoiceChannel = server.getVoiceChannelsByName(f+" BLU "+gameNum, true).get(0);
		VoiceChannel redVoiceChannel = server.getVoiceChannelsByName(f+" RED "+gameNum, true).get(0);
		for(Player p : teams[0])//BLU
		{
			Member m = p.getMember();
			addUserToVoiceChannel(bluVoiceChannel, m);
		}
		for(Player p : teams[1])//RED
		{
			Member m = p.getMember();
			addUserToVoiceChannel(redVoiceChannel, m);
		}

		
		// Configure TF2 server TODO uncomment
		if(type==Format.ULTIDUO) {
			//tf2Server1.configurePUG(ultiCfg, "ultiduo_baloo");
		}
    	else if(type==Format.FOURS) {
    		//tf2Server1.configurePUG(foursCfg, "koth_product_rcx");
    	}
    	else if(type==Format.SIXES) {
    		//tf2Server1.configurePUG(sixesCfg, "cp_process_final");
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
				//PrivateMessageManager.sendDM(jda.getUserById(player.getDiscordID()), tf2Server1.getConnectInfo() 
					//	+ "\nteam: " + assignedTeam + " class: " + Game.getClassName(type, player.getAssignedClass()));
			// TODO fix
			}
		}
	}
    private void endGame(Game g, PlayerDatabase playerDB, int ID) 
    {
    	//Update Elos
    	String result = LogParser.getResult(ID);
    	g.setWinner(result);
    	g.updatePlayerElos();
    	
    	//Update Database
    	Player[] temp = g.getRedTeam();
    	for(Player p : temp) {playerDB.updateDatabase(p);}
    	temp = g.getBluTeam();
    	for(Player p : temp) {playerDB.updateDatabase(p);}
    	
    	//Kick Users out of Voice & Delete Voice
    	String format = g.getFormat();
    	int gameNum = g.getID();
    	kickUsersFromVoiceChannel(server.getVoiceChannelsByName(format+" BLU "+gameNum,true).get(0));
    	server.getVoiceChannelsByName(format+" BLU "+gameNum,true).get(0).delete().queue();
    	kickUsersFromVoiceChannel(server.getVoiceChannelsByName(format+" RED "+gameNum,true).get(0));
    	server.getVoiceChannelsByName(format+" RED "+gameNum,true).get(0).delete().queue();
    	
    	//Remove Game Instance
    	currentGames.remove(g);
    }
    private void createVoiceChannel(String name) {
    	Channel channel = server.getController().createVoiceChannel(name).complete();
    	//FIXME: Change euler to whatever the server non-mod role may be
        channel.putPermissionOverride(server.getRolesByName("euler",true).get(0)).setDeny(Permission.VOICE_CONNECT).queue();
        channel.putPermissionOverride(server.getRolesByName("@everyone",true).get(0)).setDeny(Permission.VOICE_CONNECT).queue();
        channel.putPermissionOverride(server.getRolesByName("euler",true).get(0)).setDeny(Permission.VOICE_SPEAK).queue();
        channel.putPermissionOverride(server.getRolesByName("@everyone",true).get(0)).setDeny(Permission.VOICE_SPEAK).queue();
    }
    private void addUserToVoiceChannel(VoiceChannel c, Member m) 
    {
    	c.createPermissionOverride(m).setAllow(Permission.VOICE_CONNECT).queue();
    	c.createPermissionOverride(m).setAllow(Permission.VOICE_SPEAK).queue();
    	controller.moveVoiceMember(m,c).queue();
    }
    private void kickUsersFromVoiceChannel(VoiceChannel c) 
    {
    	//FIXME: Change general to whatever the server general is
    	for(Member m : c.getMembers()){server.getController().moveVoiceMember(m, server.getVoiceChannelsByName("General",true).get(0)).queue();}
    }
    public static Member test;
    public class VoiceChannelListener extends ListenerAdapter{
    	@Override
        public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
    		Member discordMember = event.getMember();
    		Player player = playerDB.getPlayersByDiscordID().get(discordMember.getUser().getId());
    		player.setMember(discordMember);
    		System.out.println(player.getMember());
        	String f = event.getChannelJoined().getName();
        	Format type = null;
        	if(f.equals("Ultiduo Queue")) {type=Format.ULTIDUO;}
        	else if(f.equals("4s Queue")) {type=Format.FOURS;}
        	else if(f.equals("6s Queue")) {type=Format.SIXES;}
    		if(type!=null) {
    			if(isQueueFull(type)) {startGame(type);};
    		}
    	}
    	@Override
    	public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
    		Member discordMember = event.getMember();
    		Player player = playerDB.getPlayersByDiscordID().get(discordMember.getUser().getId());
    		player.setMember(discordMember);
    		System.out.println(player.getMember());
        	String f = event.getChannelJoined().getName();
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