package io.github.sammeeeeeeee.p1ll.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface PillDao {
    @Query("SELECT * FROM pills ORDER BY createdAt")
    fun observeAll(): Flow<List<Pill>>

    @Query("SELECT * FROM pills ORDER BY createdAt")
    suspend fun all(): List<Pill>

    @Query("SELECT * FROM pills WHERE id = :id")
    suspend fun get(id: Long): Pill?

    @Insert
    suspend fun insert(pill: Pill): Long

    @Update
    suspend fun update(pill: Pill)

    @Delete
    suspend fun delete(pill: Pill)

    @Query("UPDATE pills SET stock = :stock WHERE id = :id")
    suspend fun setStock(id: Long, stock: Double?)
}

@Dao
interface DoseDao {
    @Query("SELECT * FROM doses ORDER BY day, minute")
    fun observeAll(): Flow<List<Dose>>

    @Query("SELECT * FROM doses ORDER BY day, minute")
    suspend fun all(): List<Dose>

    @Query("SELECT * FROM doses WHERE day BETWEEN :from AND :to")
    suspend fun between(from: Long, to: Long): List<Dose>

    @Query("SELECT * FROM doses WHERE pillId = :pillId AND day = :day")
    suspend fun forPillDay(pillId: Long, day: Long): List<Dose>

    @Query("SELECT * FROM doses WHERE id = :id")
    suspend fun get(id: Long): Dose?

    @Insert
    suspend fun insert(dose: Dose): Long

    @Insert
    suspend fun insertAll(doses: List<Dose>)

    @Update
    suspend fun update(dose: Dose)

    @Query("DELETE FROM doses WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(entities = [Pill::class, Dose::class], version = 2, exportSchema = true)
@TypeConverters(Converters::class)
abstract class P1llDb : RoomDatabase() {
    abstract fun pills(): PillDao
    abstract fun doses(): DoseDao

    companion object {
        fun create(context: Context) =
            Room.databaseBuilder(context, P1llDb::class.java, "p1ll.db").addMigrations(MIGRATION_1_2).build()
    }
}

/**
 * The free-text dose becomes the strength; pills per dose becomes a counted amount; supply
 * goes fractional so half pills and 2.5 ml doses count down properly. SQLite can't change a
 * column's type in place, so the table is rebuilt. Room runs migrations before it turns foreign
 * keys on, so dropping the old table leaves the doses alone.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE `pills_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                "`strength` TEXT NOT NULL, `color` INTEGER NOT NULL, `shape` TEXT NOT NULL, `schedule` TEXT NOT NULL, " +
                "`weekdays` INTEGER NOT NULL, `intervalDays` INTEGER NOT NULL, `times` TEXT NOT NULL, `startDay` INTEGER NOT NULL, " +
                "`endDay` INTEGER, `optional` INTEGER NOT NULL, `notes` TEXT NOT NULL, `stock` REAL, `amount` REAL NOT NULL, " +
                "`unit` TEXT NOT NULL, `refillAt` INTEGER, `createdAt` INTEGER NOT NULL)",
        )
        db.execSQL(
            "INSERT INTO `pills_new` (id, name, strength, color, shape, schedule, weekdays, intervalDays, times, startDay, " +
                "endDay, optional, notes, stock, amount, unit, refillAt, createdAt) " +
                "SELECT id, name, dosage, color, shape, schedule, weekdays, intervalDays, times, startDay, " +
                "endDay, optional, notes, stock, perDose, 'COUNT', refillAt, createdAt FROM `pills`",
        )
        db.execSQL("DROP TABLE `pills`")
        db.execSQL("ALTER TABLE `pills_new` RENAME TO `pills`")
    }
}
