
package com.iconloop.score.bfs;

import com.iconloop.score.bfs.util.Jwt;
import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import com.parametacorp.jwt.Payload;
import com.parametacorp.util.Converter;
import foundation.icon.did.core.Algorithm;
import foundation.icon.did.core.AlgorithmProvider;
import foundation.icon.did.core.DidKeyHolder;
import foundation.icon.did.exceptions.AlgorithmException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import score.*;
import score.impl.Crypto;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


public class BfsContentsTest extends TestBase {
    private static final ServiceManager sm = getServiceManager();
    private static final Account owner1 = sm.createAccount();
    private static final Account owner2 = sm.createAccount();
    private static final Algorithm algorithm = AlgorithmProvider.create(AlgorithmProvider.Type.ES256K);
    private static Score didScore;
    private static Score bfsContentsScore;
    private static DidKeyHolder key1;
    private static DidKeyHolder key2;

    @BeforeAll
    static void beforeAll() throws Exception {
        didScore = sm.deploy(owner1, DidScoreMock.class);
        bfsContentsScore = sm.deploy(owner1, BfsContents.class, didScore.getAddress());
        key1 = createDidAndKeyHolder("key1");
        key2 = createDidAndKeyHolder("key2");

        // setup PRE nodes
        bfsContentsScore.invoke(owner1, "add_node","TEST_NODE_0", "http://testNode0", null, null, null);
        bfsContentsScore.invoke(owner1, "add_node","TEST_NODE_1", "http://testNode1", null, null, null);
        bfsContentsScore.invoke(owner1, "add_node","TEST_NODE_2", "http://testNode2", null, null, null);
        bfsContentsScore.invoke(owner1, "add_node","TEST_NODE_3", "http://testNode3", null, null, null);
        bfsContentsScore.invoke(owner1, "add_node","TEST_NODE_4", "http://testNode4", null, null, null);
    }

    private static DidKeyHolder createDidAndKeyHolder(String kid) throws AlgorithmException {
        var keyProvider = algorithm.generateKeyProvider(kid);
        var pubkey = algorithm.publicKeyToByte(keyProvider.getPublicKey());
        var did = createDummyDid(pubkey);
        didScore.invoke(owner1, "register", did, kid, pubkey);
        return new DidKeyHolder.Builder(keyProvider)
                .did(did)
                .build();
    }

    private static String createDummyDid(byte[] seeds) {
        byte[] msgHash = Crypto.hash("sha3-256", seeds);
        byte[] buf = new byte[24];
        System.arraycopy(msgHash, 0, buf, 0, buf.length);
        return "did:icon:03:" + Converter.bytesToHex(buf);
    }

    public static class ParamsBuilder {
        private final DidKeyHolder signer;
        private final String method;
        private String cid;
        private String group;
        private int size;
        private String expire_at;

        public ParamsBuilder(DidKeyHolder signer, String method){
            this.signer = signer;
            this.method = method;
        }

        public ParamsBuilder cid(String cid){
            this.cid = cid;
            return this;
        }

        public ParamsBuilder group(String group){
            this.group = group;
            return this;
        }

        public ParamsBuilder size(int size){
            this.size = size;
            return this;
        }

        public ParamsBuilder expire_at(String expire_at){
            this.expire_at = expire_at;
            return this;
        }

        public Object[] build() throws AlgorithmException {
            var pb = new Payload.Builder(method);
            if (cid != null) {
                pb.cid(cid);
            }

            if (group != null) {
                pb.group(group);
            }

            if (size != 0) {
                pb.size(size);
            }
            if (expire_at != null) {
                pb.expire_at(expire_at);
            }
            Jwt jwt = new Jwt.Builder(signer.getKid())
                    .payload(pb.build())
                    .build();
            var signature = jwt.sign(signer);
            var timestamp = BigInteger.valueOf(sm.getBlock().getTimestamp());
            switch (method) {
                case "pin":
                    return new Object[] {
                            cid, 100, expire_at,
                            // Optional
                            (group != null) ? group : null, null, signature
                    };
                case "unpin":
                    return new Object[] {
                            cid,
                            // Optional
                            signature,
                    };
                case "update_pin":
                    return new Object[] {
                            cid, expire_at,
                            // Optional
                            signature
                    };
                case "remove_pin":
                    return new Object[] {
                            cid,
                            // Optional
                            signature
                    };
                case "update_group":
                    return new Object[] {
                            group, expire_at,
                            // Optional
                            signature
                    };
            }
            throw new IllegalArgumentException("Invalid method: " + method);
        }
    }

