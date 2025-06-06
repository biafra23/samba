/*
 * Copyright contributors to Hyperledger Besu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package samba.jsonrpc.handler.processor;

import samba.jsonrpc.exception.InvalidJsonRpcParameters;
import samba.jsonrpc.reponse.JsonRpcErrorResponse;
import samba.jsonrpc.reponse.JsonRpcMethod;
import samba.jsonrpc.reponse.JsonRpcRequestContext;
import samba.jsonrpc.reponse.JsonRpcRequestId;
import samba.jsonrpc.reponse.JsonRpcResponse;
import samba.jsonrpc.reponse.RpcErrorType;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseJsonRpcProcessor implements JsonRpcProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(BaseJsonRpcProcessor.class);

  @Override
  public JsonRpcResponse process(
      final JsonRpcRequestId id, final JsonRpcMethod method, final JsonRpcRequestContext request) {
    try {
      return method.response(request);
    } catch (final InvalidJsonRpcParameters e) {
      LOG.debug(
          "Invalid Params for method: {}, error: {}",
          method.getName(),
          e.getRpcErrorType().getMessage(),
          e);
      return new JsonRpcErrorResponse(id, e.getRpcErrorType());
    } catch (final RuntimeException e) {
      final JsonArray params = JsonObject.mapFrom(request.getRequest()).getJsonArray("params");
      LOG.error(String.format("Error processing method: %s %s", method.getName(), params), e);
      return new JsonRpcErrorResponse(id, RpcErrorType.INTERNAL_ERROR);
    }
  }
}
