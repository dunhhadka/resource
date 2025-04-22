package com.example.storesecurity.cache;

import java.util.function.Supplier;

public interface Cache<K, V> {
    boolean put(K key, V value);

    V get(K key);

    boolean isEmpty();

    int size();

    void clear();

    V getOrInsert(K key, Supplier<V> supplier);
}