    @Test
    void pinTest() throws Exception {
        //Pin
        String pinTimeStamp = GetMicrosecondTimestamp();
        bfsContentsScore.invoke(owner1, "pin",
                new ParamsBuilder(key1, "pin")
                        .cid("TEST_CID")
                        .size(100)
                        .expire_at(pinTimeStamp)
                        .build());
        var pin = (Map<String, Object>) bfsContentsScore.call("get_pin", key1.getDid(), "TEST_CID");
        System.out.println(pin);
        assertEquals(pinTimeStamp, pin.get("expire_at"));

        //Update
        String updatePinTimeStamp = GetMicrosecondTimestamp();
        bfsContentsScore.invoke(owner1, "update_pin",
                new ParamsBuilder(key1, "update_pin")
                        .cid("TEST_CID")
                        .expire_at(updatePinTimeStamp)
                        .build());
        pin = (Map<String, Object>) bfsContentsScore.call("get_pin", key1.getDid(), "TEST_CID");
        System.out.println(pin);
        assertEquals(updatePinTimeStamp, pin.get("expire_at"));


        //Unpin
        bfsContentsScore.invoke(owner1, "unpin",
                new ParamsBuilder(key1, "unpin")
                        .cid("TEST_CID")
                        .build());


        //Remove Pin
        bfsContentsScore.invoke(owner1, "remove_pin",
                new ParamsBuilder(key1, "remove_pin")
                        .cid("TEST_CID")
                        .build());
        pin = (Map<String, Object>) bfsContentsScore.call("get_pin", key1.getDid(), "TEST_CID");
        assertNull(pin);

    }

    @Test
    void groupTest() throws Exception {
        //update group
        String updateGroupTimeStamp = GetMicrosecondTimestamp();
        bfsContentsScore.invoke(owner1, "update_group",
                new ParamsBuilder(key1, "update_group")
                        .group("TEST_GROUP")
                        .expire_at(updateGroupTimeStamp)
                        .build());

        //check update group
        var group = (GroupInfo) bfsContentsScore.call("get_group", key1.getDid(), "TEST_GROUP");
        assertNotNull(group);
        assertEquals(updateGroupTimeStamp, group.getExpireAt());

        // Negative: An attempt was made to add a PIN to the group using an owner that does not match the group's registered owner
        assertThrows(UserRevertedException.class, () -> bfsContentsScore.invoke(owner1, "pin",
                new ParamsBuilder(key2, "pin")
                        .cid("TEST_CID")
                        .size(100)
                        .group("TEST_GROUP")
                        .expire_at(GetMicrosecondTimestamp())
                        .build()));

    }

    @Test
    void nodeTest() throws Exception {
        //add node
        bfsContentsScore.invoke(owner1, "add_node", "TEST_NODE", "http://testNode", null, null, null);
        var node = (NodeInfo) bfsContentsScore.call("get_node", "TEST_NODE");
        assertNotNull(node);

        //update node update
        bfsContentsScore.invoke(owner1, "update_node", "TEST_NODE", "http://updateNode", "TEST_ENDPOINT", "TEST_NAME", null);
        node = (NodeInfo) bfsContentsScore.call("get_node", "TEST_NODE");
        assertEquals("http://updateNode", node.getUrl());
        assertEquals("TEST_ENDPOINT", node.getEndpoint());
        assertEquals("TEST_NAME", node.getName());


        //remove node
        bfsContentsScore.invoke(owner1, "remove_node", "TEST_NODE");
        node = (NodeInfo) bfsContentsScore.call("get_node", "TEST_NODE");
        assertNull(node);
    }

    private String GetMicrosecondTimestamp(){
        Instant now = Instant.now();
        long seconds = now.getEpochSecond();
        int nanos = now.getNano();
        long micros = nanos / 1000;
        return String.format("%d%06d", seconds, micros);
    }

}



