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
package heros.fieldsens;

import heros.fieldsens.structs.WrappedFact;
import heros.fieldsens.structs.WrappedFactAtStatement;
import heros.utilities.DefaultValueMap;


public class SourceStmtAnnotatedMethodAnalyzer<Field, Fact, Stmt, Method>
		implements MethodAnalyzer<Field, Fact, Stmt, Method> {

	private Method method;
	private DefaultValueMap<Key<Fact, Stmt>, PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method>> perSourceAnalyzer = 
			new DefaultValueMap<Key<Fact, Stmt>, PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method>>() {
		@Override
		protected PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> createItem(Key<Fact, Stmt> key) {
			return new PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method>(method, key.fact, context);
		}
	};
	private Context<Field, Fact, Stmt, Method> context;
	private Synchronizer<Stmt> synchronizer;
	
	public SourceStmtAnnotatedMethodAnalyzer(Method method, Context<Field, Fact, Stmt, Method> context, Synchronizer<Stmt> synchronizer) {
		this.method = method;
		this.context = context;
		this.synchronizer = synchronizer;
	}
	
	@Override
	public void addIncomingEdge(CallEdge<Field, Fact, Stmt, Method> incEdge) {
		WrappedFact<Field, Fact, Stmt, Method> calleeSourceFact = incEdge.getCalleeSourceFact();
		Key<Fact, Stmt> key = new Key<Fact, Stmt>(calleeSourceFact.getFact(), null);
		PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer = perSourceAnalyzer.getOrCreate(key);
		analyzer.addIncomingEdge(incEdge);		
	}

	@Override
	public void addInitialSeed(Stmt startPoint, Fact val) {
		Key<Fact, Stmt> key = new Key<Fact, Stmt>(val, startPoint);
		perSourceAnalyzer.getOrCreate(key).addInitialSeed(startPoint);
	}

	@Override
	public void addUnbalancedReturnFlow(final WrappedFactAtStatement<Field, Fact, Stmt, Method> target, final Stmt callSite) {
		synchronizer.synchronizeOnStmt(callSite, new Runnable() {
			@Override
			public void run() {
				Key<Fact, Stmt> key = new Key<Fact, Stmt>(context.zeroValue, callSite);
				perSourceAnalyzer.getOrCreate(key).scheduleUnbalancedReturnEdgeTo(target);				
			}
		});
	}
	
	public static interface Synchronizer<Stmt> {
		void synchronizeOnStmt(Stmt stmt, Runnable job);
	}
	
	private static class Key<Fact, Stmt> {
		private Fact fact;
		private Stmt stmt;

		private Key(Fact fact, Stmt stmt) {
			this.fact = fact;
			this.stmt = stmt;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((fact == null) ? 0 : fact.hashCode());
			result = prime * result + ((stmt == null) ? 0 : stmt.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Key other = (Key) obj;
			if (fact == null) {
				if (other.fact != null)
					return false;
			} else if (!fact.equals(other.fact))
				return false;
			if (stmt == null) {
				if (other.stmt != null)
					return false;
			} else if (!stmt.equals(other.stmt))
				return false;
			return true;
		}
	}
}