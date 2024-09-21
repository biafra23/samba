package samba.domain.messages.response;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.ssz.SSZ;
import org.apache.tuweni.units.bigints.UInt64;
import samba.domain.messages.HistoryProtocolReceiveMessage;
import samba.domain.messages.MessageType;
import samba.domain.node.NodeId;


import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;


/**
 * Response message to Ping(0x00)
 */
public class Pong implements HistoryProtocolReceiveMessage {

    private final UInt64 enrSeq;
    private final Bytes customPayload;

    public Pong(UInt64 enrSeq, Bytes customPayload) {
        checkArgument(enrSeq != null && UInt64.ZERO.compareTo(enrSeq) < 0, "enrSeq cannot be null or negative");
        //  checkArgument(customPayload.size() <= MAX_CUSTOM_PAYLOAD_SIZE, "Custom payload size exceeds limit");

        this.enrSeq = enrSeq;
        this.customPayload = customPayload;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.PONG;
    }

    public Bytes getCustomPayload() {
        return customPayload;
    }

    public Optional<UInt64> getEnrSeq() {
        return Optional.ofNullable(enrSeq);
    }

    @Override
    public Bytes serialize() {
        Bytes enrSeqSerialized = SSZ.encodeUInt64(enrSeq.toLong());
        Bytes customPayloadSerialized = SSZ.encodeBytes(customPayload);
        return Bytes.concatenate(
                SSZ.encodeUInt8(getMessageType().ordinal()),
                enrSeqSerialized,
                customPayloadSerialized);
    }

    @Override
    public MessageType getType() {
        return null;
    }

    @Override
    public Pong getDeserilizedMessage() {
        return this;
    }

    public NodeId getNodeId() {
           return new NodeId() {
               @Override
               public Bytes toBytes() {
                   return  Pong.this.enrSeq.toBytes();
               }
           };
    }
}