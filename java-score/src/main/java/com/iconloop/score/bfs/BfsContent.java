package com.iconloop.score.bfs;

import score.Address;
import score.annotation.Optional;
import score.annotation.Payable;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public interface BfsContent {
    /**
     * Default allocation settings in IPFS
     *
     * @param allocation_min (Optional) Minimum number of nodes for allocation
     * @param allocation_max (Optional) Maximum number of nodes for allocation
     * @param allocation_margin (Optional) Margin value considered during allocation
     */
    void set_default_allocation_factors(@Optional BigInteger allocation_min, @Optional BigInteger allocation_max, @Optional BigInteger allocation_margin);

    /**
     * Get IPFS default allocation configuration
     * <pre>
     * Example JSON response:
     * {
     *     "jsonrpc": "2.0",
     *     "result": {
     *          "allocation_min": "0x2",
     *          "allocation_max": "0x5",
     *          "allocation_margin": "0x2"
     *     },
     *     "id": 1
     * }
     * </pre>
     * @return Default allocation factors
     */
    Map<String, Object> get_default_allocation_factors();

    /**
     * Get pin information
     * <pre>
     * Example JSON response:
     * {
     *     "jsonrpc": "2.0",
     *     "result": {
     *          "cid": "zb2rhe5P4gXftAwvA4eXQ5HJwsER2owDyS9sKaQRRVQPn93bA",
     *          "group": "parameta-group",
     *          "size": 100,
     *          "name": "parameta page",
     *          "created": 1000,
     *          "owner": "hx5b356b0c3231baa7f2b8f6833267ae1ff178f0b4",
     *          "replication_min": "0x5",
     *          "replication_max": "0xa",
     *          "user_allocations": [],
     *          "shard_size": "0x3039",
     *          "expire_at": "1747709249693524"
     *     },
     *     "id": 1
     * }
     * </pre>
     *
     * @param owner Owner of the pinned content (DID / Wallet)
     * @param cid The content identifier to be pinned.
     * @return Pin information
     */
    Map<String,Object> get_pin(String owner, String cid);

    /**
     * Pins the specified CID to the local IPFS node.
     *
     * @param cid The content identifier to be pinned.
     * @param size Data size
     * @param expire_at Pin Expiration Time (Micro second)
     * @param group (Optional) The group to which the CID belongs
     * @param name (Optional) Alias for cid
     * @param did_sign (Optional) DID signature (JWT)
     */
    void pin(String cid,
             int size,
             BigInteger expire_at,
             @Optional String group,
             @Optional String name,
             @Optional String did_sign);

    /**
     * Unpins the specified CID, allowing it to be garbage collected.
     *
     * @param cid The content identifier to be pinned.
     * @param did_sign (Optional) DID signature (JWT)
     */
    void unpin(String cid, @Optional String did_sign);

    /**
     * Updates the metadata of an existing pin.
     *
     * @param cid The content identifier to be pinned.
     * @param expire_at Pin Expiration Time (Micro second)
     * @param did_sign (Optional) DID signature (JWT)
     */
    void update_pin(String cid,
                    BigInteger expire_at,
                    @Optional String did_sign);


    /**
     * Delete pin in the contract.
     *
     * @param cid The content identifier to be pinned.
     * @param did_sign (Optional) DID signature (JWT)
     */
    void remove_pin(String cid, @Optional String did_sign);

    /**
     * Update the group.
     *
     * @param group The group to which the CID belongs
     * @param expire_at Group expiration time (Micro second)
     * @param did_sign (Optional) DID signature (JWT)
     */
    void update_group(String group, BigInteger expire_at, @Optional String did_sign);

    /**
     * Get group information
     * <pre>
     * Example JSON response:
     * {
     *     "jsonrpc": "2.0",
     *     "result": {
     *          "group": "parameta-group",
     *          "owner": "hx5b356b0c3231baa7f2b8f6833267ae1ff178f0b4",
     *          "expire_at": "1747709249693524"
     *     },
     *     "id": 1
     * }
     * </pre>
     *
     * @param owner Owner of the group
     * @param group The group to which the CID belongs
     * @return Group information
     */
    Map<String,Object> get_group(String owner, String group);

    /**
     * Get node information
     * <pre>
     * Example JSON response:
     * {
     *     "jsonrpc": "2.0",
     *     "result": {
     *           "peer_id": "zb2rhe5P4gXftAwvA4eXQ5HJwsER2owDyS9sKaQRRVQPn93bA",
     *           "url": "https://bfs-node",
     *           "endpoint": "1.1.1.1",
     *           "name": "BFS Node",
     *           "created": 1000,
     *           "owner": "hxb461234639262da5ea29cf13ee47dd09def47427"
     *      },
     *     "id": 1
     * }
     * </pre>
     *
     * @param peer_id Unique value that identifies the BFS node (Manager + IPFS Cluster + IPFS Daemon)
     * @return Node information
     */
    NodeInfo get_node(String peer_id);

    /**
     * Adds a new peer to the IPFS Cluster
     *
     * @param peer_id Unique value that identifies the BFS node (Manager + IPFS Cluster + IPFS Daemon)
     * @param url API URL
     * @param endpoint (Optional) Node connection information (Multiaddr)
     * @param name (Optional) Alias for node
     * @param owner (Optional) Owner of the node
     */
    @Payable
    void add_node(String peer_id,
                  String url,
                  @Optional String endpoint,
                  @Optional String name,
                  @Optional Address owner);


    /**
     * Removes the peer from the IPFS Cluster
     *
     * @param peer_id Unique value that identifies the BFS node (Manager + IPFS Cluster + IPFS Daemon)
     */
    void remove_node(String peer_id);

    /**
     * Update peers in IPFS Cluster
     *
     * @param peer_id Unique value that identifies the BFS node (Manager + IPFS Cluster + IPFS Daemon)
     * @param url (Optional) API URL
     * @param endpoint (Optional) Node connection information (Multiaddr)
     * @param name (Optional) Alias for node
     * @param owner (Optional) Owner of the node
     */
    @Payable
    void update_node(String peer_id,
                     @Optional String url,
                     @Optional String endpoint,
                     @Optional String name,
                     @Optional Address owner);

    /**
     * Get all nodes.
     * <pre>
     * Example JSON response:
     * {
     *     "jsonrpc": "2.0",
     *     "result": {[
     *           "peer_id": "zb2rhe5P4gXftAwvA4eXQ5HJwsER2owDyS9sKaQRRVQPn93bA",
     *           "url": "https://bfs-node",
     *           "endpoint": "1.1.1.1",
     *           "name": "BFS Node",
     *           "created": 1000,
     *           "owner": "hxb461234639262da5ea29cf13ee47dd09def47427"
     *      ]},
     *     "id": 1
     * }
     * </pre>
     *
     * @return Node Information list
     */
    List<Object> all_node();

    /**
     * Checks if all allocation peers for the pin are in a healthy state
     * <pre>
     * Example JSON response:
     * {
     *     "jsonrpc": "2.0",
     *     "result": {
     *           "cid": "zb2rhe5P4gXftAwvA4eXQ5HJwsER2owDyS9sKaQRRVQPn93bA",
     *           "owner": "hx5b356b0c3231baa7f2b8f6833267ae1ff178f0b4",
     *           "replication_min": "0x5",
     *           "replication_max": "0xa",
     *           "user_allocations": [],
     *      },
     *     "id": 1
     * }
     * </pre>
     *
     * @param cid cid Unique identifier for content in IPFS
     * @return Pin information
     */
    Map<String, Object> check_allocations(String cid);

    /**
     * Get peer information
     * {
     *     "jsonrpc": "2.0",
     *     "result": {
     *           "frontIndexOfPeers": 0,
     *           "backIndexOfPeers": 0,
     *           "NumOfPeers": 0
     *      },
     *     "id": 1
     * }
     * </pre>
     * @return Get peer information
     */
    Map<String, Object> get_info();
}