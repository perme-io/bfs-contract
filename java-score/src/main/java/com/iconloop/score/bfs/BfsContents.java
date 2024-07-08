package com.iconloop.score.bfs;

import score.*;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;


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
    private final VarDB<BigInteger> frontIndexOfPeers = Context.newVarDB("frontIndexOfPeers", BigInteger.class);
    private final VarDB<BigInteger> backIndexOfPeers = Context.newVarDB("backIndexOfPeers", BigInteger.class);
    // TODO allocationMin, allocationMax are need method to set them.
    private final VarDB<BigInteger> allocationMin = Context.newVarDB("allocationMin", BigInteger.class);
    private final VarDB<BigInteger> allocationMax = Context.newVarDB("allocationMax", BigInteger.class);
    private final VarDB<BigInteger> allocationMargin = Context.newVarDB("allocationMargin", BigInteger.class);
    private final VarDB<BigInteger> minStakeForServe = Context.newVarDB("minStakeForServe", BigInteger.class);
    private final DictDB<String, String> pinInfos;
    private final DictDB<String, String> nodeInfos;

    public BfsContents() {
        this.pinInfos = Context.newDictDB("pinInfos", String.class);
        this.nodeInfos = Context.newDictDB("nodeInfos", String.class);
    }

    public String[] makeAllocations(String[] userAllocations, int allocationMin, int allocationMax) {
        if (allocationMin > this.peers.size()) {
            Context.revert(100, "Fewer peers to allocate.");
        }
        if (allocationMin < this.allocationMin.getOrDefault(BigInteger.valueOf(1)).intValue() || allocationMin > allocationMax) {
            Context.revert(100, "AllocationMin Range Error!");
        }
        if (allocationMax < this.allocationMax.getOrDefault(BigInteger.valueOf(1)).intValue()) {
            Context.revert(100, "AllocationMax Range Error!");
        }

        int frontIndex = frontIndexOfPeers.getOrDefault(BigInteger.ZERO).intValue();
        int backIndex = backIndexOfPeers.getOrDefault(BigInteger.valueOf(this.peers.size() - 1)).intValue();
        int allocationMargin = this.allocationMargin.getOrDefault(BigInteger.valueOf(2)).intValue();

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

        frontIndexOfPeers.set(BigInteger.valueOf(allocator.getFrontIndex()));
        backIndexOfPeers.set(BigInteger.valueOf(allocator.getBackIndex()));

        return allocations;
    }

    @External(readonly=true)
    public Map<String, Object> get_pin(String cid) {
        PinInfo pininfo = PinInfo.fromString(this.pinInfos.get(cid));
        return pininfo.toMap();
    }

    @External()
    public void set_default_allocation_factors(@Optional BigInteger allocation_min, @Optional BigInteger allocation_max, @Optional BigInteger allocation_margin) {
        Context.require(Context.getCaller().equals(Context.getOwner()), "Only owner can call this method.");

        int allocationMin = this.allocationMin.getOrDefault(BigInteger.valueOf(1)).intValue();
        int allocationMax = this.allocationMax.getOrDefault(BigInteger.valueOf(1)).intValue();
        int allocationMargin = this.allocationMargin.getOrDefault(BigInteger.valueOf(2)).intValue();
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

        this.allocationMin.set(BigInteger.valueOf(allocationMin));
        this.allocationMax.set(BigInteger.valueOf(allocationMax));
        this.allocationMargin.set(BigInteger.valueOf(allocationMargin));
    }

    @External(readonly=true)
    public Map<String, Object> get_default_allocation_factors() {
        return Map.ofEntries(
                Map.entry("allocation_min", this.allocationMin.getOrDefault(BigInteger.valueOf(1))),
                Map.entry("allocation_max", this.allocationMax.getOrDefault(BigInteger.valueOf(1))),
                Map.entry("allocation_margin", this.allocationMargin.getOrDefault(BigInteger.valueOf(2)))
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

        if(!this.pinInfos.getOrDefault(cid, "").isEmpty()) {;
            // The owner of the old cid can recreate a new pin with the same cid.
            PinInfo pininfo = PinInfo.fromString(this.pinInfos.get(cid));
            if (!pininfo.getString("owner").equals(Context.getCaller().toString())) {
                Context.revert(101, "You do not have permission.");
            }
        }

        PinInfo pininfo = new PinInfo(cid);
        String owner = Context.getCaller().toString();
        if (owner_did != null) {
            Context.require(verifySign(owner_did, owner_sign), "Invalid did signature.");
            owner = owner_did;
        }
        
        int replicationMin = this.allocationMin.getOrDefault(BigInteger.valueOf(1)).intValue();
        int replicationMax = this.allocationMax.getOrDefault(BigInteger.valueOf(1)).intValue();

        if (replication_min != null && replication_min.intValue() > replicationMin) {
            replicationMin = replication_min.intValue();
        }
        if (replication_max != null && replication_max.intValue() > replicationMax) {
            replicationMax = replication_max.intValue();
        }

        String[] userAllocations = makeAllocations(user_allocations, replicationMin, replicationMax);

        pininfo.fromParams(
                tracker, name, comment, String.valueOf(Context.getBlockTimestamp()), owner,
                BigInteger.valueOf(replicationMin), BigInteger.valueOf(replicationMax), userAllocations,
                shard_size, expire_at, expire_in, BigInteger.valueOf(1));
        this.pinInfos.set(cid, pininfo.toString());

        BFSEvent(EventType.AddPin.name(), cid, "");
    }

    @External()
    public void unpin(String cid, @Optional String owner_did, @Optional byte[] owner_sign) {
        Context.require(!this.pinInfos.getOrDefault(cid, "").isEmpty(), "Invalid request target.");

        // Verify owner
        PinInfo pininfo = PinInfo.fromString(this.pinInfos.get(cid));
        String owner = Context.getCaller().toString();
        if (owner_did != null) {
            Context.require(verifySign(owner_did, owner_sign), "Invalid did signature.");
            owner = owner_did;
        }
        if (!pininfo.getString("owner").equals(owner)) {
            Context.revert(101, "You do not have permission.");
        }

        pininfo.unpin();
        this.pinInfos.set(cid, pininfo.toString());
        BFSEvent(EventType.Unpin.name(), cid, "");
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
        Context.require(!this.pinInfos.getOrDefault(cid, "").isEmpty(), "Invalid request target.");

        PinInfo pininfo = PinInfo.fromString(this.pinInfos.get(cid));
        String prevOwner = Context.getCaller().toString();
        if (owner_did != null) {
            Context.require(verifySign(owner_did, owner_sign), "Invalid did signature.");
            prevOwner = owner_did;
        }
        if (!pininfo.getString("owner").equals(prevOwner)) {
            Context.revert(101, "You do not have permission.");
        }

        int replicationMin = pininfo.getInt("replication_min");
        int replicationMax = pininfo.getInt("replication_max");

        if (replication_min != null && replication_min.intValue() >= this.allocationMin.getOrDefault(BigInteger.valueOf(1)).intValue()) {
            replicationMin = replication_min.intValue();
        }
        if (replication_max != null && replication_max.intValue() >= this.allocationMax.getOrDefault(BigInteger.valueOf(2)).intValue()) {
            replicationMax = replication_max.intValue();
        }

        String[] userAllocations = (user_allocations != null)? user_allocations : pininfo.userAllocations();
        String[] newAllocations = makeAllocations(userAllocations, replicationMin, replicationMax);

        pininfo.fromParams(
                tracker, name, comment, "", owner,
                BigInteger.valueOf(replicationMin), BigInteger.valueOf(replicationMax),
                newAllocations, BigInteger.valueOf(0), expire_at, expire_in, BigInteger.valueOf(1));

        this.pinInfos.set(cid, pininfo.toString());
        BFSEvent(EventType.UpdatePin.name(), cid, "");
        if (!Helper.ArraysEqual(userAllocations, newAllocations)) {
            BFSEvent(EventType.Reallocation.name(), cid, "");
        }
    }

    @External()
    public void remove_pin(String cid, @Optional String owner_did, @Optional byte[] owner_sign) {
        Context.require(!this.pinInfos.getOrDefault(cid, "").isEmpty(), "Invalid request target.");

        // Verify owner
        PinInfo pininfo = PinInfo.fromString(this.pinInfos.get(cid));
        String owner = Context.getCaller().toString();
        if (owner_did != null) {
            Context.require(verifySign(owner_did, owner_sign), "Invalid did signature.");
            owner = owner_did;
        }
        if (!pininfo.getString("owner").equals(owner)) {
            Context.revert(101, "You do not have permission.");
        }

        if (pininfo.getInt("state") != 0) {
            Context.revert(104, "Pinned content cannot be deleted. Please unpin first.");
        }

        this.pinInfos.set(cid, "");
        BFSEvent(EventType.RemovePin.name(), cid, "");
    }

    @External(readonly=true)
    public Map<String, Object> get_node(String peer_id) {
        NodeInfo nodeInfo = NodeInfo.fromString(this.nodeInfos.get(peer_id));
        return nodeInfo.toMap(this.peers.size());
    }

    @External()
    @Payable
    public void add_node(String peer_id,
                         @Optional String endpoint,
                         @Optional String name,
                         @Optional String comment,
                         @Optional Address owner) {
        NodeInfo nodeInfo = new NodeInfo(peer_id);
        Context.require(!peer_id.isEmpty(), "Blank key is not allowed.");
        Context.require(this.nodeInfos.getOrDefault(peer_id, "").isEmpty(), "It has already been added.");

        Address ownerAddress = (owner == null) ? Context.getCaller() : owner;
        BigInteger stake = this.minStakeForServe.getOrDefault(BigInteger.ZERO);

        if (stake != BigInteger.ZERO) {
            // You need at least this.minStakeForServe(icx) to add a node.
            Context.require(Context.getValue().compareTo(ONE_ICX.multiply(stake)) >= 0);
            stake = Context.getValue();
        }

        nodeInfo.fromParams(endpoint, name, comment, String.valueOf(Context.getBlockTimestamp()), ownerAddress, stake, BigInteger.valueOf(0));
        this.nodeInfos.set(peer_id, nodeInfo.toString());

        removeNode(peer_id);
        this.peers.add(peer_id);
        BFSEvent(EventType.AddNode.name(), peer_id, nodeInfo.getString("endpoint"));
    }

    @External()
    public void remove_node(String peer_id) {
        Context.require(!this.nodeInfos.getOrDefault(peer_id, "").isEmpty(), "Invalid request target.");

        NodeInfo nodeInfo = NodeInfo.fromString(this.nodeInfos.get(peer_id));
        if (!nodeInfo.getString("owner").equals(Context.getCaller().toString())) {
            Context.revert(101, "You do not have permission.");
        }

        this.nodeInfos.set(peer_id, "");
        removeNode(peer_id);
        BFSEvent(EventType.RemoveNode.name(), peer_id, nodeInfo.getString("endpoint"));
    }

    @External()
    @Payable
    public void update_node(String peer_id,
                            @Optional String endpoint,
                            @Optional String name,
                            @Optional String comment,
                            @Optional Address owner) {
        Context.require(!this.nodeInfos.getOrDefault(peer_id, "").isEmpty(), "Invalid request target.");

        NodeInfo nodeInfo = NodeInfo.fromString(this.nodeInfos.get(peer_id));
        if (!nodeInfo.getString("owner").equals(Context.getCaller().toString())) {
            Context.revert(101, "You do not have permission.");
        }

        Address ownerAddress = (owner == null) ? Context.getCaller() : owner;
        BigInteger stake = this.minStakeForServe.getOrDefault(BigInteger.ZERO);

        if (stake != BigInteger.ZERO) {
            BigInteger prevStake = new BigInteger(nodeInfo.getString("stake"), 16);
            BigInteger newStake = prevStake.add(Context.getValue());
            Context.require(newStake.compareTo(ONE_ICX.multiply(stake)) >= 0);
            stake = newStake;
        }

        nodeInfo.fromParams(endpoint, name, comment, "", ownerAddress, stake, BigInteger.valueOf(0));
        this.nodeInfos.set(peer_id, nodeInfo.toString());

        removeNode(peer_id);
        this.peers.add(peer_id);
        BFSEvent(EventType.UpdateNode.name(), peer_id, nodeInfo.getString("endpoint"));
    }

    @External()
    public void complain_node(String complain_from, String complain_to) {
        NodeInfo complainFrom = NodeInfo.fromString(this.nodeInfos.get(complain_from));
        if (!complainFrom.getString("owner").equals(Context.getCaller().toString())) {
            Context.revert(101, "You do not have permission.");
        }

        if (!checkPeerExist(complain_to)) {
            // TODO code, message 를 enum 으로 관리하기
            Context.revert(103, "Not exist target node.");
        }

        NodeInfo complainTo = NodeInfo.fromString(this.nodeInfos.get(complain_to));
        BigInteger isComplained = complainTo.addComplain(complain_from, Context.getBlockTimestamp(), this.peers.size());

        this.nodeInfos.set(complain_to, complainTo.toString());
        if (isComplained.equals(BigInteger.ONE)) {
            BFSEvent(EventType.Complained.name(), complain_to, complainTo.getString("endpoint"));
        }
    }

    @External(readonly=true)
    public Map<String, Object> check_allocations(String cid) {
        PinInfo pininfo = PinInfo.fromString(this.pinInfos.get(cid));
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

        return Map.ofEntries(
                Map.entry("cid", pininfo.getString("cid")),
                Map.entry("owner", pininfo.getString("owner")),
                Map.entry("replication_min", pininfo.getInt("replication_min")),
                Map.entry("replication_max", pininfo.getInt("replication_max")),
                Map.entry("user_allocations", userAllocations),
                Map.entry("complained", complained),
                Map.entry("state", pininfo.getInt("state"))
        );
    }

    @External()
    public void reallocation(String cid) {
        Context.require(!this.pinInfos.getOrDefault(cid, "").isEmpty(), "Invalid request target.");
        PinInfo pininfo = PinInfo.fromString(this.pinInfos.get(cid));

        String[] userAllocations = pininfo.userAllocations();
        String[] newAllocations = makeAllocations(userAllocations, pininfo.getInt("replication_min"), pininfo.getInt("replication_max"));
        Context.require(!Helper.ArraysEqual(userAllocations, newAllocations), "reallocation is unnecessary.");

        pininfo.reallocation(newAllocations);
        this.pinInfos.set(cid, pininfo.toString());
        BFSEvent(EventType.Reallocation.name(), cid, "");
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
            this.nodeInfos.set(peer_id, "");
        }

        this.allocationMin.set(BigInteger.valueOf(1));
        this.allocationMax.set(BigInteger.valueOf(1));
        this.allocationMargin.set(BigInteger.valueOf(2));
    }

    @External(readonly = true)
    public List<Object> all_node() {
        Object[] allNode = new Object[this.peers.size()];

        for (int i=0; i < this.peers.size(); i++) {
            NodeInfo nodeInfo = NodeInfo.fromString(this.nodeInfos.get(this.peers.get(i)));
            allNode[i] = nodeInfo.toMap(this.peers.size());
        }

        return List.of(allNode);
    }

    @External(readonly = true)
    public Map<String, Object> get_info() {
        return Map.ofEntries(
                Map.entry("frontIndexOfPeers", frontIndexOfPeers.getOrDefault(BigInteger.ZERO)),
                Map.entry("backIndexOfPeers", backIndexOfPeers.getOrDefault(BigInteger.valueOf(this.peers.size()))),
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
        NodeInfo nodeInfo = NodeInfo.fromString(this.nodeInfos.get(peer_id));
        return nodeInfo.complaints().isComplained(this.peers.size()).equals(BigInteger.ONE);
    }

    private boolean verifySign(String msg, byte[] sign) {
        byte[] msgHash = Context.hash("sha3-256", msg.getBytes());
        byte[] publicKey = Context.recoverKey("ecdsa-secp256k1", msgHash, sign, false);
        return Context.verifySignature("ecdsa-secp256k1", msgHash, sign, publicKey);
    }

    @Payable
    public void fallback() {
        // just receive incoming funds
    }

    /*
     * Events
     */
    @EventLog
    protected void BFSEvent(String event, String value1, String value2) {}
}
