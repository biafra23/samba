package samba.services.connecton.needimpl;

import org.ethereum.beacon.discovery.schema.NodeRecord;
import tech.pegasys.teku.infrastructure.async.SafeFuture;

public interface Network {

    SafeFuture<NodeRecord> connect(NodeRecord peer);

    int getPeerCount();
}
