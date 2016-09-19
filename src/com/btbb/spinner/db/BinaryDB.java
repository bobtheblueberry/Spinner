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
package com.btbb.spinner.db;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;

import com.btbb.spinner.ChunkMark;
import com.btbb.spinner.ChunkUtils.EnableTag;
import com.btbb.spinner.SpinnerPlugin;

/**
 * Crappy binary file database
 * 
 * @author Serge Humphrey
 *
 */
public class BinaryDB {

	String filename = "chunks.bin";
	SpinnerPlugin plugin;

	public BinaryDB(SpinnerPlugin plugin) {
		this.plugin = plugin;
	}

	public void load() throws IOException {
		File f = getFile();
		if (!f.exists())
			return;
		plugin.utils.getChunks().clear();
		StreamDecoder in = new StreamDecoder(f);
		in.read4();
		in.read4();
		in.read4();
		
		int mSize;
		HashMap<EnableTag,Boolean> map = new HashMap<EnableTag,Boolean>(mSize=in.read4());
		for (int i = 0; i < mSize; i++) {
			UUID id = new UUID(in.read8(),in.read8());
			map.put(new EnableTag(id,in.readStr()), (in.read() > 0) ? Boolean.TRUE : Boolean.FALSE);
		}
		plugin.utils.setEnableMap(map);
		
		ChunkMark[] marks = new ChunkMark[in.read4()];
		for (int i = 0; i < marks.length; i++) {
			UUID id = new UUID(in.read8(),in.read8());
			int x = in.read4(), z = in.read4();
			String world = in.readStr();
			String group = in.readStr();
			boolean loadAlways = (in.read() > 0) ? true : false;
			marks[i] = new ChunkMark(id, x, z, Bukkit.getWorld(world), group, loadAlways);
		}
		plugin.utils.setChunks(new ArrayList<ChunkMark>(Arrays.asList(marks)));
		in.close();
		
		//purge old data
		plugin.utils.deleteEmptyGroups();
	}

	public void save() throws IOException {
		StreamEncoder en = new StreamEncoder(getFile());
		en.write4(11);
		en.write4(0);
		en.write4(0);

		HashMap<EnableTag, Boolean> map = plugin.utils.getEnableMap();
		en.write4(map.size());
		Iterator<Entry<EnableTag, Boolean>> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Entry<EnableTag, Boolean> e = it.next();
			en.write8(e.getKey().owner.getMostSignificantBits());
			en.write8(e.getKey().owner.getLeastSignificantBits());
			en.writeStr(e.getKey().name);
			en.write((e.getValue()) ? 1 : 0);
		}
		ArrayList<ChunkMark> marks = plugin.utils.getChunks();
		en.write4(marks.size());

		for (int i = 0; i < marks.size(); i++) {
			ChunkMark m = marks.get(i);
			en.write8(m.owner.getMostSignificantBits());
			en.write8(m.owner.getLeastSignificantBits());
			en.write4(m.location.x);
			en.write4(m.location.z);
			en.writeStr(m.location.world.getName());
			en.writeStr(m.group);
			en.write(m.isLoadAlways() ? 1 : 0);
		}
		en.close();
	}

	protected File getFile() {
		return new File(plugin.getDataFolder(), filename);
	}
}
