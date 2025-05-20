package com.iconloop.score.bfs;

import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;
import java.util.Map;


public class CidInfo {
    private final String cid;
    private final Integer size;
    private final BigInteger replicationMin;
    private final BigInteger replicationMax;
    private String[] userAllocations;
    private final BigInteger shardSize;
    private Integer refCnt;

    public CidInfo(Builder builder) {
        this.cid = builder.cid;
        this.size = builder.size;
        this.replicationMin = builder.replicationMin;
        this.replicationMax = builder.replicationMax;
        this.userAllocations = builder.userAllocations;
        this.shardSize = (builder.shardSize == null) ? BigInteger.ZERO : builder.shardSize;
        this.refCnt = builder.refCnt == null ? 0 : builder.refCnt;
    }

    public String getCid() {
        return cid;
    }

    public Integer getSize() {
        return size;
    }

    public String[] getUserAllocations() {
        return userAllocations;
    }

    public void setUserReallocation(String[] allocations) {
        this.userAllocations = allocations;
    }

    public BigInteger getShardSize() {
        return shardSize;
    }

    public Integer getRefCnt(){
        return this.refCnt;
    }

    public void setRefCnt(Integer cnt){
        this.refCnt = cnt;
    }


    public BigInteger getReplicationMin() {
        return this.replicationMin;
    }

    public BigInteger getReplicationMax() {
        return this.replicationMax;
    }

    public static void writeObject(ObjectWriter w, CidInfo c) {
        String userAllocations = Helper.StringListToJsonString(c.userAllocations);
        w.writeListOfNullable(
                c.cid,
                c.size,
                c.replicationMin,
                c.replicationMax,
                userAllocations,
                c.shardSize,
                c.refCnt
        );
    }

    public static CidInfo readObject(ObjectReader r) {
        r.beginList();
        CidInfo t = new Builder()
                .cid(r.readString())
                .size(r.readInt())
                .replicationMin(r.readBigInteger())
                .replicationMax(r.readBigInteger())
                .userAllocations(Helper.JsonStringToStringList("userAllocations", r.readNullable(String.class)))
                .shardSize(r.readBigInteger())
                .refCnt(r.readInt())
                .build();
        r.end();
        return t;
    }

    public Map<String, Object> toMap() {
        return Map.ofEntries(
                Map.entry("cid", this.cid),
                Map.entry("size", this.size),
                Map.entry("replication_min", this.replicationMin),
                Map.entry("replication_max", this.replicationMax),
                Map.entry("user_allocations", this.userAllocations),
                Map.entry("shard_size", this.shardSize)
        );
    }

    public static class Builder {
        private String cid;
        private Integer size;
        private BigInteger replicationMin;
        private BigInteger replicationMax;
        private String[] userAllocations;
        private BigInteger shardSize;
        private Integer refCnt;

        public Builder cid(String cid) {
            this.cid = cid;
            return this;
        }

        public Builder size(Integer size) {
            this.size = size;
            return this;
        }

        public Builder replicationMin(BigInteger replicationMin) {
            this.replicationMin = replicationMin;
            return this;
        }

        public Builder replicationMax(BigInteger replicationMax) {
            this.replicationMax = replicationMax;
            return this;
        }

        public Builder userAllocations(String[] userAllocations) {
            this.userAllocations = userAllocations;
            return this;
        }

        public Builder shardSize(BigInteger shardSize) {
            this.shardSize = shardSize;
            return this;
        }

        public Builder refCnt(Integer refCnt){
            this.refCnt = refCnt;
            return this;
        }

        public CidInfo build() {
            return new CidInfo(this);
        }

    }
}
