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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Various utilities to do with chunk loading and ticking
 * 
 * @author Serge Humphrey
 *
 */
public class ChunkUtils implements Runnable {

	public static class EnableTag {
		final public String name;
		final public UUID owner;

		public EnableTag(UUID owner, String name) {
			this.name = name;
			this.owner = owner;
		}

		@Override
		public boolean equals(Object o) {
			return (o instanceof EnableTag) && ((EnableTag) o).name.equals(this.name)
					&& ((EnableTag) o).owner.equals(this.owner);
		}
		
		/**
		 * We have to modify this to make {@link java.util.HashMap} work correctly.
		 */
		@Override
		public int hashCode()
		{
			return name.hashCode() & owner.hashCode();
		}
		
		public String toString() {
			return "(" + this.name + "," + this.owner + ")";
		}
	}

	public class PlayerSettings {

		public PlayerSettings(boolean loadAlways) {
			this.alwaysLoaded = loadAlways;
		}

		public boolean alwaysLoaded;
		public String activeGroup = "default";
		public boolean following;

		private int maxChunks = -1;

		/**
		 * gets players max chunks
		 * 
		 * @param player
		 *            should be the same at matched UUID
		 * @return maximum allowed chunks for this player
		 */
		public int getMaxChunks(Player player) {
			if (maxChunks < 0)
				maxChunks = plugin.getMaxChunks(player);
			return maxChunks;
		}
	}

	SpinnerPlugin plugin;
	boolean enabled;
	protected final ArrayList<ChunkMark> chunks;
	protected final HashMap<EnableTag, Boolean> enableMap;
	protected final HashMap<UUID, PlayerSettings> playerSettings;

	public ChunkUtils(SpinnerPlugin plugin) {
		this.plugin = plugin;
		chunks = new ArrayList<ChunkMark>();
		enableMap = new HashMap<EnableTag, Boolean>();
		playerSettings = new HashMap<UUID, PlayerSettings>();
		plugin.getServer().getScheduler().runTaskLater(plugin, this, 10);
	}

	public void enable() {
		enabled = true;
	}

	public void disable() {
		enabled = false;
	}

	protected LinkedList<ChunkMark> getChunkMarksByPlayer(UUID player) {
		LinkedList<ChunkMark> list = new LinkedList<ChunkMark>();
		for (ChunkMark m : chunks)
			if (m.owner.equals(player))
				list.add(m);
		return list;
	}

	public LinkedList<String> getGroupsByPlayer(UUID player) {
		LinkedList<String> list = new LinkedList<String>();
		for (ChunkMark m : chunks)
			if (m.owner.equals(player) && !list.contains(m.group))
				list.add(m.group);
		return list;
	}

	protected LinkedList<ChunkMark> getChunkMarksByGroup(UUID player, String groupName) {
		LinkedList<ChunkMark> list = new LinkedList<ChunkMark>();
		for (ChunkMark m : chunks)
			if (m.owner.equals(player) && m.group.equals(groupName))
				list.add(m);
		return list;
	}

	public ArrayList<ChunkMark> getChunks() {
		return chunks;
	}

	public void setChunks(ArrayList<ChunkMark> marks) {
		this.chunks.clear();
		this.chunks.addAll(marks);
	}

	protected void updateChunks() {
		for (ChunkMark m : chunks) {
			if (shouldRemainActive(m)) {
				// World.getChunkAt() Actually loads the chunk.. weird
				Chunk c = m.getChunk();
				if (!c.isLoaded())
					c.load();
			}
		}
	}

	protected ChunkMark isChunkMarked(Chunk c) {
		for (ChunkMark m : chunks)
			if (m.location.equals(c))
				return m;
		return null;
	}

	protected boolean shouldRemainActive(ChunkMark m) {
		return (m.alwaysLoaded || isOnline(m.owner)) && isGroupEnabled(m.owner, m.group);
	}

	public boolean deleteChunk(ChunkMark m) {
		return chunks.remove(m);
	}

