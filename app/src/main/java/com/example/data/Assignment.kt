package com.example.data

import com.google.firebase.Timestamp

data class Assignment(
    val id: String = "",
    val classId: String = "",
    val title: String = "",
    val content: String = "",
    val targetWordList: List<String> = emptyList(),
    val dueDate: Timestamp? = null,
    val createdAt: Timestamp? = null
)

