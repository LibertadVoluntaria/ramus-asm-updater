package org.omnibot.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Ramus
 */
public class ExoMap<K, V> extends LinkedHashMap<K, V> implements Map<K, V> {

	private static final long serialVersionUID = -5419801265094933439L;

	public ExoMap() {
		super();
	}

	public ExoMap(final Map<? extends K, ? extends V> m) {
		super(m);
	}

	public V getAt(final int index) {
		final Collection<V> it = values();
		int i = 0;
		for (final V value : it) {
			if (i == index) {
				return value;
			}
			i++;
		}
		return null;
	}

	public V removeAt(final int index) {
		final Object[] keys = keySet().toArray();
		return remove(keys[index]);
	}

	public void set(final K key, final V value) {
		for (int i = 0; i < values().size(); i++) {
			if (getAt(i).equals(value)) {
				removeAt(i);
				put(key, value);
				break;
			}
		}
	}
}