package io.github.kellyson71.supaco.data.repository

import io.github.kellyson71.supaco.data.local.dao.ProfileDao
import io.github.kellyson71.supaco.data.local.entity.ProfileEntity
import io.github.kellyson71.supaco.data.model.Profile
import io.github.kellyson71.supaco.data.model.ProfileResponse
import io.github.kellyson71.supaco.data.model.toProfile
import io.github.kellyson71.supaco.data.remote.SuapApi

class ProfileRepository(
    private val suapApi: SuapApi,
    private val profileDao: ProfileDao,
) {
    suspend fun cached(): Profile? = profileDao.getProfile()?.toDomain()

    /** Busca na rede e grava no cache. Lança exceção em caso de falha. */
    suspend fun refresh(): ProfileResponse {
        val response = suapApi.getProfile()
        // Curso vem de outro endpoint; é opcional para não travar o login.
        val curso = runCatching { suapApi.getDadosAluno().curso }.getOrDefault("")
        profileDao.insertProfile(ProfileEntity.fromDomain(response.toProfile(curso)))
        return response
    }
}
