package samba.services;

import static tech.pegasys.teku.infrastructure.async.AsyncRunnerFactory.DEFAULT_MAX_QUEUE_SIZE;

import samba.NetworkSDK;
import samba.api.Discv5API;
import samba.api.Discv5APIClient;
import samba.api.HistoryAPI;
import samba.api.HistoryAPIClient;
import samba.api.jsonrpc.*;
import samba.api.jsonrpc.pending.PortalHistoryPing;
import samba.config.RestServerConfig;
import samba.config.SambaConfiguration;
import samba.domain.messages.IncomingRequestTalkHandler;
import samba.domain.messages.MessageType;
import samba.domain.messages.handler.FindContentHandler;
import samba.domain.messages.handler.FindNodesHandler;
import samba.domain.messages.handler.HistoryNetworkIncomingRequestHandler;
import samba.domain.messages.handler.OfferHandler;
import samba.domain.messages.handler.PingHandler;
import samba.domain.messages.utp.UTPNetworkIncomingRequestHandler;
import samba.jsonrpc.config.JsonRpcConfiguration;
import samba.jsonrpc.config.RpcMethod;
import samba.jsonrpc.health.HealthService;
import samba.jsonrpc.health.LivenessCheck;
import samba.jsonrpc.reponse.JsonRpcMethod;
import samba.network.history.HistoryNetwork;
import samba.services.connecton.ConnectionService;
import samba.services.discovery.Discv5Service;
import samba.services.jsonrpc.JsonRpcService;
import samba.services.rest.PortalAPI;
import samba.services.rest.PortalRestAPI;
import samba.services.utp.UTPManager;
import samba.storage.HistoryRocksDB;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.vertx.core.Vertx;
import org.ethereum.beacon.discovery.schema.NodeRecord;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.pegasys.teku.infrastructure.async.AsyncRunner;
import tech.pegasys.teku.infrastructure.async.SafeFuture;
import tech.pegasys.teku.infrastructure.events.EventChannels;
import tech.pegasys.teku.infrastructure.time.TimeProvider;
import tech.pegasys.teku.service.serviceutils.Service;

public class HistoryNetworkMainService extends Service implements NetworkSDK<HistoryAPI> {

  private static final Logger LOG = LoggerFactory.getLogger(HistoryNetworkMainService.class);
  private static final int DEFAULT_ASYNC_P2P_MAX_THREADS = 10;
  public static final int DEFAULT_ASYNC_P2P_MAX_QUEUE = DEFAULT_MAX_QUEUE_SIZE;

  protected volatile EventChannels eventChannels;
  protected volatile MetricsSystem metricsSystem;
  protected volatile TimeProvider timeProvider;
  protected volatile AsyncRunner asyncRunner;

  protected volatile SambaConfiguration sambaConfiguration;

  protected volatile Optional<PortalRestAPI> portalRestAPI = Optional.empty();
  protected volatile Optional<JsonRpcService> jsonRpcService = Optional.empty();

  private final Vertx vertx;
  private Discv5Service discoveryService;
  private ConnectionService connectionService;
  private HistoryNetwork historyNetwork;
  private UTPManager utpManager;
  private HistoryAPI historyAPI;
  private Discv5API discv5API;

  private final IncomingRequestTalkHandler incomingRequestTalkHandler =
      new IncomingRequestTalkHandler();

  public HistoryNetworkMainService(
      final HistoryNetworkMainServiceConfig historyNetworkMainServiceConfig,
      final SambaConfiguration sambaConfiguration,
      final Vertx vertx) {
    this.timeProvider = historyNetworkMainServiceConfig.getTimeProvider();
    this.eventChannels = historyNetworkMainServiceConfig.getEventChannels();
    this.metricsSystem = historyNetworkMainServiceConfig.getMetricsSystem();
    this.asyncRunner =
        historyNetworkMainServiceConfig.createAsyncRunner(
            "samba_discovery_service", DEFAULT_ASYNC_P2P_MAX_THREADS, DEFAULT_ASYNC_P2P_MAX_QUEUE);
    this.sambaConfiguration = sambaConfiguration;
    this.vertx = vertx;

    // TODO move nodeRecord
    final NodeRecord nodeRecord =
        Discv5Service.createNodeRecord(
            this.sambaConfiguration.getDiscoveryConfig(), this.sambaConfiguration.getSecreteKey());
    LOG.info(sambaConfiguration.generateSambaConfigurationSummary(nodeRecord));
    initDiscoveryService(nodeRecord);
    initUTPService();
    initHistoryNetwork();
    initIncomingRequestTalkHandlers();
    initConnectionService();
    initAPIs();
    initRestAPI();
    initJsonRPCService();
  }

