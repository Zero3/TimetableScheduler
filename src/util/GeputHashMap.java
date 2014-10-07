package util;

import java.util.HashMap;

public class GeputHashMap<K, V> extends HashMap<K, V>
{
	public V geput(K key, V newObject)
	{
		// If key is already in map, return existing value object
		V value = get(key);

		if (value != null)
		{
			return value;
		}

		// No? Then put the provided new object and return it as well
		put(key, newObject);
		return newObject;
	}
}