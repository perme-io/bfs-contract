package com.iconloop.score.bfs;

import score.*;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;


enum EventType {
    AddPin,
    Unpin,
    UpdatePin,
    RemovePin,
    Reallocation,
    AddNode,
    RemoveNode,
    UpdateNode,
    Complained
}

public class BfsContents {
    private static final BigInteger ONE_ICX = new BigInteger("1000000000000000000");
    // TODO Need a better way to set the allocation number between min max.

    private final ArrayDB<String> peers = Context.newArrayDB("peers", String.class);
    private final VarDB<Integer> frontIndexOfPeers = Context.newVarDB("frontIndexOfPeers", Integer.class);
    private final VarDB<Integer> backIndexOfPeers = Context.newVarDB("backIndexOfPeers", Integer.class);
    // TODO allocationMin, allocationMax are need method to set them.
    private final VarDB<Integer> allocationMin = Context.newVarDB("allocationMin", Integer.class);
    private final VarDB<Integer> allocationMax = Context.newVarDB("allocationMax", Integer.class);
    private final VarDB<Integer> allocationMargin = Context.newVarDB("allocationMargin", Integer.class);
    private final VarDB<BigInteger> minStakeForServe = Context.newVarDB("minStakeForServe", BigInteger.class);
    private final DictDB<String, PinInfo> pinInfos;
    private final DictDB<String, NodeInfo> nodeInfos;
    private final VarDB<Address> didSummaryScore = Context.newVarDB("didSummaryScore", Address.class);

    public BfsContents() {
        this.pinInfos = Context.newDictDB("pinInfos", PinInfo.class);
        this.nodeInfos = Context.newDictDB("nodeInfos", NodeInfo.class);
    }

    @External()
    public void set_did_summary_score(Address did_summary_score) {
        Context.require(Context.getCaller().equals(Context.getOwner()), "Only owner can call this method.");
        this.didSummaryScore.set(did_summary_score);
    }

    @External(readonly=true)
    public Address get_did_summary_score() {
        return this.didSummaryScore.getOrDefault(null);
    }

    public String[] makeAllocations(String[] userAllocations, Integer allocationMin, Integer allocationMax) {
        if (allocationMin > this.peers.size()) {
            Context.revert(100, "Fewer peers to allocate.");
        }
        if (allocationMin < this.allocationMin.getOrDefault(1) || allocationMin > allocationMax) {
            Context.revert(100, "AllocationMin Range Error!");
        }
        if (allocationMax < this.allocationMax.getOrDefault(1)) {
            Context.revert(100, "AllocationMax Range Error!");
        }

        int frontIndex = frontIndexOfPeers.getOrDefault(0);
        int backIndex = backIndexOfPeers.getOrDefault(this.peers.size() - 1);
        int allocationMargin = this.allocationMargin.getOrDefault(2);

        Allocator allocator = new Allocator(Helper.ArrayDBToArray(this.peers),
                                            frontIndex,
                                            backIndex,
                                            userAllocations,
                                            allocationMin,
                                            allocationMax,
                                            allocationMargin, this);

        String[] allocations = null;
        try {
            allocations = allocator.makeAllocations();
        }
        catch(Allocator.AllocatorException e) {
            Context.revert(102, e.getMessage());
        }

        frontIndexOfPeers.set(allocator.getFrontIndex());
        backIndexOfPeers.set(allocator.getBackIndex());

        return allocations;
    }

    @External(readonly=true)
    public Map<String, Object> get_pin(String cid) {
        PinInfo pininfo = this.pinInfos.get(cid);
        return pininfo.toMap();
    }

    @External()
    public void set_default_allocation_factors(@Optional BigInteger allocation_min, @Optional BigInteger allocation_max, @Optional BigInteger allocation_margin) {
        Context.require(Context.getCaller().equals(Context.getOwner()), "Only owner can call this method.");

        int allocationMin = this.allocationMin.getOrDefault(1);
        int allocationMax = this.allocationMax.getOrDefault(1);
        int allocationMargin = this.allocationMargin.getOrDefault(2);
        boolean isChanged = false;

        if (allocation_min != null && allocation_min.intValue() > 0) {
            if (allocationMin != allocation_min.intValue()) {
                allocationMin = allocation_min.intValue();
                isChanged = true;
            }
        }

        if (allocation_max != null && allocation_max.intValue() > 0) {
            if (allocationMax != allocation_max.intValue()) {
                allocationMax = allocation_max.intValue();
                isChanged = true;
            }
        }

        if (allocation_margin != null && allocation_margin.intValue() >= 0) {
            if (allocationMargin != allocation_margin.intValue()) {
                allocationMargin = allocation_margin.intValue();
                isChanged = true;
            }
        }

        Context.require(allocationMax >= allocationMin, "allocation_max must be greater than or equal to allocation_min.");
        Context.require(isChanged, "The allocation factors are not changed.");

        this.allocationMin.set(allocationMin);
        this.allocationMax.set(allocationMax);
        this.allocationMargin.set(allocationMargin);
    }

