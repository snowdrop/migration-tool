package dev.snowdrop.mapper;

import java.util.LinkedHashMap;
import java.util.Map;

public class MapperUtils {
	/**
	 * Helper to create a Map that preserves insertion order.
	 * (Java's Map.of() does not guarantee order).
	 */
	public static Map<String, String> orderedMap(String... keyValues) {
		Map<String, String> map = new LinkedHashMap<>();
		for (int i = 0; i < keyValues.length; i += 2) {
			if (i + 1 < keyValues.length) {
				map.put(keyValues[i], keyValues[i + 1]);
			}
		}
		return map;
	}
}
