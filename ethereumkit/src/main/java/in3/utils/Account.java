package in3.utils;

/**
 * Pojo that represents the result of an ecrecover operation (see: Crypto class).
 */
public class Account {
  private JSON data;

  private Account(JSON data) {
    this.data = data;
  }

  protected static Account asAccount(Object o) {
    if (o == null)
      return null;
    return new Account((JSON) o);
  }

  /**
     * address from ecrecover operation.
     */
  public String getAddress() {
    return data.getString("address");
  }

  /**
     * public key from ecrecover operation.
     */
  public String getPublicKey() {
    return data.getString("publicKey");
  }
}
