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
import soot.jimple.infoflow.android.source.data.ISourceSinkDefinitionProvider;
import soot.jimple.infoflow.android.source.data.SourceSinkDefinition;

/**
 * Parser of the permissions to method map from the University of Toronto (PScout)
 * 
 * @author Siegfried Rasthofer
 */
public class PScoutPermissionMethodParser implements ISourceSinkDefinitionProvider {
	private static final int INITIAL_SET_SIZE = 10000;

	private Set<SourceSinkDefinition> sourceList = null;
	private Set<SourceSinkDefinition> sinkList = null;
	private Set<SourceSinkDefinition> neitherList = null;

	private final String fileName;
	private final String regex = "^<(.+):\\s*(.+)\\s+(.+)\\s*\\((.*)\\)>.+?(->.+)?$";
	private final boolean SET_IMPLICIT_SOURCE_TO_SOURCE = false;
	private final boolean SET_INDIRECT_SINK_TO_SINK = false;
	
	public PScoutPermissionMethodParser(String filename){
		this.fileName = filename;
	}
	
	private void parse() {
		sourceList = new HashSet<SourceSinkDefinition>(INITIAL_SET_SIZE);
		sinkList = new HashSet<SourceSinkDefinition>(INITIAL_SET_SIZE);
		neitherList = new HashSet<SourceSinkDefinition>(INITIAL_SET_SIZE);
		
		BufferedReader rdr = readFile();
		
		String line = null;
		Pattern p = Pattern.compile(regex);
		String currentPermission = null;
		
		try {
			while ((line = rdr.readLine()) != null) {
				if(line.startsWith("Permission:"))
					currentPermission = line.substring(11);
				else{
					Matcher m = p.matcher(line);
					if(m.find()) {
						AndroidMethod singleMethod = parseMethod(m, currentPermission);
						if (singleMethod != null) {
							if (singleMethod.isSource())
								addToList(sourceList, singleMethod, currentPermission);
							else if (singleMethod.isSink())
								addToList(sinkList, singleMethod, currentPermission);
							else if (singleMethod.isNeitherNor())
								addToList(neitherList, singleMethod, currentPermission);
						}
					}
				}
			}
			
			if (rdr != null)
				rdr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	private void addToList(Set<SourceSinkDefinition> sourceList,
			AndroidMethod singleMethod, String currentPermission) {
		SourceSinkDefinition def = new SourceSinkDefinition(singleMethod);
		if (!sourceList.add(def)) {
			for (SourceSinkDefinition ssdef : sourceList)
				if (ssdef.getMethod().equals(singleMethod)) {
					singleMethod.addPermission(currentPermission);
					break;
				}	
		}
	}

	@Override
	public Set<SourceSinkDefinition> getSources() {
		if (sourceList == null || sinkList == null)
			parse();
		return this.sourceList;
	}
	
	@Override
	public Set<SourceSinkDefinition> getSinks() {
		if (sourceList == null || sinkList == null)
			parse();
		return this.sinkList;
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
	
	private AndroidMethod parseMethod(Matcher m, String currentPermission) {
		assert(m.group(1) != null && m.group(2) != null && m.group(3) != null 
				&& m.group(4) != null);
		AndroidMethod singleMethod;
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
		
		//permissions
		Set<String> permissions = new HashSet<String>();
		permissions.add(currentPermission);
		
		//create method signature
		singleMethod = new AndroidMethod(methodName, methodParameters, returnType, className, permissions);
		
		if(m.group(5) != null){
			String targets = m.group(5).substring(3);
			
			for(String target : targets.split(" "))
				if(target.startsWith("_SOURCE_")){
					singleMethod.setSource(true);
					if(target.contains("|")){
						String cat = target.substring(target.indexOf('|')+1);
						singleMethod.setCategory(returnCorrectCategory(cat));
					}
				}
				else if(target.startsWith("_SINK_")){
					singleMethod.setSink(true);
					if(target.contains("|")){
						String cat = target.substring(target.indexOf('|')+1);
						singleMethod.setCategory(returnCorrectCategory(cat));
					}
				}
				else if(target.equals("_NONE_"))
					singleMethod.setNeitherNor(true);
				else if(target.startsWith("_IMPSOURCE_")){
					if(SET_IMPLICIT_SOURCE_TO_SOURCE){
						singleMethod.setSource(true);
						if(target.contains("|")){
							String cat = target.substring(target.indexOf('|')+1);
							singleMethod.setCategory(returnCorrectCategory(cat));
						}
					}
					else
						singleMethod.setNeitherNor(true);
				}
				else if(target.startsWith("_INDSINK_")){
					if(SET_INDIRECT_SINK_TO_SINK){
						singleMethod.setSink(true);
						if(target.contains("|")){
							String cat = target.substring(target.indexOf('|')+1);
							singleMethod.setCategory(returnCorrectCategory(cat));
						}
					}
					else
						singleMethod.setNeitherNor(true);
				}
				else if(target.equals("_IGNORE_"))
					return null;
				else if(target.startsWith("-")){
					String cat = target.substring(target.indexOf('|')+1);
					singleMethod.setCategory(returnCorrectCategory(cat));
				}
				else
					throw new RuntimeException("error in target definition");
		}
		
		
		return singleMethod;
	}
	
	private CATEGORY returnCorrectCategory(String category){
		if(category.equals("_NO_CATEGORY_"))
			return CATEGORY.NO_CATEGORY;
		else if(category.equals("_HARDWARE_INFO_"))
			return CATEGORY.HARDWARE_INFO;
		else if(category.equals("_NFC_"))
			return CATEGORY.NFC;
		else if(category.equals("_PHONE_CONNECTION_"))
			return CATEGORY.PHONE_CONNECTION;
		else if(category.equals("_INTER_APP_COMMUNICATION_"))
			return CATEGORY.INTER_APP_COMMUNICATION;
		else if(category.equals("_VOIP_"))
			return CATEGORY.VOIP;
		else if(category.equals("_CONTACT_INFORMATION_"))
			return CATEGORY.CONTACT_INFORMATION;
		else if(category.equals("_UNIQUE_IDENTIFIER_"))
			return CATEGORY.UNIQUE_IDENTIFIER;
		else if(category.equals("_PHONE_STATE_"))
			return CATEGORY.PHONE_STATE;
		else if(category.equals("_SYSTEM_SETTINGS_"))
			return CATEGORY.SYSTEM_SETTINGS;
		else if(category.equals("_LOCATION_INFORMATION_"))
			return CATEGORY.LOCATION_INFORMATION;
		else if(category.equals("_NETWORK_INFORMATION_"))
			return CATEGORY.NETWORK_INFORMATION;
		else if(category.equals("_EMAIL_"))
			return CATEGORY.EMAIL;
		else if(category.equals("_SMS_MMS_"))
			return CATEGORY.SMS_MMS;
		else if(category.equals("_CALENDAR_INFORMATION_"))
			return CATEGORY.CALENDAR_INFORMATION;
		else if(category.equals("_ACCOUNT_INFORMATION_"))
			return CATEGORY.ACCOUNT_INFORMATION;
		else if(category.equals("_BLUETOOTH_"))
			return CATEGORY.BLUETOOTH;
		else if(category.equals("_ACCOUNT_SETTINGS_"))
			return CATEGORY.ACCOUNT_SETTINGS;
		else if(category.equals("_VIDEO_"))
			return CATEGORY.VIDEO;
		else if(category.equals("_AUDIO_"))
			return CATEGORY.AUDIO;
		else if(category.equals("_SYNCHRONIZATION_DATA_"))
			return CATEGORY.SYNCHRONIZATION_DATA;
		else if(category.equals("_NETWORK_"))
			return CATEGORY.NETWORK;
		else if(category.equals("_EMAIL_SETTINGS_"))
			return CATEGORY.EMAIL_SETTINGS;
		else if(category.equals("_EMAIL_INFORMATION_"))
			return CATEGORY.EMAIL_INFORMATION;
		else if(category.equals("_IMAGE_"))
			return CATEGORY.IMAGE;
		else if(category.equals("_FILE_INFORMATION_"))
			return CATEGORY.FILE_INFORMATION;
		else if(category.equals("_BLUETOOTH_INFORMATION_"))
			return CATEGORY.BLUETOOTH_INFORMATION;
		else if(category.equals("_BROWSER_INFORMATION_"))
			return CATEGORY.BROWSER_INFORMATION;
		else if(category.equals("_FILE_"))
			return CATEGORY.FILE;
		else if(category.equals("_VOIP_INFORMATION_"))
			return CATEGORY.VOIP_INFORMATION;
		else if(category.equals("_DATABASE_INFORMATION_"))
			return CATEGORY.DATABASE_INFORMATION;
		else if(category.equals("_PHONE_INFORMATION_"))
			return CATEGORY.PHONE_INFORMATION;
		else if(category.equals("_LOG_"))
			return CATEGORY.LOG;
		else
			throw new RuntimeException("The category -" + category + "- is not supported!");
	}

	@Override
	public Set<SourceSinkDefinition> getAllMethods() {
		Set<SourceSinkDefinition> sourcesSinks = new HashSet<>(sourceList.size()
				+ sinkList.size() + neitherList.size());
		sourcesSinks.addAll(sourceList);
		sourcesSinks.addAll(sinkList);
		sourcesSinks.addAll(neitherList);
		return sourcesSinks;
	}
}
