package com.iconloop.score.bfs;

public class Allocator {
    private int frontIndex;
    private int backIndex;
    private final String[] peers;
    private final String[] userAllocations;
    private final int allocationMin;
    private final int allocationMax;
    private final int allocationMargin;
    private final BfsContents bfsContents;

    // constructor
    public Allocator(String[] peers,
                     int frontIndex,
                     int backIndex,
                     String[] userAllocations,
                     int allocationMin,
                     int allocationMax,
                     int allocationMargin,
                     BfsContents bfsContents) {
        this.peers = peers;
        this.frontIndex = frontIndex;
        this.backIndex = backIndex;
        this.userAllocations = userAllocations;
        this.allocationMin = allocationMin;
        this.allocationMax = allocationMax;
        this.allocationMargin = allocationMargin;
        this.bfsContents = bfsContents;
    }

    public boolean checkPeerExist(String peer_id) {
        if (this.bfsContents == null) {
            return true;
        }
        return this.bfsContents.checkPeerExist(peer_id);
    }

    public boolean checkComplained(String peer_id) {
        if (this.bfsContents == null) {
            return false;
        }
        return this.bfsContents.checkComplained(peer_id);
    }

    private boolean checkPeerUnique(String[] peers, String peer) {
        for (String thePeer : peers) {
            if (thePeer != null && thePeer.equals(peer)) {
                return false;
            }
        }

        return true;
    }

    public String[] makeAllocations() {
        int allocationNum = this.allocationMin + this.allocationMargin;
        allocationNum = Math.min(allocationNum, this.allocationMax);
        allocationNum = Math.min(allocationNum, this.peers.length);

        String[] allocations = new String[allocationNum];
        int allocationIndex = 0;
        int allocationCount = 0;

        // Add userAllocations given by caller first
        if (this.userAllocations != null && this.userAllocations.length > 0) {
            for (String allocation : this.userAllocations) {
                if (checkPeerExist(allocation) && !checkComplained(allocation) && checkPeerUnique(allocations, allocation)) {
                    allocations[allocationIndex] = allocation;
                    allocationIndex++;
                    allocationCount++;
                }

                if (allocationIndex >= allocationNum) {
                    break;
                }
            }

            // If allocationCount exceeds allocationMin, it returns without further allocation.
            if (allocationCount >= this.allocationMin) {
                String[] allocationsSet = new String[allocationIndex];
                System.arraycopy(allocations, 0, allocationsSet, 0, allocationsSet.length);

                return allocationsSet;
            }
        }

        boolean useFrontIndex = true;
        if (this.backIndex >= this.peers.length) {
            this.frontIndex = 0;
            this.backIndex = this.peers.length - 1;
        }

        while (allocationCount < this.peers.length) {
            if (this.frontIndex > this.backIndex) {
                this.frontIndex = 0;
                this.backIndex = this.peers.length - 1;
                useFrontIndex = true;
            }

            if (useFrontIndex) {
                if (!checkPeerUnique(allocations, this.peers[this.frontIndex])) {
                    this.frontIndex++;
                    useFrontIndex = false;
                    continue;
                }
                if (!checkComplained(this.peers[frontIndex])) {
                    allocations[allocationIndex] = this.peers[this.frontIndex];
                    allocationIndex++;
                }
                this.frontIndex++;
                useFrontIndex = false;
            } else {
                if (!checkPeerUnique(allocations, this.peers[this.backIndex])) {
                    this.backIndex--;
                    useFrontIndex = true;
                    continue;
                }
                if (!checkComplained(this.peers[backIndex])) {
                    allocations[allocationIndex] = this.peers[backIndex];
                    allocationIndex++;
                }
                this.backIndex--;
                useFrontIndex = true;
            }

            if (allocationIndex >= allocationNum) {
                break;
            }

            allocationCount++;
        }

        if (allocationIndex < this.allocationMin) {
            throw new AllocatorException("There are not enough allocation nodes.");
        }

        String[] allocationsSet = new String[allocationIndex];
        System.arraycopy(allocations, 0, allocationsSet, 0, allocationsSet.length);

        return allocationsSet;
    }

    public int getFrontIndex() {
        return this.frontIndex;
    }

    public int getBackIndex() {
        return this.backIndex;
    }

    public static class AllocatorException extends RuntimeException {
        public AllocatorException(String message) {
            super(message);
        }
    }
}
