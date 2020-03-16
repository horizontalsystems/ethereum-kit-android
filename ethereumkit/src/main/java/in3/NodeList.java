package in3;

import in3.utils.JSON;

/*
 * Class that represents an aggregation of IN3 nodes.
 */
class NodeList {
  private JSON data;

  private NodeList(JSON data) {
    this.data = data;
  }

  protected static NodeList[] asNodeLists(Object o) {
    if (o == null)
      return null;
    if (o instanceof Object[]) {
      Object[] a   = (Object[]) o;
      NodeList[] s = new NodeList[a.length];
      for (int i = 0; i < s.length; i++)
        s[i] = a[i] == null ? null : new NodeList((JSON) a[i]);
      return s;
    }
    return null;
  }

  protected static NodeList asNodeList(Object o) {
    if (o == null)
      return null;
    return new NodeList((JSON) o);
  }

  /** returns an array of IN3Node */
  public IN3Node[] getNodes() {
    Object obj = data.get("nodes");
    if (obj != null) {
      return IN3Node.asIN3Nodes(obj);
    } else {
      return new IN3Node[] {};
    }
  }
}
