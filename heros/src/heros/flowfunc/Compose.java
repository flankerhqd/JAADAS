/*******************************************************************************
 * Copyright (c) 2012 Eric Bodden.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Eric Bodden - initial API and implementation
 ******************************************************************************/
package heros.flowfunc;

import heros.FlowFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;


/**
 * Represents the ordered composition of a set of flow functions.
 */
public class Compose<D> implements FlowFunction<D> {
	
	private final FlowFunction<D>[] funcs;

	private Compose(FlowFunction<D>... funcs){
		this.funcs = funcs;
	} 

	public Set<D> computeTargets(D source) {
		Set<D> curr = Sets.newHashSet();
		curr.add(source);
		for (FlowFunction<D> func : funcs) {
			Set<D> next = Sets.newHashSet();
			for(D d: curr)
				next.addAll(func.computeTargets(d));
			curr = next;
		}
		return curr;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <D> FlowFunction<D> compose(FlowFunction<D>... funcs) {
		List<FlowFunction<D>> list = new ArrayList<FlowFunction<D>>();
		for (FlowFunction<D> f : funcs) {
			if(f!=Identity.v()) {
				list.add(f);
			}
		}
		if(list.size()==1) return list.get(0);
		else if(list.isEmpty()) return Identity.v();
		return new Compose(list.toArray(new FlowFunction[list.size()]));
	}
	
}
