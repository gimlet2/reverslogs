package com.restmonkeys.logger.collections;

import java.util.LinkedHashMap;
import java.util.Map;

public class LimitedSizeMap<K, V> extends LinkedHashMap<K, V> {

    public static final int DEFAULT_MAX_SIZE = 100;
    private long maxSize = DEFAULT_MAX_SIZE;

    public LimitedSizeMap() {
    }

    public LimitedSizeMap(long maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }

}
