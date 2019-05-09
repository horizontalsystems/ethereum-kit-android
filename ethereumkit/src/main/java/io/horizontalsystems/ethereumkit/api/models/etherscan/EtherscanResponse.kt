package io.horizontalsystems.ethereumkit.api.models.etherscan

data class EtherscanResponse(
  val status: String,
  val message: String,
  val result: List<EtherscanTransaction>
)
