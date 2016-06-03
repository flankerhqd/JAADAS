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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.util.List;

import heros.fieldsens.CallEdgeResolver;
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

import com.google.common.collect.Lists;

public class ReturnSiteResolverTest {

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
	private Statement returnSite;
	private ReturnSiteResolver<String, TestFact, Statement, TestMethod> sut;
	private TestFact fact;
	private InterestCallback<String, TestFact, Statement, TestMethod> callback;
	private Resolver<String, TestFact, Statement, TestMethod> callEdgeResolver;

	@Before
	public void before() {
		analyzer = mock(PerAccessPathMethodAnalyzer.class);
		returnSite = new Statement("returnSite");
		sut = new ReturnSiteResolver<String, TestFact, Statement, TestMethod>(mock(FactMergeHandler.class), analyzer, returnSite);
		fact = new TestFact("value");
		callback = mock(InterestCallback.class);
		callEdgeResolver = mock(CallEdgeResolver.class);
	}

	@Test
	public void emptyIncomingFact() {
		sut.addIncoming(new WrappedFact<String, TestFact, Statement, TestMethod>(fact, createAccessPath(), callEdgeResolver), callEdgeResolver, getDelta());
		verify(analyzer).scheduleEdgeTo(eq(new WrappedFactAtStatement<String, TestFact, Statement, TestMethod>(returnSite, new WrappedFact<String, TestFact, Statement, TestMethod>(fact, createAccessPath(), sut))));
		assertTrue(sut.isInterestGiven());
	}

	@Test
	public void resolveViaIncomingFact() {
		sut.resolve(getDeltaConstraint("a"), callback);
		sut.addIncoming(new WrappedFact<String, TestFact, Statement, TestMethod>(fact, createAccessPath("a"), callEdgeResolver), callEdgeResolver, getDelta());
		verify(callback).interest(eq(analyzer), argThat(new ReturnSiteResolverArgumentMatcher(createAccessPath("a"))));
	}

	@Test
	public void registerCallbackAtIncomingResolver() {
		Resolver<String, TestFact, Statement, TestMethod> resolver = mock(Resolver.class);
		sut.addIncoming(new WrappedFact<String, TestFact, Statement, TestMethod>(fact, createAccessPath(), resolver), callEdgeResolver, getDelta());
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
		
		sut.addIncoming(new WrappedFact<String, TestFact, Statement, TestMethod>(fact, createAccessPath(), resolver), callEdgeResolver, getDelta());
		sut.resolve(getDeltaConstraint("a"), callback);
		
		verify(callback).interest(eq(analyzer), argThat(new ReturnSiteResolverArgumentMatcher(createAccessPath("a"))));
	}
	
	@Test
	public void resolveViaLateInterestAtIncomingResolver() {
		Resolver<String, TestFact, Statement, TestMethod> resolver = mock(Resolver.class);
		final Resolver<String, TestFact, Statement, TestMethod> nestedResolver = mock(Resolver.class);
		final List<InterestCallback> callbacks = Lists.newLinkedList();
		
		Mockito.doAnswer(new Answer(){
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				InterestCallback<String, TestFact, Statement, TestMethod> argCallback = 
						(InterestCallback<String, TestFact, Statement, TestMethod>) invocation.getArguments()[1];
				callbacks.add(argCallback);
				return null;
			}
		}).when(resolver).resolve(eq(getDeltaConstraint("a")), any(InterestCallback.class));
		
		sut.addIncoming(new WrappedFact<String, TestFact, Statement, TestMethod>(fact, createAccessPath(), resolver), callEdgeResolver, getDelta());
		sut.resolve(getDeltaConstraint("a"), callback);
		
		verify(callback, never()).interest(any(PerAccessPathMethodAnalyzer.class), any(Resolver.class));
		
