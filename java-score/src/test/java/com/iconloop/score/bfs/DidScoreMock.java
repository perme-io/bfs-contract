package com.iconloop.score.bfs;

import score.BranchDB;
import score.Context;
import score.DictDB;
import score.annotation.External;

public class DidScoreMock {
    private final BranchDB<String, DictDB<String, byte[]>> publicKeys = Context.newBranchDB("publicKeys", byte[].class);

    @External
    public void register(String did, String kid, byte[] pubkey) {
        publicKeys.at(did).set(kid, pubkey);
    }

    @External(readonly=true)
    public byte[] getPublicKey(String did, String kid) {
        return publicKeys.at(did).get(kid);
    }
}
