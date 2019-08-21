package DiscordBots.TF2PugBot;

import java.net.InetSocketAddress;
import java.util.Random;

import com.ibasco.agql.protocols.valve.source.query.SourceRconAuthStatus;
import com.ibasco.agql.protocols.valve.source.query.client.SourceRconClient;
import com.ibasco.agql.protocols.valve.source.query.exceptions.RconNotYetAuthException;

import DiscordBots.TF2PugBot.PugRunner.Format;

public class TF2Server {

	private InetSocketAddress address;
	private String rconPassword;
	private SourceRconClient rconClient;
	private String password;
	
	private String[] ultiMaps = {"ultiduo_baloo"};
	private String[] foursMaps = {"koth_product_rcx"};
	private String[] sixesMaps = {"cp_process_final"};
	
	private boolean authenticated;
	public boolean isInUse;
	
	public TF2Server(String tf2PugServerIP, int tf2PugServerPort, String tf2PugServerRCONPassword) {
		this.address = new InetSocketAddress(tf2PugServerIP, tf2PugServerPort);
		this.rconPassword = tf2PugServerRCONPassword;
		this.isInUse = false;
		// Attempt connection to server
		rconClient = new SourceRconClient();
		SourceRconAuthStatus authStatus = rconClient.authenticate(address, rconPassword).join();
		if (!authStatus.isAuthenticated()) {
			String msg = "RCON authentication to TF2 server at " + tf2PugServerIP + " with RCON password " + rconPassword + " failed. "
					+ "Check IP / pass and server status, then run the bot again.";
			System.out.println(msg);
			authenticated = false;
		}
		else {
			authenticated = true;
			System.out.println("RCON authentication to TF2 server at " + tf2PugServerIP + " with RCON password " + rconPassword + " successful!");
		}
	}
	public String getConnectInfo() {return "connect " + address.toString().split("/")[0] + ":" + address.toString().split(":")[1]+"; password " + password;}
	public String getPassword() {return password;}
	
	public void configurePUG(String cfgFileName, Format f) {
		String mapName="";
		Random r = new Random();
		if(f==Format.ULTIDUO) {mapName = ultiMaps[r.nextInt(ultiMaps.length)];}
		if(f==Format.FOURS) {mapName = foursMaps[r.nextInt(foursMaps.length)];}
		if(f==Format.SIXES) {mapName = sixesMaps[r.nextInt(sixesMaps.length)];}
		configurePUG(cfgFileName,mapName);
	}
	public void configurePUG(String cfgFileName, String mapName) 
	{
		// Change map
		String mapCommand = "changelevel " + mapName;
		System.out.println("Changing server map...");
		executeRCONCommand(mapCommand);
		
		//Wait a bit
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Exec config
		String configCommand = "exec " + cfgFileName; //TODO get this
		System.out.println("Executing config");
		executeRCONCommand(configCommand);
		
		// Change password
		String newPass = generatePassword();
		String passCommand = "sv_password " + newPass; //TODO get this
		System.out.println("Server password set to " + newPass);
		this.password = newPass;
		executeRCONCommand(passCommand);
		
		// executeRCONCommand("mp_winlimit 5");
		isInUse = true;
	}
	
	public boolean executeRCONCommand(String command) {
		if (!authenticated) {
			System.out.println("Connection to server not authenticated. Can't send command.");
			return false;
		}
		try {
			rconClient.execute(address, command).whenComplete(this::handleResponse).join();
			return true;
		} catch (RconNotYetAuthException e) {
			this.authenticated = false;
			return false;
		}
		
	}
	private void handleResponse(String response, Throwable error) {
        if (error != null) {
            System.out.println("RCON command error: " + error.getMessage());
            return;
        }
        System.out.println("RCON command sent; reply: " + response);
    }
	
	private String generatePassword() {
		Random rand = new Random();
		int num = rand.nextInt(9000000) + 1000000;
		return Integer.toString(num);
	}
	public void endGame() {
		this.isInUse = false;
	}
}
