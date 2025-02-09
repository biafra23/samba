package samba.schema.content.ssz.blockheader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import samba.domain.content.ContentProofConstants;
import samba.domain.content.ContentProofType;

import java.util.ArrayList;
import java.util.List;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Test;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;

public class BlockHeaderProofUnionTest {

  @Test
  public void testSszDecodeNone() {
    BlockHeaderProofUnion blockHeaderProofUnion =
        new BlockHeaderProofUnion(Bytes.fromHexString("0x00"));
    assertEquals(ContentProofType.NONE, blockHeaderProofUnion.getProofType());
  }

  @Test
  public void testSszDecodeBlockProofHistoricalHashesAccumulator() {
    List<Bytes32> blockProofHistoricalHashesAccumulator = new ArrayList<>();
    for (int i = 0;
        i < ContentProofConstants.BLOCK_PROOF_HISTORICAL_HASHES_ACCUMULATOR_VECTOR_SIZE;
        i++) blockProofHistoricalHashesAccumulator.add(Bytes32.repeat((byte) i));
    BlockHeaderProofUnion blockHeaderProofUnion =
        new BlockHeaderProofUnion(
            Bytes.fromHexString(
                "0x0100000000000000000000000000000000000000000000000000000000000000000101010101010101010101010101010101010101010101010101010101010101020202020202020202020202020202020202020202020202020202020202020203030303030303030303030303030303030303030303030303030303030303030404040404040404040404040404040404040404040404040404040404040404050505050505050505050505050505050505050505050505050505050505050506060606060606060606060606060606060606060606060606060606060606060707070707070707070707070707070707070707070707070707070707070707080808080808080808080808080808080808080808080808080808080808080809090909090909090909090909090909090909090909090909090909090909090a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e"));
    assertEquals(
        ContentProofType.BLOCK_PROOF_HISTORICAL_HASHES_ACCUMULATOR,
        blockHeaderProofUnion.getProofType());
    assertEquals(
        blockProofHistoricalHashesAccumulator,
        blockHeaderProofUnion.getBlockProofHistoricalHashesAccumulator());
  }

  @Test
  public void testSszDecodeBlockProofHistoricalRootsContainer() {
    List<Bytes32> beaconBlockProof = new ArrayList<>();
    for (int i = 0; i < ContentProofConstants.BEACON_BLOCK_PROOF_HISTORICAL_ROOTS_VECTOR_SIZE; i++)
      beaconBlockProof.add(Bytes32.repeat((byte) i));
    Bytes32 beaconBlockRoot = Bytes32.repeat((byte) 1);
    List<Bytes32> executionBlockProof = new ArrayList<>();
    UInt64 slot = UInt64.valueOf(1234);
    for (int i = 0; i < ContentProofConstants.EXECUTION_BLOCK_PROOF_SIZE; i++)
      executionBlockProof.add(Bytes32.repeat((byte) i));
    BlockHeaderProofUnion blockHeaderProofUnion =
        new BlockHeaderProofUnion(
            Bytes.fromHexString(
                "0x0200000000000000000000000000000000000000000000000000000000000000000101010101010101010101010101010101010101010101010101010101010101020202020202020202020202020202020202020202020202020202020202020203030303030303030303030303030303030303030303030303030303030303030404040404040404040404040404040404040404040404040404040404040404050505050505050505050505050505050505050505050505050505050505050506060606060606060606060606060606060606060606060606060606060606060707070707070707070707070707070707070707070707070707070707070707080808080808080808080808080808080808080808080808080808080808080809090909090909090909090909090909090909090909090909090909090909090a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d010101010101010101010101010101010101010101010101010101010101010100000000000000000000000000000000000000000000000000000000000000000101010101010101010101010101010101010101010101010101010101010101020202020202020202020202020202020202020202020202020202020202020203030303030303030303030303030303030303030303030303030303030303030404040404040404040404040404040404040404040404040404040404040404050505050505050505050505050505050505050505050505050505050505050506060606060606060606060606060606060606060606060606060606060606060707070707070707070707070707070707070707070707070707070707070707080808080808080808080808080808080808080808080808080808080808080809090909090909090909090909090909090909090909090909090909090909090a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0ad204000000000000"));
    BlockProofHistoricalRootsContainer blockProofHistoricalRootsContainer =
        blockHeaderProofUnion.getBlockProofHistoricalRootsContainer();
    assertEquals(
        ContentProofType.BLOCK_PROOF_HISTORICAL_ROOTS, blockHeaderProofUnion.getProofType());
    assertEquals(
        beaconBlockProof, blockProofHistoricalRootsContainer.getBeaconBlockProofHistoricalRoots());
    assertEquals(beaconBlockRoot, blockProofHistoricalRootsContainer.getBlockRoot());
    assertEquals(executionBlockProof, blockProofHistoricalRootsContainer.getExecutionBlockProof());
    assertEquals(slot, blockProofHistoricalRootsContainer.getSlot());
  }

