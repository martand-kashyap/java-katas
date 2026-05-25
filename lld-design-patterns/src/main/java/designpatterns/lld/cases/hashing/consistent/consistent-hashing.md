# Consistent Hashing

## What is Consistent Hashing?

Consistent Hashing is a distributed system technique used to distribute data (or requests) across a cluster of servers in a way that minimizes reorganization when servers are added or removed.

To understand it, we must first look at the problem it solves: **The Modulo Problem.**

## The Problem: Simple Hashing (Modulo)

In a traditional setup, to decide which server stores a key (like `user_id: 12345`), you use the modulo operator:

*(Where  is the number of servers)*

If you have 3 servers, `hash(key) % 3` works perfectly.
**The Failure:** If you add a 4th server to handle more load,  changes to 4. Suddenly, `hash(key) % 4` produces a completely different result for almost every key.

* **Result:** ~75% of your data must be moved to different servers immediately. This causes the database to crash under the load of moving data (a "cache stampede").

---

### The Solution: The Hash Ring

Consistent hashing solves this by mapping both **Servers** and **Data Keys** onto the same "circle" (a range of numbers).

#### 1. The Ring

Imagine a circle that represents all possible hash values, from  to  (a very large number). We wrap the number line around so the end connects to the beginning.

#### 2. Placing Servers

We hash the **Server IP** (or ID) to place the server at a specific point on the ring.

* Server A hashes to position 10.
* Server B hashes to position 50.
* Server C hashes to position 80.

#### 3. Placing Data

We hash the **Data Key** to a point on the ring. To find which server owns that data, we simply **move clockwise** around the ring until we hit a server.

* Key 1 (Hash 20)  Moves clockwise  Hits **Server B**.
* Key 2 (Hash 90)  Moves clockwise (wraps around)  Hits **Server A**.

---

### The Magic: Adding/Removing Servers

This is where consistent hashing shines.

**Scenario: We add "Server D"**
We place Server D on the ring at position 30 (between A and B).

* **Who is affected?** Only the keys that fall between Server A and Server D (position 10 to 30).
* **Result:** These keys now hit Server D instead of continuing to Server B.
* **Stability:** Keys mapped to Server C or Server A are untouched.

In a traditional modulo system, 100% of keys are rehashed. In Consistent Hashing, only  keys move (where  is the number of servers).

### Optimization: Virtual Nodes

In the basic ring above, if Server A and B are close together, Server B might get a tiny slice of the ring while Server C gets a huge slice. This creates "hotspots."

To fix this, we use **Virtual Nodes**.
Instead of placing "Server A" on the ring once, we hash it hundreds of times: `Server A_1`, `Server A_2` ... `Server A_100`.

This scatters "Server A" points all over the ring.

* **Benefit:** The load is statistically evenly distributed.
* **Benefit:** If a server is more powerful (e.g., has 2x RAM), we can give it 2x more virtual nodes so it captures 2x more traffic.

---

### Real-World Use Cases in Large Tech

Consistent hashing is the backbone of "Elastic" or "Serverless" architectures where nodes scale up and down frequently.

#### 1. Amazon DynamoDB & Apache Cassandra (Databases)

* **Use Case:** Data Partitioning (Sharding).
* **How it works:** These are "leaderless" databases. Every node knows the hash ring. When you write data, the node calculates the hash and forwards the data to the correct "Coordinator Node" on the ring.
* **Why:** It allows Amazon/Cassandra to add 100 new storage nodes to a cluster without taking the database offline or reshuffling petabytes of existing data.

#### 2. Discord (Chat & Voice)

* **Use Case:** Routing specific "Guilds" (Servers) to specific Voice Nodes.
* **How it works:** Discord uses a consistent hash ring to ensure that all users in the same voice channel are routed to the same backend voice server (so they can hear each other).
* **Why:** If a voice server crashes, the ring instantly remaps those specific voice channels to a new node, while other channels on other servers remain uninterrupted.

#### 3. Load Balancers (HAProxy, NGINX, AWS ALB)

* **Use Case:** Sticky Sessions (Session Affinity).
* **How it works:** If you need a user to always hit the same backend server (to keep their login session active), the load balancer hashes the User IP.
* **Why:** If one backend web server dies, consistent hashing ensures only users on that specific server are logged out/moved; everyone else stays "stuck" to their original healthy server.

#### 4. Memcached (Caching)

* **Use Case:** Distributed Caching.
* **How it works:** The *client* library (on the web server) contains the logic. It calculates `hash(key)` and knows exactly which cache server to ask.
* **Why:** It creates a "Shared Nothing" architecture. The cache servers don't talk to each other; the clients just know where to look.


