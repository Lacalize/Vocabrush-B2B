package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Update
import androidx.room.Query

@Entity(tableName = "users")
data class User(
    @PrimaryKey val uid: String = "",
    val email: String = "",
    val name: String = "",
    val passwordHash: String = "",
    val avatarColorHex: String = "#6200EE",
    val authProvider: String = "PASSWORD", // "PASSWORD" or "GOOGLE"
    val registrationDate: Long = System.currentTimeMillis(),
    val vocabGoal: Int = 30, // Default 30 aligned with PWA
    val preferredCategory: String = "technology",
    val vocabLevel: String = "PENDING", // "PENDING", "EASY", "MEDIUM", "HARD"
    val role: String = "", // empty or "teacher" / "admin"
    val classId: String = "",
    val loginCount: Int = 1,
    val totalUsageTimeSeconds: Long = 0
)

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    suspend fun getUserByUid(uid: String): User?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<User>
}
