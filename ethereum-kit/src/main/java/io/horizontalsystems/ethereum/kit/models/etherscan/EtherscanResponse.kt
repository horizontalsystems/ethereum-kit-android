package io.horizontalsystems.ethereum.kit.models.etherscan

data class EtherscanResponse(
  val status: String,
  val message: String,
  val result: List<EtherscanTransaction>
)
