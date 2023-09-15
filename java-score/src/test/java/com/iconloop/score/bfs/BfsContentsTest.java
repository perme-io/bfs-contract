package com.iconloop.score.bfs;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import foundation.icon.icx.crypto.ECDSASignature;
import foundation.icon.icx.data.Bytes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import score.Address;
import score.UserRevertedException;

import java.math.BigInteger;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;


public class BfsContentsTest extends TestBase {
    private static final ServiceManager sm = getServiceManager();
    private Account[] owners;
    private String[] owners_did;
    private ECKeyPair[] owners_keyPair;
    private Score bfsContentsScore;
    private BfsContents bfsContentsSpy;
    private Score didSummaryScore;


    @BeforeEach
    void setup() throws Exception {
        // setup accounts and deploy
        owners = new Account[3];
        owners_did = new String[3];
        owners_keyPair = new ECKeyPair[3];

        for (int i = 0; i < owners.length; i++) {
            owners[i] = sm.createAccount(100);
            owners_did[i] = "did:icon:01:" + i;

            BigInteger privKey = new BigInteger("a" + i, 16);
            BigInteger pubKey = Sign.publicKeyFromPrivate(privKey);
            owners_keyPair[i] = new ECKeyPair(privKey, pubKey);
        }

        bfsContentsScore = sm.deploy(owners[0], BfsContents.class);
        didSummaryScore = sm.deploy(owners[0], DidSummaryMock.class);

        // setup spy
        bfsContentsSpy = (BfsContents) spy(bfsContentsScore.getInstance());
        bfsContentsScore.setInstance(bfsContentsSpy);

        // setup External Score
        Address didSummaryScore_ = didSummaryScore.getAddress();
        bfsContentsScore.invoke(owners[0], "set_did_summary_score", didSummaryScore_);

        // Add public key to DID summary Contract
        for (int i = 0; i < owners.length; i++) {
            DidMessage owners_sign = makeSignature(owners_did[i], owners_keyPair[i], "publicKey", owners[i].getAddress(), "", "", BigInteger.ZERO);
            didSummaryScore.invoke(owners[i], "addPublicKey", owners_sign.getMessage(), owners_sign.getSignature());
        }

        // setup PRE nodes
        bfsContentsScore.invoke(owners[0], "add_node","TEST_NODE_0", null, null, null, null);
        bfsContentsScore.invoke(owners[0], "add_node","TEST_NODE_1", null, null, null, null);
        bfsContentsScore.invoke(owners[0], "add_node","TEST_NODE_2", null, null, null, null);
        bfsContentsScore.invoke(owners[0], "add_node","TEST_NODE_3", null, null, null, null);
        bfsContentsScore.invoke(owners[0], "add_node","TEST_NODE_4", null, null, null, null);
    }

    private DidMessage makeSignature(String did, ECKeyPair keyPair, String kid, Address from, String target, String method, BigInteger lastUpdated) {
        DidMessage message = new DidMessage(did, kid, from, target, method, lastUpdated);
        byte[] msgHash = Hash.sha3(message.getMessageForHash());
        message.setHashedMessage(msgHash);

        ECDSASignature signature = new ECDSASignature(new Bytes(keyPair.getPrivateKey()));
        BigInteger[] sig = signature.generateSignature(msgHash);
        message.setSignature(signature.recoverableSerialize(sig, msgHash));

        return message;
    }

    @Test
    void makeSignature() {
        DidMessage sign = makeSignature(owners_did[0], owners_keyPair[0], "publicKey", owners[0].getAddress(), "", "", BigInteger.ZERO);
        DidMessage sign2 = makeSignature(owners_did[0], owners_keyPair[0], "publicKey2", owners[0].getAddress(), "", "", BigInteger.ZERO);
        DidMessage sign3 = makeSignature(owners_did[0], owners_keyPair[0], "publicKey3", owners[0].getAddress(), "", "", BigInteger.ZERO);

        didSummaryScore.invoke(owners[0], "addPublicKey", sign.getMessage(), sign.getSignature());
        didSummaryScore.invoke(owners[0], "addPublicKey", sign2.getMessage(), sign2.getSignature());
        didSummaryScore.invoke(owners[0], "addPublicKey", sign3.getMessage(), sign3.getSignature());

        var key1 = (String) didSummaryScore.call("getPublicKey", owners_did[0], "publicKey");
        var key2 = (String) didSummaryScore.call("getPublicKey", owners_did[0], "publicKey2");
        var key3 = (String) didSummaryScore.call("getPublicKey", owners_did[0], "publicKey3");

        assertTrue(key1.equals(key2) && key1.equals(key3));
    }

