package org.k33nteam.jade.solver.model;


import java.util.HashMap;
import java.util.Map;

class IntentSourceType
{
	public final static int UNKNOWN = 0x0;//
	public final static int FROM_PARAM = 0x1;
	public final static int FROM_GET_INTENT = 0x3;
	public final static int FROM_NEW_ALLOC_SITE = 0x4;
	public final static int FROM_CALL_RETURN_SITE = 0x5;
	//use &0x1 to judge if intent may be potential dangerous
	
	public final static int EMPTY = 0x0;
	public final static int NOT_NULL = 0x1;
	public final static int NULL = 0x2;
	public final static int UNDECIDED = 0x4;
	public final static int ABSOLUTELY_NOT_NULL = 0x8;
	public final static int ABSOLUTELY_NULL = 0x10;
	
	public static int[] mergeMapping = new int[64];
	public static int[] intersetMapping = new int[64];
	public static String[] statusStringMapping = new String[64];
	static{
		for (int i = 0; i < mergeMapping.length; i++) {
			mergeMapping[i] = -1;
		}
		for (int i = 0; i < intersetMapping.length; i++) {
			intersetMapping[i] = -1;
		}
		for (int i = 0x8; i < 0x10; i++) {
			mergeMapping[i]= ABSOLUTELY_NOT_NULL; 
		}
		for (int i = 0x10; i < 0x20; i++) {
			mergeMapping[i]= ABSOLUTELY_NULL; 
		}
		for (int i = 0; i <= 0x10;) {
			mergeMapping[i] = i;
			i*=2;
		}
		
		mergeMapping[NOT_NULL | NULL] = UNDECIDED;
		mergeMapping[NOT_NULL | UNDECIDED] = NOT_NULL;
		mergeMapping[NULL | UNDECIDED ] = NULL;
		
		for (int i = 0x8; i < 0x10; i++) {
			intersetMapping[i]= ABSOLUTELY_NOT_NULL; 
		}
		for (int i = 0x10; i < 0x20; i++) {
			intersetMapping[i]= ABSOLUTELY_NULL; 
		}
		for (int i = 0; i <= 0x10;) {//empty
			intersetMapping[i] = i;
			i*=2;
		}
		
		statusStringMapping[EMPTY] = "EMPTY";
		statusStringMapping[NOT_NULL] = "NOT_NULL";
		statusStringMapping[NULL] = "NULL";
		statusStringMapping[UNDECIDED] = "UNDECIDED";
		statusStringMapping[ABSOLUTELY_NOT_NULL] = "ABSOLUTELY_NOT_NULL";
		statusStringMapping[ABSOLUTELY_NULL] = "ABSOLUTELY_NULL";
	}
	
	public static String translateStatus(int status)
	{
		return statusStringMapping[status];
	}
	
}
class IntentMetaData
{
	String name;
	String type;
	boolean isArray = false;
	
	public IntentMetaData()
	{
		name = "<unknown>";
		type = "<unknown>";
	}
	
	public IntentMetaData(String name, String type)
	{
		this.name = name;
		this.type = type;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof IntentMetaData)) {
			return false;
		}
		IntentMetaData rhs = (IntentMetaData)obj;

		return rhs.name.equals(this.name) && rhs.type.equals(this.type) && (rhs.isArray == this.isArray);
	}
}
class IntentData
{
	//notice bundle translate to "bundle_bundle"
	Map<IntentMetaData, Integer> intentData = new HashMap<>();
	Map<IntentMetaData, HashMap<IntentMetaData, Integer>> bundleData = new HashMap<>();//use jsonarray??
	
	public void merge(IntentData rhs)
	{
		for (IntentMetaData intentMetaData : rhs.intentData.keySet()) {
			if (this.intentData.containsKey(intentMetaData)) {
				//both have, careful here
				Integer rightStatus = rhs.intentData.get(intentMetaData);
				Integer leftStatus = this.intentData.get(intentMetaData);
				
				Integer finalStatus = IntentSourceType.mergeMapping[leftStatus | rightStatus];
				switch (finalStatus) {
				case IntentSourceType.EMPTY:
				case IntentSourceType.UNDECIDED:	
					break;
				default:
					this.intentData.put(intentMetaData, finalStatus);
					break;
				}
				
			}
			else {
				this.intentData.put(intentMetaData, rhs.intentData.get(intentMetaData));
			}
		}
	}
}