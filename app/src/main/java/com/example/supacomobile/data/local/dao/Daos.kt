package com.example.supacomobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.supacomobile.data.local.entity.BoletimEntity
import com.example.supacomobile.data.local.entity.HorarioEntity
import com.example.supacomobile.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile LIMIT 1")
    suspend fun getProfile(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)
    
    @Query("DELETE FROM profile")
    suspend fun clear()
}

@Dao
interface BoletimDao {
    @Query("SELECT * FROM boletim")
    suspend fun getBoletim(): List<BoletimEntity>
    
    @Query("SELECT * FROM boletim")
    fun observeBoletim(): Flow<List<BoletimEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoletim(items: List<BoletimEntity>)
    
    @Query("DELETE FROM boletim")
    suspend fun clear()
}

@Dao
interface HorarioDao {
    @Query("SELECT * FROM horarios")
    suspend fun getAll(): List<HorarioEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<HorarioEntity>)

    @Query("DELETE FROM horarios")
    suspend fun clear()
}
