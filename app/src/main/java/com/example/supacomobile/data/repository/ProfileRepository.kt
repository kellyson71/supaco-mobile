package com.example.supacomobile.data.repository

import com.example.supacomobile.data.local.dao.ProfileDao
import com.example.supacomobile.data.local.entity.ProfileEntity
import com.example.supacomobile.data.model.Profile
import com.example.supacomobile.data.model.toProfile
import com.example.supacomobile.data.remote.SuapApi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileRepository(
    private val suapApi: SuapApi,
    private val profileDao: ProfileDao
) {
    suspend fun getProfile(): Result<Profile> {
        // Tenta buscar no cache primeiro, se falhar ou estiver vazio, tenta rede
        return try {
            val cachedProfile = profileDao.getProfile()
            if (cachedProfile != null) {
                // Atualiza em background de forma assíncrona
                CoroutineScope(Dispatchers.IO).launch {
                    runCatching { fetchAndCacheProfile() }
                }
                Result.success(cachedProfile.toDomain())
            } else {
                fetchAndCacheProfile()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    private suspend fun fetchAndCacheProfile(): Result<Profile> {
        return try {
            val profile = suapApi.getProfile().toProfile()
            profileDao.insertProfile(ProfileEntity.fromDomain(profile))
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
