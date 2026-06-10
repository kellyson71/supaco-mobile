package com.example.supacomobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.supacomobile.data.local.dao.BoletimDao
import com.example.supacomobile.data.local.dao.HorarioDao
import com.example.supacomobile.data.local.dao.ProfileDao
import com.example.supacomobile.data.local.entity.BoletimEntity
import com.example.supacomobile.data.local.entity.HorarioEntity
import com.example.supacomobile.data.local.entity.ProfileEntity

@Database(
    entities = [ProfileEntity::class, BoletimEntity::class, HorarioEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun boletimDao(): BoletimDao
    abstract fun horarioDao(): HorarioDao
}
