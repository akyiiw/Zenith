package br.com.zenith.data.models

data class ProgressGoal(
    val title: String,
    val currentValue: Double,
    val targetValue: Double,
    val unit: String,
    val progressPercent: Float,
    val isComplete: Boolean
)