### Key Concepts

1. **The Ring:** Imagine a circle where every point is a number (a hash).
2. **Nodes:** Servers (nodes) are placed on this ring based on the hash of their ID.
3. **Keys:** Data keys are also hashed to a point on the ring.
4. **Mapping:** To find which node owns a key, we go clockwise on the ring from the key's location until we find a node.
5. **Virtual Nodes:** To distribute data more evenly, one real server is hashed multiple times (e.g., "Server1-0", "Server1-1", "Server1-2") to create multiple points on the ring.

### The Implementation

We use a `TreeMap` to simulate the ring. The **Key** is the Hash (position on ring), and the **Value** is the Server Name.

```java
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentHashing {

    // The Ring: Maps a Hash (Long) to a Server Name (String)
    private final TreeMap<Long, String> ring = new TreeMap<>();
    
    // Number of virtual nodes (replicas) per physical server
    private final int numberOfReplicas;

    public ConsistentHashing(int numberOfReplicas) {
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
        ConsistentHashing ch = new ConsistentHashing(3);

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

```

#### How the Logic Works (Step-by-Step)

1. **MD5 Hashing:** We need a way to turn a string (like "Server-A" or "User1") into a number so we can place it on the number line (the ring). We use MD5, which randomizes the output well.
2. **`tailMap(hash)`:** This is the core logic. When we search for a key's position:
* We look for a Node with a hash *higher* than the Key's hash.
* `tailMap` gives us the portion of the Ring that is "ahead" of our Key.


3. **The Wrap Around:** If `tailMap` is empty, it means our Key is essentially "higher" than the highest Node in the ring. In a circle, this means we must wrap around to the very beginning (`ring.firstKey()`).

#### Why this is "Consistent"

If you run the code, you will notice that when we remove **Server-A**:

* Keys mapped to Server-B or Server-C **do not move**.
* Only keys that were previously on Server-A are moved to the next available server.

In a standard modulo hash (`hash % N`), removing one server changes `N`, causing almost *all* keys to move. Consistent hashing minimizes this movement.

### Why do we use TreeMap for representing a ring?
Using a `TreeMap` is the most standard choice for Consistent Hashing in Java because it solves the two hardest problems of the "Ring" architecture out of the box: **Sorting** and **Navigation**.

Here are the three specific reasons why we chose it:

#### 1. Automatic Sorting (The "Ring" Structure)

Consistent hashing relies on the idea that all nodes are placed on a circle in a specific order (0 to ).

* **Without TreeMap:** If you used a `HashMap`, the keys would be random. You wouldn't know which node comes "after" another. You would have to manually sort the keys every time you looked for a node.
* **With TreeMap:** It internally keeps all keys sorted. As soon as you call `put(hash, node)`, it inserts that node into the correct position in the sorted order. This creates the linear "number line" required to simulate a ring.

#### 2. The `tailMap()` Method (The "Clockwise" Search)

The core logic of consistent hashing is: *"Find the first server with a hash greater than or equal to the Request Key."*

`TreeMap` implements the `NavigableMap` interface, which provides the `tailMap(key)` method.

* `tailMap(K fromKey)` returns a view of the portion of this map whose keys are greater than or equal to `fromKey`.
* This makes finding the "next" node incredibly easy and readable. We simply grab the first item in the `tailMap`. If the `tailMap` is empty, we know we hit the end of the line and need to wrap around to the start (the first key in the map).

#### 3. Performance Balance ()

`TreeMap` is implemented as a **Red-Black Tree** (a type of self-balancing binary search tree).

* **Search:** Finding the node for a key takes  time.
* **Add/Remove Node:** Adding or removing a server also takes  time.

If we used a sorted `ArrayList`, search would be fast (Binary Search), but adding/removing a server would be slow () because we would have to shift all the elements in the array. `TreeMap` offers the best balance for both looking up data and adding/removing servers dynamically.

#### Summary Comparison

| Data Structure | Sorted?        | Lookup "Next" Node | Add/Remove Node | Suitable?         |
|----------------|----------------|--------------------|-----------------|-------------------|
| **HashMap**    | No             | (Must scan all)    |                 | No                |
| **ArrayList**  | Yes (Manual)   | (Binary Search)    | (Slow shifting) | Okay, but complex |
| **TreeMap**    | **Yes (Auto)** | ** (`tailMap`)**   | ****            | **Perfect**       |

---

To make the Consistent Hashing implementation thread-safe for a production environment, we have two main options:

