package samba.network.history;

import samba.domain.messages.requests.FindContent;
import samba.domain.messages.requests.FindNodes;
import samba.domain.messages.requests.Offer;
import samba.domain.messages.requests.Ping;
import samba.domain.messages.response.Accept;
import samba.domain.messages.response.Content;
import samba.domain.messages.response.Nodes;
import samba.domain.messages.response.Pong;

import java.util.Optional;

import org.ethereum.beacon.discovery.schema.NodeRecord;
import tech.pegasys.teku.infrastructure.async.SafeFuture;

public interface HistoryJsonRpcRequests {

  SafeFuture<Optional<Pong>> ping(NodeRecord nodeRecord, Ping message);

  SafeFuture<Optional<Nodes>> findNodes(NodeRecord nodeRecord, FindNodes findNodes);

  SafeFuture<Optional<Content>> findContent(NodeRecord nodeRecord, FindContent findContent);

  SafeFuture<Optional<Accept>> offer(NodeRecord nodeRecord, Offer offer);

  void addEnr(String enr);

  Optional<String> getEnr(String nodeId);
}
