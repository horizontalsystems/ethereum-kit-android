package io.horizontalsystems.ethereumkit.sample

sealed class FeePriority {
    object Lowest : FeePriority()
    object Low : FeePriority()
    object Medium : FeePriority()
    object High : FeePriority()
    object Highest : FeePriority()
    class Custom(val valueInWei: Long) : FeePriority()
}
