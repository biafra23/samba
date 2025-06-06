package samba.services.connecton;

import samba.metrics.SambaMetricCategory;
import samba.network.Network;
import samba.services.discovery.Discv5Client;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.ethereum.beacon.discovery.schema.NodeRecord;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.hyperledger.besu.plugin.services.metrics.Counter;
import org.hyperledger.besu.plugin.services.metrics.LabelledMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.pegasys.teku.infrastructure.async.AsyncRunner;
import tech.pegasys.teku.infrastructure.async.Cancellable;
import tech.pegasys.teku.infrastructure.async.SafeFuture;
import tech.pegasys.teku.service.serviceutils.Service;

public class ConnectionService extends Service {

  private static final Logger LOG = LoggerFactory.getLogger(ConnectionService.class);

  protected static final Duration WARMUP_DISCOVERY_INTERVAL = Duration.ofSeconds(1);
  protected static final Duration DISCOVERY_INTERVAL = Duration.ofSeconds(30);

  private final Discv5Client discv5Client;
  private final Network network;

  private final Counter attemptedConnectionCounter;
  private final Counter successfulConnectionCounter;
  private final Counter failedConnectionCounter;
  private final AsyncRunner asyncRunner;
  private volatile Cancellable periodicPeerSearch;
  private final MetricsSystem metricsSystem;

  public ConnectionService(
      final MetricsSystem metricsSystem,
      final AsyncRunner asyncRunner,
      final Discv5Client discv5Client,
      final Network network) {

    this.asyncRunner = asyncRunner;
    this.network = network;
    this.discv5Client = discv5Client;
    this.metricsSystem = metricsSystem;
    final LabelledMetric<Counter> connectionAttemptCounter =
        metricsSystem.createLabelledCounter(
            SambaMetricCategory.NETWORK,
            "connection_attempt_count_total",
            "Total number of outbound connection attempts made",
            "status");
    attemptedConnectionCounter = connectionAttemptCounter.labels("attempted");
    successfulConnectionCounter = connectionAttemptCounter.labels("successful");
    failedConnectionCounter = connectionAttemptCounter.labels("failed");
  }

  @Override
  protected SafeFuture<?> doStart() {
    activeNodesSearchingTask();
    return SafeFuture.COMPLETE;
  }

  private void activeNodesSearchingTask() {
    searchForActivePeers().alwaysRun(this::createNextSearchPeerTask).finish(this::logSearchError);
  }

  private SafeFuture<Void> searchForActivePeers() {
    if (!isRunning()) {
      LOG.error("Not running so not searching for active peers");
      return SafeFuture.COMPLETE;
    }
    LOG.info(
        "{} active peers. Checking again in {} seconds",
        network.getNumberOfConnectedPeers(),
        DISCOVERY_INTERVAL.getSeconds());

    return discv5Client
        .streamLiveNodes()
        .orTimeout(30, TimeUnit.SECONDS)
        .handle(
            (peers, error) -> {
              if (error == null) {
                peers.stream()
                    .filter(this::isPeerValid)
                    .collect(Collectors.toSet())
                    .forEach(this::connectToPeers);
              } else {
                LOG.error("Discovery failed", error);
                // TODO  What to do ?
              }
              return null;
            });
  }

  private void connectToPeers(final NodeRecord nodeRecord) {
    LOG.debug("Attempting to connect to {}", nodeRecord.getNodeId());

    attemptedConnectionCounter.inc();
    network
        .ping(nodeRecord)
        .finish(
            peer -> {
              LOG.debug("Successfully connected to node {}", nodeRecord.getNodeId());
              successfulConnectionCounter.inc();
            },
            error -> {
              LOG.debug("Failed to connect to node: {}", nodeRecord.getNodeId());
              failedConnectionCounter.inc();
            });
  }

  private boolean isPeerValid(final NodeRecord NodeRecord) {
    // TODO What to validate?
    return true;
  }

  @Override
  protected SafeFuture<?> doStop() {
    //  network.unsubscribeConnect(peerConnectedSubscriptionId);
    cancelPeerSearchTask();
    return SafeFuture.COMPLETE;
  }

  private void cancelPeerSearchTask() {
    final Cancellable peerSearchTask = this.periodicPeerSearch;
    if (peerSearchTask != null) {
      peerSearchTask.cancel();
    }
  }

  private void createNextSearchPeerTask() {
    if (network.getNumberOfConnectedPeers() == 0) {
      LOG.debug("Retrying peer search, no connected peers yet");
      cancelPeerSearchTask();
      this.periodicPeerSearch =
          asyncRunner.runCancellableAfterDelay(
              this::activeNodesSearchingTask, WARMUP_DISCOVERY_INTERVAL, this::logSearchError);
    } else {
      LOG.debug("Establishing peer search task with long delay");
      cancelPeerSearchTask();
      this.periodicPeerSearch =
          asyncRunner.runWithFixedDelay(
              () -> searchForActivePeers().finish(this::logSearchError),
              DISCOVERY_INTERVAL,
              this::logSearchError);
    }
  }

  private void logSearchError(final Throwable throwable) {
    LOG.error("Error while searching for peers", throwable);
  }
}
