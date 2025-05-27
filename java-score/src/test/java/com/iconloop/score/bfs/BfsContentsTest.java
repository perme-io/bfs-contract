package com.iconloop.score.bfs;

import com.iconloop.score.bfs.util.Jwt;
import com.iconloop.score.test.*;
import com.parametacorp.jwt.Payload;
import com.parametacorp.util.Converter;
import foundation.icon.did.core.Algorithm;
import foundation.icon.did.core.AlgorithmProvider;
import foundation.icon.did.core.DidKeyHolder;
import foundation.icon.did.exceptions.AlgorithmException;
import org.junit.jupiter.api.*;
import score.UserRevertedException;
import score.impl.Crypto;

import java.math.BigInteger;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BfsContentsTest extends TestBase {
    private final BigInteger UNPIN_STATE = BigInteger.valueOf(1);
    private static final BigInteger ONE_SECOND = BigInteger.valueOf(1_000_000L);
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
        bfsContentsScore.invoke(owner1, "add_node", "TEST_NODE_0", "http://testNode0", null, null, null);
        bfsContentsScore.invoke(owner1, "add_node", "TEST_NODE_1", "http://testNode1", null, null, null);
        bfsContentsScore.invoke(owner1, "add_node", "TEST_NODE_2", "http://testNode2", null, null, null);
        bfsContentsScore.invoke(owner1, "add_node", "TEST_NODE_3", "http://testNode3", null, null, null);
        bfsContentsScore.invoke(owner1, "add_node", "TEST_NODE_4", "http://testNode4", null, null, null);
    }

    private static DidKeyHolder createDidAndKeyHolder(String kid) throws AlgorithmException {
        var keyProvider = algorithm.generateKeyProvider(kid);
        var pubkey = algorithm.publicKeyToByte(keyProvider.getPublicKey());
        var did = createDummyDid(pubkey);
        didScore.invoke(owner1, "register", did, kid, pubkey);
        return new DidKeyHolder.Builder(keyProvider).did(did).build();
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
        private BigInteger size;
        private BigInteger expire_at;
        private long baseHeight;

        public ParamsBuilder(DidKeyHolder signer, String method) {
            this.signer = signer;
            this.method = method;
        }

        public ParamsBuilder cid(String cid) {
            this.cid = cid;
            return this;
        }

        public ParamsBuilder group(String group) {
            this.group = group;
            return this;
        }

        public ParamsBuilder size(BigInteger size) {
            this.size = size;
            return this;
        }

        public ParamsBuilder expire_at(BigInteger expire_at) {
            this.expire_at = expire_at;
            return this;
        }

        public ParamsBuilder baseHeight(long baseHeight) {
            this.baseHeight = baseHeight;
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

            if (size != null) {
                pb.size(size);
            }
            if (expire_at != null) {
                pb.expire_at(expire_at);
            }

            if (baseHeight > 0) {
                pb.baseHeight(baseHeight);
            }

            String signature = null;
            if (signer != null){
                Jwt jwt = new Jwt.Builder(signer.getKid()).payload(pb.build()).build();
                signature = jwt.sign(signer);
            }
            switch (method) {
                case "pin":
                    return new Object[]{cid, 100, expire_at,
                            // Optional
                            (group != null) ? group : null, null, signature};
                case "unpin":
                    return new Object[]{cid,
                            // Optional
                            signature,};
                case "update_pin":
                    return new Object[]{cid, expire_at,
                            // Optional
                            signature};
                case "remove_pin":
                    return new Object[]{cid,
                            // Optional
                            signature};
                case "update_group":
                    return new Object[]{group, expire_at,
                            // Optional
                            signature};
            }
            throw new IllegalArgumentException("Invalid method: " + method);
        }
    }

    @Test
    @Order(1)
    void pinTest() throws Exception {
        // Negative: Invalid expire_at
        assertThrows(UserRevertedException.class, () -> bfsContentsScore.invoke(owner1, "pin", new ParamsBuilder(key1, "pin").cid("TEST_CID").size(BigInteger.valueOf(100)).expire_at(UNPIN_STATE).build()));

        //Pin
        BigInteger pinTimeStamp = getTimeStamp(1);
        bfsContentsScore.invoke(owner1, "pin", new ParamsBuilder(key1, "pin").cid("TEST_CID").size(BigInteger.valueOf(100)).expire_at(pinTimeStamp).build());
        var pin = (Map<String, Object>) bfsContentsScore.call("get_pin", key1.getDid(), "TEST_CID");
        System.out.println(pin);
        assertEquals(pinTimeStamp, pin.get("expire_at"));

        // Negative: try to add with the same cid
        assertThrows(UserRevertedException.class, () ->
                bfsContentsScore.invoke(owner1, "pin", new ParamsBuilder(key1, "pin").cid("TEST_CID").size(BigInteger.valueOf(100)).expire_at(pinTimeStamp).build()));


        //Pin (non did_sign)
        BigInteger pinTimeStampNonDidSign = getTimeStamp(1);
        bfsContentsScore.invoke(owner1, "pin", new ParamsBuilder(null, "pin").cid("TEST_CID_NON_DIDSIGN").size(BigInteger.valueOf(100)).expire_at(pinTimeStampNonDidSign).build());
        pin = (Map<String, Object>) bfsContentsScore.call("get_pin", owner1.getAddress().toString(), "TEST_CID_NON_DIDSIGN");
        System.out.println(pin);
        assertEquals(pinTimeStampNonDidSign, pin.get("expire_at"));


        // Positive: Another owner adds the same cid
        bfsContentsScore.invoke(owner2, "pin", new ParamsBuilder(key2, "pin").cid("TEST_CID").size(BigInteger.valueOf(100)).expire_at(pinTimeStamp).build());
        pin = (Map<String, Object>) bfsContentsScore.call("get_pin", key1.getDid(), "TEST_CID");
        System.out.println(pin);
        assertEquals(pinTimeStamp, pin.get("expire_at"));


        //Update
        BigInteger updatePinTimeStamp = getTimeStamp(1);
        bfsContentsScore.invoke(owner1, "update_pin", new ParamsBuilder(key1, "update_pin")
                .cid("TEST_CID").expire_at(updatePinTimeStamp).baseHeight(sm.getBlock().getHeight()).build());
        pin = (Map<String, Object>) bfsContentsScore.call("get_pin", key1.getDid(), "TEST_CID");
        System.out.println(pin);
        assertEquals(updatePinTimeStamp, pin.get("expire_at"));

        //Update ( non did_sign )
        BigInteger updatePinTimeStampNonDidSign = getTimeStamp(1);
        bfsContentsScore.invoke(owner1, "update_pin", new ParamsBuilder(null, "update_pin")
                .cid("TEST_CID_NON_DIDSIGN").expire_at(updatePinTimeStampNonDidSign).baseHeight(sm.getBlock().getHeight()).build());
        pin = (Map<String, Object>) bfsContentsScore.call("get_pin", owner1.getAddress().toString(), "TEST_CID_NON_DIDSIGN");
        System.out.println(pin);
        assertEquals(updatePinTimeStampNonDidSign, pin.get("expire_at"));


        // Negative: try to update with an invalid baseHeight
        final long invalidBaseHeight = sm.getBlock().getHeight() - 2;
        assertThrows(UserRevertedException.class, () -> bfsContentsScore.invoke(owner1, "update_pin",
                new ParamsBuilder(key1, "update_pin").cid("TEST_CID").expire_at(updatePinTimeStamp).baseHeight(invalidBaseHeight).build()));
        final long invalidBaseHeight2 = sm.getBlock().getHeight() + 1;
        assertThrows(UserRevertedException.class, () -> bfsContentsScore.invoke(owner1, "update_pin",
                new ParamsBuilder(key1, "update_pin").cid("TEST_CID").expire_at(updatePinTimeStamp).baseHeight(invalidBaseHeight2).build()));
        final long invalidBaseHeight3 = sm.getBlock().getHeight() + 1;
        assertThrows(UserRevertedException.class, () -> bfsContentsScore.invoke(owner1, "unpin",
                new ParamsBuilder(key1, "unpin").cid("TEST_CID").baseHeight(invalidBaseHeight3).build()));

        //Unpin
        bfsContentsScore.invoke(owner1, "unpin", new ParamsBuilder(key1, "unpin").cid("TEST_CID").baseHeight(sm.getBlock().getHeight()).build());
        pin = (Map<String, Object>) bfsContentsScore.call("get_pin", key1.getDid(), "TEST_CID");
        System.out.println(pin);
        assertEquals(UNPIN_STATE, pin.get("expire_at"));


        //Remove Pin
        bfsContentsScore.invoke(owner1, "remove_pin", new ParamsBuilder(key1, "remove_pin").cid("TEST_CID").build());
        pin = (Map<String, Object>) bfsContentsScore.call("get_pin", key1.getDid(), "TEST_CID");
        assertNull(pin);

        // Negative: Cannot remove: the item is still pin
        assertThrows(UserRevertedException.class, () -> bfsContentsScore.invoke(owner2, "remove_pin", new ParamsBuilder(key2, "remove_pin").cid("TEST_CID").build()));

        bfsContentsScore.invoke(owner2, "unpin", new ParamsBuilder(key2, "unpin").cid("TEST_CID").baseHeight(sm.getBlock().getHeight()).build());
        pin = (Map<String, Object>) bfsContentsScore.call("get_pin", key2.getDid(), "TEST_CID");
        System.out.println(pin);
        assertEquals(UNPIN_STATE, pin.get("expire_at"));

        bfsContentsScore.invoke(owner2, "remove_pin", new ParamsBuilder(key2, "remove_pin").cid("TEST_CID").build());
        pin = (Map<String, Object>) bfsContentsScore.call("get_pin", key2.getDid(), "TEST_CID");
        assertNull(pin);
    }

    @Test
    @Order(2)
    void groupTest() throws Exception {
        BigInteger pinTimeStamp = getTimeStamp(1);
        bfsContentsScore.invoke(owner1, "pin", new ParamsBuilder(key1, "pin")
                .cid("TEST_CID").size(BigInteger.valueOf(100)).group("TEST_GROUP").expire_at(pinTimeStamp).build());
        var pin = (Map<String, Object>) bfsContentsScore.call("get_pin", key1.getDid(), "TEST_CID");
        assertEquals(pinTimeStamp, pin.get("expire_at"));
        System.out.println(pin);


        //create group
        BigInteger groupTimeStamp = getTimeStamp(1);
        bfsContentsScore.invoke(owner1, "update_group",
                new ParamsBuilder(key1, "update_group").group("TEST_GROUP").expire_at(groupTimeStamp).baseHeight(sm.getBlock().getHeight()).build());
        var group = (GroupInfo) bfsContentsScore.call("get_group", key1.getDid(), "TEST_GROUP");
        assertNotNull(group);
        assertEquals(groupTimeStamp, group.getExpire_at());

        //create group ( non did_sign )
        BigInteger groupTimeStampNonDidSign = getTimeStamp(1);
        bfsContentsScore.invoke(owner1, "update_group",
                new ParamsBuilder(null, "update_group").group("TEST_GROUP_NON_DIDSIGN").expire_at(groupTimeStampNonDidSign).baseHeight(sm.getBlock().getHeight()).build());
        group = (GroupInfo) bfsContentsScore.call("get_group", owner1.getAddress().toString(), "TEST_GROUP_NON_DIDSIGN");
        assertNotNull(group);
        assertEquals(groupTimeStampNonDidSign, group.getExpire_at());

        //update group
        BigInteger updateGroupTimeStamp = getTimeStamp(1);
        bfsContentsScore.invoke(owner1, "update_group",
                new ParamsBuilder(key1, "update_group").group("TEST_GROUP").expire_at(updateGroupTimeStamp).baseHeight(sm.getBlock().getHeight()).build());
        group = (GroupInfo) bfsContentsScore.call("get_group", key1.getDid(), "TEST_GROUP");
        assertNotNull(group);
        assertEquals(updateGroupTimeStamp, group.getExpire_at());
        pin = (Map<String, Object>) bfsContentsScore.call("get_pin", key1.getDid(), "TEST_CID");
        assertEquals(updateGroupTimeStamp, pin.get("expire_at"));
        System.out.println(pin);


        //update group ( non did_sign )
        BigInteger updateGroupTimeStampNonDidSign = getTimeStamp(1);
        bfsContentsScore.invoke(owner1, "update_group",
                new ParamsBuilder(key1, "update_group").group("TEST_GROUP_NON_DIDSIGN").expire_at(updateGroupTimeStampNonDidSign).baseHeight(sm.getBlock().getHeight()).build());
        group = (GroupInfo) bfsContentsScore.call("get_group", key1.getDid(), "TEST_GROUP_NON_DIDSIGN");
        assertNotNull(group);
        assertEquals(updateGroupTimeStampNonDidSign, group.getExpire_at());


        // Negative: try to update with an invalid baseHeight
        final long invalidBaseHeight = sm.getBlock().getHeight() - 2;
        assertThrows(UserRevertedException.class, () -> bfsContentsScore.invoke(owner1, "update_group",
                new ParamsBuilder(key1, "update_group").group("TEST_GROUP").expire_at(updateGroupTimeStamp).baseHeight(invalidBaseHeight).build()));
        final long invalidBaseHeight2 = sm.getBlock().getHeight() + 1;
        assertThrows(UserRevertedException.class, () -> bfsContentsScore.invoke(owner1, "update_group",
                new ParamsBuilder(key1, "update_group").group("TEST_GROUP").expire_at(updateGroupTimeStamp).baseHeight(invalidBaseHeight2).build()));

        bfsContentsScore.invoke(owner1, "update_group",
                new ParamsBuilder(key1, "update_group").group("TEST_GROUP").expire_at(UNPIN_STATE).baseHeight(sm.getBlock().getHeight()).build());
        pin = (Map<String, Object>) bfsContentsScore.call("get_pin", key1.getDid(), "TEST_CID");
        assertEquals(UNPIN_STATE, pin.get("expire_at"));
        System.out.println(pin);


    }

    @Test
    @Order(3)
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

        // Negative: Another owner tries a node update
        assertThrows(UserRevertedException.class, () -> bfsContentsScore.invoke(owner2, "update_node", "TEST_NODE", "http://updateNode", "TEST_ENDPOINT", "TEST_NAME", null));


        //remove node
        bfsContentsScore.invoke(owner1, "remove_node", "TEST_NODE");
        node = (NodeInfo) bfsContentsScore.call("get_node", "TEST_NODE");
        assertNull(node);

        // Negative
        assertThrows(UserRevertedException.class, () -> bfsContentsScore.invoke(owner2, "remove_node", "TEST_NODE"));
    }

    private BigInteger getTimeStamp(int hour) {
        BigInteger timestampMicros = BigInteger.valueOf(sm.getBlock().getTimestamp());

        BigInteger offsetMicros = BigInteger.valueOf(hour)
                .multiply(BigInteger.valueOf(3600L))
                .multiply(ONE_SECOND);

        return timestampMicros.add(offsetMicros);
    }

}



