package org.k33nteam.jade.solver;

import com.bpodgursky.jbool_expressions.*;
import com.bpodgursky.jbool_expressions.rules.RuleSet;
import soot.Unit;
import soot.jimple.IfStmt;
import soot.toolkits.graph.UnitGraph;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/*
 * for if...else, we prefer incoming sources like getIntent rather than new Intent
 * 
 * resolve function on-the-fly, merge condition on-the-fly
 * do not consider < , > 
 */
class ConstrainInfo 
{
    Expression<IfStmt> expression;
    private static HashMap<IfStmt, Integer> mapping = new HashMap<>();
    private static int IDX = 0;

    public ConstrainInfo(ConstrainInfo in)
    {
        super();
        //this.expression = in.expression.makeCopy();
        this.expression = in.expression;
    }
    
    public ConstrainInfo()
    {
        super();
        this.expression = Literal.of(false);//设为true就会出现超集，一些情况在merge的时候被优化掉
    }
    
    public ConstrainInfo(boolean initFlow)
    {
        super();
        this.expression = Literal.of(true);
    }
    public void intersect(IfStmt stmt, boolean bool )
    {
    	if (mapping.get(stmt) == null) {
			mapping.put(stmt, IDX);
			++IDX;
		}
        Expression<IfStmt> variable = bool?Variable.of(stmt):Not.of(Variable.of(stmt));
        expression = And.of(expression, variable);
        //expression = RuleSet.simplify(expression);
    }

    public void union(ConstrainInfo info)
    {
        Expression rhs = info.expression;
        expression = Or.of(expression, rhs);
        //expression = RuleSet.simplify(expression);
    }
    
    public void copy(ConstrainInfo info)
    {
		//this.expression = info.expression.makeCopy();
    	this.expression = info.expression;
    }
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        if (this.expression == null) {
            builder.append("<<null>>");
        }
        else {
            builder.append("<( ").append(this.expression.toString()).append(" )>");
        }
        builder.append(" ");
        builder.append("\n");
        return builder.toString();
    }

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ConstrainInfo)) {
			return false;
		}
		ConstrainInfo rhs = (ConstrainInfo)obj;
		if(this.expression != null && rhs.expression != null)
		{
			return this.expression.equals(rhs.expression);
		}
		else return this.expression == null && rhs.expression == null;
		
	}
}
public class LocalConstraintFlowAnalysis extends ForwardMyBranchedFlowAnalysis<ConstrainInfo> {

	boolean shouldPrecise = false;
	public boolean isShouldPrecise() {
		return shouldPrecise;
	}

	//public final static IfStmt ENTRY_STMT = Jimple.v().newIfStmt(new JimpleLocal("zero", NullType.v()), new JIdentityStmt(new JimpleLocal("zero", NullType.v()), new JimpleLocal("zero", NullType.v())));
	public LocalConstraintFlowAnalysis(UnitGraph graph, boolean precise) {
		super(graph);
		doAnalysis();
		this.shouldPrecise = precise;
	}

	@Override
	protected void flowThrough(ConstrainInfo in, Unit s,
			List<ConstrainInfo> fallOut,
			List<ConstrainInfo> branchOuts) {
		//System.out.println("flow through: " + s);
		//System.out.println("in: "+in);
		ConstrainInfo out = new ConstrainInfo(in);
		ConstrainInfo outBranch = new ConstrainInfo(in);
		if (s instanceof IfStmt) {
			IfStmt stmt = (IfStmt)s;
			out.intersect(stmt, false);
			outBranch.intersect(stmt,true);

		}
		for( Iterator<ConstrainInfo> it = fallOut.iterator(); it.hasNext(); ) {
			//System.out.println("copying to fallout in flowthrough");
			copy(out, it.next());
		}
		for( Iterator<ConstrainInfo> it = branchOuts.iterator(); it.hasNext(); ) {
			//System.out.println("copying to branchout in flowthrough");
			copy( outBranch, it.next() );
		}
	}

	@Override
	protected void merge(ConstrainInfo in1,
			ConstrainInfo in2, ConstrainInfo out) {
		//System.out.println("merging in custom flow:");
		//System.out.println("in1: "+in1);
		//System.out.println("in2: "+in2);
		//System.out.println("oout: "+out);
		
		out.union(in1);
		out.union(in2);
		//System.out.println("out: " + out);
	}

	@Override
	protected void copy(ConstrainInfo source,
			ConstrainInfo dest) {
		//System.out.println("copying flow:");
		//System.out.println("source: " + source);
		//System.out.println("before: "+ dest);
		dest.copy(source);
		//dest.union(source);

		//System.out.println("after: "+dest);
	}

	@Override
	protected ConstrainInfo newInitialFlow() {
		//return this.shouldPrecise?new ConstrainInfo():new ConstrainInfo(true);
		return new ConstrainInfo();
	}

	@Override
	protected ConstrainInfo entryInitialFlow() {
		return new ConstrainInfo(true);
	}
	
	public String flowResult(Unit unit)
	{
		/*
		ConstrainInfo flowresult = this.getFlowBefore(unit);
		ConstrainInfo result = new ConstrainInfo();
		for(IfStmt stmt: flowresult.keySet())
		{
			if (!(flowresult.get(stmt).contains(true) && flowresult.get(stmt).contains(false))) {
				result.putAll(stmt, flowresult.get(stmt));
			}
		}
		return result;*/
		
		ConstrainInfo flowresult = this.getFlowBefore(unit);
		
		if (flowresult.expression != null) {
			return RuleSet.simplify(flowresult.expression).toString();
			//return flowresult.expression.toString();
		}
		else {
			return "<<null>>";
		}
	}

}
