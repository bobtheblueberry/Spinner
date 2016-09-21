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

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.btbb.spinner.ChunkUtils.PlayerSettings;

public class SpinnerPlayerFollower extends BukkitRunnable {

	SpinnerPlugin plugin;

	public SpinnerPlayerFollower(SpinnerPlugin p) {
		this.plugin = p;
	}

	public void checkPlayer(Player player, PlayerSettings ps) {
		if (ps == null)
			return;
		Chunk c = player.getLocation().getChunk();
		ChunkMark m = plugin.utils.lookup(c.getX(), c.getZ(), c.getWorld(), player.getUniqueId());
		if (m != null)
			return;// already marked

		int cur = plugin.utils.getChunkMarksByPlayer(player.getUniqueId()).size();
		if (cur > ps.getMaxChunks(player)) {
			player.sendMessage(
					ChatFormat.format("%red%You are not allowed to mark more than %white%%1%red% chunks.", ps.getMaxChunks(player)));
			ps.following = false;
			return;
		}
		// mark chunk
		m = new ChunkMark(player.getUniqueId(), player.getLocation(), ps.activeGroup,
				plugin.checkPermission(player, Permissions.ALWAYS_LOADED) && ps.alwaysLoaded);
		plugin.utils.getChunks().add(m);
		plugin.invokeSave();
		player.sendMessage(
				ChatFormat.format("%gold%Chunk marked. Group: %white%%1%gold%, Always Loaded: %white%%2", ps.activeGroup, m.alwaysLoaded));
	}

	@Override
	public void run() {
		Iterator<Entry<UUID, PlayerSettings>> it = plugin.utils.playerSettings.entrySet().iterator();
		while (it.hasNext()) {
			Entry<UUID, PlayerSettings> en = it.next();
			if (en.getValue() == null)
				continue;
			if (en.getValue().following) {
				Player p = plugin.getServer().getPlayer(en.getKey());
				if (p != null)
					checkPlayer(p, en.getValue());
			}
		}
	}
}
