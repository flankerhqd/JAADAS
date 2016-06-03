/*******************************************************************************
 * Copyright (c) 2015 Johannes Lerch.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Johannes Lerch - initial API and implementation
 ******************************************************************************/
package heros.utilities;

import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Maps;

public class JsonDocument {
	
	private DefaultValueMap<String, JsonDocument> documents = new DefaultValueMap<String, JsonDocument>() {
		@Override
		protected JsonDocument createItem(String key) {
			return new JsonDocument();
		}
	};
	private DefaultValueMap<String, JsonArray> arrays = new DefaultValueMap<String, JsonArray>() {
		@Override
		protected JsonArray createItem(String key) {
			return new JsonArray();
		}
	};
	private Map<String, String> keyValuePairs = Maps.newHashMap();
	
	public JsonDocument doc(String key) {
		return documents.getOrCreate(key);
	}
	
	public JsonDocument doc(String key, JsonDocument doc) {
		if(documents.containsKey(key))
			throw new IllegalArgumentException("There is already a document registered for key: "+key);
		documents.put(key, doc);
		return doc;
	}
	
	public JsonArray array(String key) {
		return arrays.getOrCreate(key);
	}
	
	public void keyValue(String key, String value) {
		keyValuePairs.put(key, value);
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		write(builder, 0);
		return builder.toString();
	}
	
	public void write(StringBuilder builder, int tabs) {
		builder.append("{\n");
		
		for(Entry<String, String> entry : keyValuePairs.entrySet()) {
			tabs(tabs+1, builder); builder.append("\""+entry.getKey()+"\": \""+entry.getValue()+"\",\n");
		}
		
		for(Entry<String, JsonArray> entry : arrays.entrySet()) {
			tabs(tabs+1, builder); builder.append("\""+entry.getKey()+"\": ");
			entry.getValue().write(builder, tabs+1);
			builder.append(",\n");
		}
		
		for(Entry<String, JsonDocument> entry : documents.entrySet()) {
			tabs(tabs+1, builder); builder.append("\""+entry.getKey()+"\": ");
			entry.getValue().write(builder, tabs+1);
			builder.append(",\n");
		}
		
		if(!keyValuePairs.isEmpty() || !arrays.isEmpty() || !documents.isEmpty())
			builder.delete(builder.length()-2, builder.length()-1); 
		
		tabs(tabs, builder); builder.append("}");
	}

	static void tabs(int tabs, StringBuilder builder) {
		for(int i=0; i<tabs; i++)
			builder.append("\t");
	}
}