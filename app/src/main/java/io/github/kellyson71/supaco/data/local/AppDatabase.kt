package io.github.kellyson71.supaco.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.kellyson71.supaco.data.local.dao.BoletimDao
import io.github.kellyson71.supaco.data.local.dao.HorarioDao
import io.github.kellyson71.supaco.data.local.dao.ProfileDao
import io.github.kellyson71.supaco.data.local.entity.BoletimEntity
import io.github.kellyson71.supaco.data.local.entity.HorarioEntity
import io.github.kellyson71.supaco.data.local.entity.ProfileEntity

@Database(
    entities = [ProfileEntity::class, BoletimEntity::class, HorarioEntity::class],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun boletimDao(): BoletimDao
    abstract fun horarioDao(): HorarioDao
}
