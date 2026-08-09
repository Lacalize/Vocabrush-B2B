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
    @PrimaryKey val email: String,
    val name: String,
    val passwordHash: String = "", // Used for customize native accounts, empty for Google
    val avatarColorHex: String = "#6200EE", // Custom profile colors setup
    val authProvider: String = "CUSTOM", // "CUSTOM" or "GOOGLE"
    val registrationDate: Long = System.currentTimeMillis(),
    val vocabGoal: Int = 10, // Daily standard objective/goal
    val preferredCategory: String = "technology", // Selected learning focus
    val vocabLevel: String = "PENDING", // "PENDING", "EASY", "MEDIUM", "HARD"
    val loginCount: Int = 1,
    val totalUsageTimeSeconds: Long = 0
)

@Dao
interface UserDao {
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
