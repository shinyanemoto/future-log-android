package com.shinyanemoto.futurelog

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray

private val Context.legacyLogDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "future_log_entries"
)

private val legacyLogEntriesKey = stringPreferencesKey("log_entries_json")

@Entity(tableName = "logs")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val timestamp: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val remindAt: Long? = null,
    val memo: String = ""
)

@Dao
interface LogDao {
    @Query("SELECT * FROM logs ORDER BY timestamp DESC")
    fun observeLogs(): Flow<List<LogEntry>>

    @Query("SELECT * FROM logs")
    suspend fun getAll(): List<LogEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LogEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<LogEntry>)

    @Query("UPDATE logs SET remindAt = :remindAt WHERE id = :id")
    suspend fun updateReminder(id: Long, remindAt: Long?)

    @Query("UPDATE logs SET memo = :memo WHERE id = :id")
    suspend fun updateMemo(id: Long, memo: String)
}

@Database(entities = [LogEntry::class], version = 4, exportSchema = false)
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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE logs ADD COLUMN memo TEXT NOT NULL DEFAULT ''")
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
        }
    }
}

object LogRepository {
    private val legacyMigrationMutex = Mutex()

    fun logs(context: Context): Flow<List<LogEntry>> = flow {
        migrateLegacyLogsIfNeeded(context)
        emitAll(AppDatabase.getInstance(context).logDao().observeLogs())
    }

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

    suspend fun updateMemo(context: Context, id: Long, memo: String) {
        AppDatabase.getInstance(context).logDao().updateMemo(id, memo.trim())
    }

    private suspend fun migrateLegacyLogsIfNeeded(context: Context) {
        legacyMigrationMutex.withLock {
            val legacyJson = runCatching {
                context.legacyLogDataStore.data.first()[legacyLogEntriesKey].orEmpty()
            }.getOrDefault("")
            val legacyEntries = parseLegacyEntries(legacyJson)
            if (legacyEntries.isEmpty()) return

            val dao = AppDatabase.getInstance(context).logDao()
            val existingKeys = dao.getAll()
                .asSequence()
                .map { LegacyLogKey(text = it.text, timestamp = it.timestamp) }
                .toHashSet()
            val missingEntries = legacyEntries.filterNot { entry ->
                LegacyLogKey(text = entry.text, timestamp = entry.timestamp) in existingKeys
            }

            if (missingEntries.isNotEmpty()) {
                dao.insertAll(missingEntries)
            }

            context.legacyLogDataStore.edit { preferences ->
                preferences.remove(legacyLogEntriesKey)
            }
        }
    }

    private fun parseLegacyEntries(json: String): List<LogEntry> {
        if (json.isBlank()) return emptyList()

        return runCatching {
            val array = JSONArray(json)
            List(array.length()) { index ->
                val obj = array.getJSONObject(index)
                val timestamp = obj.getLong("timestamp")
                LogEntry(
                    text = obj.getString("text"),
                    timestamp = timestamp,
                    createdAt = timestamp
                )
            }
        }.getOrDefault(emptyList())
    }
}

private data class LegacyLogKey(
    val text: String,
    val timestamp: Long
)
