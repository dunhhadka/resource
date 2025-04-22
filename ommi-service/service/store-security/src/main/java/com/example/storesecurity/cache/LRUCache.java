package com.example.storesecurity.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * An implementation of a thread-safe Least Recently Used (LRU) Cache.
 * Uses a HashMap for O(1) lookups and a LinkedList for tracking access order.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 */
public class LRUCache<K, V> implements Cache<K, V> {
    private final int size;
    private final Map<K, CacheElement<V>> linkedListNodeMap;
    private final LinkedList<K> doublyLinkedList;
    private final ReentrantLock lock;

    /**
     * Constructs an LRUCache with the specified size.
     *
     * @param size the maximum number of entries the cache can hold
     * @throws IllegalArgumentException if size is not positive
     */
    public LRUCache(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Cache size must be positive");
        }
        this.size = size;
        this.linkedListNodeMap = new HashMap<>(size);
        this.doublyLinkedList = new LinkedList<>();
        this.lock = new ReentrantLock();
    }

    /**
     * Adds a key-value pair to the cache. If the key exists, updates its value and moves it to the front.
     * If the cache is full, removes the least recently used item.
     *
     * @param key   the key to be added
     * @param value the value associated with the key
     * @return true if the operation is successful, false if the key is null
     */
    @Override
    public boolean put(K key, V value) {
        if (key == null) {
            return false;
        }
        return lockAndUnLock(() -> {
            CacheElement<V> element = new CacheElement<>(value);

            if (linkedListNodeMap.containsKey(key)) {
                doublyLinkedList.remove(key);
            } else if (doublyLinkedList.size() >= size) {
                evictElement();
            }

            doublyLinkedList.addFirst(key);
            linkedListNodeMap.put(key, element);
            return true;
        });
    }

    /**
     * Retrieves the value associated with the key and moves the key to the front.
     *
     * @param key the key to look up
     * @return the value associated with the key, or null if the key is not found or null
     */
    @Override
    public V get(K key) {
        if (key == null) {
            return null;
        }
        return lockAndUnLock(() -> {
            CacheElement<V> element = linkedListNodeMap.get(key);
            if (element != null) {
                doublyLinkedList.remove(key);
                doublyLinkedList.addFirst(key);
                return element.getValue();
            }
            return null;
        });
    }

    /**
     * Checks if the cache is empty.
     *
     * @return true if the cache is empty, false otherwise
     */
    @Override
    public boolean isEmpty() {
        return lockAndUnLock(linkedListNodeMap::isEmpty);
    }

    /**
     * Returns the current number of entries in the cache.
     *
     * @return the number of entries
     */
    @Override
    public int size() {
        return lockAndUnLock(linkedListNodeMap::size);
    }

    /**
     * Clears all entries in the cache.
     */
    @Override
    public void clear() {
        lockAndUnLock(() -> {
            linkedListNodeMap.clear();
            doublyLinkedList.clear();
            return null;
        });
    }

    @Override
    public V getOrInsert(K key, Supplier<V> supplier) {
        if (key == null) {
            return null;
        }

        return lockAndUnLock(() -> {
            var element = linkedListNodeMap.get(key);
            if (element != null) {
                doublyLinkedList.remove(key);
                doublyLinkedList.addFirst(key);
                return element.getValue();
            }

            var newValue = supplier.get();
            if (newValue == null) return null;

            if (this.size() >= this.size) {
                this.linkedListNodeMap.remove(doublyLinkedList.removeLast());
            }

            linkedListNodeMap.put(key, new CacheElement<>(newValue));
            doublyLinkedList.addFirst(key);

            return newValue;
        });
    }

    private void evictElement() {
        lockAndUnLock(() -> {
            K key = doublyLinkedList.removeLast();
            linkedListNodeMap.remove(key);
            return null;
        });
    }

    /**
     * Executes a supplier function within a locked context, ensuring thread-safety.
     *
     * @param supplier the function to execute
     * @param <T>      the return type of the supplier
     * @return the result of the supplier
     */
    private <T> T lockAndUnLock(Supplier<T> supplier) {
        lock.lock();
        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class CacheElement<V> {
        private V value;
    }
}