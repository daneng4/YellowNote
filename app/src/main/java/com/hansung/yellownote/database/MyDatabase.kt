package com.hansung.notedatabase

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hansung.yellownote.database.Converters

@Database(entities = [PenData::class, NoteData::class, FileData::class, ColorData::class, WordAudio::class, Playground::class],
    exportSchema = false, version = 1)
@TypeConverters(Converters::class)
abstract class MyDatabase : RoomDatabase() {
    abstract fun getMyDao() : MyDAO

    companion object {
        private var INSTANCE: MyDatabase? = null
        private val MIGRATION_1_2 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
//                database.execSQL("ALTER TABLE note_data_table ADD COLUMN last_update TEXT")
            }
        }

        fun getDatabase(context: Context) : MyDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(
                    context, MyDatabase::class.java, "yellow_note_database")
                    .addMigrations(MIGRATION_1_2)
                    .allowMainThreadQueries()
                    .addTypeConverter(Converters())
                    .build()
                // for in-memory database
                /*INSTANCE = Room.inMemoryDatabaseBuilder(
                    context, MyDatabase::class.java
                ).build()*/
            }
            return INSTANCE as MyDatabase
        }
    }
}