  private void initIncomingRequestTalkHandlers() {

    final HistoryNetworkIncomingRequestHandler historyNetworkIncomingRequestHandler =
        new HistoryNetworkIncomingRequestHandler(this.historyNetwork);
    historyNetworkIncomingRequestHandler
        .addHandler(MessageType.PING, new PingHandler())
        .addHandler(MessageType.FIND_NODES, new FindNodesHandler())
        .addHandler(MessageType.FIND_CONTENT, new FindContentHandler())
        .addHandler(MessageType.OFFER, new OfferHandler());

    final UTPNetworkIncomingRequestHandler utpNetworkIncomingRequestHandler =
        new UTPNetworkIncomingRequestHandler(this.utpManager);
    this.incomingRequestTalkHandler.addHandlers(
        historyNetworkIncomingRequestHandler, utpNetworkIncomingRequestHandler);
  }

  private void initUTPService() {
    this.utpManager = new UTPManager(this.discoveryService, this.metricsSystem);
  }

  private void initAPIs() {
    this.historyAPI = new HistoryAPIClient(this.historyNetwork);
    this.discv5API = new Discv5APIClient(this.discoveryService);
  }

  private void initJsonRPCService() {
    final JsonRpcConfiguration jsonRpcConfiguration = sambaConfiguration.getJsonRpcConfigurationn();
    if (jsonRpcConfiguration.isEnableJsonRpcServer()) {
      final Map<String, JsonRpcMethod> methods = new HashMap<>();

      methods.put(RpcMethod.CLIENT_VERSION.getMethodName(), new ClientVersion("1"));

      methods.put(RpcMethod.DISCV5_NODE_INFO.getMethodName(), new Discv5NodeInfo(this.discv5API));
      methods.put(
          RpcMethod.DISCV5_UPDATE_NODE_INFO.getMethodName(),
          new Discv5UpdateNodeInfo(this.discv5API));
      methods.put(RpcMethod.DISCV5_GET_ENR.getMethodName(), new Discv5GetEnr(this.discv5API));
      methods.put(RpcMethod.DISCV5_FIND_NODE.getMethodName(), new Discv5FindNode(this.discv5API));
      methods.put(RpcMethod.DISCV5_TALK_REQ.getMethodName(), new Discv5TalkReq(this.discv5API));
      methods.put(
          RpcMethod.DISCV5_ROUTING_TABLE_INFO.getMethodName(),
          new Discv5RoutingTableInfo(this.discv5API));
      methods.put(RpcMethod.DISCV5_ADD_ENR.getMethodName(), new Discv5AddEnr(this.discv5API));
      methods.put(RpcMethod.DISCV5_DELETE_ENR.getMethodName(), new Discv5DeleteEnr(this.discv5API));

      methods.put(
          RpcMethod.PORTAL_HISTORY_ADD_ENR.getMethodName(),
          new PortalHistoryAddEnr(this.historyAPI));
      methods.put(
          RpcMethod.PORTAL_HISTORY_GET_ENR.getMethodName(),
          new PortalHistoryGetEnr(this.historyAPI));
      methods.put(
          RpcMethod.PORTAL_HISTORY_PING.getMethodName(),
          new PortalHistoryPing(this.historyNetwork, this.discoveryService));
      methods.put(
          RpcMethod.PORTAL_HISTORY_DELETE_ENR.getMethodName(),
          new PortalHistoryDeleteEnr(this.historyAPI));
      methods.put(
          RpcMethod.PORTAL_HISTORY_FIND_NODES.getMethodName(),
          new PortalHistoryFindNodes(this.historyAPI));
      methods.put(
          RpcMethod.PORTAL_HISTORY_STORE.getMethodName(), new PortalHistoryStore(this.historyAPI));
      methods.put(
          RpcMethod.PORTAL_HISTORY_FIND_CONTENT.getMethodName(),
          new PortalHistoryFindContent(this.historyAPI));
      methods.put(
          RpcMethod.PORTAL_HISTORY_GET_CONTENT.getMethodName(),
          new PortalHistoryGetContent(this.historyAPI));
      methods.put(
          RpcMethod.PORTAL_HISTORY_TRACE_GET_CONTENT.getMethodName(),
          new PortalHistoryTraceGetContent(this.historyAPI, this.timeProvider));
      methods.put(
          RpcMethod.PORTAL_HISTORY_OFFER.getMethodName(), new PortalHistoryOffer(this.historyAPI));
      methods.put(
          RpcMethod.PORTAL_HISTORY_LOCAL_CONTENT.getMethodName(),
          new PortalHistoryLocalContent(this.historyAPI));
      methods.put(
          RpcMethod.PORTAL_HISTORY_LOOKUP_ENR.getMethodName(),
          new PortalHistoryLookupEnr(this.historyAPI));
      methods.put(
          RpcMethod.PORTAL_HISTORY_PUT_CONTENT.getMethodName(),
          new PortalHistoryPutContent(this.historyAPI));
      methods.put(
          RpcMethod.PORTAL_HISTORY_RECURSIVE_FIND_NODES.getMethodName(),
          new PortalHistoryRecursiveFindNodes(this.historyAPI));
      methods.put(RpcMethod.PORTAL_BEACON_STORE.getMethodName(), new PortalBeaconStore());

      jsonRpcService =
          Optional.of(
              new JsonRpcService(
                  this.vertx,
                  jsonRpcConfiguration,
                  metricsSystem,
                  methods,
                  new HealthService(new LivenessCheck())));
    }
  }

