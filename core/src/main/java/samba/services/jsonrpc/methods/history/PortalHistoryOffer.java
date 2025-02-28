package samba.services.jsonrpc.methods.history;

import samba.domain.messages.requests.Offer;
import samba.jsonrpc.config.RpcMethod;
import samba.jsonrpc.reponse.*;
import samba.network.history.HistoryJsonRpcRequests;
import samba.services.jsonrpc.methods.parameters.ContentItemsParameter;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.apache.tuweni.bytes.Bytes;
import org.ethereum.beacon.discovery.schema.NodeRecord;
import org.ethereum.beacon.discovery.schema.NodeRecordFactory;

public class PortalHistoryOffer implements JsonRpcMethod {

  private HistoryJsonRpcRequests historyJsonRpcRequests;

  public PortalHistoryOffer(final HistoryJsonRpcRequests historyJsonRpcRequests) {
    this.historyJsonRpcRequests = historyJsonRpcRequests;
  }

  @Override
  public String getName() {
    return RpcMethod.PORTAL_HISTORY_OFFER.getMethodName();
  }

  @Override
  public JsonRpcResponse response(JsonRpcRequestContext requestContext) {
    try {
      String enr = requestContext.getRequiredParameter(0, String.class);
      ContentItemsParameter contentItemsParameter =
          requestContext.getRequiredParameter(1, ContentItemsParameter.class);
      if (contentItemsParameter.isNotValid())
        return createJsonRpcInvalidRequestResponse(requestContext);

      List<Bytes> contentKeys = contentItemsParameter.getContentKeys();
      List<Bytes> content = contentItemsParameter.getContentValues();

      final NodeRecord nodeRecord = NodeRecordFactory.DEFAULT.fromEnr(enr);

      Optional<Bytes> contentKeysBitList =
          this.historyJsonRpcRequests.offer(nodeRecord, content, new Offer(contentKeys)).get();

      if (contentKeysBitList.isEmpty()) {
        return createJsonRpcInvalidRequestResponse(requestContext);
      }
      return new JsonRpcSuccessResponse(
          requestContext.getRequest().getId(), contentKeysBitList.get().toHexString());

    } catch (JsonRpcParameter.JsonRpcParameterException
        | InterruptedException
        | ExecutionException e) {
      return createJsonRpcInvalidRequestResponse(requestContext);
    }
  }
}
