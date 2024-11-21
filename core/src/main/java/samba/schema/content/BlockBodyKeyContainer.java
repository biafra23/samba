package samba.schema.content;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.plugin.data.BlockBody;
import tech.pegasys.teku.infrastructure.ssz.containers.Container1;
import tech.pegasys.teku.infrastructure.ssz.containers.ContainerSchema1;
import tech.pegasys.teku.infrastructure.ssz.primitive.SszBytes32;
import tech.pegasys.teku.infrastructure.ssz.schema.SszPrimitiveSchemas;
import tech.pegasys.teku.infrastructure.ssz.tree.TreeNode;

public class BlockBodyKeyContainer extends Container1<BlockBodyKeyContainer, SszBytes32> {

    protected BlockBodyKeyContainer(Bytes32 block_hash) {
        super(BlockBodyKeySchema.INSTANCE, SszBytes32.of(block_hash));
    }

    public BlockBodyKeyContainer(TreeNode backingNode) {
        super(BlockBodyKeySchema.INSTANCE, backingNode);
    }

    public Bytes32 getBlockHash() {
        return getField0().get();
    }

    public static BlockBodyKeyContainer decodeBlockBodyKey(Bytes packet ) {
        BlockBodyKeyContainer.BlockBodyKeySchema schema = BlockBodyKeyContainer.BlockBodyKeySchema.INSTANCE;
        return schema.sszDeserialize(packet);
    }

    public static class BlockBodyKeySchema extends ContainerSchema1<BlockBodyKeyContainer, SszBytes32> {

        public static final BlockBodyKeyContainer.BlockBodyKeySchema INSTANCE = new BlockBodyKeyContainer.BlockBodyKeySchema();

        protected BlockBodyKeySchema() {
            super(SszPrimitiveSchemas.BYTES32_SCHEMA);
        }

        @Override
        public BlockBodyKeyContainer createFromBackingNode(TreeNode node) {
            return new BlockBodyKeyContainer(node);
        }
    }
}
