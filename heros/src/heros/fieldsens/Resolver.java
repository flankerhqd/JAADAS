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

import heros.fieldsens.FlowFunction.Constraint;

import java.util.List;

import com.google.common.collect.Lists;

public abstract class Resolver<Field, Fact, Stmt, Method> {

	private boolean interest = false;
	private List<InterestCallback<Field, Fact, Stmt, Method>> interestCallbacks = Lists.newLinkedList();
	protected PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer;
	private boolean canBeResolvedEmpty = false;
	
	public Resolver(PerAccessPathMethodAnalyzer<Field, Fact, Stmt, Method> analyzer) {
		this.analyzer = analyzer;
	}

	public abstract void resolve(Constraint<Field> constraint, InterestCallback<Field, Fact, Stmt, Method> callback);
	
	public void interest() {
		if(interest)
			return;

		log("Interest given");
		interest = true;
		for(InterestCallback<Field, Fact, Stmt, Method> callback : Lists.newLinkedList(interestCallbacks)) {
			callback.interest(analyzer, this);
		}
		
		if(canBeResolvedEmpty)
			interestCallbacks = null;
	}
	
	protected void canBeResolvedEmpty() {
		if(canBeResolvedEmpty)
			return;
		
		canBeResolvedEmpty = true;
		for(InterestCallback<Field, Fact, Stmt, Method> callback : Lists.newLinkedList(interestCallbacks)) {
			callback.canBeResolvedEmpty();
		}
		
		if(interest)
			interestCallbacks = null;
	}

	public boolean isInterestGiven() {
		return interest;
	}

	protected void registerCallback(InterestCallback<Field, Fact, Stmt, Method> callback) {
		if(interest) {
			callback.interest(analyzer, this);
		}
		else {
			log("Callback registered");
			interestCallbacks.add(callback);
		}

		if(canBeResolvedEmpty)
			callback.canBeResolvedEmpty();
	}
	
	protected abstract void log(String message);
	
}
