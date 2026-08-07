package com.example.filamentdemo.model

enum class Complexity {
    Beginner,
    Intermediate,
    Advanced
}

data class SampleItem(
    val id: String,
    val title: String,
    val description: String,
    val complexity: Complexity
)
