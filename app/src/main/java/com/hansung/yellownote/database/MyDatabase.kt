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

        fun getDatabase(context: Context) : MyDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(
                    context, MyDatabase::class.java, "yellow_note_database")
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