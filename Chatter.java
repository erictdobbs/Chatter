package com.github.erictdobbs.chatter;

import java.io.File;
import java.io.IOException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;


public class Chatter extends JavaPlugin{

	/**
	 * @param args
	 */
    
	@Override
	public void onEnable() {
		File dir = new File("plugins\\chatter");
		if(!dir.exists()) {
	        if (!(new File("plugins\\chatter")).mkdirs()) {
	        	getLogger().info("Problem! Couldn't create plugins\\chatter!");
	        }
		}
		
		String[] files = {"alias", "log", "responses", "inventory", "variables"};
		
		for (String fileName : files) {
			try {
				File txtFile = new File("plugins\\chatter\\" + fileName + ".txt");
				if (!txtFile.exists()) txtFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
        this.saveDefaultConfig();
        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(new PlayerListener(this), this);
	}

	@Override
	public void onDisable() {

	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){

		if(cmd.getName().equalsIgnoreCase("chatBot") && !(sender instanceof Player)){
			String message = "";
			for (String arg : args) message += arg + " ";
			
			getLogger().info( PlayerListener.getReply("server", message) );
		}
		return true;
	}
	
}
