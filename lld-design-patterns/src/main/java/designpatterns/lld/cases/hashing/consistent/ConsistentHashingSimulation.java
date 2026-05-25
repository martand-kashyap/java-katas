package designpatterns.lld.cases.hashing.consistent;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class ConsistentHashingSimulation {

    // --- (Reuse our Consistent Hashing Logic) ---
    static class ConsistentHashing {
        private final TreeMap<Long, String> ring = new TreeMap<>();
        private final int numberOfReplicas;

        public ConsistentHashing(int numberOfReplicas) {
            this.numberOfReplicas = numberOfReplicas;
        }

        public void addNode(String node) {
            for (int i = 0; i < numberOfReplicas; i++) {
                long hash = generateHash(node + "-" + i);
                ring.put(hash, node);
            }
        }

        public String getNode(String key) {
            if (ring.isEmpty()) return null;
            long hash = generateHash(key);
            if (!ring.containsKey(hash)) {
                SortedMap<Long, String> tailMap = ring.tailMap(hash);
                hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
            }
            return ring.get(hash);
        }

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
                throw new RuntimeException("MD5 failed", e);
            }
        }
    }

    // --- Simulation Runner ---
    public static void runSimulation(int virtualNodes) {
        ConsistentHashing ch = new ConsistentHashing(virtualNodes);
        
        // 1. Add 5 Physical Servers
        String[] servers = {"Server_A", "Server_B", "Server_C", "Server_D", "Server_E"};
        for (String s : servers) ch.addNode(s);

        // 2. Distribute 100,000 Keys
        Map<String, Integer> loadMap = new HashMap<>();
        for (String s : servers) loadMap.put(s, 0);

        for (int i = 0; i < 100000; i++) {
            String key = "User-Session-" + i;
            String assignedNode = ch.getNode(key);
            loadMap.put(assignedNode, loadMap.get(assignedNode) + 1);
        }

        // 3. Calculate Stats
        System.out.println("\n--- Simulation with " + virtualNodes + " Virtual Nodes ---");
        
        // Ideal load is Total Keys / Total Servers = 100,000 / 5 = 20,000
        double idealLoad = 20000.0;
        double maxVariance = 0.0;

        for (String server : servers) {
            int load = loadMap.get(server);
            double variance = (Math.abs(load - idealLoad) / idealLoad) * 100.0;
            if (variance > maxVariance) maxVariance = variance;
            
            System.out.printf("%s: %6d keys (Variance: %.2f%%)\n", server, load, variance);
        }
        System.out.printf(">> Max Variance: %.2f%%\n", maxVariance);
    }

    public static void main(String[] args) {
        // Scenario A: Poor distribution
        runSimulation(10); 
        
        // Scenario B: Good distribution
        runSimulation(200);
    }
}
