/**
 *    
 *    Copyright (C) 2016 Serge Humphrey <sergehumphrey@gmail.com>
 * 
 *    This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   *MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *   
 */
package com.btbb.spinner;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.btbb.spinner.ChatFormat.PageBuilder;
import com.btbb.spinner.ChunkUtils.PlayerSettings;
import com.evilmidget38.NameFetcher;
import com.evilmidget38.NameTimestampPair;
import com.evilmidget38.UUIDFetcher;

public class SpinnerCommands implements CommandExecutor {
	SpinnerPlugin plugin;

	public SpinnerCommands(SpinnerPlugin p) {
		this.plugin = p;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!command.getLabel().equals("spinner"))
			return false;
		if (args.length == 0) {
			sender.sendMessage(ChatFormat.format("%red%Use /%1 help to show a list of commands", label));
			return true;
		}

		// do the command in a thread so we can lookup UUID's from mojang
		// without hanging the server
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new ThreadedCommand(sender, command, label, args));
		return true;
	}

	protected UUID getUUIDbyName(String name) {
		if (name.length() < 3)
			return null;
		// try online player first
		for (Player p : plugin.getServer().getOnlinePlayers()) {
			if (p.getName().equalsIgnoreCase(name))
				return p.getUniqueId();
		}
		// offline players..
		for (OfflinePlayer p : plugin.getServer().getOfflinePlayers()) {
			if (p.getName().equalsIgnoreCase(name))
				return p.getUniqueId();
		}
		// try getting it from mojang...
		UUIDFetcher fetcher = new UUIDFetcher(Arrays.asList(name));
		Map<String, UUID> response = null;
		try {
			response = fetcher.call();
		} catch (Exception e) {
			plugin.getLogger().warning("Exception while running UUIDFetcher");
			e.printStackTrace();
		}
		if (response.size() > 0)
			return response.values().iterator().next();
		return null;
	}

	protected String getPlayerbyUUID(UUID id) {
		if (id == null)
			return null;
		// try online player first
		for (Player p : plugin.getServer().getOnlinePlayers()) {
			if (p.getUniqueId().equals(id))
				return p.getName();
		}
		// offline players..
		OfflinePlayer p = plugin.getServer().getOfflinePlayer(id);
		if (p != null)
			return p.getName();

		// try getting it from mojang...
		NameFetcher fetcher = new NameFetcher(id);
		List<NameTimestampPair> response = null;
		try {
			response = fetcher.call();
		} catch (Exception e) {
			plugin.getLogger().warning("Exception while running NameFetcher");
			e.printStackTrace();
		}
		Iterator<NameTimestampPair> i = response.iterator();
		while (i.hasNext()) {
			NameTimestampPair entry = i.next();
			if (i.hasNext())
				continue;
			else // send the last one
				return entry.name;
		}
		return null;
	}

	protected class ThreadedCommand implements Runnable {
		CommandSender sender;
		Command command;
		String label;
		String[] args;

		public ThreadedCommand(CommandSender sender, Command command, String label, String[] args) {
			this.sender = sender;
			this.command = command;
			this.label = label;
			this.args = args;
		}

		@Override
		public void run() {
			if (runCommand())
				return;
			message("%red%No such command: /%1 %2", label, args[0]);

		}

		private boolean checkPermission(String perm) {
			return plugin.checkPermission(sender, perm);
		}

		private void message(String msg, Object... objects) {
			sender.sendMessage(ChatFormat.format(msg, objects));
		}

		private void messageChunkinfo(ChunkMark m, boolean showOwner) {
			sender.sendMessage(getChunkinfoMsg(m, showOwner));
		}

		private String getChunkinfoMsg(ChunkMark m, boolean showOwner) {
			return ChatFormat.format("%3%gray%(%1, %2)%6|%green%G:%gray%%4%6=%gray%%7%6|%gray%%5", m.location.x * 16, m.location.z * 16,
					showOwner ? ChatColor.GRAY + getPlayerbyUUID(m.owner) + ChatColor.GOLD + "|" : "", m.group,
					(m.alwaysLoaded) ? "Always" : "When on", ChatColor.GOLD, (m.isGroupEnabled()) ? "enabled" : "disabled");
		}

		private boolean runCommand() {

			boolean isPlayer = (sender instanceof Player);
			Player player = (isPlayer) ? (Player) sender : null;
			String arg1 = args[0].toLowerCase();
			String arg2 = (args.length > 1) ? args[1] : null;
			String arg3 = (args.length > 2) ? args[2] : null;
			String arg4 = (args.length > 3) ? args[3] : null;

			// list, reload, clearplayer, status
			if (arg1.equals("list")) {
				LinkedList<ChunkMark> list;
				String plr = sender.getName();
				boolean isOther = false;
				if (arg2 != null && !NumberUtils.isNumber(arg2)) {
					// check permissions
					if (!checkPermission(Permissions.LIST_OTHERS)) {
						message("%red%Sorry, you do not have permission to list other player's details");
						return true;
					}
					// find the player
					UUID id = getUUIDbyName(arg2);
					if (id == null) {
						message("%red%No such player exists: %green%%1", arg2);
						return true;
					}
					list = plugin.utils.getChunkMarksByPlayer(id);
					if (list.size() < 1) {
						message("%gold%Player has no chunks marked.");
						return true;
					}
					plr = arg2;
					isOther = true;
				} else {
					if (!checkPermission(Permissions.USE)) {
						message("%red%Sorry, you do not have permission to use Spinner");
						return true;
					}
					// list for player
					if (!isPlayer) {
						message("%red%You must specify a player");
						return true;
					}
					list = plugin.utils.getChunkMarksByPlayer(player.getUniqueId());
				}
				PageBuilder pages = ChatFormat.getPageBuilder();
				for (ChunkMark m : list)
					pages.addPlainMessage(getChunkinfoMsg(m, false));
				pages.send(sender, ChatFormat.format("%gold%Chunks by %yellow%%1%gold%: %white%%2", plr, list.size()),
						label + " " + arg1 + ((isOther) ? " " + plr : ""), (isOther) ? arg3 : arg2);
				return true;
				/// XXX Listall
			} else if (arg1.equals("listall")) {
				if (!checkPermission(Permissions.LIST_OTHERS)) {
					message("%red%Sorry, you do not have permission to list other player's details");
					return true;
				}

				List<ChunkMark> list = plugin.utils.getChunks();
				PageBuilder pages = ChatFormat.getPageBuilder();
				for (ChunkMark m : list)
					pages.addPlainMessage(getChunkinfoMsg(m, true));
				pages.send(sender, ChatFormat.format("%gold%Total marked chunks: %white%%1", list.size()), label + " " + arg1, arg2);
				return true;
				/// XXX RELOAD
			} else if (arg1.equals("reload")) {
				if (!checkPermission(Permissions.RELOAD)) {
					message("%red%You do not have permission to reload.");
					return true;
				}
				plugin.reloadConfig();
				message("%gold%Config reloaded.");
				return true;
			} else if (arg1.equals("clearplayer")) {
				if (!checkPermission(Permissions.CLEARPLAYER)) {
					message("%red%Sorry, you do not have permission to clear player's details");
					return true;
				}
				if (arg2 == null) {
					message("%red%Usage: /%1 <player>", label);
					return true;
				}
				UUID id = getUUIDbyName(arg2);
				if (id == null) {
					message("%red%No such player: %white%%1", arg2);
					return true;
				}
				LinkedList<ChunkMark> list = plugin.utils.getChunkMarksByPlayer(id);
				if (list.size() < 1) {
					message("%red%Player %yellow%%1 %red%has no chunks marked", arg2);
					return true;
				}
				for (ChunkMark m : list)
					plugin.utils.deleteChunk(m);
				message("%gold%Deleted %yellow%%1's %white%%2%gold% chunks.", arg2, list.size());
				plugin.invokeSave();
				return true;
			} else if (arg1.equals("status")) {
				if (!checkPermission(Permissions.STATUS)) {
					message("%red%You do not have permission to use Spinner Status");
					return true;
				}
				int total = plugin.utils.chunks.size();
				int loaded = 0;
				for (ChunkMark m : plugin.utils.getChunks())
					if (plugin.utils.shouldRemainActive(m))
						loaded++;// Does not prevent duplicate chunks, but
									// whatevs

				LinkedList<UUID> players = new LinkedList<UUID>();
				for (ChunkMark m : plugin.utils.getChunks())
					if (!players.contains(m.owner))
						players.add(m.owner);
				message("%gold%Spinner is loading %white%%1%gold% of %white%%2%gold% total marked chunks by %white%%3%gold% players.",
						loaded, total, players.size());
				return true;
			} else if (arg1.equals("help")) {
				if (!checkPermission(Permissions.USE)) {
					message("%red%Sorry, you do not have permission to use Spinner");
					return true;
				}
				help();
				return true;
			}

			if (!isPlayer) {
				message("%red%This command shall not be executed through console.");
				return true;
			}
			// mark, enable/disable <tag>

			if (arg1.equals("mark")) {
				if (!checkPermission(Permissions.MARK)) {
					message("%red%You do not have permission to use Spinner");
					return true;
				}
				// check to see if it's already marked
				ChunkMark m = plugin.utils.lookup(player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ(),
						player.getWorld(), player.getUniqueId());
				if (m != null) {
					message("%red%This chunk is already marked");
					return true;
				}

				PlayerSettings ps = plugin.utils.getPlayerSettings(player);
				int cur = plugin.utils.getChunkMarksByPlayer(player.getUniqueId()).size();
				if (cur > ps.getMaxChunks(player)) {
					message("%red%You are not allowed to mark more than %white%%1%red% chunks.", ps.getMaxChunks(player));
					return true;
				}

				// mark chunk
				m = new ChunkMark(player.getUniqueId(), player.getLocation(), ps.activeGroup,
						checkPermission(Permissions.ALWAYS_LOADED) && ps.alwaysLoaded);
				plugin.utils.getChunks().add(m);
				plugin.invokeSave();
				message("%gold%Chunk marked.");
				messageChunkinfo(m, true);
				return true;
			} else if (arg1.equals("unmark")) {
				if (!checkPermission(Permissions.MARK)) {
					message("%red%You do not have permission to use Spinner");
					return true;
				}
				ChunkMark m = plugin.utils.lookup(player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ(),
						player.getWorld(), player.getUniqueId());
				if (m == null) {
					message("%red%This chunk is not marked");
					return true;
				}
				plugin.utils.deleteChunk(m);
				message("%gold%Chunk has been unmarked");
				plugin.invokeSave();
				return true;
			} else if (arg1.equals("enable")) {
				if (!checkPermission(Permissions.USE)) {
					message("%red%You do not have permission to use Spinner");
					return true;
				}
				if (arg2 == null) {
					message("%red%Usage: %1 %2 <group>", label, arg1);
					return true;
				}
				if (!plugin.utils.groupExists(player.getUniqueId(), arg2)) {
					message("%red%No such group: %white%%1", arg2);
					return true;
				}
				if (plugin.utils.isGroupEnabled(player.getUniqueId(), arg2)) {
					message("%red%That group is already enabled");
					return true;
				}
				plugin.utils.setGroupEnabled(player.getUniqueId(), arg2, true);
				message("%gold%Group %white%%1%gold% has been enabled", arg2);
				plugin.invokeSave();
				return true;
			} else if (arg1.equals("disable")) {
				if (!checkPermission(Permissions.USE)) {
					message("%red%You do not have permission to use Spinner");
					return true;
				}
				if (arg2 == null) {
					message("%red%Usage: %1 %2 <group>", label, arg1);
					return true;
				}
				if (!plugin.utils.groupExists(player.getUniqueId(), arg2)) {
					message("%red%No such group: %white%%1", arg2);
					return true;
				}
				if (!plugin.utils.isGroupEnabled(player.getUniqueId(), arg2)) {
					message("%red%That group is already disabled");
					return true;
				}
				plugin.utils.setGroupEnabled(player.getUniqueId(), arg2, false);
				message("%gold%Group %white%%1%gold% has been disabled", arg2);
				plugin.invokeSave();
				return true;
			} else if (arg1.equals("group")) {
				if (!checkPermission(Permissions.USE)) {
					message("%red%You do not have permission to use Spinner");
					return true;
				}
				boolean valid = validName(arg2);

				message("%gold%Current group %gray%%1", (!valid) ? plugin.utils.getPlayerSettings(player).activeGroup : arg2);
				if (arg2 == null) {
					message("%red%Usage /%1 <groupname>", label);
				} else if (!valid)
					message("%red%Invalid group name", label);
				else
					plugin.utils.getPlayerSettings(player).activeGroup = arg2;

				return true;
			} else if (arg1.startsWith("always")) {
				if (!checkPermission(Permissions.ALWAYS_LOADED)) {
					message("%red%You do not have permission to use Always Loaded");
					return true;
				}
				if (arg2 == null || !(arg2.equalsIgnoreCase("true") || arg2.equalsIgnoreCase("false"))) {
					message("%red%Usage /%1 %2 <true|false> <groupname>", label, arg1);
					return true;
				}
				boolean alwaysLoaded = arg2.equalsIgnoreCase("true");
				if (arg3 != null)
					if (!plugin.utils.groupExists(player.getUniqueId(), arg3)) {
						message("%red%No such group: %white%%1", arg3);
					} else {
						plugin.utils.setAlwaysLoaded(player.getUniqueId(), arg3, alwaysLoaded);
						message("%gold%Always Loaded for group %white%%1%gold% has been set to %white%%2", arg3, arg2);
					}
				else {
					plugin.utils.getPlayerSettings(player).alwaysLoaded = alwaysLoaded;
					if (alwaysLoaded)
						message("%gold%Newly marked chunks will now be always loaded");
					else
						message("%gold%Newly marked chunks will only be loaded when you are online");

				}

				return true;
			} else if (arg1.startsWith("follow")) {
				if (!checkPermission(Permissions.MARK)) {
					message("%red%You do not have permission to use Spinner");
					return true;
				}
				PlayerSettings ps = plugin.utils.getPlayerSettings(player);
				int cur = plugin.utils.getChunkMarksByPlayer(player.getUniqueId()).size();
				if (cur > ps.getMaxChunks(player)) {
					message("%red%You are not allowed to mark more than %white%%1%red% chunks.", ps.getMaxChunks(player));
					return true;
				}
				// follow player
				ps.following = true;
				message("%gold%Walk around to mark chunks..");
				return true;
			} else if (arg1.startsWith("unfollow")) {
				if (!checkPermission(Permissions.MARK)) {
					message("%red%You do not have permission to use Spinner");
					return true;
				}
				// unfollow player
				plugin.utils.getPlayerSettings(player).following = false;
				message("%gold%I've lost your trace");
				return true;
			} else if (arg1.startsWith("info")) {
				if (!checkPermission(Permissions.USE)) {
					message("%red%You do not have permission to use Spinner");
					return true;
				}
				int x = player.getLocation().getChunk().getX(), z = player.getLocation().getChunk().getZ();
				ChunkMark[] marks = plugin.utils.lookupMultiple(x, z, player.getLocation().getWorld());
				if (marks.length == 0) {
					message("%gold%Chunk %white%(%1,%white%%2)%gold% at %white%(%3,%4)%gold% is not marked.", x, z, x * 16, z * 16);
					return true;
				}

				if (marks.length == 1) {
					if (marks[0].owner.equals(player.getUniqueId()))
						message("%gold%You have marked this chunk");
					else
						message("%gold%This chunk has been marked by %white%%1", getPlayerbyUUID(marks[0].owner));
					messageChunkinfo(marks[0], false);
					return true;
				}
				message("%gold%This chunk has been marked by %white%%1 %gold%players.", marks.length);
				for (ChunkMark m : marks) {
					messageChunkinfo(m, true);
				}
				return true;
			} else if (arg1.equals("clear")) {
				if (!checkPermission(Permissions.USE)) {
					message("%red%Sorry, you do not have permission to use Spinner");
					return true;
				}
				if (arg2 == null) {
					plugin.utils.clearPlayer(player.getUniqueId());
					message("%gold%Chunk marks cleared.");
				} else {
					// look for group...
					if (!plugin.utils.groupExists(player.getUniqueId(), arg2)) {
						message("%gold%No such group: %white%%1", arg2);
						return true;
					}
					plugin.utils.clearGroup(player.getUniqueId(), arg2);
					message("%gold%Group %white%%1%gold% has been cleared.", arg2);
				}
				plugin.invokeSave();
				return true;
				// XXX Listbygroup
			} else if (arg1.equals("listbygroup")) {
				if (arg2 == null) {
					message("%red%Usage: /%1 %2 <group>%3", label, arg1, (checkPermission(Permissions.LIST_OTHERS)) ? " (player)" : "");
					return true;
				}

				if (arg3 != null && !NumberUtils.isNumber(arg3)) {
					// check permissions
					if (!checkPermission(Permissions.LIST_OTHERS)) {
						message("%red%Sorry, you do not have permission to list other player's details");
						return true;
					}
					// find the player
					UUID id = getUUIDbyName(arg3);
					if (id == null) {
						message("%red%No such player exists: %green%%1", arg3);
						return true;
					}
					if (!plugin.utils.groupExists(id, arg2)) {
						message("%red%No such group for player %white%%1%red%: %white%%2", arg3, arg2);
						return true;
					}
					LinkedList<ChunkMark> list = plugin.utils.getChunkMarksByGroup(id, arg2);
					if (list.size() < 1) {
						message("%gold%Player has no chunks marked under group %white%%1", arg2);
						return true;
					}
					PageBuilder p = ChatFormat.getPageBuilder();
					for (ChunkMark m : list)
						p.addPlainMessage(getChunkinfoMsg(m, false));
					p.send(player, ChatFormat.format("%gold%Total marked by %yellow%%3%gold% chunks in group %white%%1%gold%: %white%%2",
							arg2, list.size(), arg3), arg1 + " " + arg2 + " " + arg3, arg4);
					return true;
				}
				if (!checkPermission(Permissions.USE)) {
					message("%red%Sorry, you do not have permission to use Spinner");
					return true;
				}
				// list for player
				LinkedList<ChunkMark> list = plugin.utils.getChunkMarksByPlayer(player.getUniqueId());
				if (!plugin.utils.groupExists(player.getUniqueId(), arg2)) {
					message("%red%No such group: %white%%1", arg2);
					return true;
				}
				list = plugin.utils.getChunkMarksByGroup(player.getUniqueId(), arg2);
				if (list.size() < 1) {
					message("%gold%No chunks marked under group %white%%1", arg2);
					return true;
				}
				PageBuilder p = ChatFormat.getPageBuilder();
				for (ChunkMark m : list)
					p.addPlainMessage(getChunkinfoMsg(m, false));
				p.send(player, ChatFormat.format("%gold%Total marked chunks in group %white%%1%gold%: %white%%2", arg2, list.size()),
						arg1 + " " + arg2, arg3);
				return true;
			} else if (arg1.startsWith("listgroup")) {

				if (arg2 != null && !NumberUtils.isNumber(arg2)) {
					// check permissions
					if (!checkPermission(Permissions.LIST_OTHERS)) {
						message("%red%Sorry, you do not have permission to list other player's details");
						return true;
					}
					// find the player
					UUID id = getUUIDbyName(arg2);
					if (id == null) {
						message("%red%No such player exists: %green%%1", arg2);
						return true;
					}
					LinkedList<String> list = plugin.utils.getGroupsByPlayer(id);
					if (list.size() < 1) {
						message("%gold%Player has no chunks marked under group %white%%1", arg2);
						return true;
					}
					PageBuilder p = ChatFormat.getPageBuilder();
					for (String m : list)
						p.addFormattedMessage("%gray%%1 %2", m,
								(plugin.utils.isGroupEnabled(id, m)) ? ChatColor.GREEN + "Enabled" : "Disabled");
					p.send(player, ChatFormat.format("%gold%Total groups by %yellow%%1%gold%: %white%%2", arg2, list.size()),
							arg1 + " " + arg2, arg3);
					return true;
				}
				if (!checkPermission(Permissions.USE)) {
					message("%red%Sorry, you do not have permission to use Spinner");
					return true;
				}
				LinkedList<String> list = plugin.utils.getGroupsByPlayer(player.getUniqueId());
				if (list.size() < 1) {
					message("%gold%Player has no chunks marked under group %white%%1", arg2);
					return true;
				}
				PageBuilder p = ChatFormat.getPageBuilder();
				for (String m : list)
					p.addFormattedMessage("%gray%%1 %2", m,
							(plugin.utils.isGroupEnabled(player.getUniqueId(), m)) ? ChatColor.GREEN + "Enabled" : "Disabled");
				p.send(player, ChatFormat.format("%gold%Total groups: %white%%1", list.size()), arg1, arg2);
				return true;
			}

			return false;
		}

		/*
		 *
		 * 
		 * 
		 * 
		 * listbygroup <group> [player] listgroups [player] help
		 * 
		 * 
		 */
		private void help() {
			PageBuilder b = ChatFormat.getPageBuilder();
			if (checkPermission(Permissions.MARK)) {
				b.addFormattedMessage("%gold%/%1 mark:%white% Marks chunk at your location to stay loaded", label);
				b.addFormattedMessage("%gold%/%1 unmark:%white% Unmarks chunk at your location", label);
				b.addFormattedMessage("%gold%/%1 follow:%white% Marks chunks as you walk around", label);
				b.addFormattedMessage("%gold%/%1 unfollow:%white% Stop marking chunks", label);
			}

			if (checkPermission(Permissions.LIST_OTHERS))
				b.addFormattedMessage("%gold%/%1 list %gray%[player]%gold%:%white% Lists marked chunks, %gray%player%gold% is optional", label);
			else if (checkPermission(Permissions.USE))
				b.addFormattedMessage("%gold%/%1 list:%white% Lists your marked chunks", label);
			if (checkPermission(Permissions.USE)) {
				b.addFormattedMessage("%gold%/%1 info:%white% Displays information about your location", label);
				b.addFormattedMessage(
						"%gold%/%1 clear %gray%[group]%gold%:%white% Clears all marked chunks, or %gray%group%white% if specified", label);
				b.addFormattedMessage(
						"%gold%/%1 group %gray%[group name]%gold%:%white% Sets the group to use, or leave blank to display current group",
						label);

				b.addFormattedMessage("%gold%/%1 enable <group>:%white% Enables group", label);
				b.addFormattedMessage("%gold%/%1 disable <group>:%white% Disabled group. Disabled chunks will not be loaded", label);
			}
			if (checkPermission(Permissions.LIST_OTHERS)) {
				b.addFormattedMessage(
						"%gold%/%1 listbygroup <group> %gray%[player]%gold%:%white% Lists chunks from specified group from specified player (optional)",
						label);
				b.addFormattedMessage("%gold%/%1 listgroups %gray%[player]%yellow%:%white% Lists groups of specified player (optional)",
						label);

			}
			if (checkPermission(Permissions.ALWAYS_LOADED))
				b.addFormattedMessage("%gold%/%1 always:%white% Use to toggle always loaded, or loaded when you're online", label);
			if (checkPermission(Permissions.RELOAD))
				b.addFormattedMessage("%gold%/%1 reload:%white% Reloads config, and max chunk permissions", label);
			if (checkPermission(Permissions.CLEARPLAYER))
				b.addFormattedMessage("%gold%/%1 clearplayer <player>:%white% Clears <players> marked chunks", label);
			if (checkPermission(Permissions.STATUS))
				b.addFormattedMessage("%gold%/%1 status:%white% Displays status of plugin with great info such as loaded chunks", label);

			if (checkPermission(Permissions.USE))
				b.addFormattedMessage("%gold%/%1 help %gray%[n]%yellow%:%white% Displays this help page", label);

			b.send(sender, ChatFormat.format("%red%S%blue%p%gray%i%yellow%nn%green%e%gold%r %lpurple%H%yellow%e%green%l%daqua%p"), label + " " + args[0],
					(args.length > 1) ? args[1] : null, true);

		}
	}

	protected static boolean validName(String name) {
		return name != null && name.length() > 2 && name.length() < 24 && !name.matches("(?i).*[^a-z0-9_].*");
	}
}
