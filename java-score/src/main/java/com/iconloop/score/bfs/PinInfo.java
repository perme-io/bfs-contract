package com.iconloop.score.bfs;

import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;
import java.util.Map;


public class PinInfo {
    private final String cid;
    private final String group;
    private final String name;
    private final long created;
    private final String owner;
    private BigInteger expireAt;
    private long lastUpdated;

    public PinInfo(Builder builder) {
        this.cid = builder.cid;
        this.group = (builder.group == null) ? "" : builder.group;
        this.name = (builder.name == null) ? "" : builder.name;
        this.created = builder.created;
        this.owner = builder.owner;
        this.expireAt = builder.expireAt;
        this.lastUpdated = Math.max(builder.lastUpdated, created);
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

    public long getCreated() {
        return created;
    }

    public String getOwner() {
        return this.owner;
    }

    public BigInteger getExpire_at() {
        return expireAt;
    }

    public long getLastUpdated() {
        return this.lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setExpireAt(BigInteger expireAt) {
        this.expireAt = expireAt;
    }

    public void update(Builder attrs) {
        if (attrs.expireAt != null) {
            this.expireAt = attrs.expireAt;
        }
        this.lastUpdated = attrs.lastUpdated;
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
                .created(r.readLong())
                .owner(r.readString())
                .expireAt(r.readBigInteger())
                .lastUpdated(r.readLong())
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
                Map.entry("last_updated", this.lastUpdated)
        );
    }

    public static class Builder {
        private String cid;
        private String group;
        private String name;
        private long created;
        private String owner;
        private BigInteger expireAt;
        private long lastUpdated;

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

        public Builder created(long created) {
            this.created = created;
            return this;
        }

        public Builder owner(String owner) {
            this.owner = owner;
            return this;
        }

        public Builder expireAt(BigInteger expireAt){
            this.expireAt = expireAt;
            return this;
        }

        public Builder lastUpdated(long lastUpdated){
            this.lastUpdated = lastUpdated;
            return this;
        }

        public PinInfo build() {
            return new PinInfo(this);
        }

    }
}