  @Test
  public void testSszDecodeBlockProofHistoricalSummariesContainer() {
    List<Bytes32> beaconBlockProof = new ArrayList<>();
    for (int i = 0;
        i < ContentProofConstants.BEACON_BLOCK_PROOF_HISTORICAL_SUMMARIES_VECTOR_SIZE;
        i++) beaconBlockProof.add(Bytes32.repeat((byte) i));
    Bytes32 beaconBlockRoot = Bytes32.repeat((byte) 1);
    List<Bytes32> executionBlockProof = new ArrayList<>();
    UInt64 slot = UInt64.valueOf(1234);
    for (int i = 0; i < ContentProofConstants.EXECUTION_BLOCK_PROOF_LIMIT; i++)
      executionBlockProof.add(Bytes32.repeat((byte) i));
    BlockHeaderProofUnion blockHeaderProofUnion =
        new BlockHeaderProofUnion(
            Bytes.fromHexString(
                "0x0300000000000000000000000000000000000000000000000000000000000000000101010101010101010101010101010101010101010101010101010101010101020202020202020202020202020202020202020202020202020202020202020203030303030303030303030303030303030303030303030303030303030303030404040404040404040404040404040404040404040404040404040404040404050505050505050505050505050505050505050505050505050505050505050506060606060606060606060606060606060606060606060606060606060606060707070707070707070707070707070707070707070707070707070707070707080808080808080808080808080808080808080808080808080808080808080809090909090909090909090909090909090909090909090909090909090909090a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0101010101010101010101010101010101010101010101010101010101010101cc010000d20400000000000000000000000000000000000000000000000000000000000000000000000000000101010101010101010101010101010101010101010101010101010101010101020202020202020202020202020202020202020202020202020202020202020203030303030303030303030303030303030303030303030303030303030303030404040404040404040404040404040404040404040404040404040404040404050505050505050505050505050505050505050505050505050505050505050506060606060606060606060606060606060606060606060606060606060606060707070707070707070707070707070707070707070707070707070707070707080808080808080808080808080808080808080808080808080808080808080809090909090909090909090909090909090909090909090909090909090909090a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b"));
    BlockProofHistoricalSummariesContainer blockProofHistoricalSummariesContainer =
        blockHeaderProofUnion.getBlockProofHistoricalSummariesContainer();
    assertEquals(
        ContentProofType.BLOCK_PROOF_HISTORICAL_SUMMARIES, blockHeaderProofUnion.getProofType());
    assertEquals(
        beaconBlockProof,
        blockProofHistoricalSummariesContainer.getBeaconBlockProofHistoricalSummaries());
    assertEquals(beaconBlockRoot, blockProofHistoricalSummariesContainer.getBlockRoot());
    assertEquals(
        executionBlockProof, blockProofHistoricalSummariesContainer.getExecutionBlockProof());
    assertEquals(slot, blockProofHistoricalSummariesContainer.getSlot());
  }

  @Test
  public void testSszEncodeNone() {
    BlockHeaderProofUnion blockHeaderProofUnion = new BlockHeaderProofUnion(ContentProofType.NONE);
    Bytes encodedBlockHeaderProofUnion = blockHeaderProofUnion.sszSerialize();
    assertEquals(encodedBlockHeaderProofUnion, Bytes.fromHexString("0x00"));
  }

  @Test
  public void testSszEncodeBlockProofHistoricalHashesAccumulator() {
    List<Bytes32> blockProofHistoricalHashesAccumulator = new ArrayList<>();
    for (int i = 0;
        i < ContentProofConstants.BLOCK_PROOF_HISTORICAL_HASHES_ACCUMULATOR_VECTOR_SIZE;
        i++) blockProofHistoricalHashesAccumulator.add(Bytes32.repeat((byte) i));
    BlockHeaderProofUnion blockHeaderProofUnion =
        new BlockHeaderProofUnion(
            ContentProofType.BLOCK_PROOF_HISTORICAL_HASHES_ACCUMULATOR,
            blockProofHistoricalHashesAccumulator);
    Bytes encodedBlockHeaderProofUnion = blockHeaderProofUnion.sszSerialize();
    assertEquals(
        encodedBlockHeaderProofUnion,
        Bytes.fromHexString(
            "0x0100000000000000000000000000000000000000000000000000000000000000000101010101010101010101010101010101010101010101010101010101010101020202020202020202020202020202020202020202020202020202020202020203030303030303030303030303030303030303030303030303030303030303030404040404040404040404040404040404040404040404040404040404040404050505050505050505050505050505050505050505050505050505050505050506060606060606060606060606060606060606060606060606060606060606060707070707070707070707070707070707070707070707070707070707070707080808080808080808080808080808080808080808080808080808080808080809090909090909090909090909090909090909090909090909090909090909090a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e0e"));
  }

