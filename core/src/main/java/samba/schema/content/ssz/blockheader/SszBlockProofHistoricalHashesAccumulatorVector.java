package samba.schema.content.ssz.blockheader;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.infrastructure.ssz.collections.SszBytes32Vector;
import tech.pegasys.teku.infrastructure.ssz.primitive.SszBytes32;
import tech.pegasys.teku.infrastructure.ssz.schema.collections.SszBytes32VectorSchema;

public class SszBlockProofHistoricalHashesAccumulatorVector {

  private static final int BLOCK_PROOF_HISTORICAL_HASHES_ACCUMULATOR_VECTOR_SIZE = 15;
  private static final SszBytes32VectorSchema<SszBytes32Vector> schema =
      SszBytes32VectorSchema.create(BLOCK_PROOF_HISTORICAL_HASHES_ACCUMULATOR_VECTOR_SIZE);
  private final SszBytes32Vector BlockProofHistoricalHashesAccumulator;

  public SszBlockProofHistoricalHashesAccumulatorVector(
      SszBytes32Vector BlockProofHistoricalHashesAccumulator) {
    this.BlockProofHistoricalHashesAccumulator = BlockProofHistoricalHashesAccumulator;
  }

  public SszBlockProofHistoricalHashesAccumulatorVector(
      List<Bytes32> blockProofHistoricalHashesAccumulator) {
    if (blockProofHistoricalHashesAccumulator.size()
        != BLOCK_PROOF_HISTORICAL_HASHES_ACCUMULATOR_VECTOR_SIZE) {
      throw new IllegalArgumentException(
          "BlockProofHistoricalHashesAccumulator size is not equal to "
              + BLOCK_PROOF_HISTORICAL_HASHES_ACCUMULATOR_VECTOR_SIZE);
    }
    this.BlockProofHistoricalHashesAccumulator =
        schema.createFromElements(createSszBytes32List(blockProofHistoricalHashesAccumulator));
  }

  public SszBlockProofHistoricalHashesAccumulatorVector(
      Bytes BlockProofHistoricalHashesAccumulator) {
    this.BlockProofHistoricalHashesAccumulator =
        schema.sszDeserialize(BlockProofHistoricalHashesAccumulator);
  }

  public static SszBytes32VectorSchema<SszBytes32Vector> getSchema() {
    return schema;
  }

  public static SszBytes32Vector createVector(List<Bytes32> BlockProofHistoricalHashesAccumulator) {
    if (BlockProofHistoricalHashesAccumulator.size()
        != BLOCK_PROOF_HISTORICAL_HASHES_ACCUMULATOR_VECTOR_SIZE) {
      throw new IllegalArgumentException(
          "BlockProofHistoricalHashesAccumulator size is not equal to "
              + BLOCK_PROOF_HISTORICAL_HASHES_ACCUMULATOR_VECTOR_SIZE);
    }
    return schema.createFromElements(createSszBytes32List(BlockProofHistoricalHashesAccumulator));
  }

  public static List<Bytes32> decodeVector(SszBytes32Vector BlockProofHistoricalHashesAccumulator) {
    return BlockProofHistoricalHashesAccumulator.stream()
        .map(SszBytes32::get)
        .collect(Collectors.toList());
  }

  public static List<Bytes32> decodeVector(Bytes BlockProofHistoricalHashesAccumulator) {
    SszBlockProofHistoricalHashesAccumulatorVector vector =
        new SszBlockProofHistoricalHashesAccumulatorVector(BlockProofHistoricalHashesAccumulator);
    return vector.getDecodedVector();
  }

  public SszBytes32Vector getEncodedVector() {
    return BlockProofHistoricalHashesAccumulator;
  }

  public List<Bytes32> getDecodedVector() {
    return BlockProofHistoricalHashesAccumulator.stream()
        .map(SszBytes32::get)
        .collect(Collectors.toList());
  }

  private static List<SszBytes32> createSszBytes32List(
      List<Bytes32> blockProofHistoricalHashesAccumulator) {
    return blockProofHistoricalHashesAccumulator.stream()
        .map(SszBytes32::of)
        .collect(Collectors.toList());
  }

  public Bytes sszSerialize() {
    return BlockProofHistoricalHashesAccumulator.sszSerialize();
  }
}
