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

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import com.btbb.spinner.ChunkUtils.PlayerSettings;

public class SpinnerListener implements Listener {

	SpinnerPlugin plugin;
	ChunkUtils utils;

	public SpinnerListener(SpinnerPlugin p, ChunkUtils utils) {
		this.plugin = p;
		this.utils = utils;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onChunkUnload(ChunkUnloadEvent event) {
		ChunkMark cm = utils.isChunkMarked(event.getChunk());
		if (cm == null || !utils.shouldRemainActive(cm))
			return;
		event.setCancelled(true);
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Location from = event.getFrom(), to = event.getTo();
		if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == from.getBlockZ())
			return;
		PlayerSettings ps = utils.playerSettings.get(event.getPlayer().getUniqueId());
		if (ps == null)
			return;
		if (ps.following && (ps.movement++ % 10) == 0) {
			Player player = event.getPlayer();
			ChunkMark m = utils.lookup(to.getChunk().getX(), to.getChunk().getZ(), to.getWorld(), player.getUniqueId());
			if (m != null)
				return;// already marked

			int cur = plugin.utils.getChunkMarksByPlayer(player.getUniqueId()).size();
			if (cur > ps.getMaxChunks(player)) {
				player.sendMessage(ChatFormat.format(
						"%red%You are not allowed to mark more than %white%%1%red% chunks.", ps.getMaxChunks(player)));
				ps.following = false;
				return;
			}
			// mark chunk
			m = new ChunkMark(player.getUniqueId(), player.getLocation(), ps.activeGroup,
					plugin.checkPermission(player, Permissions.ALWAYS_LOADED) && ps.alwaysLoaded);
			plugin.utils.getChunks().add(m);
			plugin.invokeSave();
			player.sendMessage(ChatFormat.format(
					"%gold%Chunk marked. Group: %white%%1%gold%, Always Loaded: %white%%2",
					ps.activeGroup, m.alwaysLoaded));
		}
	}
}
