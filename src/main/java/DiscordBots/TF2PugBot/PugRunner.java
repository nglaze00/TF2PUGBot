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
// TODO add server isInUse
public class PugRunner
{

	
	// Sample sensitive info
	/**
	static String token = "token-goes-here";
	static String discordPugServerId = "608443004776349721"; // Do event.getGuild().getId() to get server ID
	static String tf2PugServerIP = "ip.address.here"; //FIXME: Store in TF2Server.java
    static int tf2PugServerPort = 11111; //FIXME: Store in TF2Server.java
    static String tf2PugServerRCONPassword = "rconpassgoeshere"; //FIXME: Store in TF2Server.java
    **/
	
    // wild's server
    /**
    static String tf2PugServerIP = "192.223.26.144"; //FIXME: Store in TF2Server.java
    static int tf2PugServerPort = 27015; //FIXME: Store in TF2Server.java
    static String tf2PugServerRCONPassword = "gaussdeeznuts101"; //FIXME: Store in TF2Server.java
    **/
	
	
    // serveme
    static String tf2PugServerIP = "la.serveme.tf"; //FIXME: Store in TF2Server.java
    static int tf2PugServerPort = 27025; //FIXME: Store in TF2Server.java
    static String tf2PugServerRCONPassword = "gauss"; //FIXME: Store in TF2Server.java
    
    // cfg file names (filename.cfg must be on server)
    static String sixesCfg = "ugc_6v_standard"; //FIXME: Store in TF2Server.java
    static String ultiCfg = "etf2l_ultiduo"; //FIXME: Store in TF2Server.java
    static String foursCfg = "ugc_4v_koth"; // Not on server //FIXME: Store in TF2Server.java
	
    static String token = "NjA5MjA2MDU1MDk1ODk0MDIx.XWCY7A.P1VoVjRTLwggtyCuA3RMOP6v57o";
	static String discordPugServerId = "608443004776349721"; // Do event.getGuild().getId() to get server ID
    
	static Guild server;
	static GuildController controller;
	static VoiceChannel ultiQueue;
    static VoiceChannel foursQueue;
    static VoiceChannel sixesQueue;
    
    static PlayerDatabase playerDB;
    
    static JDA jda;
   
    public ArrayList<Game> currentGames = new ArrayList<Game>();
	public enum Format{ULTIDUO,FOURS,SIXES}
	TF2Server tf2Server;
	
	public PugRunner() {
		tf2Server = new TF2Server(tf2PugServerIP, tf2PugServerPort, tf2PugServerRCONPassword);
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
            // Test team sorter
            //startGame(Format.ULTIDUO);
            
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

				for(int i = 0; i < currentGames.size(); i++) 
				{
					Game g = currentGames.get(i);
					int ID = LogParser.matchEndID(g);
					
					if(ID!=-1) endGame(g,playerDB,ID);
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
		
		
		
		// Form Teams
		List<Member> queueMembers = server.getVoiceChannelsByName(f+" Queue",true).get(0).getMembers();
		HashMap<String, Player> playersByDiscordID = playerDB.getPlayersByDiscordID();
		ArrayList<Player> queuePlayers = new ArrayList<Player>();
		for (Member queueMember : queueMembers) {
			Player player = playersByDiscordID.get(queueMember.getUser().getId());
			queuePlayers.add(player);
		}
		// ArrayList<Player> queuePlayers = new ArrayList<Player>(playerDB.getPlayersBySteamID().values());
		// queuePlayers.remove(0);
		Player[][] teams = game.sortIntoTeams(queuePlayers);
		

		//Add Players to VCs & tell them match has started
		VoiceChannel bluVoiceChannel = server.getVoiceChannelsByName(f+" BLU "+gameNum, true).get(0);
		VoiceChannel redVoiceChannel = server.getVoiceChannelsByName(f+" RED "+gameNum, true).get(0);
		for(Player p : teams[0]) //BLU
		{
			addUserToVoiceChannel(bluVoiceChannel, p.getMember());
			PrivateMessageManager.sendDM(p.getMember().getUser(),"Getting server setup... will send server info soon");
		}
		for(Player p : teams[1]) //RED
		{
			addUserToVoiceChannel(redVoiceChannel, p.getMember());
			PrivateMessageManager.sendDM(p.getUser(),"Getting server setup... will send server info soon");
		}

		// Configure TF2 server
		tf2Server.configurePUG(getCfgName(format), format);
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
				PrivateMessageManager.sendDM(jda.getUserById(player.getDiscordID()), tf2Server.getConnectInfo() 
						+ "\nteam: " + assignedTeam + " class: " + Game.getClassName(format, player.getAssignedClass()));
			}
		}
		currentGames.add(game);
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
    	//channel.putPermissionOverride(server.getRolesByName("@everyone",true).get(0)).setDeny(Permission.)
    	channel.putPermissionOverride(server.getRolesByName("@everyone",true).get(0)).setDeny(Permission.VOICE_SPEAK).queue();
    }
    private void addUserToVoiceChannel(VoiceChannel c, Member m) 
    {
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
    		catch(NullPointerException e) 
    		{
    			server.getController().moveVoiceMember(discordMember, server.getVoiceChannelsByName("General",true).get(0)).queue();
    			PrivateMessageManager.sendDM(discordMember.getUser(), "You are not in the player database yet, please use !profile and set yourself up!");
    		}
    		catch(Exception e){e.printStackTrace();}
    	}
    	@Override
    	public void onGuildVoiceMove(GuildVoiceMoveEvent event) 
    	{
    		Member discordMember = event.getMember();
		String f = event.getChannelJoined().getName();
    		Format type = null;
        	if(f.equals("Ultiduo Queue")) {type=Format.ULTIDUO;}
        	else if(f.equals("4s Queue")) {type=Format.FOURS;}
        	else if(f.equals("6s Queue")) {type=Format.SIXES;}
        	if(type!=null) 
        	{
        		server.getController().createVoiceChannel("temp");
        		server.getController().moveVoiceMember(discordMember, server.getVoiceChannelsByName("temp", true).get(0)).queue();
        		server.getVoiceChannelsByName("temp",true).get(0).delete().queue();
        		PrivateMessageManager.sendDM(discordMember.getUser(), "Please rejoin the queue, there's a temporary bug with moving voice channels");
        	}
    	}
    }
}
