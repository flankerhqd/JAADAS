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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import heros.fieldsens.CallEdgeResolver;
import heros.fieldsens.ControlFlowJoinResolver;
import heros.fieldsens.InterestCallback;
import heros.fieldsens.PerAccessPathMethodAnalyzer;
import heros.fieldsens.Resolver;
import heros.fieldsens.ReturnSiteResolver;
import heros.fieldsens.AccessPath.Delta;
import heros.fieldsens.structs.DeltaConstraint;
import heros.fieldsens.structs.WrappedFact;
import heros.fieldsens.structs.WrappedFactAtStatement;
import heros.utilities.Statement;
import heros.utilities.TestFact;
import heros.utilities.TestMethod;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.verification.VerificationMode;

public class ControlFlowJoinResolverTest {

	private static DeltaConstraint<String> getDeltaConstraint(String... fieldRefs) {
		return new DeltaConstraint<String>(getDelta(fieldRefs));
	}

	private static Delta<String> getDelta(String... fieldRefs) {
		AccessPath<String> accPath = createAccessPath(fieldRefs);
		return new AccessPath<String>().getDeltaTo(accPath);
	}

	protected static AccessPath<String> createAccessPath(String... fieldRefs) {
		AccessPath<String> accPath = new AccessPath<String>();
		for (String fieldRef : fieldRefs) {
			accPath = accPath.append(fieldRef);
		}
		return accPath;
	}

	private PerAccessPathMethodAnalyzer<String, TestFact, Statement, TestMethod> analyzer;
	private Statement joinStmt;
	private ControlFlowJoinResolver<String, TestFact, Statement, TestMethod> sut;
	private TestFact fact;
	private InterestCallback<String, TestFact, Statement, TestMethod> callback;
	private Resolver<String, TestFact, Statement, TestMethod> callEdgeResolver;

	@Before
	public void before() {
		analyzer = mock(PerAccessPathMethodAnalyzer.class);
		joinStmt = new Statement("joinStmt");
		sut = new ControlFlowJoinResolver<String, TestFact, Statement, TestMethod>(mock(FactMergeHandler.class), analyzer, joinStmt);
		fact = new TestFact("value");
		callback = mock(InterestCallback.class);
		callEdgeResolver = mock(CallEdgeResolver.class);
	}

	@Test
	public void emptyIncomingFact() {
		sut.addIncoming(new WrappedFact<String, TestFact, Statement, TestMethod>(fact, createAccessPath(), callEdgeResolver));
		verify(analyzer).processFlowFromJoinStmt(eq(new WrappedFactAtStatement<String, TestFact, Statement, TestMethod>(joinStmt, new WrappedFact<String, TestFact, Statement, TestMethod>(fact, createAccessPath(), sut))));
		assertTrue(sut.isInterestGiven());
	}

	@Test
	public void resolveViaIncomingFact() {
		sut.resolve(getDeltaConstraint("a"), callback);
		sut.addIncoming(new WrappedFact<String, TestFact, Statement, TestMethod>(fact, createAccessPath("a"), callEdgeResolver));
		verify(callback).interest(eq(analyzer), argThat(new ResolverArgumentMatcher(createAccessPath("a"))));
	}

	@Test
	public void registerCallbackAtIncomingResolver() {
		Resolver<String, TestFact, Statement, TestMethod> resolver = mock(Resolver.class);
		sut.addIncoming(new WrappedFact<String, TestFact, Statement, TestMethod>(fact, createAccessPath(), resolver));
		sut.resolve(getDeltaConstraint("a"), callback);
		verify(resolver).resolve(eq(getDeltaConstraint("a")), any(InterestCallback.class));
	}
	
	@Test
	public void resolveViaIncomingResolver() {
		Resolver<String, TestFact, Statement, TestMethod> resolver = mock(Resolver.class);
		final Resolver<String, TestFact, Statement, TestMethod> nestedResolver = mock(Resolver.class);
		Mockito.doAnswer(new Answer(){
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				InterestCallback<String, TestFact, Statement, TestMethod> argCallback = 
						(InterestCallback<String, TestFact, Statement, TestMethod>) invocation.getArguments()[1];
				argCallback.interest(analyzer, nestedResolver);
				return null;
			}
		}).when(resolver).resolve(eq(getDeltaConstraint("a")), any(InterestCallback.class));
		
		sut.addIncoming(new WrappedFact<String, TestFact, Statement, TestMethod>(fact, createAccessPath(), resolver));
		sut.resolve(getDeltaConstraint("a"), callback);
		
		verify(callback).interest(eq(analyzer), argThat(new ResolverArgumentMatcher(createAccessPath("a"))));
	}
	
	
	private class ResolverArgumentMatcher extends
			ArgumentMatcher<ReturnSiteResolver<String, TestFact, Statement, TestMethod>> {

		private AccessPath<String> accPath;

		public ResolverArgumentMatcher(AccessPath<String> accPath) {
			this.accPath = accPath;
		}

		@Override
		public boolean matches(Object argument) {
			ControlFlowJoinResolver resolver = (ControlFlowJoinResolver) argument;
			return resolver.isInterestGiven() && resolver.getResolvedAccessPath().equals(accPath) && resolver.getJoinStmt().equals(joinStmt);
		}
	}
}
