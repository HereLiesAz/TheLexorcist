package com.hereliesaz.lexorcist.model

data class Script(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val content: String,
    val rating: Float,
    val ratingsCount: Int,
    val comments: List<Comment>
)

data class Template(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val content: String,
    val rating: Float,
    val ratingsCount: Int,
    val comments: List<Comment>
)

data class Comment(
    val author: String,
    val comment: String
)
