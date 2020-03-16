package in3;

/*
 * Constants for props of an IN3 node.
 */
public final class NodeProps {
  /* filter out nodes which are providing no proof */
  public static final long NODE_PROP_PROOF = 0x1;
  /* filter out nodes other then which have capability of the same RPC endpoint may also accept requests for different chains */
  public static final long NODE_PROP_MULTICHAIN = 0x2;
  /* filter out non-archive supporting nodes */
  public static final long NODE_PROP_ARCHIVE = 0x4;
  /* filter out non-http nodes  */
  public static final long NODE_PROP_HTTP = 0x8;
  /* filter out nodes that don't support binary encoding */
  public static final long NODE_PROP_BINARY = 0x10;
  /* filter out non-onion nodes */
  public static final long NODE_PROP_ONION = 0x20;
  /* filter out nodes that do not provide stats */
  public static final long NODE_PROP_STATS = 0x100;
}
