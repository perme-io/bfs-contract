package com.iconloop.score.bfs;

import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;
import java.util.Map;


public class PinInfo {
    private final String cid;
    private String tracker;
    private String name;
    private String comment;
    private String created;
    private String owner;
    private Integer replicationMin;
    private Integer replicationMax;
    private String[] userAllocations;
    private BigInteger shardSize;
    private String expireAt;
    private String expireIn;
    private BigInteger state;


    public PinInfo(String cid,
                   String tracker,
                   String name,
                   String comment,
                   String created,
                   String owner,
                   Integer replicationMin,
                   Integer replicationMax,
                   String[] userAllocations,
                   BigInteger shardSize,
                   String expireAt,
                   String expireIn,
                   BigInteger state) {
        this.cid = cid;
        this.tracker = tracker;
        this.name = name;
        this.comment = comment;
        this.created = created;
        this.owner = owner;
        this.replicationMin = replicationMin;
        this.replicationMax = replicationMax;
        this.userAllocations = userAllocations;
        this.shardSize = (shardSize == null) ? BigInteger.ZERO : shardSize;
        this.expireAt = expireAt;
        this.expireIn = expireIn;
        this.state = state;
    }

    public void update(String tracker,
                       String name,
                       String comment,
                       String created,
                       String owner,
                       Integer replicationMin,
                       Integer replicationMax,
                       String[] userAllocations,
                       BigInteger shardSize,
                       String expireAt,
                       String expireIn,
                       BigInteger state) {
        this.tracker = (tracker == null) ? this.tracker : tracker;
        this.name = (name == null) ? this.name : name;
        this.comment = (comment == null) ? this.comment : comment;
        this.created = (created == null) ? this.created : created;
        this.owner = (owner == null) ? this.owner : owner;
        this.replicationMin = (replicationMin == 0) ? this.replicationMin : replicationMin;
        this.replicationMax = (replicationMax == 0) ? this.replicationMax : replicationMax;
        this.userAllocations = (userAllocations == null) ? this.userAllocations : userAllocations;
        this.shardSize = (shardSize.intValue() == 0) ? this.shardSize : shardSize;
        this.expireAt = (expireAt == null) ? this.expireAt : expireAt;
        this.expireIn = (expireIn == null) ? this.expireIn : expireIn;
        this.state = (state.intValue() == 0) ? this.state : state;
    }

    public boolean checkOwner(String owner) {
        return this.owner.equals(owner);
    }

    public void unpin() {
        this.state = BigInteger.ZERO;
    }

    public String[] userAllocations() {
        return this.userAllocations;
    }

    public void reallocation(String[] allocations) {
        this.userAllocations = allocations;
    }

    public Integer getReplicationMin() {
        return this.replicationMin;
    }

    public Integer getReplicationMax() {
        return this.replicationMax;
    }

    public BigInteger getState() {
        return this.state;
    }

    public static void writeObject(ObjectWriter w, PinInfo t) {
        String userAllocations = Helper.StringListToJsonString(t.userAllocations);
        w.beginList(13);
        w.writeNullable(t.cid);
        w.writeNullable(t.tracker);
        w.writeNullable(t.name);
        w.writeNullable(t.comment);
        w.writeNullable(t.created);
        w.writeNullable(t.owner);
        w.write(t.replicationMin);
        w.write(t.replicationMax);
        w.write(userAllocations);
        w.write(t.shardSize);
        w.writeNullable(t.expireAt);
        w.writeNullable(t.expireIn);
        w.write(t.state);
        w.end();
    }

    public static PinInfo readObject(ObjectReader r) {
        r.beginList();
        PinInfo t = new PinInfo(
                r.readNullable(String.class),
                r.readNullable(String.class),
                r.readNullable(String.class),
                r.readNullable(String.class),
                r.readNullable(String.class),
                r.readNullable(String.class),
                r.readInt(),
                r.readInt(),
                Helper.JsonStringToStringList("userAllocations", r.readNullable(String.class)),
                r.readBigInteger(),
                r.readNullable(String.class),
                r.readNullable(String.class),
                r.readBigInteger()
        );
        r.end();
        return t;
    }

    public Map<String, Object> toMap() {
        return Map.ofEntries(
                Map.entry("cid", this.cid),
                Map.entry("tracker", this.tracker),
                Map.entry("name", this.name),
                Map.entry("comment", this.comment),
                Map.entry("created", this.created),
                Map.entry("owner", this.owner),
                Map.entry("replication_min", this.replicationMin),
                Map.entry("replication_max", this.replicationMax),
                Map.entry("user_allocations", this.userAllocations),
                Map.entry("shard_size", this.shardSize),
                Map.entry("expire_at", this.expireAt),
                Map.entry("expire_in", this.expireIn),
                Map.entry("state", this.state)
        );
    }
}
