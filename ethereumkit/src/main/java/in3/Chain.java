package in3;

/**
 * Constants for Chain-specs
 */
public final class Chain {
  /**
     * support for multiple chains, a client can then switch between different chains (but consumes more memory)
     */
  public static final long MULTICHAIN = 0x0;

  /**
     * use mainnet
     */
  public static final long MAINNET = 0x01;
  /**
     * use kovan testnet
     */
  public static final long KOVAN = 0x2a;
  /**
     * use tobalaba testnet
     */
  public static final long TOBALABA = 0x44d;
  /**
     * use goerli testnet
     */
  public static final long GOERLI = 0x5;
  /**
     * use evan testnet
     */
  public static final long EVAN = 0x4b1;
  /**
     * use ipfs
     */
  public static final long IPFS = 0x7d0;
  /**
     * use volta test net
     */
  public static final long VOLTA = 0x12046;
  /**
     * use local client 
     */
  public static final long LOCAL = 0xFFFF;
}
