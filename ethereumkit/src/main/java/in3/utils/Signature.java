package in3.utils;

/*
 * Class that represents a Signature
 */
public class Signature {

  private JSON data;

  private static final String MESSAGE      = "message";
  private static final String MESSAGE_HASH = "messageHash";
  private static final String SIGNATURE    = "signature";
  private static final String R            = "r";
  private static final String S            = "s";
  private static final String V            = "v";

  private Signature(JSON data) {
    this.data = data;
  }

  protected static Signature asSignature(Object o) {
    if (o == null)
      return null;
    return new Signature((JSON) o);
  }

  /*
   * returns the message
   */
  public String getMessage() {
    return data.getString(MESSAGE);
  }
  /*
   * returns the hash of the message
   */
  public String getMessageHash() {
    return data.getString(MESSAGE_HASH);
  }

  /*
   * returns the signature
   */
  public String getSignature() {
    return data.getString(SIGNATURE);
  }

  /*
   * returns R
   */
  public String getR() {
    return data.getString(R);
  }

  /*
   * returns S
   */
  public String getS() {
    return data.getString(S);
  }

  /*
   * returns V
   */
  public long getV() {
    return data.getLong(V);
  }
}
