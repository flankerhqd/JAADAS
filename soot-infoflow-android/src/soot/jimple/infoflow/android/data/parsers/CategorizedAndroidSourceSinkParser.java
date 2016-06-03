/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package soot.jimple.infoflow.android.data.parsers;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.android.data.AndroidMethod.CATEGORY;

/**
 * This parser parses the categorized sources and sink file (only one a time) for specific categories.
 * 
 * @author Siegfried Rasthofer
 */
public class CategorizedAndroidSourceSinkParser{
	private Set<CATEGORY> categories;
	private final String fileName;
	private boolean isSources;
	private boolean isSinks;
	
	private final String regex = "^<(.+):\\s*(.+)\\s+(.+)\\s*\\((.*)\\)>.+?\\((.+)\\)$";
	
	public CategorizedAndroidSourceSinkParser(Set<CATEGORY> categories, String filename, boolean isSources, boolean isSinks){
		this.categories = categories;
		this.fileName = filename;
		this.isSources = isSources;
		this.isSinks = isSinks;
	}

	
	public Set<AndroidMethod> parse() throws IOException {
		Set<AndroidMethod> methods = new HashSet<AndroidMethod>();
		
		BufferedReader rdr = readFile();
		if (rdr == null)
			throw new RuntimeException("Could not read source/sink file");
		
		String line = null;
		Pattern p = Pattern.compile(regex);
		
		while ((line = rdr.readLine()) != null) {
			Matcher m = p.matcher(line);
			if(m.find()) {
				CATEGORY cat = CATEGORY.valueOf(m.group(5));
				
				if(categories.contains(CATEGORY.ALL) || categories.contains(cat)){
					AndroidMethod method = parseMethod(m);
					method.setCategory(cat);
					
					if(isSources)
						method.setSource(true);
					else if(isSinks)
						method.setSink(true);
					else
						throw new RuntimeException("Oops, something went all wonky!");
					
					methods.add(method);
				}
			}
		}
		
		try {
			if (rdr != null)
				rdr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return methods;
	}
		
	private BufferedReader readFile(){
		FileReader fr = null;
		BufferedReader br = null;
		try {
			fr = new FileReader(fileName);
			br = new BufferedReader(fr);
		}catch(FileNotFoundException ex){
			ex.printStackTrace();
		} 
		
		return br;
	}
	
	private AndroidMethod parseMethod(Matcher m) {
		assert(m.group(1) != null && m.group(2) != null && m.group(3) != null 
				&& m.group(4) != null);
		int groupIdx = 1;
		
		//class name
		String className = m.group(groupIdx++).trim();
		
		//return type
		String returnType = m.group(groupIdx++).trim();

		
		//method name
		String methodName = m.group(groupIdx++).trim();
		
		//method parameter
		List<String> methodParameters = new ArrayList<String>();
		String params = m.group(groupIdx++).trim();
		if (!params.isEmpty())
			for (String parameter : params.split(","))
				methodParameters.add(parameter.trim());
	
		
		//create method signature
		return new AndroidMethod(methodName, methodParameters, returnType, className);
	}


}
