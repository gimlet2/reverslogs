package com.restmonkeys.logger.collections;

import java.util.ArrayList;
import java.util.Collection;

public class LimitedSizeList<T> extends ArrayList<T> {

    public static final int DEFAULT_MAX_SIZE = 100;
    private int maxSize = DEFAULT_MAX_SIZE;

    public LimitedSizeList(int maxSize) {
        this.maxSize = maxSize;
    }

    public LimitedSizeList() {
    }

    public LimitedSizeList(Collection<? extends T> c) {
        super(c);
    }

    @Override
    public boolean add(T t) {
        boolean result = super.add(t);
        if (this.size() >= maxSize) {
            removeFirst(size() - maxSize + 1);
        }
        return result;
    }

    private void removeFirst(int count) {
        removeRange(0, count);
    }

    @Override
    public void add(int index, T element) {
        super.add(index, element);
        if (this.size() >= maxSize) {
            removeFirst(size() - maxSize + 1);
        }
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean result = super.addAll(c);
        if (this.size() >= maxSize) {
            removeFirst(size() - maxSize + c.size());
        }
        return result;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        boolean result = super.addAll(index, c);
        if (this.size() >= maxSize) {
            removeFirst(size() - maxSize + c.size());
        }
        return result;
    }
}
