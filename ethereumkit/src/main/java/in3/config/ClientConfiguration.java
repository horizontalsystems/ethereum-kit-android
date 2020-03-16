package in3.config;

import in3.Proof;
import in3.utils.JSON;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Configuration Object for Incubed Client. It holds the state for the root 
 * of the configuration tree. Should be retrieved from the client instance as IN3#getConfig()
 */
public class ClientConfiguration implements Configuration {

  // Based on core/client/client_init.c
  private Integer requestCount;
  private Boolean autoUpdateList;
  private Proof   proof = Proof.standard;
  private Integer maxAttempts;
  private Integer signatureCount;
  private Integer finality;
  private Boolean includeCode;
  private Boolean keepIn3;
  private Boolean useBinary;
  private Boolean useHttp;
  private Long    maxCodeCache;
  private Long    timeout;
  private Long    minDeposit;
  private Long    nodeProps;
  private Long    nodeLimit;
  private Integer replaceLatestBlock;
  private String  rpc;
  private Long    maxBlockCache;
  private Boolean stats;

  private String serialzedState;

  private HashMap<Long, ChainConfiguration> chainsConfig = new HashMap<Long, ChainConfiguration>();

  // Make the constructor private in order to ensure people use client.getConfig()
  private ClientConfiguration() {}

  public Integer getRequestCount() {
    return requestCount;
  }

  /** sets the number of requests send when getting a first answer */
  public void setRequestCount(int requestCount) {
    this.requestCount = requestCount;
  }

  public Boolean isAutoUpdateList() {
    return autoUpdateList;
  }

  /**
     * activates the auto update.if true the nodelist will be automaticly updated if
     * the lastBlock is newer
     */
  public void setAutoUpdateList(boolean autoUpdateList) {
    this.autoUpdateList = autoUpdateList;
  }

  public Proof getProof() {
    return proof;
  }

  /** sets the type of proof used */
  public void setProof(Proof proof) {
    this.proof = proof;
  }

  public Integer getMaxAttempts() {
    return maxAttempts;
  }