    @External(readonly=true)
    public Map<String, Object> get_default_allocation_factors() {
        return Map.ofEntries(
                Map.entry("allocation_min", this.allocationMin.getOrDefault(1)),
                Map.entry("allocation_max", this.allocationMax.getOrDefault(1)),
                Map.entry("allocation_margin", this.allocationMargin.getOrDefault(2))
        );
    }

    @External()
    public void set_min_stake_value(BigInteger min_stake_for_serve) {
        Context.require(Context.getCaller().equals(Context.getOwner()), "Only owner can call this method.");
        this.minStakeForServe.set(min_stake_for_serve);
    }

    @External(readonly=true)
    public Map<String, Object> get_min_stake_value() {
        return Map.ofEntries(
                Map.entry("min_stake_for_serve", this.minStakeForServe.getOrDefault(BigInteger.valueOf(0)))
        );
    }

    @External()
    public void pin(String cid,
                    String tracker,
                    @Optional String owner_did,
                    @Optional byte[] owner_sign,
                    @Optional String name,
                    @Optional String comment,
                    @Optional BigInteger replication_min,
                    @Optional BigInteger replication_max,
                    @Optional String[] user_allocations,
                    @Optional BigInteger shard_size,
                    @Optional String expire_at,
                    @Optional String expire_in) {
        Context.require(!cid.isEmpty(), "Blank key is not allowed.");

        String owner = Context.getCaller().toString();
        if (owner_did != null) {
            DidMessage didMessage = getDidMessage(owner_did, Context.getCaller(), cid, "pin", BigInteger.ZERO, owner_sign);
            owner = didMessage.did;
            Context.require(cid.equals(didMessage.getTarget()), "Invalid Content(PinInfo) target.");
        }

        PinInfo pininfo = this.pinInfos.get(cid);
        if(pininfo != null) {
            // The owner of the old cid can recreate a new pin with the same cid.
            if (!pininfo.checkOwner(owner)) {
                Context.revert(101, "You do not have permission.");
            }
        }

        Integer replicationMin = this.allocationMin.getOrDefault(1);
        Integer replicationMax = this.allocationMax.getOrDefault(1);

        if (replication_min != null && replication_min.intValue() > replicationMin) {
            replicationMin = replication_min.intValue();
        }
        if (replication_max != null && replication_max.intValue() > replicationMax) {
            replicationMax = replication_max.intValue();
        }

        String[] userAllocations = makeAllocations(user_allocations, replicationMin, replicationMax);
        pininfo = new PinInfo(cid, tracker, name, comment, String.valueOf(Context.getBlockTimestamp()), owner,
                replicationMin, replicationMax, userAllocations,
                shard_size, expire_at, expire_in, BigInteger.valueOf(1), null);
        this.pinInfos.set(cid, pininfo);

        BFSEvent(EventType.AddPin.name(), cid, owner, pininfo.getLastUpdated());
    }

    @External()
    public void unpin(String cid, @Optional String owner_did, @Optional byte[] owner_sign) {
        PinInfo pininfo = this.pinInfos.get(cid);
        Context.require(pininfo != null, "Invalid request target.");

        // Verify owner
        String owner = Context.getCaller().toString();
        if (owner_did != null) {
            DidMessage didMessage = getDidMessage(owner_did, Context.getCaller(), cid, "unpin", pininfo.getLastUpdated(), owner_sign);
            owner = didMessage.did;
            Context.require(pininfo.checkLastUpdated(didMessage.getLastUpdated()), "Invalid Content(PinInfo) lastUpdated.");
            Context.require(cid.equals(didMessage.getTarget()), "Invalid Content(PinInfo) target.");
        }

        if (!pininfo.checkOwner(owner)) {
            Context.revert(101, "You do not have permission.");
        }

        pininfo.unpin();
        this.pinInfos.set(cid, pininfo);
        BFSEvent(EventType.Unpin.name(), cid, owner, pininfo.getLastUpdated());
    }