    @Test
    void pin() {
        bfsContentsScore.invoke(owners[0], "pin","TEST_CID_A000", "TEST_TRACKER_URL", null, null, null, null, null, null, null, null, null, null);
        var pinInfo = (Map<String, Object>) bfsContentsScore.call("get_pin", "TEST_CID_A000");
        verify(bfsContentsSpy).BFSEvent(EventType.AddPin.name(), "TEST_CID_A000", owners[0].getAddress().toString(), (BigInteger) pinInfo.get("last_updated"));
    }

    @Test
    void pinByDid() {
        DidMessage sign = makeSignature(owners_did[0], owners_keyPair[0], "publicKey", owners[0].getAddress(), "TEST_CID_A000", "pin", BigInteger.ZERO);
        bfsContentsScore.invoke(owners[0], "pin", "TEST_CID_A000", "TEST_TRACKER_URL", sign.getMessage(), sign.getSignature(), null, null, null, null, null, null, null, null);

        var pinInfo = (Map<String, Object>) bfsContentsScore.call("get_pin", "TEST_CID_A000");
        verify(bfsContentsSpy).BFSEvent(EventType.AddPin.name(), "TEST_CID_A000", owners_did[0], (BigInteger) pinInfo.get("last_updated"));
    }

    @Test
    void unpin() {
        bfsContentsScore.invoke(owners[0], "pin","TEST_CID_A000", "TEST_TRACKER_URL", null, null, null, null, null, null, null, null, null, null);
        bfsContentsScore.invoke(owners[0], "unpin","TEST_CID_A000");
        var pinInfo = (Map<String, Object>) bfsContentsScore.call("get_pin", "TEST_CID_A000");
        verify(bfsContentsSpy).BFSEvent(EventType.Unpin.name(), "TEST_CID_A000", owners[0].getAddress().toString(), (BigInteger) pinInfo.get("last_updated"));
    }

    @Test
    void unpinByDid() {
        DidMessage sign_1 = makeSignature(owners_did[0], owners_keyPair[0], "publicKey", owners[0].getAddress(), "TEST_CID_A000", "pin", BigInteger.ZERO);
        bfsContentsScore.invoke(owners[0], "pin","TEST_CID_A000", "TEST_TRACKER_URL", sign_1.getMessage(), sign_1.getSignature(), null, null, null, null, null, null, null, null);

        var pinInfo = (Map<String, Object>) bfsContentsScore.call("get_pin", "TEST_CID_A000");
        DidMessage sign_2 = makeSignature(owners_did[0], owners_keyPair[0], "publicKey", owners[0].getAddress(), "TEST_CID_A000", "unpin", (BigInteger) pinInfo.get("last_updated"));
        bfsContentsScore.invoke(owners[0], "unpin","TEST_CID_A000", sign_2.getMessage(), sign_2.getSignature());
        pinInfo = (Map<String, Object>) bfsContentsScore.call("get_pin", "TEST_CID_A000");
        verify(bfsContentsSpy).BFSEvent(EventType.Unpin.name(), "TEST_CID_A000", owners_did[0], (BigInteger) pinInfo.get("last_updated"));
    }

    @Test
    void updatePin() {
        bfsContentsScore.invoke(owners[0], "pin","TEST_CID_A000", "TEST_TRACKER_URL", null, null, null, null, null, null, null, null, null, null);
        bfsContentsScore.invoke(owners[0], "update_pin","TEST_CID_A000", null, null, "TEST_TRACKER_URL_2", null, null, null, null, null, null, null, null);
        var pinInfo = (Map<String, Object>) bfsContentsScore.call("get_pin", "TEST_CID_A000");
        verify(bfsContentsSpy).BFSEvent(EventType.UpdatePin.name(), "TEST_CID_A000", owners[0].getAddress().toString(), (BigInteger) pinInfo.get("last_updated"));
    }

    @Test
    void updatePinByDid() {
        DidMessage sign_1 = makeSignature(owners_did[0], owners_keyPair[0], "publicKey", owners[0].getAddress(), "TEST_CID_A000", "pin", BigInteger.ZERO);
        bfsContentsScore.invoke(owners[0], "pin", "TEST_CID_A000", "TEST_TRACKER_URL", sign_1.getMessage(), sign_1.getSignature(), null, null, null, null, null, null, null, null);

        var pinInfo = (Map<String, Object>) bfsContentsScore.call("get_pin", "TEST_CID_A000");
        DidMessage sign_2 = makeSignature(owners_did[0], owners_keyPair[0], "publicKey", owners[0].getAddress(), "TEST_CID_A000", "update_pin", (BigInteger) pinInfo.get("last_updated"));
        bfsContentsScore.invoke(owners[0], "update_pin","TEST_CID_A000", sign_2.getMessage(), sign_2.getSignature(), "TEST_TRACKER_URL_2", null, null, null, null, null, null, null, null);
        pinInfo = (Map<String, Object>) bfsContentsScore.call("get_pin", "TEST_CID_A000");
        verify(bfsContentsSpy).BFSEvent(EventType.UpdatePin.name(), "TEST_CID_A000", owners_did[0], (BigInteger) pinInfo.get("last_updated"));
    }

