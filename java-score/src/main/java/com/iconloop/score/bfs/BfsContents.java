package com.iconloop.score.bfs;

import com.parametacorp.jwt.Payload;
import score.Address;
import score.ArrayDB;
import score.BranchDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;
import scorex.util.HashMap;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

enum EventType {
    AddPin,
    UnPin,
    UpdatePin,
    RemovePin,
    Reallocation,
    AddNode,
    RemoveNode,
    UpdateNode,
    UpdateGroup
}

public class BfsContents implements BfsContent, BfsContentEvent{
    private final BigInteger UNPIN_STATE = BigInteger.valueOf(1);
    // TODO Need a better way to set the allocation number between min max.
    private final ArrayDB<String> peers = Context.newArrayDB("peers", String.class);
    private final VarDB<Integer> frontIndexOfPeers = Context.newVarDB("frontIndexOfPeers", Integer.class);
    private final VarDB<Integer> backIndexOfPeers = Context.newVarDB("backIndexOfPeers", Integer.class);
    // TODO allocationMin, allocationMax are need method to set them.
    private final VarDB<Integer> allocationMin = Context.newVarDB("allocationMin", Integer.class);
    private final VarDB<Integer> allocationMax = Context.newVarDB("allocationMax", Integer.class);
    private final VarDB<Integer> allocationMargin = Context.newVarDB("allocationMargin", Integer.class);
    private final VarDB<BigInteger> shardSize = Context.newVarDB("shardSize", BigInteger.class);
    private final BranchDB<String, DictDB<String, PinInfo>> pinInfos = Context.newBranchDB("pinInfos", PinInfo.class);
    private final DictDB<String, CidInfo> cidInfos = Context.newDictDB("cidInfos", CidInfo.class);
    private final DictDB<String, NodeInfo> nodeInfos = Context.newDictDB("nodeInfos", NodeInfo.class);
    private final BranchDB<String, DictDB<String, GroupInfo>> groupInfos = Context.newBranchDB("groupInfos", GroupInfo.class);
    private final VarDB<Address> didScore = Context.newVarDB("didScore", Address.class);

    public BfsContents(@Optional Address did_score) {
        if (did_score != null) {
            this.didScore.set(did_score);
        }
    }

    @External
    public void set_did_score(Address did_score) {
        Context.require(Context.getCaller().equals(Context.getOwner()), "Only owner can call this method.");
        this.didScore.set(did_score);
    }

    @External(readonly=true)
    public Address get_did_score() {
        return this.didScore.get();
    }

    @External
    public void set_shard_size(BigInteger shard_size) {
        Context.require(Context.getCaller().equals(Context.getOwner()), "Only owner can call this method.");
        Context.require(shard_size.compareTo(BigInteger.ZERO) > 0, "Shard size must be greater than 0.");
        this.shardSize.set(shard_size);
    }

    @External(readonly=true)
    public BigInteger get_shard_size() {
        return this.shardSize.get();
    }

