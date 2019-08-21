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

import DiscordBots.TF2PugBot.PugRunner.Format;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;

/**
 * Hello world!
 *
 */

// TODO make all this shit not static
// TODO kick out boys who dont have profile DMed
// TODO add more maps, support for multiple servers
// TODO use lowest elo --> higher elo team, instead of the opposite
public class PugRunner
{
	static String token = "token_goes_here";
    //TEST SERVER
	//static String discordPugServerId = "609217958950207508"; // Do event.getGuild().getId() to get server ID; update for gausspugs
    //GAUSSPUGS
	static String discordPugServerId = "server_id_here";
	
    // serveme
    static String tf2PugServerIP = "dorothy.serveme.tf"; //FIXME: Store in TF2Server.java
    static int tf2PugServerPort = 27025; //FIXME: Store in TF2Server.java
    static String tf2PugServerRCONPassword = "gauss"; //FIXME: Store in TF2Server.java
    static String sixesCfg = "ugc_6v_standard"; //FIXME: Store in TF2Server.java
    static String ultiCfg = "etf2l_ultiduo"; //FIXME: Store in TF2Server.java
    static String foursCfg = "ugc_4v_koth"; // Not on server //FIXME: Store in TF2Server.java
	
	static Guild server;
	static GuildController controller;
	static VoiceChannel ultiQueue;
    static VoiceChannel foursQueue;
    static VoiceChannel sixesQueue;
    
    static PlayerDatabase playerDB;
    
    static JDA jda;
   
    public ArrayList<Game> currentGames = new ArrayList<Game>();
	public enum Format{ULTIDUO,FOURS,SIXES}
	TF2Server tf2Server1;
	
	public PugRunner() {
		tf2Server1 = new TF2Server(tf2PugServerIP, tf2PugServerPort, tf2PugServerRCONPassword);
		// tf2Server1.configurePUG(ultiCfg, Format.ULTIDUO); tests changing map
		System.out.println("GAUSS");

        try //Setup Listeners and JDA
        {
        	playerDB = new PlayerDatabase("gausspugs_players");
            jda = new JDABuilder(token)         // The token of the account that is logging in.
                    .addEventListener(new PrivateMessageManager(playerDB))  //DM Channel Listener
                    .addEventListener(this.new VoiceChannelListener()) //Voice Channel Listener
                    .build();
            jda.awaitReady(); // Blocking guarantees that JDA will be completely loaded.
            setDiscordServer(jda, discordPugServerId);
            
            System.out.println("Finished Building JDA / Discord server connection!");
            
        }
        catch (LoginException e){e.printStackTrace();}
        catch (InterruptedException e){e.printStackTrace();}
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		long timestamp = calendar.getTimeInMillis() / 1000L;
		while(true)
		{
			calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			if(((calendar.getTimeInMillis() / 1000L)-timestamp)>=5) //FIXME ~30s between checks
			{
				timestamp = calendar.getTimeInMillis() / 1000L;
				System.out.println(currentGames.size());
				for(Game g : currentGames) 
				{
					System.out.println("A");
					int ID = LogParser.matchEndID(g);
					System.out.println(ID);
					if(ID!=-1) {endGame(g,playerDB,ID);}
				}
			}
		}
	}
	public static void main(String[] args)
    {
    	
        PugRunner bot = new PugRunner();//Sets up EndGame Checker and Voice Listener
        
    }
    public static void setDiscordServer(JDA jda, String serverId) {
    	server = jda.getGuildById(serverId);
    	ultiQueue = server.getVoiceChannelById(613601993646276618L);
    	foursQueue = server.getVoiceChannelById(613602455116185620L);
    	sixesQueue = server.getVoiceChannelById(613602582300196865L);
    	controller = new GuildController(server);
    }
    
