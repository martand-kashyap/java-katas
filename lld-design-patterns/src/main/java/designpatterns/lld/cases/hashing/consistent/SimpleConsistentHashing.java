package designpatterns.lld.cases.hashing.consistent;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.SortedMap;
import java.util.TreeMap;

public class SimpleConsistentHashing {

    // The Ring: Maps a Hash (Long) to a Server Name (String)
    private final TreeMap<Long, String> ring = new TreeMap<>();
    
    // Number of virtual nodes (replicas) per physical server
    private final int numberOfReplicas;

    public SimpleConsistentHashing(int numberOfReplicas) {
        this.numberOfReplicas = numberOfReplicas;
    }

    // 1. Add a Node (Server) to the ring
    public void addNode(String node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            // Create a virtual node ID, e.g., "Server1-0", "Server1-1"
            String virtualNodeId = node + "-" + i;
            long hash = generateHash(virtualNodeId);
            ring.put(hash, node);
        }
        System.out.println("Added node: " + node);
    }

    // 2. Remove a Node from the ring
    public void removeNode(String node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            String virtualNodeId = node + "-" + i;
            long hash = generateHash(virtualNodeId);
            ring.remove(hash);
        }
        System.out.println("Removed node: " + node);
    }

    // 3. Get the Node responsible for a specific data key
    public String getNode(String key) {
        if (ring.isEmpty()) {
            return null;
        }

        long hash = generateHash(key);

        // Standard Case: Find the first node clockwise (greater than or equal to hash)
        if (!ring.containsKey(hash)) {
            // tailMap returns all entries with keys >= hash
            SortedMap<Long, String> tailMap = ring.tailMap(hash);
            
            // If tailMap is empty, it means we wrapped around the circle.
            // Go back to the very first node.
            if (tailMap.isEmpty()) {
                hash = ring.firstKey();
            } else {
                // Otherwise, take the first key from the tailMap
                hash = tailMap.firstKey();
            }
        }

        return ring.get(hash);
    }

    // Helper: A simple MD5 hash function to generate positions on the ring
    private long generateHash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes());
            
            // Convert the first 4 bytes of the MD5 digest into a long
            // (Standard technique to get a 32-bit integer, projected to long)
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

    // --- Demo ---
    public static void main(String[] args) {
        // Create the ring with 3 virtual nodes per server
        SimpleConsistentHashing ch = new SimpleConsistentHashing(3);

        // Add physical servers
        ch.addNode("Server-A");
        ch.addNode("Server-B");
        ch.addNode("Server-C");

        System.out.println("--- Routing Keys ---");
        
        String[] keys = {"User1", "User2", "User3", "Order123", "Payment999"};

        for (String key : keys) {
            System.out.println("Key '" + key + "' is mapped to -> " + ch.getNode(key));
        }
        
        System.out.println("\n--- Removing Server-A ---");
        ch.removeNode("Server-A");
        
        System.out.println("--- Re-Routing Keys ---");
        for (String key : keys) {
            System.out.println("Key '" + key + "' is mapped to -> " + ch.getNode(key));
        }
    }
}
