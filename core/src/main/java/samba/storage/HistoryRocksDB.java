package samba.storage;

import static com.google.common.base.Preconditions.*;

import samba.domain.content.*;
import samba.rocksdb.*;
import samba.rocksdb.exceptions.StorageException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.plugin.services.MetricsSystem;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;

public class HistoryRocksDB implements HistoryDB {

  protected static final Logger LOG = LogManager.getLogger(HistoryRocksDB.class);
  private final RocksDBInstance rocksDBInstance;

  public HistoryRocksDB(
      Path path, MetricsSystem metricsSystem, RocksDBMetricsFactory rocksDBMetricsFactory)
      throws StorageException {
    this.rocksDBInstance =
        new RocksDBInstance(
            RocksDBConfiguration.createDefault(path),
            Arrays.asList(KeyValueSegment.values()),
            List.of(),
            metricsSystem,
            rocksDBMetricsFactory);
  }

  public HistoryRocksDB(RocksDBInstance rocksDBInstance) {
    this.rocksDBInstance = rocksDBInstance;
  }

  public static HistoryRocksDB create(MetricsSystem metricsSystem, final Path dataDirectory) {
    try {
      StorageFactory storageFactory = new StorageFactory(metricsSystem, dataDirectory);
      return storageFactory.create();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // TODO: reduce the verbosity of this method once is ready.

  @Override
  public boolean saveContent(Bytes sszKey, Bytes sszValue) {
    try {
      ContentKey contentKey = ContentUtil.createContentKeyFromSszBytes(sszKey).get();
      LOG.debug(
          "Store {} with Key: {} and Value {}", contentKey.getContentType(), sszKey, sszValue);
      switch (contentKey.getContentType()) {
        case ContentType.BLOCK_HEADER -> {
          Bytes blockHashKey = contentKey.getBlockHashSsz();

          if (!ContentUtil.isBlockHeaderValid(blockHashKey, sszValue)) {
            LOG.info("BlockHeader for blockHeaderKey: {} is invalid", blockHashKey);
            break;
          }

          Optional<ContentBlockHeader> blockHeader =
              ContentUtil.createBlockHeaderfromSszBytes(sszValue);
          if (blockHeader.isEmpty()) {
            LOG.info("BlockHeader for blockHashKey: {} is invalid", blockHashKey);
            break;
          }
          System.out.println(blockHeader.get().getBlockHeader().getNumber());
          Bytes blockNumberKey =
              new ContentKey(
                      ContentType.BLOCK_HEADER_BY_NUMBER,
                      UInt64.valueOf(blockHeader.get().getBlockHeader().getNumber()))
                  .getBlockNumberSsz();
          save(KeyValueSegment.BLOCK_HASH_BY_BLOCK_NUMBER, blockNumberKey, blockHashKey);
          save(KeyValueSegment.BLOCK_HEADER, blockHashKey, sszValue); // TODO async
        }
        case ContentType.BLOCK_BODY -> {
          Bytes blockHash = contentKey.getBlockHashSsz();

          this.getBlockHeaderByBlockHash(blockHash)
              .ifPresentOrElse(
                  blockHeader -> {
                    if (ContentUtil.isBlockBodyValid(blockHeader, sszValue)) {
                      save(KeyValueSegment.BLOCK_BODY, blockHash, sszValue);
                    } else {
                      LOG.info("BlockBody for blockHash: {} is invalid", blockHash);
                    }
                  },
                  () -> {
                    // TODO trigger a lookup: Query X nearest  until either content is found or no
                    // peers are left to query.
                    LOG.info("Block Header for {} not found locally", blockHash);
                  });
        }
        case ContentType.RECEIPT -> {
          Bytes blockHash = contentKey.getBlockHashSsz();
          // TODO should we do any validation?
          save(KeyValueSegment.RECEIPT, blockHash, sszValue);
        }
        case ContentType.BLOCK_HEADER_BY_NUMBER -> {
          Bytes blockNumberKey = contentKey.getBlockNumberSsz();

          if (!ContentUtil.isBlockHeaderValid(blockNumberKey, sszValue)) {
            LOG.info("BlockHeader for blockNumber: {} is invalid", blockNumberKey);
            break;
          }

          Optional<Bytes> blockHashKey = getBlockHashByBlockNumber(blockNumberKey);
          if (blockHashKey.isEmpty()) {
            Optional<ContentBlockHeader> blockHeader =
                ContentUtil.createBlockHeaderfromSszBytes(sszValue);
            if (blockHeader.isEmpty()) {
              LOG.info("BlockHeader for blockNumber: {} is invalid", blockNumberKey);
              break;
            }
            blockHashKey =
                Optional.of(
                    new ContentKey(
                            ContentType.BLOCK_HEADER,
                            blockHeader.get().getBlockHeader().getHash().copy())
                        .getBlockHashSsz());
            if (blockHashKey.isEmpty()) {
              LOG.info("BlockHash for blockNumber: {} is invalid", blockNumberKey);
              break;
            }
            save(KeyValueSegment.BLOCK_HASH_BY_BLOCK_NUMBER, blockNumberKey, blockHashKey.get());
          }
          save(KeyValueSegment.BLOCK_HEADER, blockHashKey.get(), sszValue);
        }
        default ->
            throw new IllegalArgumentException(
                String.format("CONTENT: Invalid payload type %s", contentKey.getContentType()));
      }
      return true;
    } catch (Exception e) {
      LOG.info("Content could not be saved. ContentKey: {} , ContentValue{}", sszKey, sszValue);
      return false;
    }
  }

  @Override
  public Optional<ContentBlockHeader> getBlockHeaderByBlockHash(Bytes blockHash) {
    Optional<byte[]> databaseBlockHeader =
        this.rocksDBInstance.get(KeyValueSegment.BLOCK_HEADER, blockHash.toArray());
    if (databaseBlockHeader.isEmpty()) return Optional.empty();
    return ContentUtil.createBlockHeaderfromSszBytes(Bytes.wrap(databaseBlockHeader.get()));
  }

  @Override
  public Optional<ContentBlockBody> getBlockBodyByBlockHash(Bytes blockHash) {
    Optional<byte[]> databaseBlockBody =
        this.rocksDBInstance.get(KeyValueSegment.BLOCK_BODY, blockHash.toArray());
    if (databaseBlockBody.isEmpty()) return Optional.empty();
    return ContentUtil.createBlockBodyFromSszBytes(Bytes.wrap(databaseBlockBody.get()));
  }

  @Override
  public Optional<ContentReceipts> getReceiptsByBlockHash(Bytes blockHash) {
    Optional<byte[]> databaseReceipts =
        this.rocksDBInstance.get(KeyValueSegment.RECEIPT, blockHash.toArray());
    if (databaseReceipts.isEmpty()) return Optional.empty();
    return ContentUtil.createReceiptsFromSszBytes(Bytes.wrap(databaseReceipts.get()));
  }

  @Override
  public Optional<Bytes> getBlockHashByBlockNumber(Bytes blockNumberKey) {
    Optional<byte[]> databaseKey =
        this.rocksDBInstance.get(
            KeyValueSegment.BLOCK_HASH_BY_BLOCK_NUMBER, blockNumberKey.toArray());
    if (databaseKey.isEmpty()) return Optional.empty();
    return Optional.of(Bytes.wrap(databaseKey.get()));
  }

  @Override
  public Optional<Bytes> get(ContentType contentType, Bytes contentKey) {
    Optional<byte[]> databaseContent =
        this.rocksDBInstance.get(getSegmentFromContentType(contentType), contentKey.toArray());
    if (databaseContent.isEmpty()) return Optional.empty();
    return Optional.of(Bytes.wrap(databaseContent.get()));
  }

  private void save(Segment segment, Bytes key, Bytes content) {
    checkArgument(
        !content.isEmpty(),
        "Content should have more than 1 byte when persisting {}",
        segment.getName());
    KeyValueStorageTransaction tx = rocksDBInstance.startTransaction();
    tx.put(segment, key.toArray(), content.toArray());
    tx.commit();
  }

  private Segment getSegmentFromContentType(ContentType contentType) {
    return switch (contentType) {
      case ContentType.BLOCK_HEADER -> KeyValueSegment.BLOCK_HEADER;
      case ContentType.BLOCK_BODY -> KeyValueSegment.BLOCK_BODY;
      case ContentType.RECEIPT -> KeyValueSegment.RECEIPT;
      case ContentType.BLOCK_HEADER_BY_NUMBER -> KeyValueSegment.BLOCK_HASH_BY_BLOCK_NUMBER;
    };
  }

  public void close() {
    this.rocksDBInstance.close();
  }
}
