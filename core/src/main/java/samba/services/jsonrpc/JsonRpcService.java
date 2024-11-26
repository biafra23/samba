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
package samba.services.jsonrpc;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxException;
import io.vertx.core.http.*;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;


import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import samba.services.jsonrpc.config.JsonRpcConfiguration;
import samba.services.jsonrpc.config.TimeoutOptions;
import samba.services.jsonrpc.exception.JsonRpcServiceException;
import samba.services.jsonrpc.handler.*;
import samba.services.jsonrpc.handler.processor.BaseJsonRpcProcessor;
import samba.services.jsonrpc.health.HealthService;
import samba.services.jsonrpc.reponse.JsonRpcMethod;
import tech.pegasys.teku.infrastructure.async.SafeFuture;
import tech.pegasys.teku.service.serviceutils.Service;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

//TODO Tracker, OpenTelemetrySystem,  dataDir          The data directory where requests can be buffered
//TODO websocket, authentication, traceFormacts, scheduler, NatService
public class JsonRpcService extends Service {

    private static final Logger LOG = LoggerFactory.getLogger(JsonRpcService.class);
    private static final String APPLICATION_JSON = "application/json";

    private final Vertx vertx;
    private final JsonRpcConfiguration config;
    private final Map<String, JsonRpcMethod> rpcMethods;


    private final int maxActiveConnections;
    private final AtomicInteger activeConnectionsCount = new AtomicInteger();


    private HttpServer httpServer;
    private final HealthService livenessService;
    //private final HealthService readinessService;


    public JsonRpcService(final Vertx vertx, final JsonRpcConfiguration config,
                          final MetricsSystem metricsSystem,
                          final Map<String, JsonRpcMethod> methods,
                          final HealthService livenessService) {
        validateConfig(config);
        this.config = config;
        this.vertx = vertx;
        this.rpcMethods = methods;
        this.livenessService = livenessService;
        //this.readinessService = readinessService;
        this.maxActiveConnections = config.getMaxActiveConnections();
    }


    @Override
    protected SafeFuture<?> doStart() {
        LOG.info("Starting JSON-RPC service on {}:{}", config.getHost(), config.getPort());
        LOG.debug("max number of active connections {}", maxActiveConnections);
        final CompletableFuture<Void> resultFuture = new CompletableFuture<>();
        try {
            httpServer = vertx.createHttpServer(this.getHttpServerOptions());
            httpServer.connectionHandler(this.createConnectionHandler());
            httpServer
                    .requestHandler(buildRouter())
                    .listen(
                            res -> {
                                if (!res.failed()) {
                                    resultFuture.complete(null);
                                    config.setPort(httpServer.actualPort());
                                    LOG.info("JSON-RPC service started and listening on {}:{}", config.getHost(), config.getPort());
                                    return;
                                }

                                httpServer = null;
                                resultFuture.completeExceptionally(
                                        new JsonRpcServiceException(String.format("Failed to bind JSON-RPC listener to %s:%s: %s",
                                                config.getHost(),
                                                config.getPort(), res.cause().getMessage()))
                                );
                            });
        } catch (final VertxException listenException) {
            httpServer = null;
            resultFuture.completeExceptionally(new JsonRpcServiceException(String.format("JSON-RPC listener failed to start: %s", listenException.getMessage())));
        }
        return SafeFuture.COMPLETE;
    }

    @Override
    protected SafeFuture<?> doStop() {
        if (httpServer == null)  return SafeFuture.COMPLETE;
        final CompletableFuture<Void> resultFuture = new CompletableFuture<>();
        httpServer.close(
                res -> {
                    if (res.failed()) {
                        resultFuture.completeExceptionally(res.cause());
                    } else {
                        httpServer = null;
                        resultFuture.complete(null);
                    }
                });
        return SafeFuture.COMPLETE;
    }