  /** sets the max number of attempts before giving up */
  public void setMaxAttempts(int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  public Integer getSignatureCount() {
    return signatureCount;
  }

  /** sets the number of signatures used to proof the blockhash. */
  public void setSignatureCount(int signatureCount) {
    this.signatureCount = signatureCount;
  }

  public Boolean isStats() {
    return stats;
  }

  /** if true (default) the request will be counted as part of the regular stats, if not they are not shown as part of the dashboard. */
  public void setStats(boolean stats) {
    this.stats = stats;
  }

  public Integer getFinality() {
    return finality;
  }

  /** sets the number of signatures in percent required for the request */
  public void setFinality(int finality) {
    this.finality = finality;
  }

  public Boolean isIncludeCode() {
    return includeCode;
  }

  /* TODO ask what is this to Simon */
  public void setIncludeCode(boolean includeCode) {
    this.includeCode = includeCode;
  }

  public Boolean isKeepIn3() {
    return keepIn3;
  }

  /* preserve in3 section of the rpc call response intact */
  public void setKeepIn3(boolean keepIn3) {
    this.keepIn3 = keepIn3;
  }

  public Boolean isUseBinary() {
    return useBinary;
  }

  /* use binary payload instead of json */
  public void setUseBinary(boolean useBinary) {
    this.useBinary = useBinary;
  }

  public Boolean isUseHttp() {
    return useHttp;
  }

  /* allow transport to use non-ssl */
  public void setUseHttp(boolean useHttp) {
    this.useHttp = useHttp;
  }

  public Long getMaxCodeCache() {
    return maxCodeCache;
  }

  /** sets number of max bytes used to cache the code in memory */
  public void setMaxCodeCache(long maxCodeCache) {
    this.maxCodeCache = maxCodeCache;
  }

  public Long getTimeout() {
    return timeout;
  }

  /**
     * specifies the number of milliseconds before the request times out. increasing
     * may be helpful if the device uses a slow connection.
     */
  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  public Long getMinDeposit() {
    return minDeposit;
  }

  /**
     * sets min stake of the server. Only nodes owning at least this amount will be
     * chosen.
     */
  public void setMinDeposit(long minDeposit) {
    this.minDeposit = minDeposit;
  }

  public Long getNodeProps() {
    return nodeProps;
  }

  /* used to filter nodes according to its capabilities */
  public void setNodeProps(long nodeProps) {
    this.nodeProps = nodeProps;
  }

  public Long getNodeLimit() {
    return nodeLimit;
  }

  /** sets the limit of nodes to store in the client. */
  public void setNodeLimit(long nodeLimit) {
    this.nodeLimit = nodeLimit;
  }

  public Integer getReplaceLatestBlock() {
    return replaceLatestBlock;
  }

  /** replaces the *latest* with blockNumber- specified value */
  public void setReplaceLatestBlock(int replaceLatestBlock) {
    this.replaceLatestBlock = replaceLatestBlock;
  }

  public String getRpc() {
    return rpc;
  }

  /** setup an custom rpc source for requests by setting Chain to local and proof to none */
  public void setRpc(String rpc) {
    this.rpc = rpc;
  }

  public Long getMaxBlockCache() {
    return maxBlockCache;
  }

  /** sets the number of blocks cached in memory */
  public void setMaxBlockCache(long maxBlockCache) {
    this.maxBlockCache = maxBlockCache;
  }

  public HashMap<Long, ChainConfiguration> getNodesConfig() {
    return chainsConfig;
  }

  public void setChainsConfig(HashMap<Long, ChainConfiguration> chainsConfig) {
    this.chainsConfig = chainsConfig;
  }

  protected void addChainConfiguration(ChainConfiguration configuration) {
    /*
         * This is stored in a HashMap to ensure uniqueness between chains without changing NodeConfiguration equals or toHash methods
         */
    chainsConfig.put(configuration.getChain(), configuration);
  }

  public void markAsSynced() {
    serialzedState = toJSON();
  }

  public boolean isSynced() {
    return toJSON().equals(serialzedState);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");

    if (getRequestCount() != null) {
      JSON.appendKey(sb, "requestCount", getRequestCount());
    }
    if (isAutoUpdateList() != null) {
      JSON.appendKey(sb, "autoUpdateList", isAutoUpdateList());
    }
    if (getProof() != null) {
      JSON.appendKey(sb, "proof", JSON.asString(getProof()));
    }
    if (getMaxAttempts() != null) {
      JSON.appendKey(sb, "maxAttempts", getMaxAttempts());
    }
    if (getSignatureCount() != null) {
      JSON.appendKey(sb, "signatureCount", getSignatureCount());
    }
    if (getFinality() != null) {
      JSON.appendKey(sb, "finality", getFinality());
    }
    if (isIncludeCode() != null) {
      JSON.appendKey(sb, "includeCode", isIncludeCode());
    }
    if (isKeepIn3() != null) {
      JSON.appendKey(sb, "keepIn3", isKeepIn3());
    }
    if (isStats() != null) {
      JSON.appendKey(sb, "stats", isStats());
    }
    if (isUseBinary() != null) {
      JSON.appendKey(sb, "useBinary", isUseBinary());
    }
    if (isUseHttp() != null) {
      JSON.appendKey(sb, "useHttp", isUseHttp());
    }
    if (getMaxCodeCache() != null) {
      JSON.appendKey(sb, "maxCodeCache", getMaxCodeCache());
    }
    if (getTimeout() != null) {
      JSON.appendKey(sb, "timeout", getTimeout());
    }
    if (getMinDeposit() != null) {
      JSON.appendKey(sb, "minDeposit", getMinDeposit());
    }
    if (getNodeProps() != null) {
      JSON.appendKey(sb, "nodeProps", getNodeProps());
    }
    if (getNodeLimit() != null) {
      JSON.appendKey(sb, "nodeLimit", getNodeLimit());
    }
    if (getReplaceLatestBlock() != null) {
      JSON.appendKey(sb, "replaceLatestBlock", getReplaceLatestBlock());
    }
    if (getRpc() != null) {
      JSON.appendKey(sb, "rpc", getRpc());
    }
    if (getMaxBlockCache() != null) {
      JSON.appendKey(sb, "maxBlockCache", getMaxBlockCache());
    }

    if (!chainsConfig.isEmpty()) {
      StringBuilder sb2 = new StringBuilder("{");
      for (ChainConfiguration chainConfig : chainsConfig.values()) {
        JSON.appendKey(sb2, JSON.asString(chainConfig.getChain()), chainConfig);
      }

      sb2.setCharAt(sb2.length() - 1, '}');
      JSON.appendKey(sb, "nodes", sb2);
    }

    sb.setCharAt(sb.length() - 1, '}');
    return sb.toString();
  }

  @Override
  public String toJSON() {
    return JSON.toJson(this);
  }
}
