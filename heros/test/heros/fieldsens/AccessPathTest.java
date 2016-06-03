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

import static heros.fieldsens.AccessPath.PrefixTestResult.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import heros.fieldsens.AccessPath.Delta;
import heros.fieldsens.AccessPath.PrefixTestResult;

import org.junit.Test;

import com.google.common.collect.Sets;

public class AccessPathTest {

	public static AccessPath<String> ap(String ap) {
		Pattern pattern = Pattern.compile("(\\.|\\^)?([^\\.\\^]+)");
		Matcher matcher = pattern.matcher(ap);
		AccessPath<String> accessPath = new AccessPath<String>();
		boolean addedExclusions = false;
		
		while(matcher.find()) {
			String separator = matcher.group(1);
			String identifier = matcher.group(2);
			
			if(".".equals(separator) || separator == null) {
				if(addedExclusions)
					throw new IllegalArgumentException("Access path contains field references after exclusions.");
				accessPath = accessPath.append(identifier);
			} else {
				addedExclusions=true;
				String[] excl = identifier.split(",");
				accessPath = accessPath.appendExcludedFieldReference(excl);
			}
		}
		return accessPath;
	}
	
	@Test
	public void append() {
		AccessPath<String> sut = ap("a");
		assertEquals(ap("a.b"), sut.append("b"));
	}
	
	@Test
	public void addOnExclusion() {
		AccessPath<String> sut = ap("^a");
		assertEquals(ap("b"), sut.append("b"));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void addMergedFieldsOnSingleExclusion() {
		AccessPath<String> sut = ap("^a");
		sut.append("a");	
	}
	
	@Test
	public void prepend() {
		assertEquals(ap("c.a.b"), ap("a.b").prepend("c"));
	}
	
	@Test
	public void remove() {
		assertEquals(ap("b"), ap("a.b").removeFirst());
	}
	
	@Test
	public void deltaDepth1() {
		assertArrayEquals(new String[] { "b" }, ap("a").getDeltaTo(ap("a.b")).accesses);
	}
	
	@Test
	public void deltaDepth2() {
		assertArrayEquals(new String[] { "b", "c" }, ap("a").getDeltaTo(ap("a.b.c")).accesses);
	}
	
	@Test
	public void deltaOnNonEmptyAccPathsWithExclusions() {
		Delta<String> delta = ap("a^f").getDeltaTo(ap("a.b^g"));
		assertArrayEquals(new Object[] { "b" }, delta.accesses);
		assertEquals(Sets.newHashSet("g"), delta.exclusions);
	}
	
	@Test
	public void deltaOnPotentialPrefix() {
		assertEquals(Sets.newHashSet("f", "g"), ap("^f").getDeltaTo(ap("^g")).exclusions);
	}
	
	@Test
	public void emptyDeltaOnEqualExclusions() {
		AccessPath<String> actual = ap("^f");
		Object[] accesses = actual.getDeltaTo(ap("^f")).accesses;
		assertEquals(0, accesses.length);
		assertTrue(actual.getDeltaTo(ap("^f")).exclusions.equals(Sets.newHashSet("f")));
	}
	
	@Test
	public void multipleExclPrefixOfMultipleExcl() {
		assertEquals(PrefixTestResult.POTENTIAL_PREFIX, ap("^f,g").isPrefixOf(ap("^f,h")));
	}
	
	@Test
	public void testBaseValuePrefixOfFieldAccess() {
		assertEquals(GUARANTEED_PREFIX, ap("").isPrefixOf(ap("f")));
		assertEquals(NO_PREFIX, ap("f").isPrefixOf(ap("")));
	}
	
	@Test
	public void testBaseValueIdentity() {
		assertEquals(GUARANTEED_PREFIX, ap("").isPrefixOf(ap("")));
	}
	
	@Test
	public void testFieldAccessPrefixOfFieldAccess() {
		assertEquals(GUARANTEED_PREFIX, ap("b").isPrefixOf(ap("b.c")));
		assertEquals(NO_PREFIX, ap("b.c").isPrefixOf(ap("b")));
	}
	
	@Test
	public void testPrefixOfFieldAccessWithExclusion() {
		assertEquals(GUARANTEED_PREFIX,ap("^f").isPrefixOf(ap("g")));
		assertEquals(NO_PREFIX,ap("g").isPrefixOf(ap("^f")));
	}
	
	@Test
	public void testIdentityWithExclusion() {
		assertEquals(GUARANTEED_PREFIX,ap("^f").isPrefixOf(ap("^f")));
		assertEquals(GUARANTEED_PREFIX,ap("^f,g").isPrefixOf(ap("^f,g")));
	}
	
	@Test
	public void testDifferentExclusions() {
		assertEquals(POTENTIAL_PREFIX,ap("^f").isPrefixOf(ap("^g")));
	}
	
	@Test
	public void testMixedFieldAccess() {
		assertEquals(GUARANTEED_PREFIX,ap("^f").isPrefixOf(ap("g.g")));
		assertEquals(NO_PREFIX,ap("^f").isPrefixOf(ap("f.h")));
		assertEquals(GUARANTEED_PREFIX,ap("f").isPrefixOf(ap("f^g")));
	}
	
	@Test
	public void testMultipleExclusions() {
		assertEquals(NO_PREFIX,ap("^f,g").isPrefixOf(ap("^f")));
		assertEquals(POTENTIAL_PREFIX,ap("^f,h").isPrefixOf(ap("^f,g")));
		assertEquals(NO_PREFIX,ap("^f,g").isPrefixOf(ap("^g")));
		assertEquals(GUARANTEED_PREFIX,ap("^f").isPrefixOf(ap("^f,g")));
	}

	@Test
	public void testDifferentAccessPathLength() {
		assertEquals(GUARANTEED_PREFIX,ap("^f").isPrefixOf(ap("g.h")));
	}
	
	@Test
	public void testExclusionRequiresFieldAccess() {
		assertEquals(GUARANTEED_PREFIX,ap("").isPrefixOf(ap("^f")));
		assertEquals(NO_PREFIX, ap("^f").isPrefixOf(ap("")));
		
		assertEquals(GUARANTEED_PREFIX,ap("f").isPrefixOf(ap("f^g")));
		assertEquals(NO_PREFIX,ap("f^g").isPrefixOf(ap("f")));
		
		assertEquals(GUARANTEED_PREFIX,ap("f").isPrefixOf(ap("f^g^h")));
		assertEquals(NO_PREFIX,ap("f^g^h").isPrefixOf(ap("f")));
	}
	
}
