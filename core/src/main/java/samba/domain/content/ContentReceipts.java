package samba.domain.content;

import samba.schema.content.ssz.receipt.SszReceiptList;

import java.util.List;

import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.ethereum.core.TransactionReceipt;

public class ContentReceipts {

  private final SszReceiptList sszReceiptList;

  public ContentReceipts(SszReceiptList sszReceiptList) {
    this.sszReceiptList = sszReceiptList;
  }

  public ContentReceipts(Bytes sszBytes) {
    this.sszReceiptList = SszReceiptList.decodeBytes(sszBytes);
  }

  public ContentReceipts(List<TransactionReceipt> transactionReceipts) {
    this.sszReceiptList = new SszReceiptList(transactionReceipts);
  }

  public static ContentReceipts decode(Bytes sszReceipts) {
    return new ContentReceipts(sszReceipts);
  }

  public SszReceiptList getSszReceiptList() {
    return sszReceiptList;
  }

  public List<TransactionReceipt> getTransactionReceipts() {
    return sszReceiptList.getDecodedList();
  }

  public List<Bytes> getReceiptsRLP() {
    return sszReceiptList.getReceiptsRLP();
  }

  public Bytes getSszBytes() {
    return sszReceiptList.sszSerialize();
  }
}
