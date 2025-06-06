package samba.util;

import java.util.List;
import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.ethereum.beacon.discovery.schema.NodeRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.pegasys.teku.infrastructure.ssz.SszList;
import tech.pegasys.teku.infrastructure.ssz.primitive.SszByte;
import tech.pegasys.teku.infrastructure.ssz.schema.SszListSchema;
import tech.pegasys.teku.infrastructure.ssz.schema.SszPrimitiveSchemas;

public class ProtocolVersionUtil {

  public static Logger LOG = LoggerFactory.getLogger(ProtocolVersionUtil.class);

  public static final List<Integer> SUPPORTED_PROTOCOL_VERSIONS = List.of(0, 1);
  public static final int MAX_VERSION_COUNT = 8;
  private static final SszListSchema<SszByte, SszList<SszByte>> protocolVersionListSchema =
      (SszListSchema<SszByte, SszList<SszByte>>)
          SszListSchema.create(SszPrimitiveSchemas.UINT8_SCHEMA, MAX_VERSION_COUNT);

  public static void setSupportedProtocolVersions(NodeRecord nodeRecord) {
    setSupportedProtocolVersions(nodeRecord, SUPPORTED_PROTOCOL_VERSIONS);
  }

  public static void setSupportedProtocolVersions(
      NodeRecord nodeRecord, List<Integer> protocolVersions) {
    if (protocolVersions.size() > MAX_VERSION_COUNT) {
      throw new IllegalArgumentException("Protocol versions list exceeds maximum size");
    }
    nodeRecord.set(
        "pv",
        protocolVersionListSchema
            .createFromElements(protocolVersions.stream().sorted().map(SszByte::of).toList())
            .sszSerialize());
  }

  public static List<Integer> getSupportedProtocolVersions(NodeRecord nodeRecord) {
    try {
      SszList<SszByte> protocolVersionList =
          protocolVersionListSchema.sszDeserialize((Bytes) nodeRecord.get("pv"));
      return protocolVersionList.stream().map(b -> Byte.toUnsignedInt(b.get())).sorted().toList();
    } catch (Exception e) {
      LOG.debug("Failed to parse protocol versions from node record: {}", nodeRecord.asEnr());
      return List.of(0);
    }
  }

  public static Bytes supportedProtocolVersionsBytes() {
    return supportedProtocolVersionsBytes(SUPPORTED_PROTOCOL_VERSIONS);
  }

  public static Bytes supportedProtocolVersionsBytes(List<Integer> protocolVersions) {
    if (protocolVersions.size() > MAX_VERSION_COUNT) {
      throw new IllegalArgumentException("Protocol versions list exceeds maximum size");
    }
    return protocolVersionListSchema
        .createFromElements(protocolVersions.stream().sorted().map(SszByte::of).toList())
        .sszSerialize();
  }

  public static Optional<Integer> getHighestSupportedProtocolVersion(
      List<Integer> otherSupportedVersions) {
    return getHighestSupportedProtocolVersion(otherSupportedVersions, SUPPORTED_PROTOCOL_VERSIONS);
  }

  public static Optional<Integer> getHighestSupportedProtocolVersion(
      List<Integer> otherSupportedVersions, List<Integer> localSupportedVersions) {
    return localSupportedVersions.stream()
        .filter(otherSupportedVersions::contains)
        .max(Integer::compareTo);
  }
}
