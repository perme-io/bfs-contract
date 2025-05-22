package com.iconloop.score.bfs;

import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;


public class GroupInfo {
    private final String group;
    private BigInteger expireAt;
    private final String owner;
    private long lastUpdated;

    public GroupInfo(Builder builder) {
        this.group = builder.group;
        this.owner = builder.owner;
        this.expireAt = builder.expireAt;
        this.lastUpdated = builder.lastUpdated;
    }

    public String getGroup() {
        return group;
    }

    public String getOwner() {
        return owner;
    }

    public BigInteger getExpire_at() {
        return this.expireAt;
    }

    public long lastUpdated() {
        return this.lastUpdated;
    }

    public void update(Builder attrs) {
        if (attrs.expireAt != null) {
            this.expireAt = attrs.expireAt;
        }
        this.lastUpdated = attrs.lastUpdated;
    }

    public boolean checkOwner(String owner) {
        return this.owner.equals(owner);
    }


    public static void writeObject(ObjectWriter w, GroupInfo i) {
        w.writeListOfNullable(
                i.group,
                i.owner,
                i.expireAt,
                i.lastUpdated
        );
    }

    public static GroupInfo readObject(ObjectReader r) {
        r.beginList();
        GroupInfo l = new GroupInfo.Builder()
                .group(r.readString())
                .owner(r.readString())
                .expireAt(r.readBigInteger())
                .lastUpdated(r.readLong())
                .build();

        r.end();
        return l;
    }

    public static class Builder {
        private String group;
        private String owner;
        private BigInteger expireAt;
        private long lastUpdated;

        public Builder group(String group) {
            this.group = group;
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

        public GroupInfo build() {
            return new GroupInfo(this);
        }
    }
}
