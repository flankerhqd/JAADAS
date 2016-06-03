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

import java.util.List;

import com.google.common.collect.Lists;

public class JsonArray {
	
	private List<String> items = Lists.newLinkedList();
	
	public void add(String item) {
		items.add(item);
	}

	public void write(StringBuilder builder, int tabs) {
		builder.append("[\n");
		for(String item: items) {
			JsonDocument.tabs(tabs+1, builder); builder.append("\""+item+"\",\n");
		}
		JsonDocument.tabs(tabs, builder); builder.append("]");
	}
}