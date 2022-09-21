package com.hansung.notedatabase

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface MyDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)  // INSERT, key 충돌이 나면 새 데이터로 교체
    suspend fun insertPenData(pen: PenData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteData(note: NoteData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColorData(color: ColorData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWordAudio(wordAudio:WordAudio)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayground(playground: Playground)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFileData(file:FileData)

    @Query("SELECT * FROM pen_data_table")
    fun getAllPenData(): List<PenData>

    @Query("SELECT * FROM pen_data_table WHERE isActive=1")
    fun getActivePenData(): List<PenData>        // LiveData<> 사용

    @Query("SELECT count(*) FROM pen_data_table")
    fun getPenDataCount() : Int

    @Query("SELECT * FROM color_table")
    fun getColorData() : List<ColorData>

    @Query("SELECT count(*) FROM color_table")
    fun getColorDataCount() : Int

    @Query("SELECT * FROM note_data_table")
    fun getAllNoteData(): List<NoteData>

    @Query("SELECT * FROM file_data_table WHERE fileName=:sfile")
    fun getFileDataByFileName(sfile:String):List<FileData>


    @Query("UPDATE pen_data_table SET width = :width, color= :color ,isActive= :isActive WHERE PenMode =:mode")
    fun updatePenData(mode:String, width: Float?, color: Int?, isActive: Boolean?)

    @Query("UPDATE color_table SET color= :color WHERE ButtonName =:buttonName")
    fun updateColorData(buttonName:String, color: Int)

    @Delete
    suspend fun deleteNoteData(noteData: NoteData) // primary key is used to find the student


//    @Transaction
//    @Query("SELECT * FROM student_table WHERE student_id = :id")
//    suspend fun getStudentsWithEnrollment(id: Int): List<StudentWithEnrollments>
//
//    @Query("SELECT * FROM class_table WHERE id = :id")
//    suspend fun getClassInfo(id: Int): List<ClassInfo>
}