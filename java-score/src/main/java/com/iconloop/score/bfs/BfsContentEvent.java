package com.iconloop.score.bfs;

import score.annotation.EventLog;

public interface BfsContentEvent {
    /**
     *
     * Notifies that the BFS event has been executed.
     *
     * @param event BFS Event
     * @param value1
     * @param value2
     *
     * BFSEvent handles multiple BFS events within a single EventLog
     *  Add Pin -> event: "AddPin", value1: "cid", value2:"owner"
     *  UnPin -> event: "UnPin", value1: "cid", value2:"owner"
     *  Update Pin -> event: "UpdatePin", value1: "cid", value2:"owner"
     *  Remove Pin -> event : "RemovePin", value1: "cid", value2:"owner"
     *  Update Group -> event : "UpdateGroup", value1: "group", value2: "owner"
     *  Add Node -> event : "AddNode", value1: "peer_id", value2: "endpoint"
     *  Remove Node -> event : "RemoveNode", value1: "peer_id", value2: "endpoint"
     *  Update Node -> event : "UpdateNode", value1: "peer_id", value2: "endpoint"
     */
    @EventLog(indexed=1)
    void BFSEvent(String event, String value1, String value2);
}