	protected boolean isOnline(UUID id) {
		Player p = plugin.getServer().getPlayer(id);
		return p != null && p.isOnline();
	}

	public boolean groupExists(UUID id, String groupName) {
		boolean answer = groupName.equals("default") || enableMap.containsKey(new EnableTag(id, groupName));
		if (answer)
			return true;
		// do a more thorough search..
		for (ChunkMark m : chunks)
			if (m.owner.equals(id) && m.group.equals(groupName))
				return true;
		return false;
	}

	public void setGroupEnabled(UUID id, String groupName, Boolean value) {
		enableMap.put(new EnableTag(id, groupName), value);		
	}

	public boolean isGroupEnabled(UUID id, String groupName) {
		Boolean val = enableMap.get(new EnableTag(id, groupName));
		return (val == null) ? true : val;
	}

	public void clearGroup(UUID player, String groupName) {
		for (ChunkMark m : new ArrayList<ChunkMark>(chunks))
			if (m.owner.equals(player) && m.group.equals(groupName))
				chunks.remove(m);
	}

	public void deleteEmptyGroups() {
		for (EnableTag et : new HashMap<EnableTag, Boolean>(enableMap).keySet())
			if (!groupExists(et.owner, et.name))
				enableMap.remove(et);
	}

	/**
	 * 
	 * @param x
	 *            chunk's co-ordinates, not the same as player world
	 *            co-ordinates
	 * @param z
	 *            chunk's co-ordinates, not the same as player world
	 *            co-ordinates
	 * @param world
	 * @return
	 */
	public ChunkMark lookup(int x, int z, World world, UUID owner) {
		for (ChunkMark m : chunks)
			if (m.location.x == x && m.location.z == z && m.location.world.equals(world) && m.owner.equals(owner))
				return m;
		return null;
	}

	/**
	 * 
	 * @param x
	 *            chunk's co-ordinates, not the same as player world
	 *            co-ordinates
	 * @param z
	 *            chunk's co-ordinates, not the same as player world
	 *            co-ordinates
	 * @param world
	 * @return
	 */
	public ChunkMark lookup(int x, int z, World world) {
		for (ChunkMark m : chunks)
			if (m.location.x == x && m.location.z == z && m.location.world.equals(world))
				return m;
		return null;
	}

	public ChunkMark[] lookupMultiple(int x, int z, World world) {
		LinkedList<ChunkMark> list = new LinkedList<ChunkMark>();
		for (ChunkMark m : chunks)
			if (m.location.x == x && m.location.z == z && m.location.world.equals(world))
				list.add(m);
		return list.toArray(new ChunkMark[list.size()]);
	}

	@Override
	public void run() {
		plugin.getServer().getScheduler().runTaskLater(plugin, this, 200);
		if (enabled)
			updateChunks();
	}

	public HashMap<EnableTag, Boolean> getEnableMap() {
		return enableMap;
	}

	public void setEnableMap(HashMap<EnableTag, Boolean> map) {
		this.enableMap.clear();
		this.enableMap.putAll(map);
	}

	public PlayerSettings getPlayerSettings(Player player) {
		if (!playerSettings.containsKey(player.getUniqueId()))
			playerSettings.put(player.getUniqueId(),
					new PlayerSettings(player.hasPermission(Permissions.ALWAYS_LOADED)));
		return playerSettings.get(player.getUniqueId());
	}

	public void setAlwaysLoaded(UUID player, String groupName, boolean alwaysLoaded) {
		for (ChunkMark m : chunks)
			if (m.owner.equals(player) && m.group.equals(groupName))
				m.setLoadAlways(alwaysLoaded);
	}

	public void clearPlayer(UUID player) {
		for (ChunkMark m : new ArrayList<ChunkMark>(chunks))
			if (m.owner.equals(player))
				chunks.remove(m);
	}

	protected void reload() {
		// reset some player settings
		for (PlayerSettings ps : playerSettings.values())
			ps.maxChunks = -1;
	}
}
