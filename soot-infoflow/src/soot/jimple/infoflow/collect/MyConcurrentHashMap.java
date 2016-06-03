package soot.jimple.infoflow.collect;

import java.util.concurrent.ConcurrentHashMap;

public class MyConcurrentHashMap<K, V> extends ConcurrentHashMap<K, V> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6591113627062569214L;
	
	/**
	 * Puts the new key/value-pair if no mapping for the given key was in the in
	 * the map before, otherwise it returns the existing mapping. 
	 * @param key The key to check and put if it is not already in the map
	 * @param value The value to put
	 * @return The old value if one was present in the map for the given key,
	 * otherwise, the newly registered value
	 */
	public V putIfAbsentElseGet(K key, V value) {
		V oldVal = this.putIfAbsent(key, value);
		return oldVal == null ? value : oldVal;
	}

}