    @External()
    public void update_pin(String cid,
                           @Optional String owner_did,
                           @Optional byte[] owner_sign,
                           @Optional String tracker,
                           @Optional String name,
                           @Optional String comment,
                           @Optional String owner,
                           @Optional BigInteger replication_min,
                           @Optional BigInteger replication_max,
                           @Optional String[] user_allocations,
                           @Optional String expire_at,
                           @Optional String expire_in) {
        PinInfo pininfo = this.pinInfos.get(cid);
        Context.require(pininfo != null, "Invalid request target.");

        String prevOwner = Context.getCaller().toString();
        if (owner_did != null) {
            DidMessage didMessage = getDidMessage(owner_did, Context.getCaller(), cid, "update_pin", pininfo.getLastUpdated(), owner_sign);
            prevOwner = didMessage.did;
            Context.require(pininfo.checkLastUpdated(didMessage.getLastUpdated()), "Invalid Content(PinInfo) lastUpdated.");
            Context.require(cid.equals(didMessage.getTarget()), "Invalid Content(PinInfo) target.");
        }
        if (!pininfo.checkOwner(prevOwner)) {
            Context.revert(101, "You do not have permission.");
        }

        Integer replicationMin = pininfo.getReplicationMin();
        Integer replicationMax = pininfo.getReplicationMax();

        if (replication_min != null && replication_min.compareTo(BigInteger.valueOf(this.allocationMin.getOrDefault(1))) >= 0) {
            replicationMin = replication_min.intValue();
        }
        if (replication_max != null && replication_max.compareTo(BigInteger.valueOf(this.allocationMax.getOrDefault(2))) >= 0) {
            replicationMax = replication_max.intValue();
        }

        String[] userAllocations = (user_allocations != null)? user_allocations : pininfo.userAllocations();
        String[] newAllocations = makeAllocations(userAllocations, replicationMin, replicationMax);

        pininfo.update(
                tracker, name, comment, null, owner, replicationMin, replicationMax,
                newAllocations, BigInteger.ZERO, expire_at, expire_in, BigInteger.ONE);

        this.pinInfos.set(cid, pininfo);
        BFSEvent(EventType.UpdatePin.name(), cid, pininfo.getOwner(), pininfo.getLastUpdated());
        if (!Helper.ArraysEqual(userAllocations, newAllocations)) {
            BFSEvent(EventType.Reallocation.name(), cid, pininfo.getOwner(), pininfo.getLastUpdated());
        }
    }

    @External()
    public void remove_pin(String cid, @Optional String owner_did, @Optional byte[] owner_sign) {
        PinInfo pininfo = this.pinInfos.get(cid);
        Context.require(pininfo != null, "Invalid request target.");

        // Verify owner
        String owner = Context.getCaller().toString();
        if (owner_did != null) {
            DidMessage didMessage = getDidMessage(owner_did, Context.getCaller(), cid, "remove_pin", pininfo.getLastUpdated(), owner_sign);
            owner = didMessage.did;
            Context.require(pininfo.checkLastUpdated(didMessage.getLastUpdated()), "Invalid Content(PinInfo) lastUpdated.");
            Context.require(cid.equals(didMessage.getTarget()), "Invalid Content(PinInfo) target.");
        }
        if (!pininfo.checkOwner(owner)) {
            Context.revert(101, "You do not have permission.");
        }

        if (!pininfo.getState().equals(BigInteger.ZERO)) {
            Context.revert(104, "Pinned content cannot be deleted. Please unpin first.");
        }

        this.pinInfos.set(cid, null);
        BFSEvent(EventType.RemovePin.name(), cid, owner, pininfo.getLastUpdated());
    }

    @External(readonly=true)
    public Map<String, Object> get_node(String peer_id) {
        NodeInfo nodeInfo = this.nodeInfos.get(peer_id);
        return nodeInfo.toMap(this.peers.size());
    }

    @External()
    @Payable
    public void add_node(String peer_id,
                         @Optional String endpoint,
                         @Optional String name,
                         @Optional String comment,
                         @Optional Address owner) {
        Context.require(!peer_id.isEmpty(), "Blank key is not allowed.");
        Context.require(this.nodeInfos.get(peer_id) == null, "It has already been added.");

        Address ownerAddress = (owner == null) ? Context.getCaller() : owner;
        BigInteger stake = this.minStakeForServe.getOrDefault(BigInteger.ZERO);

        if (!stake.equals(BigInteger.ZERO)) {
            // You need at least this.minStakeForServe(icx) to add a node.
            Context.require(Context.getValue().compareTo(ONE_ICX.multiply(stake)) >= 0);
            stake = Context.getValue();
        }

        NodeInfo nodeInfo = new NodeInfo(peer_id, name, endpoint, comment,
                String.valueOf(Context.getBlockTimestamp()), ownerAddress, stake, BigInteger.valueOf(0), "");
        this.nodeInfos.set(peer_id, nodeInfo);

        removeNode(peer_id);
        this.peers.add(peer_id);
        BFSEvent(EventType.AddNode.name(), peer_id, nodeInfo.getEndpoint(), BigInteger.ZERO);
    }

