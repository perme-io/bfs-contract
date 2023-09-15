package com.iconloop.score.bfs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;


class MockBfsContents extends BfsContents {
    private int complainPeerMode;

    public MockBfsContents() {
        super();
        this.complainPeerMode = 0;
    }

    public void setComplainPeerMode(int mode) {
        this.complainPeerMode = mode;
    }

    public boolean checkPeerExist(String peer_id) {
        return true;
    }

    public boolean checkComplained(String peer_id) {
        if (complainPeerMode == 1) {
            return peer_id.equals("Alice3");
        }
        if (complainPeerMode == 2) {
            return peer_id.equals("Alice0") || peer_id.equals("Alice2");
        }
        if (complainPeerMode == 3) {
            return peer_id.equals("Alice0") || peer_id.equals("Alice4");
        }
        return false;
    }
}

public class AllocationTest {
    private boolean debugPrintMode;

    public AllocationTest() {
        this.debugPrintMode = false;
    }

    private void debugPrint(String log) {
        if (debugPrintMode) {
            System.out.println(log);
        }
    }

    @Test
    void makeAllocationTest() {
        this.debugPrintMode = true;
        MockBfsContents bfsContents = new MockBfsContents();
        bfsContents.setComplainPeerMode(0);

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

        bfsContents.setComplainPeerMode(1);

        allocator = new Allocator(peers, 2, 3, null, 2, 5, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice2", "Alice0", "Alice4", "Alice1"}, allocator.makeAllocations()));

        allocator = new Allocator(peers, 2, 2, null, 3, 5, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice2", "Alice0", "Alice4", "Alice1"}, allocator.makeAllocations()));

        allocator = new Allocator(peers, 1, 1, null, 3, 3, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice1", "Alice0", "Alice4"}, allocator.makeAllocations()));

        allocator = new Allocator(peers, 2, 2, null, 2, 3, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice2", "Alice0", "Alice4"}, allocator.makeAllocations()));

        allocator = new Allocator(peers, 3, 5, null, 2, 4, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice0", "Alice4", "Alice1", "Alice2"}, allocator.makeAllocations()));

        allocator = new Allocator(peers, 3, 6, null, 2, 4, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice0", "Alice4", "Alice1", "Alice2"}, allocator.makeAllocations()));

        allocator = new Allocator(peers, 9, 10, null, 2, 4, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice0", "Alice4", "Alice1", "Alice2"}, allocator.makeAllocations()));

        allocator = new Allocator(peers, 5, 5, null, 4, 4, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice0", "Alice4", "Alice1", "Alice2"}, allocator.makeAllocations()));

        allocator = new Allocator(peers, 1, 4, null, 4, 4, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice1", "Alice4", "Alice2", "Alice0"}, allocator.makeAllocations()));

        bfsContents.setComplainPeerMode(2);

        allocator = new Allocator(peers, 2, 3, null, 2, 5, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice3", "Alice4", "Alice1"}, allocator.makeAllocations()));

        allocator = new Allocator(peers, 2, 2, null, 3, 5, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice4", "Alice1", "Alice3"}, allocator.makeAllocations()));

        allocator = new Allocator(peers, 1, 1, null, 3, 3, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice1", "Alice4", "Alice3"}, allocator.makeAllocations()));

        allocator = new Allocator(peers, 2, 2, null, 2, 3, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice4", "Alice1", "Alice3"}, allocator.makeAllocations()));

        allocator = new Allocator(peers, 3, 5, null, 2, 4, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice4", "Alice1", "Alice3"}, allocator.makeAllocations()));

        allocator = new Allocator(peers, 3, 6, null, 2, 4, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice4", "Alice1", "Alice3"}, allocator.makeAllocations()));

        allocator = new Allocator(peers, 9, 10, null, 2, 4, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice4", "Alice1", "Alice3"}, allocator.makeAllocations()));

        bfsContents.setComplainPeerMode(3);

        allocator = new Allocator(peers, 2, 3, null, 2, 5, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice2", "Alice3", "Alice1"}, allocator.makeAllocations()));

        allocator = new Allocator(peers, 2, 2, null, 3, 5, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice2", "Alice1", "Alice3"}, allocator.makeAllocations()));

        allocator = new Allocator(peers, 1, 1, null, 3, 3, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice1", "Alice3", "Alice2"}, allocator.makeAllocations()));

        allocator = new Allocator(peers, 2, 2, null, 2, 3, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice2", "Alice1", "Alice3"}, allocator.makeAllocations()));

        allocator = new Allocator(peers, 3, 5, null, 2, 4, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice1", "Alice3", "Alice2"}, allocator.makeAllocations()));

        allocator = new Allocator(peers, 3, 6, null, 2, 4, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice1", "Alice3", "Alice2"}, allocator.makeAllocations()));

        allocator = new Allocator(peers, 9, 10, null, 2, 4, 2, bfsContents);
        assertTrue(Helper.ArraysEqual(new String[]{"Alice1", "Alice3", "Alice2"}, allocator.makeAllocations()));
        debugPrint(Helper.StringListToJsonString(allocator.makeAllocations()));
    }
}