    private Router buildRouter() {
        final Router router = Router.router(vertx);
        // Verify Host header to avoid rebind attack. ?
        router.route().handler(new DenyRouteToBlockedHostHandler(this.config));
        router.errorHandler(403, new Logging403ErrorHandler());
        //router.route().handler(this::createSpan);
        router.route().handler(this.createCorsHandler());
        router.route().handler(BodyHandler.create().setBodyLimit(config.getMaxRequestContentLength()));//.setUploadsDirectory(dataDir.resolve("uploads").toString()).setDeleteUploadedFilesOnEnd(true));
        router.route("/").method(HttpMethod.GET).handler(this::handleEmptyRequest);

        router.route(HealthService.LIVENESS_PATH).method(HttpMethod.GET).handler(livenessService::handleRequest);
        //router.route(HealthService.READINESS_PATH).method(HttpMethod.GET).handler(readinessService::handleRequest);

        Route mainRoute = router.route("/").method(HttpMethod.POST).produces(APPLICATION_JSON);
        mainRoute.handler(this.createJsonParserHandler());
        mainRoute.handler(this.createTimeoutHandler());
        mainRoute.blockingHandler(this.createExecutiorHandler());
        return router;
    }

    private Handler<RoutingContext> createTimeoutHandler() {
        checkArgument(rpcMethods != null);
        final TimeoutOptions globalOptions = new TimeoutOptions(config.getHttpTimeoutSec());
        return TimeoutHandler.handler(Optional.of(globalOptions),
                rpcMethods.keySet().stream().collect(Collectors.toMap(Function.identity(), ignored -> globalOptions)));
    }

    private Handler<RoutingContext> createJsonParserHandler() {
        return JsonRpcParserHandler.handler();
    }

    private Handler<RoutingContext> createExecutiorHandler() {
        return JsonRpcExecutorHandler.handler(new JsonRpcExecutor(new BaseJsonRpcProcessor(), rpcMethods), config);
    }

    private CorsHandler createCorsHandler() {
        return CorsHandler.create().addRelativeOrigin(buildCorsRegexFromConfig()).allowedHeader("*").allowedHeader("content-type");
    }

    private Handler<HttpConnection> createConnectionHandler() {
        return connection -> {
            if (activeConnectionsCount.get() >= maxActiveConnections) {
                LOG.warn("Rejecting new connection from {}. Max {} active connections limit reached.", connection.remoteAddress(), activeConnectionsCount.getAndIncrement());
                connection.close();
            } else {
                LOG.info("Opened connection from {}. Total of active connections: {}/{}", connection.remoteAddress(), activeConnectionsCount.incrementAndGet(), maxActiveConnections);
            }
            connection.closeHandler(c -> LOG.debug("Connection closed from {}. Total of active connections: {}/{}", connection.remoteAddress(), activeConnectionsCount.decrementAndGet(), maxActiveConnections));
        };
    }


    private void validateConfig(final JsonRpcConfiguration config) {
        checkArgument(config.getPort() == 0 || NetworkUtility.isValidPort(config.getPort()), "Invalid port configuration.");
        checkArgument(config.getHost() != null, "Required host is not configured.");
        checkArgument(config.getMaxActiveConnections() > 0, "Invalid max active connections configuration.");
    }

    private HttpServerOptions getHttpServerOptions() {
        return new HttpServerOptions()
                .setHost(config.getHost())
                .setPort(config.getPort())
                .setHandle100ContinueAutomatically(true)
                .setCompressionSupported(true);
    }

    private void handleEmptyRequest(final RoutingContext routingContext) {
        routingContext.response().setStatusCode(201).end();
    }

    private String buildCorsRegexFromConfig() {
        if (config.getCorsAllowedDomains().isEmpty()) {
            return "";
        }
        if (config.getCorsAllowedDomains().contains("*")) {
            return ".*";
        }
        final StringJoiner stringJoiner = new StringJoiner("|");
        config.getCorsAllowedDomains().stream().filter(s -> !s.isEmpty()).forEach(stringJoiner::add);
        return stringJoiner.toString();

    }
}