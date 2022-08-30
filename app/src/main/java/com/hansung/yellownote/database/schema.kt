package com.hansung.notedatabase

import androidx.room.*


@Entity(tableName = "note_data_table")    // 테이블 이름을 student_table로 지정함
data class NoteData (
    @PrimaryKey @ColumnInfo(name="NoteName")val noteName: String,
    val lastPageNo:Int,
    val recordFileLocation:String
)

@Entity(tableName = "file_data_table",
    primaryKeys = ["fileName"],
    foreignKeys = [ForeignKey(entity = NoteData::class, parentColumns = ["NoteName"], childColumns = ["fileName"])
    ]
)
data class FileData (
    val fileName:String,
    val pageNo:Int,
    val drawingInfo:String
)

@Entity(tableName = "word_audio_table")
data class WordAudio (
    @PrimaryKey @ColumnInfo(name="WordName")val wordName:String,
    val audioLocation:String
)

@Entity(tableName = "playground_table")
data class Playground (
    @PrimaryKey @ColumnInfo(name="ImageName")val imageName:String,
    val imageLocation:String
)