    public String[] makeAllocations(Integer allocationMin, Integer allocationMax) {
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
                                            new String[]{},
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

    @Override
    @External(readonly=true)
    public Map<String,Object> get_pin(String owner, String cid) {
        PinInfo pinInfo = this.pinInfos.at(owner).get(cid);
        CidInfo cidInfo = this.cidInfos.get(cid);

        if(pinInfo == null || cidInfo == null){
            return null;
        }

        Map<String, Object> retVal = new HashMap<>();
        retVal.putAll(pinInfo.toMap());
        retVal.putAll(cidInfo.toMap());
        GroupInfo groupInfo = get_group(owner, pinInfo.getGroup());
        retVal.put("expire_at", (groupInfo != null) ? groupInfo.getExpire_at() : pinInfo.getExpire_at());
        return retVal;
    }

    @Override
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

    @Override
    @External(readonly=true)
    public Map<String, Object> get_default_allocation_factors() {
        return Map.ofEntries(
                Map.entry("allocation_min", this.allocationMin.getOrDefault(1)),
                Map.entry("allocation_max", this.allocationMax.getOrDefault(1)),
                Map.entry("allocation_margin", this.allocationMargin.getOrDefault(2))
        );
    }

    @Override
    @External()
    public void pin(String cid,
                    BigInteger size,
                    BigInteger expire_at,
                    @Optional String group,
                    @Optional String name,
                    @Optional String did_sign) {
        Context.require(!cid.isEmpty(), "Blank key is not allowed.");
        Context.require(expire_at.compareTo(BigInteger.ZERO) > 0, "expire_at must be greater than 0.");
        BigInteger blockTimestamp = BigInteger.valueOf(Context.getBlockTimestamp());
        Context.require(expire_at.compareTo(blockTimestamp) > 0, "expire_at must be greater than blockTimestamp");

        String owner = Context.getCaller().toString();
        if (did_sign != null) {
            var expected = new Payload.Builder("pin")
                    .cid(cid)
                    .size(size)
                    .expire_at(expire_at)
                    .build();
            owner = getVerifiedDid(did_sign, expected);
        }

        Context.require(get_pin(owner, cid) == null, "cid already exists");

        CidInfo cidInfo = this.cidInfos.get(cid);
        if (cidInfo == null) {
            // If the cid does not exist, create a new cid.
            Integer replicationMin = this.allocationMin.getOrDefault(1);
            Integer replicationMax = this.allocationMax.getOrDefault(1);
            String[] userAllocations = makeAllocations(replicationMin, replicationMax);
            var cidBuilder = new CidInfo.Builder()
                    .cid(cid)
                    .size(size)
                    .replicationMin(BigInteger.valueOf(replicationMin))
                    .replicationMax(BigInteger.valueOf(replicationMax))
                    .userAllocations(userAllocations)
                    .shardSize(this.shardSize.get());

            cidInfo = cidBuilder.build();
        }
        // If pin requests, increase the number of references.
        cidInfo.setRefCnt(cidInfo.getRefCnt()+1);

        var pinBuilder = new PinInfo.Builder()
                .cid(cid)
                .group(group)
                .name(name)
                .created(Context.getBlockHeight())
                .owner(owner)
                .expireAt(expire_at)
                .lastUpdated(Context.getBlockHeight());

        PinInfo pinInfo = pinBuilder.build();

        this.pinInfos.at(owner).set(cid, pinInfo);
        this.cidInfos.set(cid, cidInfo);

        BFSEvent(EventType.AddPin.name(), cid, owner);
    }

    @Override
    @External()
    public void unpin(String cid, @Optional String did_sign) {
        Context.require(!cid.isEmpty(), "Blank key is not allowed.");
        String owner = Context.getCaller().toString();
        PinInfo pinInfo = null;
        if (did_sign != null) {
            var sigChecker = new SignatureChecker();
            Context.require(sigChecker.verifySig(get_did_score(), did_sign), "failed to verify did_sign");
            owner = sigChecker.getOwnerId();
            pinInfo = this.pinInfos.at(owner).get(cid);
            Context.require(pinInfo != null, "Invalid request(unpin) target.");
            var expected = new Payload.Builder("unpin")
                    .cid(cid)
                    .baseHeight(pinInfo.getLastUpdated())
                    .build();

            Context.require(sigChecker.validatePayload(expected), "failed to validate payload");
        }else{
            pinInfo = this.pinInfos.at(owner).get(cid);
            Context.require(pinInfo != null, "Invalid request(unpin) target.");
        }


        pinInfo.setExpireAt(UNPIN_STATE);
        pinInfo.setLastUpdated(Context.getBlockHeight());

        CidInfo cidInfo = this.cidInfos.get(cid);
        cidInfo.setRefCnt(cidInfo.getRefCnt()-1);

        this.pinInfos.at(owner).set(cid, pinInfo);
        this.cidInfos.set(cid, cidInfo);

        BFSEvent(EventType.UnPin.name(), cid, owner);
    }

    @Override
    @External()
    public void update_pin(String cid,
                           BigInteger expire_at,
                           @Optional String did_sign) {
        Context.require(!cid.isEmpty(), "Blank key is not allowed.");
        BigInteger blockTimestamp = BigInteger.valueOf(Context.getBlockTimestamp());
        Context.require(expire_at.compareTo(blockTimestamp) > 0, "expire_at must be greater than blockTimestamp");

        PinInfo pinInfo = null;
        String owner = Context.getCaller().toString();
        if (did_sign != null) {
            var sigChecker = new SignatureChecker();
            Context.require(sigChecker.verifySig(get_did_score(), did_sign), "failed to verify did_sign");
            owner = sigChecker.getOwnerId();
            pinInfo = this.pinInfos.at(owner).get(cid);
            Context.require(pinInfo != null, "Invalid request(update_pin) target.");
            var expected = new Payload.Builder("update_pin")
                    .cid(cid)
                    .expire_at(expire_at)
                    .baseHeight(pinInfo.getLastUpdated())
                    .build();

            Context.require(sigChecker.validatePayload(expected), "failed to validate payload");
        }else{
            pinInfo = this.pinInfos.at(owner).get(cid);
            Context.require(pinInfo != null, "Invalid request(update_pin) target.");
        }

        var attrs = new PinInfo.Builder()
                .expireAt(expire_at);
        attrs.lastUpdated(Context.getBlockHeight());

        pinInfo.update(attrs);

        this.pinInfos.at(owner).set(cid, pinInfo);
        BFSEvent(EventType.UpdatePin.name(), cid, pinInfo.getOwner());
    }


    @Override
    @External(readonly=true)
    public NodeInfo get_node(String peer_id) {
        return this.nodeInfos.get(peer_id);
    }

    @Override
    @External()
    @Payable
    public void add_node(String peer_id,
                         String url,
                         @Optional String endpoint,
                         @Optional String name,
                         @Optional Address owner) {
        Context.require(!peer_id.isEmpty(), "Blank key is not allowed.");
        Context.require(url.startsWith("http://") || url.startsWith("https://"), "Invalid URL format.");
        Context.require(this.nodeInfos.get(peer_id) == null, "It has already been added.");

        Address ownerAddress = (owner == null) ? Context.getCaller() : owner;

        var nodeBuilder = new NodeInfo.Builder()
                .peerId(peer_id)
                .url(url)
                .name(name)
                .endpoint(endpoint)
                .created(Context.getBlockHeight())
                .owner(ownerAddress);

        NodeInfo nodeInfo = nodeBuilder.build();
        this.nodeInfos.set(peer_id, nodeInfo);

        removeNode(peer_id);
        this.peers.add(peer_id);

        if (endpoint != null) {
            BFSEvent(EventType.AddNode.name(), peer_id, endpoint);
        }else {
            BFSEvent(EventType.AddNode.name(), peer_id, "");
        }
    }

    @Override
    @External()
    public void remove_node(String peer_id) {
        Context.require(this.nodeInfos.get(peer_id) != null, "Invalid request(remove_node) target.");

        NodeInfo nodeInfo = this.nodeInfos.get(peer_id);
        if (!nodeInfo.checkOwner(Context.getCaller())) {
            Context.revert(101, "You do not have permission. (remove_node)");
        }

        this.nodeInfos.set(peer_id, null);
        removeNode(peer_id);
        if (nodeInfo.getEndpoint() != null) {
            BFSEvent(EventType.RemoveNode.name(), peer_id, nodeInfo.getEndpoint());
        }else {
            BFSEvent(EventType.RemoveNode.name(), peer_id, "");
        }

    }

    @Override
    @External()
    @Payable
    public void update_node(String peer_id,
                            @Optional String url,
                            @Optional String endpoint,
                            @Optional String name,
                            @Optional Address owner) {
        Context.require(this.nodeInfos.get(peer_id) != null, "Invalid request(update_node) target.");
        Context.require(url == null || url.startsWith("http://") || url.startsWith("https://"), "Invalid URL format.");

        NodeInfo nodeInfo = this.nodeInfos.get(peer_id);
        if (!nodeInfo.checkOwner(Context.getCaller())) {
            Context.revert(101, "You do not have permission. (update_node)");
        }

        Address ownerAddress = (owner == null) ? Context.getCaller() : owner;

        nodeInfo.update(name, url, endpoint, ownerAddress);
        this.nodeInfos.set(peer_id, nodeInfo);

        removeNode(peer_id);
        this.peers.add(peer_id);
        if(nodeInfo.getEndpoint() != null) {
            BFSEvent(EventType.UpdateNode.name(), peer_id, nodeInfo.getEndpoint());
        }else{
            BFSEvent(EventType.UpdateNode.name(), peer_id, "");
        }

    }

    @Override
    @External(readonly=true)
    public Map<String, Object> check_allocations(String cid) {
        CidInfo cidInfo = this.cidInfos.get(cid);
        if (cidInfo == null) {
            return null;
        }
        return Map.ofEntries(
                Map.entry("cid", cidInfo.getCid()),
                Map.entry("size", cidInfo.getSize()),
                Map.entry("replication_min", cidInfo.getReplication_min()),
                Map.entry("replication_max", cidInfo.getReplication_max()),
                Map.entry("user_allocations", cidInfo.getUser_allocations())
        );
    }

    @Override
    @External(readonly = true)
    public List<Object> all_node() {
        Object[] allNode = new Object[this.peers.size()];

        for (int i=0; i < this.peers.size(); i++) {
            NodeInfo nodeInfo = this.nodeInfos.get(this.peers.get(i));
            allNode[i] = nodeInfo;
        }

        return List.of(allNode);
    }

    @Override
    @External(readonly = true)
    public Map<String, Object> get_info() {
        return Map.ofEntries(
                Map.entry("frontIndexOfPeers", frontIndexOfPeers.getOrDefault(0)),
                Map.entry("backIndexOfPeers", backIndexOfPeers.getOrDefault(this.peers.size())),
                Map.entry("NumOfPeers", this.peers.size())
        );
    }

    @Override
    @External
    public void update_group(String group, BigInteger expire_at, @Optional String did_sign) {
        Context.require(!group.isEmpty(), "Blank key is not allowed.");
        String owner = Context.getCaller().toString();
        GroupInfo groupInfo = null;
        if (did_sign != null) {
            var sigChecker = new SignatureChecker();
            Context.require(sigChecker.verifySig(get_did_score(), did_sign), "failed to verify did_sign");
            owner = sigChecker.getOwnerId();
            groupInfo = get_group(owner, group);

            var updated = (groupInfo != null) ? groupInfo.getLast_updated() : 1;
            var expectedPayload = new Payload.Builder("update_group")
                    .group(group)
                    .expire_at(expire_at)
                    .baseHeight(updated)
                    .build();
            Context.require(sigChecker.validatePayload(expectedPayload), "failed to validate payload");
        }else{
            groupInfo = get_group(owner, group);
        }

        if (groupInfo != null) {
            // Update an existing group
            var attrs = new GroupInfo.Builder()
                    .expireAt(expire_at);
            attrs.lastUpdated(Context.getBlockHeight());

            groupInfo.update(attrs);
        }else{
            // Create a new group
            groupInfo = new GroupInfo.Builder()
                    .group(group)
                    .expireAt(expire_at)
                    .owner(owner)
                    .lastUpdated(Context.getBlockHeight())
                    .created(Context.getBlockHeight())
                    .build();
        }
        this.groupInfos.at(owner).set(group, groupInfo);
        BFSEvent(EventType.UpdateGroup.name(), group, owner);
    }

    @Override
    @External(readonly = true)
    public GroupInfo get_group(String owner, String group) {
        return this.groupInfos.at(owner).get(group);
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

    private String getVerifiedDid(String did_sign, Payload expected){
        var sigChecker = new SignatureChecker();
        Context.require(sigChecker.verifySig(get_did_score(), did_sign), "failed to verify did_sign");
        Context.require(sigChecker.validatePayload(expected), "failed to validate payload");
        return sigChecker.getOwnerId();
    }

    /*
     * Events
     */
    @EventLog
    public void BFSEvent(String event, String value1, String value2) {}
}