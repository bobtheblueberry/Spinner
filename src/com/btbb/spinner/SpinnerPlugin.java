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

import java.io.IOException;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;

import com.btbb.spinner.db.BinaryDB;

public class SpinnerPlugin extends JavaPlugin {

	BinaryDB database;
	public ChunkUtils utils;
	public static SpinnerPlugin plugin;
	SpinnerPlayerFollower follower;

	public void onEnable() {
		plugin = this;
		database = new BinaryDB(this);
		utils = new ChunkUtils(this);
		if (!getDataFolder().exists())
			getDataFolder().mkdirs();
		getCommand("spinner").setExecutor(new SpinnerCommands(this));
		getServer().getPluginManager().registerEvents(new ChunkListener(this), this);
		invokeLoad();
		invokePlayerFollower();
		utils.enable();
	}

	public void onDisable() {
		utils.disable();
	}

	public void invokeLoad() {
		getServer().getScheduler().runTask(this, new Runnable() {

			@Override
			public void run() {
				try {
					database.load();
				} catch (IOException e) {
					System.err.println("Cannot load database");
					e.printStackTrace();
				}
			}
		});
	}

	public void invokePlayerFollower() {
		if (follower != null)
			follower.cancel();
		follower = new SpinnerPlayerFollower(this);
		follower.runTaskTimer(this, 200, 100);
	}

	public void invokeSave() {
		getServer().getScheduler().runTask(this, new Runnable() {

			@Override
			public void run() {
				try {
					database.save();
				} catch (IOException e) {
					System.err.println("Cannot load database");
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public void reloadConfig() {
		super.reloadConfig();
		utils.reload();
	}

	protected boolean checkPermission(CommandSender sender, String perm) {
		if (!(sender instanceof Player))
			return true;
		Player p = (Player) sender;
		if (p.isOp())
			return true;
		return p.hasPermission(perm);
	}

	protected int getMaxChunks(Player player) {
		int max_chunks = -1;
		for (PermissionAttachmentInfo perm : player.getEffectivePermissions()) {
			String permString = perm.getPermission();
			try {
				if (permString.startsWith(Permissions.MAX_CHUNKS)) {
					String[] amount = permString.split("\\.");
					max_chunks = Math.max(Integer.parseInt(amount[2]), max_chunks);
				}
			} catch (Exception e) {
			}
		}
		return max_chunks;
	}

}
