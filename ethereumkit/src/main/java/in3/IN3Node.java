package in3;

/*
 * Class that represents an IN3 node.
 */
import in3.utils.JSON;

public class IN3Node {
  private JSON data;

  private IN3Node(JSON data) {
    this.data = data;
  }

  protected static IN3Node[] asIN3Nodes(Object o) {
    if (o == null)
      return null;
    if (o instanceof Object[]) {
      Object[] a  = (Object[]) o;
      IN3Node[] s = new IN3Node[a.length];
      for (int i = 0; i < s.length; i++)
        s[i] = a[i] == null ? null : new IN3Node((JSON) a[i]);
      return s;
    }
    return null;
  }

  protected static IN3Node asNode(Object o) {
    if (o == null)
      return null;
    return new IN3Node((JSON) o);
  }

  /*
   * returns the url of the node
   */
  public String getUrl() {
    return JSON.asString(data.get("url"));
  }

  /*
   * returns the address of the node
   */
  public String getAddress() {
    return JSON.asString(data.get("address"));
  }

  /*
   * returns the index of the node
   */
  public int getIndex() {
    return JSON.asInt(data.get("index"));
  }

  /*
   * returns the deposit for the the node
   */
  public String getDeposit() {
    return JSON.asString(data.get("deposit"));
  }

  /*
   * returns the props of the the node
   */
  public long getProps() {
    return JSON.asLong(data.get("props"));
  }

  /*
   * returns the timeout for the the node
   */
  public int getTimeout() {
    return JSON.asInt(data.get("timeout"));
  }

  /*
   * returns the register time of the the node
   */
  public int getRegisterTime() {
    return JSON.asInt(data.get("registerTime"));
  }

  /*
   * returns the weight of the node
   */
  public int getWeight() {
    return JSON.asInt(data.get("weight"));
  }
}
