package in3;

import in3.utils.JSON;

/*
 * Class that represents a Block
 */
public class BlockID {
  private Long   number;
  private String hash;

  private BlockID() {}

  /*
   * creates an instance from a hashString
   */
  public static BlockID fromHash(String hash) {
    BlockID blockId = new BlockID();
    blockId.setHash(hash);

    return blockId;
  }

  /*
   * creates an instance from a block number.
   */
  public static BlockID fromNumber(long number) {
    BlockID blockId = new BlockID();
    blockId.setNumber(number);

    return blockId;
  }

  /*
   * returns the number of the BlockId
   */
  public Long getNumber() {
    return number;
  }

  /*
   * sets the number of the BlockId
   */
  public void setNumber(long block) {
    this.number = block;
  }

  /*
   * returns the hash of the BlockId
   */
  public String getHash() {
    return hash;
  }

  /*
   * sets the hash of the BlockId
   */
  public void setHash(String hash) {
    this.hash = hash;
  }

  /*
   * returns the json representation of the BlockID
   */
  public String toJSON() {
    return JSON.toJson(this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");

    if (getNumber() != null) {
      JSON.appendKey(sb, "blockNumber", getNumber());
    }

    if (getHash() != null) {
      JSON.appendKey(sb, "hash", getHash());
    }

    sb.setCharAt(sb.length() - 1, '}');

    return sb.toString();
  }
}