  private void initHistoryNetwork() {
    this.historyNetwork =
        new HistoryNetwork(
            this.discoveryService,
            new HistoryRocksDB(
                sambaConfiguration.getDataPath(),
                sambaConfiguration.getStorageConfig(),
                metricsSystem),
            this.utpManager,
            metricsSystem);
  }

  private void initConnectionService() {
    this.connectionService =
        new ConnectionService(
            this.metricsSystem, this.asyncRunner, this.discoveryService, this.historyNetwork);
  }

  protected void initDiscoveryService(NodeRecord nodeRecord) {
    this.discoveryService =
        new Discv5Service(
            this.metricsSystem,
            this.asyncRunner,
            this.sambaConfiguration.getDiscoveryConfig(),
            this.sambaConfiguration.getSecreteKey(),
            nodeRecord,
            this.incomingRequestTalkHandler);
  }

  @Override
  protected SafeFuture<?> doStart() {
    LOG.debug("Starting {}", this.getClass().getSimpleName());
    this.incomingRequestTalkHandler.start();
    return SafeFuture.allOfFailFast(this.discoveryService.start())
        .thenCompose(__ -> this.connectionService.start())
        .thenCompose(
            __ ->
                jsonRpcService.map(JsonRpcService::start).orElse(SafeFuture.completedFuture(null)))
        .thenCompose(
            __ -> portalRestAPI.map(PortalRestAPI::start).orElse(SafeFuture.completedFuture(null)));
  }

  public void initRestAPI() {
    final RestServerConfig restServerConfig = sambaConfiguration.getRestServerConfig();
    if (restServerConfig.isEnableRestServer()) {
      portalRestAPI =
          Optional.of(
              new PortalAPI(
                  sambaConfiguration.getRestServerConfig(),
                  eventChannels,
                  asyncRunner,
                  timeProvider));
    }
  }

  @Override
  protected SafeFuture<?> doStop() {
    LOG.debug("Stopping {}", this.getClass().getSimpleName());
    return SafeFuture.allOf(
        discoveryService.stop(),
        connectionService.stop(),
        portalRestAPI.map(PortalRestAPI::stop).orElse(SafeFuture.completedFuture(null)));
  }

  @Override
  public HistoryAPI getSDK() {
    return new HistoryAPIClient(this.historyNetwork);
  }

  @Override
  public Discv5API getDiscv5API() {
    return new Discv5APIClient(this.discoveryService);
  }
}
