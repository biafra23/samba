package samba.schema.content.ssz.blockbody;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.core.Transaction;
import org.hyperledger.besu.ethereum.core.Withdrawal;
import tech.pegasys.teku.infrastructure.ssz.SszList;
import tech.pegasys.teku.infrastructure.ssz.collections.SszByteList;
import tech.pegasys.teku.infrastructure.ssz.containers.Container3;
import tech.pegasys.teku.infrastructure.ssz.containers.ContainerSchema3;
import tech.pegasys.teku.infrastructure.ssz.tree.TreeNode;

public class BlockBodyPostShanghaiContainer
    extends Container3<
        BlockBodyPostShanghaiContainer, SszList<SszByteList>, SszByteList, SszList<SszByteList>> {
  public BlockBodyPostShanghaiContainer(
      List<Transaction> transactions, List<BlockHeader> uncles, List<Withdrawal> withdrawals) {
    super(
        BlockBodyPostShanghaiContainerSchema.INSTANCE,
        SszTransactionList.createSszList(transactions),
        SszUnclesByteList.createSszUncles(uncles),
        SszWithdrawalList.createSszList(withdrawals));
  }

  public BlockBodyPostShanghaiContainer(TreeNode backingNode) {
    super(BlockBodyPostShanghaiContainerSchema.INSTANCE, backingNode);
  }

  public List<Transaction> getTransactions() {
    return SszTransactionList.decodeSszList(getField0());
  }

  public List<Bytes> getTransactionsRLP() {
    return getField0().stream().map(SszByteList::getBytes).collect(Collectors.toList());
  }

  public List<BlockHeader> getUncles() {
    return SszUnclesByteList.decodeSszUncles(getField1());
  }

  public Bytes getUnclesRLP() {
    return getField1().getBytes();
  }

  public List<Withdrawal> getWithdrawals() {
    return SszWithdrawalList.decodeSszList(getField2());
  }

  public List<Bytes> getWithdrawalsRLP() {
    return getField2().stream().map(SszByteList::getBytes).collect(Collectors.toList());
  }

  public SszList<SszByteList> getWithdrawalsSsz() {
    return getField0();
  }

  public static BlockBodyPostShanghaiContainer decodeBytes(Bytes bytes) {
    BlockBodyPostShanghaiContainerSchema schema = BlockBodyPostShanghaiContainerSchema.INSTANCE;
    BlockBodyPostShanghaiContainer decodedBytes = schema.sszDeserialize(bytes);
    return decodedBytes;
  }

  public static class BlockBodyPostShanghaiContainerSchema
      extends ContainerSchema3<
          BlockBodyPostShanghaiContainer, SszList<SszByteList>, SszByteList, SszList<SszByteList>> {

    public static final BlockBodyPostShanghaiContainerSchema INSTANCE =
        new BlockBodyPostShanghaiContainerSchema();

    private BlockBodyPostShanghaiContainerSchema() {
      super(
          SszTransactionList.getSchema(),
          SszUnclesByteList.getSchema(),
          SszWithdrawalList.getSchema());
    }

    @Override
    public BlockBodyPostShanghaiContainer createFromBackingNode(TreeNode node) {
      return new BlockBodyPostShanghaiContainer(node);
    }
  }
}
