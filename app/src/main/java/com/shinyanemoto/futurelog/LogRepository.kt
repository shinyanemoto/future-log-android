package com.shinyanemoto.futurelog

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "logs")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val timestamp: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val remindAt: Long? = null
)

@Dao
interface LogDao {
    @Query("SELECT * FROM logs ORDER BY timestamp DESC")
    fun observeLogs(): Flow<List<LogEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LogEntry)

    @Query("UPDATE logs SET remindAt = :remindAt WHERE id = :id")
    suspend fun updateReminder(id: Long, remindAt: Long?)
}

@Database(entities = [LogEntry::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao

    companion object {
        private const val DB_NAME = "future_log.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE logs ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE logs ADD COLUMN remindAt INTEGER")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
        }
    }
}

object LogRepository {
    fun logs(context: Context): Flow<List<LogEntry>> =
        AppDatabase.getInstance(context).logDao().observeLogs()

    suspend fun addLog(context: Context, text: String, remindAt: Long?) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val now = System.currentTimeMillis()
        AppDatabase.getInstance(context).logDao().insert(
            LogEntry(
                text = trimmed,
                timestamp = now,
                createdAt = now,
                remindAt = remindAt
            )
        )
    }

    suspend fun updateReminder(context: Context, id: Long, remindAt: Long?) {
        AppDatabase.getInstance(context).logDao().updateReminder(id, remindAt)
    }
}
