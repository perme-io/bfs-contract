package com.iconloop.score.bfs;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.*;

import java.math.BigInteger;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;


public class BfsContentsTest extends TestBase {
    private static final ServiceManager sm = getServiceManager();
    private Account[] owners;
    private Score bfsContentsScore;
    private BfsContents bfsContentsSpy;

    private String owner_did;
    private byte[] did_sign;

    @BeforeEach
    void setup() throws Exception {
        // setup accounts and deploy
        owners = new Account[3];
        for (int i = 0; i < owners.length; i++) {
            owners[i] = sm.createAccount(100);
        }

        bfsContentsScore = sm.deploy(owners[0], BfsContents.class);

        // setup spy
        bfsContentsSpy = (BfsContents) spy(bfsContentsScore.getInstance());
        bfsContentsScore.setInstance(bfsContentsSpy);

        // setup for owner did
        owner_did = "did:icon:01:765949249b90e78641c51840c7a6a7d4a3383d9c8a6327bd";

        BigInteger privKey = new BigInteger("97ddae0f3a25b92268175400149d65d6887b9cefaf28ea2c078e05cdc15a3c0a", 16);
        BigInteger pubKey = Sign.publicKeyFromPrivate(privKey);
        ECKeyPair keyPair = new ECKeyPair(privKey, pubKey);

        byte[] didHash_as_msg = Hash.sha3(owner_did.getBytes());
        Sign.SignatureData signature = Sign.signMessage(didHash_as_msg, keyPair, false);
        did_sign = new byte[65];
        System.arraycopy(signature.getR(), 0, did_sign, 0, 32);

        // setup PRE nodes
        bfsContentsScore.invoke(owners[0], "add_node","TEST_NODE_0", null, null, null, null);
        bfsContentsScore.invoke(owners[0], "add_node","TEST_NODE_1", null, null, null, null);
        bfsContentsScore.invoke(owners[0], "add_node","TEST_NODE_2", null, null, null, null);
        bfsContentsScore.invoke(owners[0], "add_node","TEST_NODE_3", null, null, null, null);
        bfsContentsScore.invoke(owners[0], "add_node","TEST_NODE_4", null, null, null, null);
    }

    @Test
    void pin() {
        bfsContentsScore.invoke(owners[0], "pin","TEST_CID_A000", "TEST_TRACKER_URL", null, null, null, null, null, null, null, null, null, null);
        verify(bfsContentsSpy).BFSEvent(EventType.AddPin.name(), "TEST_CID_A000", "");
    }

    @Test
    void pinByDid() {
        bfsContentsScore.invoke(owners[0], "pin","TEST_CID_A000", "TEST_TRACKER_URL", owner_did, did_sign, null, null, null, null, null, null, null, null);
        verify(bfsContentsSpy).BFSEvent(EventType.AddPin.name(), "TEST_CID_A000", "");
    }

    @Test
    void unpin() {
        bfsContentsScore.invoke(owners[0], "pin","TEST_CID_A000", "TEST_TRACKER_URL", null, null, null, null, null, null, null, null, null, null);
        bfsContentsScore.invoke(owners[0], "unpin","TEST_CID_A000");
        verify(bfsContentsSpy).BFSEvent(EventType.Unpin.name(), "TEST_CID_A000", "");
    }

    @Test
    void unpinByDid() {
        bfsContentsScore.invoke(owners[0], "pin","TEST_CID_A000", "TEST_TRACKER_URL", owner_did, did_sign, null, null, null, null, null, null, null, null);
        bfsContentsScore.invoke(owners[0], "unpin","TEST_CID_A000", owner_did, did_sign);
        verify(bfsContentsSpy).BFSEvent(EventType.Unpin.name(), "TEST_CID_A000", "");
    }

    @Test
    void updatePin() {
        bfsContentsScore.invoke(owners[0], "pin","TEST_CID_A000", "TEST_TRACKER_URL", null, null, null, null, null, null, null, null, null, null);
        bfsContentsScore.invoke(owners[0], "update_pin","TEST_CID_A000", null, null, "TEST_TRACKER_URL_2", null, null, null, null, null, null, null, null);
        verify(bfsContentsSpy).BFSEvent(EventType.UpdatePin.name(), "TEST_CID_A000", "");
    }

    @Test
    void updatePinByDid() {
        bfsContentsScore.invoke(owners[0], "pin","TEST_CID_A000", "TEST_TRACKER_URL", owner_did, did_sign, null, null, null, null, null, null, null, null);
        bfsContentsScore.invoke(owners[0], "update_pin","TEST_CID_A000", owner_did, did_sign, "TEST_TRACKER_URL_2", null, null, null, null, null, null, null, null);
        verify(bfsContentsSpy).BFSEvent(EventType.UpdatePin.name(), "TEST_CID_A000", "");
    }

    @Test
    void removePin() {
        bfsContentsScore.invoke(owners[0], "pin","TEST_CID_A000", "TEST_TRACKER_URL", null, null, null, null, null, null, null, null, null, null);
        bfsContentsScore.invoke(owners[0], "unpin","TEST_CID_A000");
        bfsContentsScore.invoke(owners[0], "remove_pin","TEST_CID_A000");
        verify(bfsContentsSpy).BFSEvent(EventType.RemovePin.name(), "TEST_CID_A000", "");
    }

    @Test
    void removePinByDid() {
        bfsContentsScore.invoke(owners[0], "pin","TEST_CID_A000", "TEST_TRACKER_URL", owner_did, did_sign, null, null, null, null, null, null, null, null);
        bfsContentsScore.invoke(owners[0], "unpin","TEST_CID_A000", owner_did, did_sign);
        bfsContentsScore.invoke(owners[0], "remove_pin","TEST_CID_A000", owner_did, did_sign);
        verify(bfsContentsSpy).BFSEvent(EventType.RemovePin.name(), "TEST_CID_A000", "");
    }
}
