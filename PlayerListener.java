package com.github.erictdobbs.chatter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handle events for all Player related events
 * @author erictdobbs
 */

public class PlayerListener implements Listener {
    private static Chatter plugin;

    public PlayerListener(Chatter instance) {
        plugin = instance;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
    }

    @EventHandler
    public void onPreCommand(PlayerCommandPreprocessEvent event) {
	    String playerName = event.getPlayer().getName();
	    String message = event.getMessage().trim();
	    if (message.startsWith("me")) reply(playerName, message);
    }
    
    @EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent event) {
	    String playerName = event.getPlayer().getName();
	    String message = event.getMessage().trim();
	    reply(playerName, message);
	}
    
    public static void reply(String name, String message) {
    	// Given a player name and message from an event, 
    	// pass information to chatBot, get response, 
    	// then display to all players
    	String botName = plugin.getConfig().getString("botName");
	    final Boolean learnMode = plugin.getConfig().getBoolean("learnMode");
	    String reply = "<" + botName + "> " + getReply(name, message).trim();
	    final String replyFinal = reply;
	    if (!reply.equalsIgnoreCase("<" + botName + "> ")) {
	    	Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
	    		public void run() {
	    			plugin.getLogger().info(replyFinal);
	    			if (!learnMode)
		    			for (Player p : Bukkit.getOnlinePlayers())
		    				p.sendMessage(replyFinal);
		              	}
	            }, plugin.getConfig().getInt("chatDelay"));
	    }
    	
    }

	public static String getReply(String name, String message) {
		return getReply(name, message, 0);
	}

	public static String getReply(String name, String message, int loopCount) {
		if (loopCount > 3) return "Whoa, I'm dizzy...";
		
	    String botName = plugin.getConfig().getString("botName");
	    Boolean addressed = false;
	    
	    // Remove "botName, ", "botName: ", "botName- ", etc
    	if (message.toLowerCase().startsWith(botName.toLowerCase())) {
    		message = message.split(" ", 2)[1].trim();
    		addressed = true;
    	}
		
		String arg1 = "";
		String command = "";
		String arg2 = "";
		if (message.contains("<") && message.contains(">")) {
			String split[] = message.split("<|>");
			if (split.length == 3) {
				arg1 = split[0].replaceAll("\\|","").trim();
				command = split[1].trim();
				arg2 = split[2].replaceAll("\\|","").trim();
			}
		}
		
    	// User using command
    	if(!command.isEmpty()) {
    		if("reply|is".contains(command.toLowerCase())) {
    			if(arg1.isEmpty() || arg2.isEmpty()) return error();
    			String index = Integer.toString(countLinesInFile("responses.txt"));
    			addDataToFile("responses.txt", 
    					index + "|" + arg1 + "|reply|" + 
						arg2 + "|" + name + "|" + getTime());
    			return "Okay, " + name + ".";
    		}
    		else if("alias|aka".contains(command.toLowerCase())) {
    			if(!arg1.isEmpty()) return "Error, usage: <alias> newName";
    			setAlias(name, arg2);
    			return "Okay, " + name + ". I'll call you " + arg2 + ".";
    		}
    		else if("forward|pass".contains(command.toLowerCase())) {
    			if(arg1.isEmpty() || arg2.isEmpty()) return error();
    			String index = Integer.toString(countLinesInFile("responses.txt"));
    			addDataToFile("responses.txt", 
    					index + "|" + arg1 + "|forward|" + 
						arg2 + "|" + name + "|" + getTime());
    			return "Okay, " + name + ".";
    		}
    		else if("add|variable".contains(command.toLowerCase())) {
    			if(arg1.isEmpty() || arg2.isEmpty()) return "Error, usage: $var <add> variable";
    			if(arg1.charAt(0) != '$') return "Error, usage: $var <add> variable";
    			String index = Integer.toString(countLinesInFile("variables.txt"));
    			addDataToFile("variables.txt",
    					index + "|" + arg1 + "|" + arg2
    					+ "|" + name + "|" + getTime());
    			return "Okay, " + name + ".";
    		}
    	}
    	
		
    	// User asking for dice result?
    	char charArray[] = message.toCharArray();
    	Boolean diceFormat = true;
    	String allowed = "0123456789 +-dD";
    	for (char c : charArray)
    		if( !allowed.contains(Character.toString(c)) ) 
    			diceFormat = false;
    	if (diceFormat) return(replyRandNum(message));
    	
    	
    	// Find responses addressed to bot
    	if (addressed) {
	    	ArrayList<String> responses = pullDataFromFile("responses.txt", botName + " " + message, 1);
	    	if (responses.size() > 0) { 
	    		String reply = responses.get( (int)(Math.random()*responses.size()) );
	    		String type = reply.split("\\|")[2];
	    		reply = reply.split("\\|")[3];
				if (type.equalsIgnoreCase("reply")) return replaceVariables(reply, name);
				if (type.equalsIgnoreCase("forward")) return getReply(name, reply, loopCount+1); 
	    	}
    	}
    	
    	
    	// Find response matching entire message
    	ArrayList<String> responses = pullDataFromFile("responses.txt", message, 1);
    	if (responses.size() > 0) { 
    		String reply = responses.get( (int)(Math.random()*responses.size()) );
    		String type = reply.split("\\|")[2];
    		reply = reply.split("\\|")[3];
			if (type.equalsIgnoreCase("reply")) return replaceVariables(reply, name);
			if (type.equalsIgnoreCase("forward")) return getReply(name, reply, loopCount+1); 
    	}

        return "";
    }
    
	public static String replaceVariables(String str, String name) {
		String botName = plugin.getConfig().getString("botName");
		str = str.replace("$botName", botName);
		str = str.replace("$digit", Integer.toString((int)(Math.random()*10)));
		str = str.replace("$nonzero", Integer.toString((int)(1+Math.random()*9)));
		str = str.replace("$name", name);
		str = str.replace("$alias", getAlias(name));
		str = str.replace("$time", getTime());

		int index;
		while( (index = str.indexOf("$")) != -1 && index < str.length()-1) {
			String varName = "$" + str.substring(index+1).split("\\W",2)[0];
			plugin.getLogger().info("Would swap " + varName + " for " + getRandVariable(varName) + ".");
			str = str.replace(varName, getRandVariable(varName));
		}

		/*
		String otherVars[] = str.split("$");
		for (String var : otherVars) {
			String trimmedVar = "$" + var.split("\\W", 2)[0].trim();
			//str = str.replace(trimmedVar, getRandVariable(trimmedVar));
			plugin.getLogger().info("Would swap " + trimmedVar + " for " + getRandVariable(trimmedVar) + ".");
		}*/
		return str;
	}
	
	public static String getRandVariable(String varType) {
		// Takes a variable like "$color" and pulls a random 
		// matching result from the variable file
		ArrayList<String> choices = pullDataFromFile("variables.txt", varType, 1);
		if (choices.size() > 0)
			return choices.get( (int)(Math.random()*choices.size()) ).split("\\|")[2];;
		return "";
	}
	
    public static String getWords(ArrayList<String> words, int indexStart, int indexEnd) {
    	String result = "";
    	if (indexStart >= words.size() || indexEnd >= words.size() || indexStart > indexEnd)
    		return result;
    	for(int ii=indexStart; ii<=indexEnd; ii++) {
    		result += words.get(ii) + " ";
    	}
    	return result.trim();
    }
    
    public static String error() {
    	return "I don't understand...";
    }
    
    public static String getAlias(String name) {
    	ArrayList<String> aliasList = pullDataFromFile("alias.txt", name, 0);
    	if (aliasList.size() > 0) {
    		String aliasLine = aliasList.get(aliasList.size()-1);
    		return aliasLine.split("\\|")[1];
    	}
    	return name;
    }
 
    public static void setAlias(String name, String newName) {
    	removeDataFromFile("alias.txt", name);
    	addDataToFile("alias.txt", name + "|" + newName + "|" + getTime());
    }
    
    public static int countLinesInFile(String filename) {
    	String directory = "plugins\\chatter\\";
		addDataToFile(filename, "");
		int counter=0;
		BufferedReader br = null; 
		try {
			String line;
			br = new BufferedReader(new FileReader(directory + filename));
			while ((line = br.readLine()) != null) if (!line.isEmpty()) counter++;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null) br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return counter;
    }
    
	public static ArrayList<String> pullDataFromFile(String filename, String key, int index) {
		// Given a filename, return lines with matching key at the given index
		String directory = "plugins\\chatter\\";
		addDataToFile(filename, "");
		ArrayList<String> strings = new ArrayList<String>(); 
		BufferedReader br = null; 
		try {
			String line;
			br = new BufferedReader(new FileReader(directory + filename));
			while ((line = br.readLine()) != null) {
				String[] lineSplit = line.split("\\|");
				if (lineSplit.length <= index) continue;
				if (lineSplit[index].replaceAll("\\W", "").equalsIgnoreCase(key.replaceAll("\\W", "")))
					strings.add(line); // Matches, ignoring punctuation and spaces
					
				//if (lineSplit[index].equalsIgnoreCase(key))
				//	strings.add(line); 
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null) br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return strings;
	}
	
	public static void removeDataFromFile(String filename, String key) {
		// Given a filename, remove all instances starting with the key
		String directory = "plugins\\chatter\\";
		addDataToFile(filename, "");
		try {

	        File inFile = new File(directory + filename);
	        File tempFile = new File(inFile.getAbsolutePath() + ".tmp");

	        BufferedReader br = new BufferedReader(new FileReader(directory + filename));
	        PrintWriter pw = new PrintWriter(new FileWriter(tempFile));
	        String line = null;

	        // Read from the original file and write to the new
	        // unless content matches data to be removed.
	        while ((line = br.readLine()) != null) {
	            if (!line.trim().startsWith(key) && !line.isEmpty()) {
	                pw.println(line);
	                pw.flush();
	            }
	        }
	        pw.close();
	        br.close();

	        // Delete the original file
	        if (!inFile.delete()) {
	            System.out.println("Could not delete file");
	            return;
	        }
	        // Rename the new file to the filename the original file had.
	        if (!tempFile.renameTo(inFile))
	            System.out.println("Could not rename file");

	    } catch (FileNotFoundException ex) {
	        ex.printStackTrace();
	    } catch (IOException ex) {
	        ex.printStackTrace();
	    }
	}
	
	public static void addDataToFile(String filename, String line) {
		// Given a filename, add the given line to the end
		if (line.replaceAll("\\s", "").isEmpty()) return;
		String directory = "plugins\\chatter\\";
		try {
			File file = new File(directory + filename);
			// if file does not exists, create it
			if (!file.exists()) file.createNewFile();
			FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write( line.trim() + "\r\n");
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void log(String line) {
	      Calendar cal = Calendar.getInstance();
	      addDataToFile("log.txt", cal.getTime() + " - " + line.trim());
	}
	
	public static String replyRandNum(String str) {
		// Parse and evaluate a string of the form 1d20+ 4 + 3d4 -2
		str = str.replaceAll(" ", "");
		str = str.replaceAll("\\+", "_\\+_");
		str = str.replaceAll("\\-", "_\\-_");
		String terms[] = str.split("_");
		int total = 0;
		int plusOrMinus = 1; // 1 for plus, -1 for minus
		
		String reply = "";
		for (String term : terms) {
			if (term.isEmpty()) continue;
			else if (term.equals("-")) {
				plusOrMinus = -1;
				reply += "-";
			}
			else if (term.equals("+")) {
				plusOrMinus = 1;
				reply += "+";
			}
			else if (term.contains("d")) {
				int dIndex = term.indexOf("d");
				if (dIndex == -1) dIndex = term.indexOf("D");
				if (dIndex == -1) continue;
				if (dIndex == 0) {
					int rollMax = Integer.valueOf(term.substring(1));
					total += (int)(1+Math.random()*rollMax*plusOrMinus);
					reply += "1d" + Integer.toString(rollMax);
				}
				else {
					int numRolls = Integer.valueOf(term.substring(0, dIndex));
					int rollMax = Integer.valueOf(term.substring(dIndex+1));
					for (int ii=0; ii < numRolls; ii++)
						total += (int)(1+Math.random()*rollMax*plusOrMinus);

					reply += Integer.toString(numRolls) + "d" + Integer.toString(rollMax);
				}
			}
			else {
				int num = Integer.valueOf(term)*plusOrMinus;
				total += num;
				reply += Integer.toString(num);
			}
		}
		reply += " = " + Integer.toString(total) + ".";
		return reply;
	}
	
	public static String getTime() {
	      Calendar cal = Calendar.getInstance();
	      return ("" + cal.getTime());
		
	}
}