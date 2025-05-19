package com.iconloop.score.bfs;

import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;
import java.util.Map;


public class PinInfo {
    private final String cid;
    private final String group;
    private final String name;
    private final String created;
    private final String owner;
    private String expireAt;
    private BigInteger lastUpdated;

    public PinInfo(Builder builder) {
        this.cid = builder.cid;
        this.group = (builder.group == null) ? "" : builder.group;
        this.name = (builder.name == null) ? "" : builder.name;
        this.created = builder.created;
        this.owner = builder.owner;
        this.expireAt = builder.expireAt;
        this.lastUpdated = (builder.lastUpdated != null) ? builder.lastUpdated : BigInteger.valueOf(Context.getBlockTimestamp());
    }

    public String getCid() {
        return cid;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getCreated() {
        return created;
    }

    public String getOwner() {
        return this.owner;
    }

    public String getExpireAt() {
        return expireAt;
    }

    public BigInteger getLastUpdated() {
        return this.lastUpdated;
    }

    public void update(String expireAt) {
        this.expireAt = (expireAt == null) ? this.expireAt : expireAt;
        this.lastUpdated = BigInteger.valueOf(Context.getBlockTimestamp());
    }

    public boolean checkOwner(String owner) {
        return this.owner.equals(owner);
    }

    public void unpin() {
        this.lastUpdated = BigInteger.valueOf(Context.getBlockTimestamp());
    }

    public boolean checkLastUpdated(BigInteger lastUpdated) {
        return this.lastUpdated.equals(lastUpdated);
    }

    public static void writeObject(ObjectWriter w, PinInfo i) {
       w.writeListOfNullable(
               i.cid,
               i.group,
               i.name,
               i.created,
               i.owner,
               i.expireAt,
               i.lastUpdated
       );
    }

    public static PinInfo readObject(ObjectReader r) {
        r.beginList();
        PinInfo l = new Builder()
                .cid(r.readString())
                .group(r.readNullable(String.class))
                .name(r.readNullable(String.class))
                .created(r.readString())
                .owner(r.readString())
                .expireAt(r.readString())
                .lastUpdated(r.readBigInteger())
                .build();

        r.end();
        return l;
    }

    public Map<String, Object> toMap() {
        return Map.ofEntries(
                Map.entry("cid", this.cid),
                Map.entry("group", this.group),
                Map.entry("name", this.name),
                Map.entry("created", this.created),
                Map.entry("owner", this.owner),
                Map.entry("expire_at", this.expireAt),
                Map.entry("last_updated", this.lastUpdated)
        );
    }

    @Override
    public String toString() {
        return "PinInfo{" +
                "cid='" + cid + '\'' +
                ", group='" + group + '\'' +
                ", name='" + name + '\'' +
                ", created='" + created + '\'' +
                ", owner='" + owner + '\'' +
                ", expireAt='" + expireAt + '\'' +
                ", lastUpdated=" + lastUpdated +
                '}';
    }


    public static class Builder {
        private String cid;
        private String group;
        private String name;
        private String created;
        private String owner;
        private String expireAt;
        private BigInteger lastUpdated;

        public Builder cid(String cid) {
            this.cid = cid;
            return this;
        }

        public Builder group(String group) {
            this.group = group;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder created(String created) {
            this.created = created;
            return this;
        }

        public Builder owner(String owner) {
            this.owner = owner;
            return this;
        }

        public Builder expireAt(String expireAt){
            this.expireAt = expireAt;
            return this;
        }

        public Builder lastUpdated(BigInteger lastUpdated){
            this.lastUpdated = lastUpdated;
            return this;
        }

        public PinInfo build() {
            return new PinInfo(this);
        }

    }
}