  @Test
  public void testSszEncodeBlockProofHistoricalRootsContainer() {
    List<Bytes32> beaconBlockProof = new ArrayList<>();
    for (int i = 0; i < ContentProofConstants.BEACON_BLOCK_PROOF_HISTORICAL_ROOTS_VECTOR_SIZE; i++)
      beaconBlockProof.add(Bytes32.repeat((byte) i));
    Bytes32 beaconBlockRoot = Bytes32.repeat((byte) 1);
    List<Bytes32> executionBlockProof = new ArrayList<>();
    UInt64 slot = UInt64.valueOf(1234);
    for (int i = 0; i < ContentProofConstants.EXECUTION_BLOCK_PROOF_SIZE; i++)
      executionBlockProof.add(Bytes32.repeat((byte) i));
    BlockHeaderProofUnion blockHeaderProofUnion =
        new BlockHeaderProofUnion(
            ContentProofType.BLOCK_PROOF_HISTORICAL_ROOTS,
            beaconBlockProof,
            beaconBlockRoot,
            executionBlockProof,
            slot);
    Bytes encodedBlockHeaderProofUnion = blockHeaderProofUnion.sszSerialize();
    assertEquals(
        encodedBlockHeaderProofUnion,
        Bytes.fromHexString(
            "0x0200000000000000000000000000000000000000000000000000000000000000000101010101010101010101010101010101010101010101010101010101010101020202020202020202020202020202020202020202020202020202020202020203030303030303030303030303030303030303030303030303030303030303030404040404040404040404040404040404040404040404040404040404040404050505050505050505050505050505050505050505050505050505050505050506060606060606060606060606060606060606060606060606060606060606060707070707070707070707070707070707070707070707070707070707070707080808080808080808080808080808080808080808080808080808080808080809090909090909090909090909090909090909090909090909090909090909090a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d0d010101010101010101010101010101010101010101010101010101010101010100000000000000000000000000000000000000000000000000000000000000000101010101010101010101010101010101010101010101010101010101010101020202020202020202020202020202020202020202020202020202020202020203030303030303030303030303030303030303030303030303030303030303030404040404040404040404040404040404040404040404040404040404040404050505050505050505050505050505050505050505050505050505050505050506060606060606060606060606060606060606060606060606060606060606060707070707070707070707070707070707070707070707070707070707070707080808080808080808080808080808080808080808080808080808080808080809090909090909090909090909090909090909090909090909090909090909090a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0ad204000000000000"));
  }

  @Test
  public void testSszEncodeBlockProofHistoricalSummariesContainer() {
    List<Bytes32> beaconBlockProof = new ArrayList<>();
    for (int i = 0;
        i < ContentProofConstants.BEACON_BLOCK_PROOF_HISTORICAL_SUMMARIES_VECTOR_SIZE;
        i++) beaconBlockProof.add(Bytes32.repeat((byte) i));
    Bytes32 beaconBlockRoot = Bytes32.repeat((byte) 1);
    List<Bytes32> executionBlockProof = new ArrayList<>();
    UInt64 slot = UInt64.valueOf(1234);
    for (int i = 0; i < ContentProofConstants.EXECUTION_BLOCK_PROOF_LIMIT; i++)
      executionBlockProof.add(Bytes32.repeat((byte) i));
    BlockHeaderProofUnion blockHeaderProofUnion =
        new BlockHeaderProofUnion(
            ContentProofType.BLOCK_PROOF_HISTORICAL_SUMMARIES,
            beaconBlockProof,
            beaconBlockRoot,
            executionBlockProof,
            slot);
    Bytes encodedBlockHeaderProofUnion = blockHeaderProofUnion.sszSerialize();
    assertEquals(
        encodedBlockHeaderProofUnion,
        Bytes.fromHexString(
            "0x0300000000000000000000000000000000000000000000000000000000000000000101010101010101010101010101010101010101010101010101010101010101020202020202020202020202020202020202020202020202020202020202020203030303030303030303030303030303030303030303030303030303030303030404040404040404040404040404040404040404040404040404040404040404050505050505050505050505050505050505050505050505050505050505050506060606060606060606060606060606060606060606060606060606060606060707070707070707070707070707070707070707070707070707070707070707080808080808080808080808080808080808080808080808080808080808080809090909090909090909090909090909090909090909090909090909090909090a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0c0101010101010101010101010101010101010101010101010101010101010101cc010000d20400000000000000000000000000000000000000000000000000000000000000000000000000000101010101010101010101010101010101010101010101010101010101010101020202020202020202020202020202020202020202020202020202020202020203030303030303030303030303030303030303030303030303030303030303030404040404040404040404040404040404040404040404040404040404040404050505050505050505050505050505050505050505050505050505050505050506060606060606060606060606060606060606060606060606060606060606060707070707070707070707070707070707070707070707070707070707070707080808080808080808080808080808080808080808080808080808080808080809090909090909090909090909090909090909090909090909090909090909090a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0a0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b"));
  }
}
