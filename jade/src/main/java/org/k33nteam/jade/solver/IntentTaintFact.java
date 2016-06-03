package org.k33nteam.jade.solver;

import org.k33nteam.jade.solver.model.IntentSource;
import soot.SootField;
import soot.SootMethod;
import soot.Value;
import soot.toolkits.scalar.Pair;

import java.util.HashMap;
import java.util.Map;


class DataObject implements Cloneable
{
	public static DataObject getRecurTail(DataObject object)
	{
		while (!object.clear) {
			object = object.data;
		}
		return object;
	}
	public static DataObject getRecurSecondTail(DataObject object)
	{
		DataObject second = null;
		while (!object.clear) {
			second = object;
			object = object.data;
		}
		return second;
	}
	public static DataObject getRecurThirdTail(DataObject object)
	{
		DataObject tail = getRecurTail(object);
		return tail.father.father;
	}
	String key;
	DataObject data;
	DataObject father;
	public void setStatus(int status) {
		this.status = status;
	}
	int status = 0x3;// top 0x1 empty, 0x2 not empyt 0x0 down

	public String getKey() {
		return key;
	}

	public void setKeyAndData(String key, DataObject data) {
		this.key = key;
		this.data = data;
		this.clear = false;
		data.father = this;
	}

	public boolean isClear() {
		return clear;
	}

	public DataObject getData() {
		return data;
	}

 	private DataObject() {
		
	}
 	public static DataObject newInstance()
 	{
 		return new DataObject();
 	}
	@Override
	protected DataObject clone()  {
		DataObject object = new DataObject();
		if (!this.clear) {
			object.key = this.key;
			object.data = this.data.clone();
			object.status = this.status;
			object.clear = false;
			object.data.father = object;
			object.father = this.father;
		}
		return object;
	}
	
	boolean clear = true;
	
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("[ ");
		builder.append(key);
		builder.append(" : ");
		builder.append(data == null?"null":data.toString());
		String statusString = "";
		switch (this.status) {
		case 0x0:
			statusString = "unknown";
			break;
		case 0x1:
			statusString = "empty";
			break;
		case 0x2:
			statusString = "non-empty";
			break;
		case 0x3:
			statusString = "full";
		default:
			break;
		}
		builder.append(", ").append(statusString);
		builder.append(" ]");
		return builder.toString();
	}
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof DataObject)) {
			return false;
		}
		DataObject rhs = (DataObject)obj;
		if (this.key == null && rhs.key == null) {
			return true;
		}
		else if(this.key != null && rhs.key != null) {
			return this.key.equals(rhs.key) && this.data.equals(rhs.data);
		}
		else {
			return false;
		}
	}
}
class MultipleDataObject implements Cloneable
{
	//
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
	//
	
	private int status = EMPTY;
	Map<String, MultipleDataObject> mapping = new HashMap<>();

	private static MultipleDataObject NEW_EMPTY_INSTANCE = new MultipleDataObject();
 	private MultipleDataObject() {
		
	}
 	public static MultipleDataObject newInstance()
 	{
 		return NEW_EMPTY_INSTANCE;
 	}
	@Override
	protected MultipleDataObject clone()  {
		MultipleDataObject dataObject = new MultipleDataObject();
		if (!this.clear) {
			for (String string : mapping.keySet()) {
				dataObject.mapping.put(string, mapping.get(string).clone());
			}
			dataObject.status = this.status;
		}
		return dataObject;
	}
	
	boolean clear = true;
	
	@Override
	public String toString()
	{
		return "[ " + this.mapping + " ]";
	}
}
class IntentTaintFact
{
	Map<Value, Pair<IntentSource, DataObject>> facts = new HashMap<>();
	static Map<SootMethod, IntentSource> sources = new HashMap<>();
	static Map<Integer, IntentSource> params = new HashMap<>();
	static Map<SootField, IntentSource> fields = new HashMap<>();
	
	public static void reset()
	{
		sources.clear();
		params.clear();
		fields.clear();
	}
	static IntentSource FROM_GET_INTENT = new IntentSource();
	public void killValue(Value Value)
	{
		facts.remove(Value);
	}
	
	public void copyValue(Value dest, Value src)
	{
		if (facts.containsKey(src)) {
			facts.put(dest, facts.get(src));
		}
		else {
			killValue(dest);
		}
	}
	public void copy(IntentTaintFact fact)
	{
		//this.sources.clear();
		this.facts.clear();
		this.merge(fact);
	}
	public void merge(IntentTaintFact fact)
	{
		//this.sources.putAll(fact.sources);
		this.facts.putAll(fact.facts);
	}
	public IntentSource getGetIntent()
	{
		return FROM_GET_INTENT;
	}
	
	public void putPair(Value Value, Pair<IntentSource, DataObject> pair)
	{
		facts.put(Value, pair);
	}
	
	public IntentSource getIntentSourceFromMethod(SootMethod method)
	{
		if (sources.containsKey(method)) {
			return sources.get(method);
		}
		else {
			sources.put(method, new IntentSource());
			return sources.get(method);
		}
	}
	
	public IntentSource getIntentSourceFromParam(Integer integer)
	{
		if (params.containsKey(integer)) {
			return params.get(integer);
		}
		else {
			params.put(integer, new IntentSource());
			return params.get(integer);
		}
	}
	public IntentSource getIntentSourceFromField(SootField field)
	{
		if (fields.containsKey(field)) {
			return fields.get(field);
		}
		else {
			fields.put(field, new IntentSource());
			return fields.get(field);
		}
	}
	public Pair<IntentSource, DataObject> getPairFromValue(Value value)
	{
		return facts.get(value);
	}
	
	public static DataObject getRecurTail(DataObject object)
	{
		while (!object.clear) {
			object = object.data;
		}
		return object;
	}
	
	public void clear()
	{
		//this.sources.clear();
		this.facts.clear();
	}

	@Override
	public String toString() {
		return "[ " + this.facts.toString() + ", " + sources.toString() + " ]";
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof IntentTaintFact && facts.equals(((IntentTaintFact) obj).facts);
	}
	
}