    @External()
    public void remove_node(String peer_id) {
        Context.require(this.nodeInfos.get(peer_id) != null, "Invalid request target.");

        NodeInfo nodeInfo = this.nodeInfos.get(peer_id);
        if (!nodeInfo.checkOwner(Context.getCaller())) {
            Context.revert(101, "You do not have permission.");
        }

        this.nodeInfos.set(peer_id, null);
        removeNode(peer_id);
        BFSEvent(EventType.RemoveNode.name(), peer_id, nodeInfo.getEndpoint(), BigInteger.ZERO);
    }

    @External()
    @Payable
    public void update_node(String peer_id,
                            @Optional String endpoint,
                            @Optional String name,
                            @Optional String comment,
                            @Optional Address owner) {
        Context.require(this.nodeInfos.get(peer_id) != null, "Invalid request target.");

        NodeInfo nodeInfo = this.nodeInfos.get(peer_id);
        if (!nodeInfo.checkOwner(Context.getCaller())) {
            Context.revert(101, "You do not have permission.");
        }

        Address ownerAddress = (owner == null) ? Context.getCaller() : owner;
        BigInteger stake = this.minStakeForServe.getOrDefault(BigInteger.ZERO);

        if (!stake.equals(BigInteger.ZERO)) {
            BigInteger prevStake = nodeInfo.getStake();
            BigInteger newStake = prevStake.add(Context.getValue());
            Context.require(newStake.compareTo(ONE_ICX.multiply(stake)) >= 0);
            stake = newStake;
        }

        nodeInfo.update(endpoint, name, comment, null, ownerAddress, stake, BigInteger.valueOf(0));
        this.nodeInfos.set(peer_id, nodeInfo);

        removeNode(peer_id);
        this.peers.add(peer_id);
        BFSEvent(EventType.UpdateNode.name(), peer_id, nodeInfo.getEndpoint(), BigInteger.ZERO);
    }

    @External()
    public void complain_node(String complain_from, String complain_to) {
        NodeInfo complainFrom = this.nodeInfos.get(complain_from);
        if (!complainFrom.checkOwner(Context.getCaller())) {
            Context.revert(101, "You do not have permission.");
        }

        if (!checkPeerExist(complain_to)) {
            // TODO code, message 를 enum 으로 관리하기
            Context.revert(103, "Not exist target node.");
        }

        NodeInfo complainTo = this.nodeInfos.get(complain_to);
        BigInteger isComplained = complainTo.addComplain(complain_from, Context.getBlockTimestamp(), this.peers.size());

        this.nodeInfos.set(complain_to, complainTo);
        if (isComplained.equals(BigInteger.ONE)) {
            BFSEvent(EventType.Complained.name(), complain_to, complainTo.getEndpoint(), BigInteger.ZERO);
        }
    }

    @External(readonly=true)
    public Map<String, Object> check_allocations(String cid) {
        PinInfo pininfo = this.pinInfos.get(cid);
        String[] userAllocations = pininfo.userAllocations();

        StringBuilder builder = new StringBuilder();
        for (String allocation : userAllocations) {
            if (!checkPeerExist(allocation) || checkComplained(allocation)) {
                if (builder.length() > 0) {
                    builder.append(",");
                }
                builder.append("\"").append(allocation).append("\"");
            }
        }
        String[] complained = Helper.JsonStringToStringList("complained", builder.toString());

        Map<String, Object> pinInfoMap = pininfo.toMap();
        return Map.ofEntries(
                Map.entry("cid", pinInfoMap.get("cid")),
                Map.entry("owner", pinInfoMap.get("owner")),
                Map.entry("replication_min", pinInfoMap.get("replication_min")),
                Map.entry("replication_max", pinInfoMap.get("replication_max")),
                Map.entry("user_allocations", pinInfoMap.get("user_allocations")),
                Map.entry("complained", complained),
                Map.entry("state", pinInfoMap.get("state"))
        );
    }

