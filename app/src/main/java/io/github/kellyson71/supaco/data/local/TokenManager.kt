package io.github.kellyson71.supaco.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore

/** Armazenamento dos tokens (interface para testar a camada de rede sem Android). */
interface TokenStore {
    fun saveToken(access: String, refresh: String)
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun clear()
}

class TokenManager(private val context: Context) : TokenStore {

    private val sharedPreferences: SharedPreferences = openOrReset()

    /**
     * Em alguns aparelhos o Keystore corrompe a chave mestra (restauração de backup,
     * troca de bloqueio de tela) e o EncryptedSharedPreferences lança exceção ao abrir.
     * Nesse caso apagamos as prefs e a chave e recomeçamos — o usuário só precisa
     * logar de novo, em vez de o app travar na abertura.
     */
    private fun openOrReset(): SharedPreferences = try {
        create()
    } catch (e: Exception) {
        Log.w(TAG, "Prefs criptografadas ilegíveis, recriando", e)
        context.deleteSharedPreferences(PREFS_NAME)
        runCatching {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                .deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        }
        create()
    }

    private fun create(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun saveToken(access: String, refresh: String) {
        sharedPreferences.edit {
            putString(ACCESS_TOKEN, access)
            putString(REFRESH_TOKEN, refresh)
        }
    }

    override fun getAccessToken(): String? = sharedPreferences.getString(ACCESS_TOKEN, null)
    override fun getRefreshToken(): String? = sharedPreferences.getString(REFRESH_TOKEN, null)?.takeIf { it.isNotBlank() }

    override fun clear() {
        sharedPreferences.edit(commit = true) { clear() }
    }

    private companion object {
        const val TAG = "TokenManager"
        const val PREFS_NAME = "supaco_secure_prefs"
        const val ACCESS_TOKEN = "access_token"
        const val REFRESH_TOKEN = "refresh_token"
    }
}
