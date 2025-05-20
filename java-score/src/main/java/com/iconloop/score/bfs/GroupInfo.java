package com.iconloop.score.bfs;

import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;


public class GroupInfo {
    private final String group;
    private String expireAt;
    private final String owner;
    private BigInteger lastUpdated;

    public GroupInfo(Builder builder) {
        this.group = builder.group;
        this.owner = builder.owner;
        this.expireAt = builder.expireAt;
        this.lastUpdated = (builder.lastUpdated != null) ? builder.lastUpdated : BigInteger.valueOf(Context.getBlockTimestamp());
    }

    public String getGroup() {
        return group;
    }

    public String getOwner() {
        return owner;
    }

    public String getExpire_at() {
        return this.expireAt;
    }

    public BigInteger lastUpdated() {
        return this.lastUpdated;
    }


    public void update(String expireAt) {
        this.expireAt = (expireAt == null) ? this.expireAt : expireAt;
        this.lastUpdated = BigInteger.valueOf(Context.getBlockTimestamp());
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
                .expireAt(r.readString())
                .lastUpdated(r.readBigInteger())
                .build();

        r.end();
        return l;
    }

    public static class Builder {
        private String group;
        private String owner;
        private String expireAt;
        private BigInteger lastUpdated;

        public Builder group(String group) {
            this.group = group;
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

        public GroupInfo build() {
            return new GroupInfo(this);
        }
    }
}
