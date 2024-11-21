package samba.schema.content;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import samba.schema.messages.ssz.containers.PingContainer;
import tech.pegasys.teku.infrastructure.ssz.containers.Container1;
import tech.pegasys.teku.infrastructure.ssz.containers.ContainerSchema1;
import tech.pegasys.teku.infrastructure.ssz.primitive.SszBytes32;
import tech.pegasys.teku.infrastructure.ssz.schema.SszPrimitiveSchemas;
import tech.pegasys.teku.infrastructure.ssz.tree.TreeNode;

public class BlockHeaderKeyContainer extends Container1<BlockHeaderKeyContainer, SszBytes32> {

    protected BlockHeaderKeyContainer(Bytes32 block_hash) {
        super(BlockHeaderKeySchema.INSTANCE, SszBytes32.of(block_hash));
    }

    public BlockHeaderKeyContainer(TreeNode backingNode) {
        super(BlockHeaderKeySchema.INSTANCE, backingNode);
    }

    public Bytes32 getBlockHash() {
        return getField0().get();
    }

    public static BlockHeaderKeyContainer  decodeBlockHeaderKey(Bytes packet ) {
        BlockHeaderKeyContainer.BlockHeaderKeySchema schema = BlockHeaderKeyContainer.BlockHeaderKeySchema.INSTANCE;
        return schema.sszDeserialize(packet);
    }

    public static class BlockHeaderKeySchema extends ContainerSchema1<BlockHeaderKeyContainer, SszBytes32> {

        public static final BlockHeaderKeyContainer.BlockHeaderKeySchema INSTANCE = new BlockHeaderKeyContainer.BlockHeaderKeySchema();

        protected BlockHeaderKeySchema() {
            super(SszPrimitiveSchemas.BYTES32_SCHEMA);
        }

        @Override
        public BlockHeaderKeyContainer createFromBackingNode(TreeNode node) {
            return new BlockHeaderKeyContainer(node);
        }
    }
}
