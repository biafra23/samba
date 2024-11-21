package samba.schema.content;

import tech.pegasys.teku.infrastructure.ssz.containers.Container1;
import tech.pegasys.teku.infrastructure.ssz.containers.ContainerSchema1;
import tech.pegasys.teku.infrastructure.ssz.primitive.SszBytes32;

public class BlockHeaderContainer extends Container1<BlockHeaderContainer, SszBytes32> {

    protected BlockHeaderContainer(ContainerSchema1<BlockHeaderContainer, SszBytes32> schema) {
        super(schema);
    }
}
