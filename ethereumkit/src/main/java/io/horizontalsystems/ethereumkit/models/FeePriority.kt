package io.horizontalsystems.ethereumkit.models

sealed class FeePriority {
    object Lowest : FeePriority()
    object Low : FeePriority()
    object Medium : FeePriority()
    object High : FeePriority()
    object Highest : FeePriority()
    class Custom(val valueInWei: Long) : FeePriority()
}
