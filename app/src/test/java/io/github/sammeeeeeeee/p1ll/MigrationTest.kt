package io.github.sammeeeeeeee.p1ll

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import io.github.sammeeeeeeee.p1ll.data.DoseUnit
import io.github.sammeeeeeeee.p1ll.data.MIGRATION_1_2
import io.github.sammeeeeeeee.p1ll.data.P1llDb
import io.github.sammeeeeeeee.p1ll.data.Pill
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class MigrationTest {
    @Test
    fun version1PillsKeepTheirHistoryAndCountInPills() = runBlocking {
        val app = RuntimeEnvironment.getApplication()
        val file = app.getDatabasePath("migration.db").apply { parentFile?.mkdirs(); delete() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL(
                "CREATE TABLE `pills` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `dosage` TEXT NOT NULL, " +
                    "`color` INTEGER NOT NULL, `shape` TEXT NOT NULL, `schedule` TEXT NOT NULL, `weekdays` INTEGER NOT NULL, " +
                    "`intervalDays` INTEGER NOT NULL, `times` TEXT NOT NULL, `startDay` INTEGER NOT NULL, `endDay` INTEGER, " +
                    "`optional` INTEGER NOT NULL, `notes` TEXT NOT NULL, `stock` INTEGER, `perDose` INTEGER NOT NULL, " +
                    "`refillAt` INTEGER, `createdAt` INTEGER NOT NULL)",
            )
            db.execSQL(
                "CREATE TABLE `doses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `pillId` INTEGER NOT NULL, `day` INTEGER NOT NULL, " +
                    "`minute` INTEGER NOT NULL, `status` TEXT NOT NULL, `at` INTEGER NOT NULL, FOREIGN KEY(`pillId`) REFERENCES `pills`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE INDEX `index_doses_pillId_day` ON `doses` (`pillId`, `day`)")
            db.execSQL(
                "INSERT INTO pills VALUES (7, 'Metformin', '500 mg', 4, 'CAPSULE', 'DAILY', 127, 2, '480,1140', 100, NULL, 0, 'with food', 20, 2, 5, 0)",
            )
            db.execSQL("INSERT INTO doses (pillId, day, minute, status, at) VALUES (7, 120, 480, 'TAKEN', 0)")
            db.version = 1
        }

        val room = Room.databaseBuilder(app, P1llDb::class.java, "migration.db").addMigrations(MIGRATION_1_2).build()
        val pill = room.pills().get(7)!!
        assertEquals("500 mg", pill.strength)
        assertEquals(2.0, pill.amount, 0.0)
        assertEquals(DoseUnit.COUNT, pill.unit)
        assertEquals(20.0, pill.stock!!, 0.0)
        assertEquals("with food", pill.notes)
        assertEquals(listOf(480, 1140), pill.times)
        // Rebuilding the table must not cascade into the dose history.
        assertEquals(1, room.doses().forPillDay(7, 120).size)
        // Ids carry on after the old ones.
        assertEquals(8L, room.pills().insert(Pill(name = "New", startDay = 0, createdAt = 0)))
        // Foreign keys are live once open, so the survival above is a real check.
        room.pills().delete(pill)
        assertEquals(0, room.doses().forPillDay(7, 120).size)
        room.close()
    }
}
