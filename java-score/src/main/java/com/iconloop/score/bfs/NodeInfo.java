package com.iconloop.score.bfs;

import score.Address;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;
import java.util.Map;


public class NodeInfo {
    private final String peerId;
    private String name;
    private String endpoint;
    private String comment;
    private String created;
    private Address owner;
    private BigInteger stake;
    private BigInteger reward;
    private String complaints;
    private Boolean complained;

    public NodeInfo(String peer_id,
                    String name,
                    String endpoint,
                    String comment,
                    String created,
                    Address owner,
                    BigInteger stake,
                    BigInteger reward,
                    String complaints,
                    Boolean complained) {
        this.peerId = peer_id;
        this.name = name;
        this.endpoint = endpoint;
        this.comment = comment;
        this.created = created;
        this.owner = owner;
        this.stake = stake;
        this.reward = reward;
        this.complaints = complaints;
        this.complained = complained;
    }

    public void update(String name,
                       String endpoint,
                       String comment,
                       String created,
                       Address owner,
                       BigInteger stake,
                       BigInteger reward) {
        this.name = (name == null) ? this.name : name;
        this.endpoint = (endpoint == null) ? this.endpoint : endpoint;
        this.comment = (comment == null) ? this.comment : comment;
        this.created = (created == null) ? this.created : created;
        this.owner = (owner == null) ? this.owner : owner;
        this.stake = (stake == null) ? this.stake : stake;
        this.reward = (reward == null) ? this.reward : reward;
    }

    public boolean checkOwner(Address owner) {
        return this.owner.equals(owner);
    }

    public String getEndpoint() {
        return this.endpoint;
    }

    public BigInteger getStake() {
        return this.stake;
    }

    public boolean addComplain(String complainFrom, long timestamp, int nodeCount) {
        if(!this.complained) {
            Complaints complaints = new Complaints(this.complaints);
            complaints.addComplain(complainFrom, timestamp);
            this.complaints = complaints.toString();
            this.complained = complaints.isComplained(nodeCount).equals(BigInteger.ONE);
        }
        return this.complained;
    }

    public boolean isComplained() {
        return this.complained;
    }

    public static void writeObject(ObjectWriter w, NodeInfo t) {
        w.beginList(10);
        w.writeNullable(t.peerId);
        w.writeNullable(t.name);
        w.writeNullable(t.endpoint);
        w.writeNullable(t.comment);
        w.writeNullable(t.created);
        w.write(t.owner);
        w.writeNullable(t.stake);
        w.writeNullable(t.reward);
        w.writeNullable(t.complaints);
        w.write(t.complained);
        w.end();
    }

    public static NodeInfo readObject(ObjectReader r) {
        r.beginList();
        NodeInfo t = new NodeInfo(
                r.readNullable(String.class),
                r.readNullable(String.class),
                r.readNullable(String.class),
                r.readNullable(String.class),
                r.readNullable(String.class),
                r.readAddress(),
                r.readNullable(BigInteger.class),
                r.readNullable(BigInteger.class),
                r.readNullable(String.class),
                r.readBoolean());
        r.end();
        return t;
    }

    public Map<String, Object> toMap() {
        Complaints complaints = new Complaints(this.complaints);
        return Map.ofEntries(
                Map.entry("peer_id", this.peerId),
                Map.entry("endpoint", this.endpoint),
                Map.entry("name", this.name),
                Map.entry("comment", this.comment),
                Map.entry("created", this.created),
                Map.entry("owner", this.owner.toString()),
                Map.entry("stake", (this.stake == null) ? BigInteger.ZERO : this.stake),
                Map.entry("reward", (this.reward == null) ? BigInteger.ZERO : this.reward),
                Map.entry("complaints", complaints.toMap()),
                Map.entry("complained", this.complained)
        );
    }
}