    public static boolean isQueueFull(Format f) 
    {
    	if(f==Format.ULTIDUO && ultiQueue.getMembers().size()==4) 		{return true;}
    	else if(f==Format.FOURS && foursQueue.getMembers().size()==8)	{return true;} 
    	else if(f==Format.SIXES && sixesQueue.getMembers().size()==12)  {return true;}
    	return false;
    }
    public static String getCfgName(Format format) {
    	switch(format) {
    	case ULTIDUO: return ultiCfg;
    	
    	case FOURS:   return foursCfg;
    	
    	case SIXES:   return sixesCfg;
    	default: return null;
    	}
    }
    private void startGame(Format format) {
    	//Get Format String
    	String f = "";
    	if(format==Format.ULTIDUO) 	{f="Ultiduo";}
    	else if(format==Format.FOURS) {f="4s";}
    	else if(format==Format.SIXES) {f="6s";}
    	
    	//Create Voice Channels
		List<VoiceChannel> channel = server.getVoiceChannelsByName(f+" BLU",true);
		int gameNum = channel.size()+1;
		createVoiceChannel(f+" BLU "+gameNum);createVoiceChannel(f+" RED "+gameNum);
		
		//Create Game Instance
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		long startTime = calendar.getTimeInMillis() / 1000L;
		Game game = new Game(startTime, format);
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
		for(Player p : teams[0]) 
		{
			System.out.println(p.getSteamID()); 
			addUserToVoiceChannel(bluVoiceChannel, p.getMember());}//BLU
		for(Player p : teams[1]) {addUserToVoiceChannel(redVoiceChannel, p.getMember());}//RED
		
		// Configure TF2 server TODO uncomment
		if(format==Format.ULTIDUO) {
			//tf2Server1.configurePUG(ultiCfg, "ultiduo_baloo");
		}
    	else if(format==Format.FOURS) {
    		//tf2Server1.configurePUG(foursCfg, "koth_product_rcx");
    	}
    	else if(format==Format.SIXES) {
    		//tf2Server1.configurePUG(sixesCfg, "cp_process_final");
    	}
		// Configure TF2 server
		tf2Server1.configurePUG(getCfgName(format), format);
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
						+ "\nteam: " + assignedTeam + " class: " + Game.getClassName(format, player.getAssignedClass()));
			}
		}
	}
    private void endGame(Game g, PlayerDatabase playerDB, int ID) 
    {
    	//Update Elos
    	String result = LogParser.getResult(ID);
    	g.setWinner(result);
    	g.updatePlayerElos();
    	g.resetPlayerClassAssignments();
    	
    	//Update Database
    	Player[] temp = g.getRedTeam();
    	for(Player p : temp) 
    	{
    		playerDB.updateDatabase(p);
    		PrivateMessageManager.sendDM(p.getMember().getUser(),"Logs for the match can be found at logs.tf/"+ID);
    	}
    	
    	temp = g.getBluTeam();
    	for(Player p : temp) 
    	{
    		playerDB.updateDatabase(p);
    		PrivateMessageManager.sendDM(p.getMember().getUser(),"Logs for the match can be found at logs.tf/"+ID);
    	}
    	
    	//Kick Users out of Voice & Delete Voice
    	String format = g.getFormat();
    	int gameNum = g.getID();
    	kickUsersFromVoiceChannel(server.getVoiceChannelsByName(format+" BLU "+gameNum,true).get(0));
    	try{Thread.sleep(1000);} 
    	catch(Exception e) {}
    	server.getVoiceChannelsByName(format+" BLU "+gameNum,true).get(0).delete().queue();
    	kickUsersFromVoiceChannel(server.getVoiceChannelsByName(format+" RED "+gameNum,true).get(0));
    	try{Thread.sleep(1000);} 
    	catch(Exception e) {}
    	server.getVoiceChannelsByName(format+" RED "+gameNum,true).get(0).delete().queue();
    	
    	//Remove Game Instance
    	currentGames.remove(g);
    }
    private void createVoiceChannel(String name) {
    	Channel channel = server.getController().createVoiceChannel(name).complete();
        channel.putPermissionOverride(server.getRolesByName("@everyone",true).get(0)).setDeny(Permission.VOICE_CONNECT).queue();
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
    public class VoiceChannelListener extends ListenerAdapter{
    	@Override
        public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
    		Member discordMember = event.getMember();
    		try
    		{
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
    		catch(Exception e)
    		{
    			e.printStackTrace();
    			server.getController().moveVoiceMember(discordMember, server.getVoiceChannelsByName("General",true).get(0)).queue();
    			PrivateMessageManager.sendDM(discordMember.getUser(), "You are not in the player database yet, please use !profile and set yourself up!");
    		}
    	}
    	@Override
    	public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
    		Member discordMember = event.getMember();
    		try 
    		{
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
			catch(Exception e)
    		{
				e.printStackTrace();
				server.getController().moveVoiceMember(discordMember, server.getVoiceChannelsByName("General",true).get(0)).queue();
				PrivateMessageManager.sendDM(discordMember.getUser(), "You are not in the player database yet, please use !profile and set yourself up!");
			}
    	}
    }
}