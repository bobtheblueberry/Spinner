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
package com.evilmidget38;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class NameFetcher implements Callable<List<NameTimestampPair>> {
	private static final String PROFILE_URL = "https://api.mojang.com/user/profiles/";
	private final JSONParser jsonParser = new JSONParser();
	private final UUID id;

	public NameFetcher(UUID id) {
		this.id = id;
	}

	public List<NameTimestampPair> call() throws Exception {
		List<NameTimestampPair> namePairs = new LinkedList<NameTimestampPair>();
		HttpURLConnection connection = createConnection(id);
		JSONArray array = (JSONArray) jsonParser.parse(new InputStreamReader(connection.getInputStream()));
		for (Object profile : array) {
			JSONObject jsonProfile = (JSONObject) profile;
			String name = (String) jsonProfile.get("name");
			Object o = jsonProfile.get("changedToAt");
			Long changed = -1L;
			if (o != null)
				changed = (o instanceof Long) ? (long) o : Long.parseLong(o.toString());
			namePairs.add(new NameTimestampPair(name, changed));
		}

		return namePairs;
	}

	private static HttpURLConnection createConnection(UUID id) throws Exception {
		URL url = new URL(PROFILE_URL + id.toString().replaceAll("-", "") + "/names");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setUseCaches(false);
		connection.setDoInput(true);
		connection.setDoOutput(true);
		return connection;
	}
}