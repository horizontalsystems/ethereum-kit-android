package io.horizontalsystems.ethereumkit.models.etherscan

data class EtherscanResponse(
  val status: String,
  val message: String,
  val result: List<EtherscanTransaction>
)
