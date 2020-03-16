package in3;

import in3.utils.JSON;

public class SignedBlockHash {
  private JSON data;

  private SignedBlockHash(JSON data) {
    this.data = data;
  }

  protected static SignedBlockHash[] asSignedBlockHashs(Object o) {
    if (o == null)
      return null;
    if (o instanceof Object[]) {
      Object[] a          = (Object[]) o;
      SignedBlockHash[] s = new SignedBlockHash[a.length];
      for (int i = 0; i < s.length; i++)
        s[i] = a[i] == null ? null : new SignedBlockHash((JSON) a[i]);
      return s;
    }
    return null;
  }

  protected SignedBlockHash asSignedBlockHash(Object o) {
    if (o == null)
      return null;
    return new SignedBlockHash((JSON) o);
  }

  public String getBlockHash() {
    return data.getString("blockHash");
  }

  public long getBlock() {
    return data.getLong("block");
  }

  public String getR() {
    return data.getString("r");
  }

  public String getS() {
    return data.getString("s");
  }

  public long getV() {
    return data.getLong("v");
  }

  public String getMsgHash() {
    return data.getString("msgHash");
  }
}
