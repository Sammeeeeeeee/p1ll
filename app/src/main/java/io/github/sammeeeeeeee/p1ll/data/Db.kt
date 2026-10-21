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
    suspend fun setStock(id: Long, stock: Int?)
}

@Dao
interface DoseDao {
    @Query("SELECT * FROM doses ORDER BY day, minute")
    fun observeAll(): Flow<List<Dose>>

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

@Database(entities = [Pill::class, Dose::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class P1llDb : RoomDatabase() {
    abstract fun pills(): PillDao
    abstract fun doses(): DoseDao

    companion object {
        fun create(context: Context) =
            Room.databaseBuilder(context, P1llDb::class.java, "p1ll.db").build()
    }
}
