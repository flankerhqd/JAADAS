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

import static com.google.common.collect.Sets.newHashSet;
import heros.FlowFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * Represents the union of a set of flow functions.
 */
public class Union<D> implements FlowFunction<D> {
	
	private final FlowFunction<D>[] funcs;

	private Union(FlowFunction<D>... funcs){
		this.funcs = funcs;
	} 

	public Set<D> computeTargets(D source) {
		Set<D> res = newHashSet();
		for (FlowFunction<D> func : funcs) {
			res.addAll(func.computeTargets(source));
		}
		return res;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <D> FlowFunction<D> union(FlowFunction<D>... funcs) {
		List<FlowFunction<D>> list = new ArrayList<FlowFunction<D>>();
		for (FlowFunction<D> f : funcs) {
			if(f!=Identity.v()) {
				list.add(f);
			}
		}
		if(list.size()==1) return list.get(0);
		else if(list.isEmpty()) return Identity.v();
		return new Union(list.toArray(new FlowFunction[list.size()]));
	}
	
}