		assertEquals(1, callbacks.size());
		Resolver transitiveResolver = mock(Resolver.class);
		callbacks.get(0).interest(analyzer, transitiveResolver);
		verify(callback).interest(eq(analyzer), argThat(new ReturnSiteResolverArgumentMatcher(createAccessPath("a"))));
	}
	
	@Test
	public void resolveViaDelta() {
		sut.addIncoming(new WrappedFact<String, TestFact, Statement, TestMethod>(fact, createAccessPath(), callEdgeResolver), callEdgeResolver, getDelta("a"));
		sut.resolve(getDeltaConstraint("a"), callback);
		verify(callback).interest(eq(analyzer), argThat(new ReturnSiteResolverArgumentMatcher(createAccessPath("a"))));
	}
	
	@Test
	public void resolveViaDeltaTwice() {
		final InterestCallback<String, TestFact, Statement, TestMethod> innerCallback = mock(InterestCallback.class);
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				ReturnSiteResolver<String, TestFact, Statement, TestMethod> resolver = (ReturnSiteResolver<String, TestFact, Statement, TestMethod>) invocation.getArguments()[1];
				resolver.resolve(getDeltaConstraint("b"), innerCallback);
				return null;
			}
		}).when(callback).interest(eq(analyzer), argThat(new ReturnSiteResolverArgumentMatcher(createAccessPath("a"))));
		
		sut.addIncoming(new WrappedFact<String, TestFact, Statement, TestMethod>(fact, createAccessPath(), callEdgeResolver), callEdgeResolver, getDelta("a", "b"));
		sut.resolve(getDeltaConstraint("a"), callback);
		
		verify(innerCallback).interest(eq(analyzer), argThat(new ReturnSiteResolverArgumentMatcher(createAccessPath("a", "b"))));
	}
	
	@Test
	public void resolveViaDeltaAndThenViaCallSite() {
		final InterestCallback<String, TestFact, Statement, TestMethod> innerCallback = mock(InterestCallback.class);
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				ReturnSiteResolver<String, TestFact, Statement, TestMethod> resolver = (ReturnSiteResolver<String, TestFact, Statement, TestMethod>) invocation.getArguments()[1];
				resolver.resolve(getDeltaConstraint("b"), innerCallback);
				return null;
			}
		}).when(callback).interest(eq(analyzer), argThat(new ReturnSiteResolverArgumentMatcher(createAccessPath("a"))));
		
		sut.addIncoming(new WrappedFact<String, TestFact, Statement, TestMethod>(fact, createAccessPath(), callEdgeResolver), callEdgeResolver, getDelta("a"));
		sut.resolve(getDeltaConstraint("a"), callback);
		verify(innerCallback).canBeResolvedEmpty();
	}

	@Test
	public void resolveViaCallEdgeResolverAtCallSite() {
		sut.addIncoming(new WrappedFact<String, TestFact, Statement, TestMethod>(fact, createAccessPath(), callEdgeResolver), callEdgeResolver, getDelta());
		sut.resolve(getDeltaConstraint("a"), callback);
		verify(callback).canBeResolvedEmpty();
	}
	
	@Test
	public void resolveViaResolverAtCallSite() {
		Resolver<String, TestFact, Statement, TestMethod> resolver = mock(Resolver.class);
		sut.addIncoming(new WrappedFact<String, TestFact, Statement, TestMethod>(fact, createAccessPath(), callEdgeResolver), resolver, getDelta());
		sut.resolve(getDeltaConstraint("a"), callback);
		verify(resolver).resolve(eq(getDeltaConstraint("a")), any(InterestCallback.class));
	}
	
	@Test
	public void resolveViaResolverAtCallSiteTwice() {
		Resolver<String, TestFact, Statement, TestMethod> resolver = mock(Resolver.class);
		final Resolver<String, TestFact, Statement, TestMethod> nestedResolver = mock(Resolver.class);
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				InterestCallback innerCallback = (InterestCallback) invocation.getArguments()[1];
				innerCallback.interest(analyzer, nestedResolver);
				return null;
			}
		}).when(resolver).resolve(eq(getDeltaConstraint("a")), any(InterestCallback.class));
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				InterestCallback innerCallback = (InterestCallback) invocation.getArguments()[1];
				innerCallback.interest(analyzer, nestedResolver);
				return null;
			}
		}).when(nestedResolver).resolve(eq(getDeltaConstraint("b")), any(InterestCallback.class));
		
		final InterestCallback<String, TestFact, Statement, TestMethod> secondCallback = mock(InterestCallback.class);
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				ReturnSiteResolver<String, TestFact, Statement, TestMethod> resolver = (ReturnSiteResolver) invocation.getArguments()[1];
				resolver.resolve(getDeltaConstraint("b"), secondCallback);
				return null;
			}
			
		}).when(callback).interest(eq(analyzer), argThat(new ReturnSiteResolverArgumentMatcher(createAccessPath("a"))));
		
		sut.addIncoming(new WrappedFact<String, TestFact, Statement, TestMethod>(fact, createAccessPath(), callEdgeResolver), resolver, getDelta());
		sut.resolve(getDeltaConstraint("a"), callback);
		
		verify(secondCallback).interest(eq(analyzer), argThat(new ReturnSiteResolverArgumentMatcher(createAccessPath("a", "b"))));
	}
	
	@Test
	public void resolveAsEmptyViaIncomingResolver() {
		Resolver<String, TestFact, Statement, TestMethod> resolver = mock(Resolver.class);
		Delta<String> delta = new AccessPath<String>().getDeltaTo(new AccessPath<String>().appendExcludedFieldReference(new String("a")));
		
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				InterestCallback innerCallback = (InterestCallback) invocation.getArguments()[1];
				innerCallback.canBeResolvedEmpty();
				return null;
			}
		}).when(resolver).resolve(eq(getDeltaConstraint("a")), any(InterestCallback.class));

		sut.addIncoming(new WrappedFact<String, TestFact, Statement, TestMethod>(fact, createAccessPath(), resolver), callEdgeResolver, delta);
		sut.resolve(getDeltaConstraint("a"), callback);
		
		verify(callback, never()).canBeResolvedEmpty();
		verify(callback, never()).interest(any(PerAccessPathMethodAnalyzer.class), any(Resolver.class));
	}
	
	@Test
	public void resolveViaCallSiteResolver() {
		Resolver<String, TestFact, Statement, TestMethod> resolver = mock(Resolver.class);
		
		sut.addIncoming(new WrappedFact<String, TestFact, Statement, TestMethod>(fact, createAccessPath(), callEdgeResolver), resolver, getDelta());
		sut.resolve(getDeltaConstraint("a"), callback);
		
		verify(resolver).resolve(eq(getDeltaConstraint("a")), any(InterestCallback.class));
	}
	
	private class ReturnSiteResolverArgumentMatcher extends
			ArgumentMatcher<ReturnSiteResolver<String, TestFact, Statement, TestMethod>> {

		private AccessPath<String> accPath;

		public ReturnSiteResolverArgumentMatcher(AccessPath<String> accPath) {
			this.accPath = accPath;
		}

		@Override
		public boolean matches(Object argument) {
			ReturnSiteResolver resolver = (ReturnSiteResolver) argument;
			return resolver.isInterestGiven() && resolver.getResolvedAccessPath().equals(accPath) && resolver.getReturnSite().equals(returnSite);
		}
	}
}