1. **ConcurrentSkipListMap:** A simpler, drop-in replacement for `TreeMap` that is inherently thread-safe.
2. **ReadWriteLock:** Keeping the `TreeMap` but explicitly managing locks.

Since we just discussed *why* `TreeMap` is useful, the **ReadWriteLock** approach is often better to learn. It clearly separates "Routing" (Reading) from "Scaling" (Writing).

In a production system:

* **Reads (99.9%):** Thousands of user requests happen every second. We want these to happen in parallel without blocking each other.
* **Writes (0.1%):** Servers are added or removed rarely. When this happens, we must block everything briefly to update the ring safely.

### Thread-Safe Implementation (using `ReadWriteLock`)

We wrap our `TreeMap` logic with a `ReentrantReadWriteLock`.

```java
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

```

### Why this is better than `synchronized`

You might wonder, "Why not just add the `synchronized` keyword to every method?"

* **Synchronized:** If you used `synchronized`, only **one** thread could get a node at a time. If you had 10,000 requests, request #10,000 would have to wait for the previous 9,999 to finish. This creates a massive bottleneck.
* **ReadWriteLock:** Allows all 10,000 requests to read the map **simultaneously**. They only pause if you are actively adding or removing a server.

### The "Lock-Free" Alternative

If you want to avoid locks entirely, you can swap `TreeMap` for `ConcurrentSkipListMap`.

* **Pros:** No explicit locking code needed; very fast.
* **Cons:** It uses a "Skip List" data structure internally, which uses slightly more memory than a Tree.
* **Usage:** It implements the same interface (`NavigableMap`), so you can literally just change `new TreeMap<>()` to `new ConcurrentSkipListMap<>()` and remove the locks.

---

Calculating the optimal number of "virtual nodes" (vNodes) is a balancing act between **Load Balancing** (making sure everyone gets equal work) and **Memory Usage** (keeping the `TreeMap` small).

There isn't one single "magic number," but there is a widely accepted mathematical rule of thumb used by systems like Amazon Dynamo and Apache Cassandra.

### 1. The Problem: Data Skew

If you have very few vNodes (e.g., 1 or 2 per server), the random distribution of MD5 hashes often leads to "Hotspots."

* **Server A** might cover 60% of the ring.
* **Server B** might cover only 10%.

This is called **Data Skew**. To fix this, we chop the server into many small vNodes scattered randomly across the ring.

### 2. The Mathematical Rule: 

Statistically, the difference in load distribution drops proportional to the square root of the number of virtual nodes ().

If you want a standard deviation (error rate) of roughly **5% to 10%** in your load balancing, you typically need **100 to 200 virtual nodes** per physical server.

#### The Trade-off Table

Here is how the balance improves as you increase virtual nodes (assuming a standard MD5 hashing distribution):

| Virtual Nodes (K) | Standard Deviation (Load Variance) | Memory Overhead |
| --- | --- | --- |
| **1** | ~100% (Very Poor) | Negligible |
| **5-10** | ~30% (Poor) | Very Low |
| **100** | ~10% (Good) | Moderate |
| **200** | ~7% (Better) | Higher |
| **1000** | ~3.2% (Excellent) | Very High |

**Verdict:** Most production systems (like Cassandra) default to **256 virtual nodes**. This is the "sweet spot" where the load is balanced enough (variance < 5-10%) without bloating the memory map too much.

### 3. Practical Constraints

While "more is better" for balancing, there is a cost:

1. **Memory Usage:**
If you have 1,000 physical servers and use 1,000 vNodes each, your `TreeMap` will have **1 Million entries**.
* Java Objects differ in size, but 1M entries in a `TreeMap` might consume ~100MB - 200MB of heap space. This is usually acceptable for a server application, but keep it in mind.


2. **Rebalancing Storms:**
When you add a new physical server, you must calculate 256 new hashes and insert them into the ring. This takes slightly more CPU time, but it is usually negligible compared to the network cost of moving the data.

### 4. Handling Different Server Capacities (Weights)

Not all servers are equal. If Server A has 64GB RAM and Server B has 16GB RAM, you don't want them to have the same number of keys.

You can use virtual nodes to solve this effortlessly: **Assign vNodes proportional to capacity.**

* **Server A (Strong):** Assign **200 vNodes**.
* **Server B (Weak):** Assign **50 vNodes**.

Server A will naturally "capture" 4x more data on the ring than Server B, purely by probability.

### Summary

For a standard Java implementation:

* **Start with 150 - 200 virtual nodes.**
* If your `TreeMap` gets too large (millions of entries) or lookups slow down, reduce this number.
* If you notice one server is working 20% harder than others, increase this number.

---