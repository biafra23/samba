package examples;

import samba.utp.UTPClient;
import samba.utp.network.TransportLayer;
import samba.utp.network.udp.UDPTransportLayer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Offer {

  public static void main(String args[])
      throws ExecutionException, InterruptedException, IOException {
    UDPTransportLayer udpTransportLayer = new UDPTransportLayer("localhost", 13345);

    UTPClient chanel = new UTPClient(udpTransportLayer);
    startListeningIncomingPackets(udpTransportLayer, chanel);

    chanel.connect(333).thenCompose(v -> chanel.write(getContentToSend("Content21"))).get();
  }

  private static ByteBuffer getContentToSend(String inputString) {
    byte[] byteArray = inputString.getBytes(StandardCharsets.UTF_8);
    ByteBuffer buffer = ByteBuffer.allocate(byteArray.length);
    buffer.put(byteArray);
    buffer.flip();
    System.out.println("Content to send:" + StandardCharsets.UTF_8.decode(buffer));
    return buffer;
  }

  public static void startListeningIncomingPackets(
      TransportLayer transportLayer, UTPClient utpClient) {
    CompletableFuture.runAsync(
        () -> {
          while (true) {
            if (utpClient.isAlive()) {
              try {
                utpClient.receivePacket(transportLayer.onPacketReceive());
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
          }
        });
  }
}
