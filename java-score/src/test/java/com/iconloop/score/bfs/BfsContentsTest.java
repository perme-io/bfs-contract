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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;


class DidSignature {
    public String message;
    public byte[] signature;

    public DidSignature(String message, byte[] recoverableSerialize) {
        this.message = message;
        this.signature = recoverableSerialize;
    }
}


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
            DidSignature owners_sign = makeSignature(owners_did[i], owners_keyPair[i], "publicKey", "target", BigInteger.ZERO);
            didSummaryScore.invoke(owners[0], "addPublicKey", owners_sign.message, "publicKey", owners_sign.signature);
        }

        // setup PRE nodes
        bfsContentsScore.invoke(owners[0], "add_node","TEST_NODE_0", null, null, null, null);
        bfsContentsScore.invoke(owners[0], "add_node","TEST_NODE_1", null, null, null, null);
        bfsContentsScore.invoke(owners[0], "add_node","TEST_NODE_2", null, null, null, null);
        bfsContentsScore.invoke(owners[0], "add_node","TEST_NODE_3", null, null, null, null);
        bfsContentsScore.invoke(owners[0], "add_node","TEST_NODE_4", null, null, null, null);
    }

    private DidSignature makeSignature(String did, ECKeyPair keyPair, String kid, String target, BigInteger nonce) {
        String message = did + "#" + kid + "#" + target + "#" + nonce.toString(10);
        byte[] msgHash = Hash.sha3(message.getBytes());
        byte[] signMsg = Hash.sha3(msgHash);

        foundation.icon.icx.crypto.ECDSASignature signature = new ECDSASignature(new Bytes(keyPair.getPrivateKey()));
        BigInteger[] sig = signature.generateSignature(msgHash);
        byte[] recoverableSerialize = signature.recoverableSerialize(sig, msgHash);

        return new DidSignature(message, recoverableSerialize);
    }

    @Test
    void makeSignature() {
        DidSignature sign = makeSignature(owners_did[0], owners_keyPair[0], "publicKey", "target", BigInteger.ZERO);
        DidSignature sign2 = makeSignature(owners_did[0], owners_keyPair[0], "publicKey2", "target2", BigInteger.ZERO);
        DidSignature sign3 = makeSignature(owners_did[0], owners_keyPair[0], "publicKey3", "target3", BigInteger.ZERO);

        didSummaryScore.invoke(owners[0], "addPublicKey", sign.message, "publicKey", sign.signature);
        didSummaryScore.invoke(owners[0], "addPublicKey", sign2.message, "publicKey2", sign2.signature);
        didSummaryScore.invoke(owners[0], "addPublicKey", sign3.message, "publicKey3", sign3.signature);

        var key1 = (String) didSummaryScore.call("getPublicKey", owners_did[0], "publicKey");
        var key2 = (String) didSummaryScore.call("getPublicKey", owners_did[0], "publicKey2");
        var key3 = (String) didSummaryScore.call("getPublicKey", owners_did[0], "publicKey3");

        assertTrue(key1.equals(key2) && key1.equals(key3));
    }

    @Test
    void pin() {
        bfsContentsScore.invoke(owners[0], "pin","TEST_CID_A000", "TEST_TRACKER_URL", null, null, null, null, null, null, null, null, null, null);
        verify(bfsContentsSpy).BFSEvent(EventType.AddPin.name(), "TEST_CID_A000", "", BigInteger.ZERO);
    }

    @Test
    void pinByDid() {
        DidSignature sign = makeSignature(owners_did[0], owners_keyPair[0], "publicKey", "TEST_CID_A000", BigInteger.ZERO);
        bfsContentsScore.invoke(owners[0], "pin", "TEST_CID_A000", "TEST_TRACKER_URL", sign.message, sign.signature, null, null, null, null, null, null, null, null);
        verify(bfsContentsSpy).BFSEvent(EventType.AddPin.name(), "TEST_CID_A000", "", BigInteger.ZERO);
    }

    @Test
    void unpin() {
        bfsContentsScore.invoke(owners[0], "pin","TEST_CID_A000", "TEST_TRACKER_URL", null, null, null, null, null, null, null, null, null, null);
        bfsContentsScore.invoke(owners[0], "unpin","TEST_CID_A000");
        verify(bfsContentsSpy).BFSEvent(EventType.Unpin.name(), "TEST_CID_A000", "", BigInteger.ONE);
    }

    @Test
    void unpinByDid() {
        DidSignature sign_1 = makeSignature(owners_did[0], owners_keyPair[0], "publicKey", "TEST_CID_A000", BigInteger.ZERO);
        bfsContentsScore.invoke(owners[0], "pin","TEST_CID_A000", "TEST_TRACKER_URL", sign_1.message, sign_1.signature, null, null, null, null, null, null, null, null);

        DidSignature sign_2 = makeSignature(owners_did[0], owners_keyPair[0], "publicKey", "TEST_CID_A000", BigInteger.ONE);
        bfsContentsScore.invoke(owners[0], "unpin","TEST_CID_A000", sign_2.message, sign_2.signature);
        verify(bfsContentsSpy).BFSEvent(EventType.Unpin.name(), "TEST_CID_A000", "", BigInteger.ONE);
    }

    @Test
    void updatePin() {
        bfsContentsScore.invoke(owners[0], "pin","TEST_CID_A000", "TEST_TRACKER_URL", null, null, null, null, null, null, null, null, null, null);
        bfsContentsScore.invoke(owners[0], "update_pin","TEST_CID_A000", null, null, "TEST_TRACKER_URL_2", null, null, null, null, null, null, null, null);
        verify(bfsContentsSpy).BFSEvent(EventType.UpdatePin.name(), "TEST_CID_A000", "", BigInteger.ONE);
    }

    @Test
    void updatePinByDid() {
        DidSignature sign_1 = makeSignature(owners_did[0], owners_keyPair[0], "publicKey", "TEST_CID_A000", BigInteger.ZERO);
        bfsContentsScore.invoke(owners[0], "pin", "TEST_CID_A000", "TEST_TRACKER_URL", sign_1.message, sign_1.signature, null, null, null, null, null, null, null, null);

        DidSignature sign_2 = makeSignature(owners_did[0], owners_keyPair[0], "publicKey", "TEST_CID_A000", BigInteger.ONE);
        bfsContentsScore.invoke(owners[0], "update_pin","TEST_CID_A000", sign_2.message, sign_2.signature, "TEST_TRACKER_URL_2", null, null, null, null, null, null, null, null);
        verify(bfsContentsSpy).BFSEvent(EventType.UpdatePin.name(), "TEST_CID_A000", "", BigInteger.ONE);
    }

    @Test
    void preventUpdatePinByOthers() {
        DidSignature sign_1 = makeSignature(owners_did[0], owners_keyPair[0], "publicKey", "TEST_CID_A000", BigInteger.ZERO);
        bfsContentsScore.invoke(owners[0], "pin", "TEST_CID_A000", "TEST_TRACKER_URL", sign_1.message, sign_1.signature, null, null, null, null, null, null, null, null);

        DidSignature sign_2 = makeSignature(owners_did[1], owners_keyPair[1], "publicKey", "TEST_CID_A000", BigInteger.ONE);
        assertThrows(UserRevertedException.class, () ->
                bfsContentsScore.invoke(owners[0], "update_pin","TEST_CID_A000", sign_2.message, sign_2.signature, "TEST_TRACKER_URL_2", null, null, null, null, null, null, null, null));
    }


    @Test
    void removePin() {
        bfsContentsScore.invoke(owners[0], "pin","TEST_CID_A000", "TEST_TRACKER_URL", null, null, null, null, null, null, null, null, null, null);
        bfsContentsScore.invoke(owners[0], "unpin","TEST_CID_A000");
        bfsContentsScore.invoke(owners[0], "remove_pin","TEST_CID_A000");
        verify(bfsContentsSpy).BFSEvent(EventType.RemovePin.name(), "TEST_CID_A000", "", BigInteger.ONE);
    }

    @Test
    void removePinByDid() {
        DidSignature sign_1 = makeSignature(owners_did[0], owners_keyPair[0], "publicKey", "TEST_CID_A000", BigInteger.ZERO);
        bfsContentsScore.invoke(owners[0], "pin","TEST_CID_A000", "TEST_TRACKER_URL", sign_1.message, sign_1.signature, null, null, null, null, null, null, null, null);

        DidSignature sign_2 = makeSignature(owners_did[0], owners_keyPair[0], "publicKey", "TEST_CID_A000", BigInteger.ONE);
        bfsContentsScore.invoke(owners[0], "unpin", "TEST_CID_A000", sign_2.message, sign_2.signature);

        DidSignature sign_3 = makeSignature(owners_did[0], owners_keyPair[0], "publicKey", "TEST_CID_A000", BigInteger.TWO);
        bfsContentsScore.invoke(owners[0], "remove_pin", "TEST_CID_A000", sign_3.message, sign_3.signature);
        verify(bfsContentsSpy).BFSEvent(EventType.RemovePin.name(), "TEST_CID_A000", "", BigInteger.TWO);
    }

    @Test
    void reallocationByDid() {
        DidSignature sign_1 = makeSignature(owners_did[0], owners_keyPair[0], "publicKey", "TEST_CID_A000", BigInteger.ZERO);
        bfsContentsScore.invoke(owners[0], "pin", "TEST_CID_A000", "TEST_TRACKER_URL", sign_1.message, sign_1.signature, null, null, null, null, null, null, null, null);
        verify(bfsContentsSpy).BFSEvent(EventType.AddPin.name(), "TEST_CID_A000", "", BigInteger.ZERO);

        assertThrows(UserRevertedException.class, () ->
                bfsContentsScore.invoke(owners[0], "reallocation","TEST_CID_A000"));
    }
}
