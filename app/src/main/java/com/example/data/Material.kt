package com.example.data

import com.google.firebase.Timestamp

// classes/{classId}/materials/{materialId} - teacher-uploaded reading library, parallel to
// Assignment but with no due date and no target-word highlighting (free reading).
data class Material(
    val id: String = "",
    val classId: String = "",
    val title: String = "",
    val content: String = "",
    val unit: String = "",
    val createdAt: Timestamp? = null
)