    @Test
    void preventUpdatePinByOthers() {
        DidMessage sign_1 = makeSignature(owners_did[0], owners_keyPair[0], "publicKey", owners[0].getAddress(), "TEST_CID_A000", "pin", BigInteger.ZERO);
        bfsContentsScore.invoke(owners[0], "pin", "TEST_CID_A000", "TEST_TRACKER_URL", sign_1.getMessage(), sign_1.getSignature(), null, null, null, null, null, null, null, null);

        var pinInfo = (Map<String, Object>) bfsContentsScore.call("get_pin", "TEST_CID_A000");
        DidMessage sign_2 = makeSignature(owners_did[1], owners_keyPair[1], "publicKey", owners[0].getAddress(), "TEST_CID_A000", "update_pin", (BigInteger) pinInfo.get("last_updated"));
        assertThrows(UserRevertedException.class, () ->
                bfsContentsScore.invoke(owners[0], "update_pin","TEST_CID_A000", sign_2.getMessage(), sign_2.getSignature(), "TEST_TRACKER_URL_2", null, null, null, null, null, null, null, null));
    }


    @Test
    void removePin() {
        bfsContentsScore.invoke(owners[0], "pin","TEST_CID_A000", "TEST_TRACKER_URL", null, null, null, null, null, null, null, null, null, null);
        bfsContentsScore.invoke(owners[0], "unpin","TEST_CID_A000");
        var pinInfo = (Map<String, Object>) bfsContentsScore.call("get_pin", "TEST_CID_A000");
        bfsContentsScore.invoke(owners[0], "remove_pin","TEST_CID_A000");
        verify(bfsContentsSpy).BFSEvent(EventType.RemovePin.name(), "TEST_CID_A000", owners[0].getAddress().toString(), (BigInteger) pinInfo.get("last_updated"));
    }

    @Test
    void removePinByDid() {
        DidMessage sign_1 = makeSignature(owners_did[0], owners_keyPair[0], "publicKey", owners[0].getAddress(), "TEST_CID_A000", "pin", BigInteger.ZERO);
        bfsContentsScore.invoke(owners[0], "pin","TEST_CID_A000", "TEST_TRACKER_URL", sign_1.getMessage(), sign_1.getSignature(), "", "", null, null, null, null, null, null);

        var pinInfo = (Map<String, Object>) bfsContentsScore.call("get_pin", "TEST_CID_A000");
        DidMessage sign_2 = makeSignature(owners_did[0], owners_keyPair[0], "publicKey", owners[0].getAddress(), "TEST_CID_A000", "unpin", (BigInteger) pinInfo.get("last_updated"));
        bfsContentsScore.invoke(owners[0], "unpin", "TEST_CID_A000", sign_2.getMessage(), sign_2.getSignature());

        pinInfo = (Map<String, Object>) bfsContentsScore.call("get_pin", "TEST_CID_A000");
        DidMessage sign_3 = makeSignature(owners_did[0], owners_keyPair[0], "publicKey", owners[0].getAddress(), "TEST_CID_A000", "remove_pin", (BigInteger) pinInfo.get("last_updated"));
        bfsContentsScore.invoke(owners[0], "remove_pin", "TEST_CID_A000", sign_3.getMessage(), sign_3.getSignature());
        verify(bfsContentsSpy).BFSEvent(EventType.RemovePin.name(), "TEST_CID_A000", owners_did[0], (BigInteger) pinInfo.get("last_updated"));
    }

    @Test
    void reallocationByDid() {
        DidMessage sign_1 = makeSignature(owners_did[0], owners_keyPair[0], "publicKey", owners[0].getAddress(), "TEST_CID_A000", "pin", BigInteger.ZERO);
        bfsContentsScore.invoke(owners[0], "pin", "TEST_CID_A000", "TEST_TRACKER_URL", sign_1.getMessage(), sign_1.getSignature(), null, null, null, null, null, null, null, null);
        var pinInfo = (Map<String, Object>) bfsContentsScore.call("get_pin", "TEST_CID_A000");
        verify(bfsContentsSpy).BFSEvent(EventType.AddPin.name(), "TEST_CID_A000", owners_did[0], (BigInteger) pinInfo.get("last_updated"));

        assertThrows(UserRevertedException.class, () ->
                bfsContentsScore.invoke(owners[0], "reallocation","TEST_CID_A000"));
    }
}
