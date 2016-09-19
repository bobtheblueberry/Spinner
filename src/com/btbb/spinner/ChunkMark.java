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

import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

public class ChunkMark {

	public final UUID owner;
	public final ChunkLocation location;
	public final String group;

	boolean alwaysLoaded;

	/**
	 * 
	 * Creates a new ChunkMark. Chunk Marks have groups so they can be
	 * enabled/disabled based on group name.
	 * 
	 * @param owner
	 *            Player who owns this chunk mark
	 * @param l
	 *            Chunk location
	 * @param group
	 *            Name of chunk group
	 * @param loadAlways
	 *            when set to false, the chunk will only be loaded when the
	 *            player who marked it is online
	 */

	public ChunkMark(UUID owner, Location l, String group, boolean loadAlways) {
		this(owner, l.getChunk().getX(), l.getChunk().getZ(), l.getWorld(), group, loadAlways);
	}

	public ChunkMark(UUID owner, int x, int z, World world, String group, boolean loadAlways) {
		this.owner = owner;
		location = new ChunkLocation(x, z, world);
		this.group = group;
		this.alwaysLoaded = loadAlways;
	}

	public Chunk getChunk() {
		return location.world.getChunkAt(location.x, location.z);
	}

	public void setLoadAlways(boolean always) {
		this.alwaysLoaded = always;
	}

	public boolean isLoadAlways() {
		return alwaysLoaded;
	}

	public boolean isGroupEnabled() {
		return SpinnerPlugin.plugin.utils.isGroupEnabled(owner, group);
	}

	public class ChunkLocation {

		public final int x, z;
		public final World world;

		public ChunkLocation(int x, int z, World world) {
			this.x = x;
			this.z = z;
			this.world = world;
		}

		public boolean equals(Object o) {
			if (o == null)
				return false;
			if (!(o instanceof ChunkLocation))
				if ((o instanceof Chunk))
					return ((Chunk) o).getX() == this.x && ((Chunk) o).getZ() == this.z
							&& ((Chunk) o).getWorld().equals(this.world);
				else
					return false;
			ChunkLocation c = (ChunkLocation) o;
			return c.x == this.x && c.z == this.z && c.world.equals(world);
		}
	}

	public boolean equals(Object o) {
		if (o == null || !(o instanceof ChunkMark))
			return false;
		ChunkMark other = (ChunkMark) o;
		return other.alwaysLoaded == this.alwaysLoaded && other.location.equals(this.location)
				&& other.group.equals(this.group) && other.owner.equals(this.owner);
	}

	public String toString() {
		return "ChunkMark (" + location.world.getName() + "," + location.x + "," + location.z + ") (" + group + ","
				+ alwaysLoaded + ")";
	}
}
