package samba.schema.content;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.infrastructure.ssz.containers.Container1;
import tech.pegasys.teku.infrastructure.ssz.containers.ContainerSchema1;
import tech.pegasys.teku.infrastructure.ssz.primitive.SszBytes32;
import tech.pegasys.teku.infrastructure.ssz.primitive.SszUInt64;
import tech.pegasys.teku.infrastructure.ssz.schema.SszPrimitiveSchemas;
import tech.pegasys.teku.infrastructure.ssz.tree.TreeNode;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;

public class BlockNumberKeyContainer extends Container1<BlockNumberKeyContainer, SszUInt64> {

    protected BlockNumberKeyContainer(UInt64 block_number) {
        super(BlockNumberKeySchema.INSTANCE, SszUInt64.of(block_number));
    }

    public BlockNumberKeyContainer(TreeNode backingNode) {
        super(BlockNumberKeySchema.INSTANCE, backingNode);
    }

    public UInt64 getBlockNumber() {
        return getField0().get();
    }

    public static BlockNumberKeyContainer decodeBlockNumberKey(Bytes packet) {
        BlockNumberKeyContainer.BlockNumberKeySchema schema = BlockNumberKeyContainer.BlockNumberKeySchema.INSTANCE;
        return schema.sszDeserialize(packet);
    }

    public static class BlockNumberKeySchema extends ContainerSchema1<BlockNumberKeyContainer, SszUInt64> {

        public static final BlockNumberKeyContainer.BlockNumberKeySchema INSTANCE = new BlockNumberKeyContainer.BlockNumberKeySchema();

        protected BlockNumberKeySchema() {
            super(SszPrimitiveSchemas.UINT64_SCHEMA);
        }

        @Override
        public BlockNumberKeyContainer createFromBackingNode(TreeNode node) {
            return new BlockNumberKeyContainer(node);
        }
    }
}
