package org.k33nteam.jade.propagation.track;

import soot.Value;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;

public class TrackFact extends Pair<Value, Stmt> // first for currently tainting value, second for taint source value. Taint always made in AssignStmt via CallToReturn
{
	public TrackFact(Value value, Stmt originStmt)
	{
		super(value, originStmt);
	}	
}