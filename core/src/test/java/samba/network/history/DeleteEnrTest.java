package samba.network.history;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import samba.network.history.api.HistoryNetworkInternalAPI;
import samba.network.history.routingtable.RoutingTable;
import samba.services.discovery.Discv5Client;
import samba.services.utp.UTPManager;
import samba.storage.HistoryDB;

import java.lang.reflect.Field;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;

@SuppressWarnings("unchecked")
public class DeleteEnrTest {

  private HistoryNetworkInternalAPI historyNetwork;
  private final String nodeId =
      "0xbb19e64f21d50187b61f4c68b2090db0a3283fe54021902822ff6ea0132568be";

  @BeforeEach
  public void setUp() {
    this.historyNetwork =
        new HistoryNetwork(
            mock(Discv5Client.class),
            mock(HistoryDB.class),
            mock(UTPManager.class),
            mock(MetricsSystem.class));
  }

  @Test
  public void testDeleteEnrThatIsNotPresent() throws IllegalAccessException {

    RoutingTable mockedRoutingTable = mock(RoutingTable.class);
    Field field =
        ReflectionUtils.findFields(
                HistoryNetwork.class,
                f -> f.getName().equals("routingTable"),
                ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
            .get(0);
    field.setAccessible(true);
    field.set(historyNetwork, mockedRoutingTable);

    when(mockedRoutingTable.findNode(any(Bytes.class))).thenReturn(Optional.empty());

    boolean result = historyNetwork.deleteEnr(nodeId);
    assertFalse(result);
  }
}
