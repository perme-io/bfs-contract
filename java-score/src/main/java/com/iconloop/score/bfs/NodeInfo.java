package com.iconloop.score.bfs;

import score.Address;
import score.ObjectReader;
import score.ObjectWriter;


public class NodeInfo {
    private final String peerId;
    private String url;
    private String name;
    private String endpoint;
    private final long created;
    private Address owner;

    public NodeInfo(Builder builder) {
        this.peerId = builder.peerId;
        this.url = builder.url;
        this.name = (builder.name == null) ? "" : builder.name;
        this.endpoint = (builder.endpoint == null) ? "" : builder.endpoint;
        this.created = builder.created;
        this.owner = builder.owner;
    }

    public String getPeer_id() {
        return peerId;
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public long getCreated() {
        return created;
    }

    public Address getOwner() {
        return owner;
    }

    public void update(String name,
                       String url,
                       String endpoint,
                       Address owner) {
        this.name = (name == null) ? this.name : name;
        this.url = (url == null) ? this.url : url;
        this.endpoint = (endpoint == null) ? this.endpoint : endpoint;
        this.owner = (owner == null) ? this.owner : owner;
    }


    public boolean checkOwner(Address owner) {
        return this.owner.equals(owner);
    }

    public static void writeObject(ObjectWriter w, NodeInfo n) {
        w.writeListOfNullable(
                n.peerId,
                n.url,
                n.name,
                n.endpoint,
                n.created,
                n.owner
        );
    }

    public static NodeInfo readObject(ObjectReader r) {
        r.beginList();
        NodeInfo t = new Builder()
                .peerId(r.readString())
                .url(r.readString())
                .name(r.readNullable(String.class))
                .endpoint(r.readNullable(String.class))
                .created(r.readLong())
                .owner(r.readAddress()).build();
        r.end();
        return t;
    }

    @Override
    public String toString() {
        return "NodeInfo{" +
                "peerId='" + peerId + '\'' +
                ", url='" + url + '\'' +
                ", name='" + name + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", created='" + created + '\'' +
                ", owner=" + owner +
                '}';
    }

    public static class Builder {
        private String peerId;
        private String url;
        private String name;
        private String endpoint;
        private long created;
        private Address owner;

        public Builder peerId(String peerId) {
            this.peerId = peerId;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder created(long created) {
            this.created = created;
            return this;
        }

        public Builder owner(Address owner) {
            this.owner = owner;
            return this;
        }

        public NodeInfo build() {
            return new NodeInfo(this);
        }
    }

}
