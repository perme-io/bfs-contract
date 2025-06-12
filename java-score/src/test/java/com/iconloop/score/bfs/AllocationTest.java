package com.iconloop.score.bfs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MockBfsContents extends BfsContents {
    public MockBfsContents() {
        super(null);
    }

    public boolean checkPeerExist(String peer_id) {
        return true;
    }
}

public class AllocationTest {

    @Test
    void makeAllocationTest() {
        MockBfsContents bfsContents = new MockBfsContents();
        String[] peers = new String[]{"Alice0", "Alice1", "Alice2", "Alice3", "Alice4"};

        Allocator allocator = new Allocator(peers, 0, -1, new String[]{"Alice1", "Alice1"}, 2, 3, 0, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice1", "Alice0"}, allocator.makeAllocations()));

        allocator = new Allocator(peers, 0, -1, null, 3, 5, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice0", "Alice4", "Alice1", "Alice3", "Alice2"}, allocator.makeAllocations()));

        allocator = new Allocator(peers, 1, 1, null, 3, 5, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice1", "Alice0", "Alice4", "Alice3", "Alice2"}, allocator.makeAllocations()));

        allocator = new Allocator(peers, 1, 1, null, 3, 3, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice1", "Alice0", "Alice4"}, allocator.makeAllocations()));

        allocator = new Allocator(peers, 2, 2, null, 2, 3, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice2", "Alice0", "Alice4"}, allocator.makeAllocations()));

        allocator = new Allocator(peers, 3, 3, null, 2, 4, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice3", "Alice0", "Alice4", "Alice1"}, allocator.makeAllocations()));

        allocator = new Allocator(peers, 3, 5, null, 2, 4, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice0", "Alice4", "Alice1", "Alice3"}, allocator.makeAllocations()));

        allocator = new Allocator(peers, 3, 6, null, 2, 4, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice0", "Alice4", "Alice1", "Alice3"}, allocator.makeAllocations()));

        allocator = new Allocator(peers, 9, 10, null, 2, 4, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice0", "Alice4", "Alice1", "Alice3"}, allocator.makeAllocations()));
        System.out.println(Helper.StringListToJsonString(allocator.makeAllocations()));
    }
}
