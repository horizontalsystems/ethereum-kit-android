package in3.config;

import in3.utils.JSON;

/**
 * Configuration Object for Incubed Client. It represents the node of a nodelist.
 */
public class NodeConfiguration {
  private String url;
  private Long   props;
  private String address;

  public NodeConfiguration(ChainConfiguration config) {
    config.addNodeConfig(this);
  }

  public String getUrl() {
    return url;
  }

  // TODO Use an actual URL object to enforce semantic validation on java side
  /* the url of the node */
  public void setUrl(String url) {
    this.url = url;
  }

  public long getProps() {
    return props;
  }

  /* used to identify the capabilities of the node */
  public void setProps(long props) {
    this.props = props;
  }

  public String getAddress() {
    return address;
  }

  /* address of the server */
  public void setAddress(String address) {
    this.address = address;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    if (url != null) {
      JSON.appendKey(sb, "url", getUrl());
    }

    if (props != null) {
      JSON.appendKey(sb, "props", getProps());
    }

    if (address != null) {
      JSON.appendKey(sb, "address", getAddress());
    }

    sb.setCharAt(sb.length() - 1, '}');
    return sb.toString();
  }
}