    @External()
    public void reallocation(String cid) {
        PinInfo pininfo = this.pinInfos.get(cid);
        Context.require(pininfo != null, "Invalid request target.");

        String[] userAllocations = pininfo.userAllocations();
        String[] newAllocations = makeAllocations(userAllocations, pininfo.getReplicationMin(), pininfo.getReplicationMax());
        Context.require(!Helper.ArraysEqual(userAllocations, newAllocations), "reallocation is unnecessary.");

        pininfo.reallocation(newAllocations);
        this.pinInfos.set(cid, pininfo);
        BFSEvent(EventType.Reallocation.name(), cid, Context.getCaller().toString(), pininfo.getLastUpdated());
    }

    @External()
    public void reset__() {
        // TODO 개발 과정에서 컨트랙트 리셋 용도로 사용하는 임시 함수, 운영을 위한 배포시에는 이 메소드는 전체 제거되어야 함.
        // Check permission
        Context.require(Context.getOwner().equals(Context.getCaller()), "You do not have permission.");

        String peer_id;
        int peer_count = this.peers.size();
        for (int i = 0; i < peer_count; i++) {
            peer_id = this.peers.pop();
            this.nodeInfos.set(peer_id, null);
        }

        this.allocationMin.set(1);
        this.allocationMax.set(1);
        this.allocationMargin.set(2);
    }

    @External(readonly = true)
    public List<Object> all_node() {
        Object[] allNode = new Object[this.peers.size()];

        for (int i=0; i < this.peers.size(); i++) {
            NodeInfo nodeInfo = this.nodeInfos.get(this.peers.get(i));
            allNode[i] = nodeInfo.toMap(this.peers.size());
        }

        return List.of(allNode);
    }

    @External(readonly = true)
    public Map<String, Object> get_info() {
        return Map.ofEntries(
                Map.entry("frontIndexOfPeers", frontIndexOfPeers.getOrDefault(0)),
                Map.entry("backIndexOfPeers", backIndexOfPeers.getOrDefault(this.peers.size())),
                Map.entry("NumOfPeers", this.peers.size())
        );
    }

    private void removeNode(String peer_id) {
        if (!checkPeerExist(peer_id)) {
            return;
        }

        String top = this.peers.pop();
        if (!top.equals(peer_id)) {
            for (int i = 0; i < this.peers.size(); i++) {
                if (peer_id.equals(this.peers.get(i))) {
                    this.peers.set(i, top);
                    break;
                }
            }
        }
    }

    public boolean checkPeerExist(String peer_id) {
        //TODO: iteration is not efficient. Consider to use a Map.
        for (int i = 0; i < this.peers.size(); i++) {
            if (peer_id.equals(this.peers.get(i))) {
                return true;
            }
        }
        return false;
    }

    public boolean checkComplained(String peer_id) {
        NodeInfo nodeInfo = this.nodeInfos.get(peer_id);
        return nodeInfo.complaints().isComplained(this.peers.size()).equals(BigInteger.ONE);
    }

    private boolean verifySign(DidMessage msg, byte[] sign) {
        if (this.didSummaryScore.getOrDefault(null) == null) {
            Context.revert(102, "No External SCORE to verify DID.");
        }

        String publicKey = (String) Context.call(this.didSummaryScore.get(), "getPublicKey", msg.did, msg.kid);
        byte[] recoveredKeyBytes = Context.recoverKey("ecdsa-secp256k1", msg.getHashedMessage(), sign, false);
        String recoveredKey = new BigInteger(recoveredKeyBytes).toString(16);

//        System.out.println("publicKey(verifySign): " + publicKey);
//        System.out.println("recoveredKey(verifySign): " + recoveredKey);

        return publicKey.equals(recoveredKey);
    }

    private DidMessage getDidMessage(String msg, Address from, String target, String method, BigInteger lastUpdated, byte[] sign) {
        DidMessage message = DidMessage.parse(msg);
        message.update(from, target, method, lastUpdated);
        byte[] hashedMessage = Context.hash("keccak-256", message.getMessageForHash());
        message.setHashedMessage(hashedMessage);

//        System.out.println("receivedMessage: " + msg);
//        System.out.println("generatedMessage: " + message.getMessage());
        Context.require(message.getMessage().equals(msg), "Invalid did message.");
        Context.require(verifySign(message, sign), "Invalid did signature.");
        return message;
    }

    @Payable
    public void fallback() {
        // just receive incoming funds
    }

    /*
     * Events
     */
    @EventLog
    protected void BFSEvent(String event, String value1, String value2, BigInteger nonce) {}
}
