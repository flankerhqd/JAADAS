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
package heros;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A utility class for creating default seeds that cause an analysis to simply start at a given statement.
 * This is useful if seeding is performed entirely through flow functions as used to be the case in 
 * earlier versions of Heros.
 */
public class DefaultSeeds {
	
	public static <N,D> Map<N,Set<D>> make(Iterable<N> units, D zeroNode) {
		Map<N,Set<D>> res = new HashMap<N, Set<D>>();
		for (N n : units) {
			res.put(n, Collections.singleton(zeroNode));
		}
		return res;
	}

}
