package io.github.kellyson71.supaco.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.kellyson71.supaco.data.local.entity.BoletimEntity
import io.github.kellyson71.supaco.data.local.entity.HorarioEntity
import io.github.kellyson71.supaco.data.local.entity.ProfileEntity

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile LIMIT 1")
    suspend fun getProfile(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)
}

@Dao
interface BoletimDao {
    @Query("SELECT * FROM boletim")
    suspend fun getBoletim(): List<BoletimEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoletim(items: List<BoletimEntity>)

    @Query("DELETE FROM boletim")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(items: List<BoletimEntity>) {
        clear()
        insertBoletim(items)
    }
}

@Dao
interface HorarioDao {
    @Query("SELECT * FROM horarios")
    suspend fun getAll(): List<HorarioEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<HorarioEntity>)

    @Query("DELETE FROM horarios")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(items: List<HorarioEntity>) {
        clear()
        insertAll(items)
    }
}
