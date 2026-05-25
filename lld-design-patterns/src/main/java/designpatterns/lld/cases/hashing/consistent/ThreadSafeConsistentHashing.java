package designpatterns.lld.cases.hashing.consistent;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ThreadSafeConsistentHashing {

    private final TreeMap<Long, String> ring = new TreeMap<>();
    private final int numberOfReplicas;
    
    // The Lock Manager
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock readLock = rwl.readLock();
    private final Lock writeLock = rwl.writeLock();

    public ThreadSafeConsistentHashing(int numberOfReplicas) {
        this.numberOfReplicas = numberOfReplicas;
    }

    public void addNode(String node) {
        // Acquire Write Lock: No one can read or write until we finish
        writeLock.lock();
        try {
            for (int i = 0; i < numberOfReplicas; i++) {
                String virtualNodeId = node + "-" + i;
                long hash = generateHash(virtualNodeId);
                ring.put(hash, node);
            }
            System.out.println("Added node: " + node);
        } finally {
            // Always unlock in a 'finally' block to prevent deadlocks
            writeLock.unlock();
        }
    }

    public void removeNode(String node) {
        writeLock.lock();
        try {
            for (int i = 0; i < numberOfReplicas; i++) {
                String virtualNodeId = node + "-" + i;
                long hash = generateHash(virtualNodeId);
                ring.remove(hash);
            }
            System.out.println("Removed node: " + node);
        } finally {
            writeLock.unlock();
        }
    }

    public String getNode(String key) {
        // Acquire Read Lock: Multiple threads can enter here at the same time
        // But if a Write Lock is active, they will wait.
        readLock.lock();
        try {
            if (ring.isEmpty()) {
                return null;
            }

            long hash = generateHash(key);

            if (!ring.containsKey(hash)) {
                SortedMap<Long, String> tailMap = ring.tailMap(hash);
                hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
            }

            return ring.get(hash);
        } finally {
            readLock.unlock();
        }
    }

    // (Helper: MD5 generateHash method remains the same as previous example)
    private long generateHash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes());
            long h = 0;
            for (int i = 0; i < 4; i++) {
                h <<= 8;
                h |= ((int) digest[i]) & 0xFF;
            }
            return h;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not supported", e);
        }
    }
}
