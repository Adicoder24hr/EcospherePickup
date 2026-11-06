
package com.ecosphere.wastepickup.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class TokenManager(context: Context) {

    private val preferences = context.applicationContext
        .getSharedPreferences(
            "ecosphere_secure_session",
            Context.MODE_PRIVATE
        )

    private companion object {
        const val KEY_ALIAS = "ecosphere_jwt_key"
        const val TOKEN_IV = "token_iv"
        const val TOKEN_DATA = "token_data"
    }

    private fun getOrCreateKey(): SecretKey {

        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        val existingKey = keyStore.getKey(
            KEY_ALIAS,
            null
        ) as? SecretKey

        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(
                KeyProperties.BLOCK_MODE_GCM
            )
            .setEncryptionPaddings(
                KeyProperties.ENCRYPTION_PADDING_NONE
            )
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keySpec)

        return keyGenerator.generateKey()
    }

    fun saveToken(token: String) {

        require(token.isNotBlank()) {
            "Token cannot be empty"
        }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateKey()
        )

        val encryptedData = cipher.doFinal(
            token.toByteArray(Charsets.UTF_8)
        )

        preferences.edit()
            .putString(
                TOKEN_IV,
                Base64.encodeToString(
                    cipher.iv,
                    Base64.NO_WRAP
                )
            )
            .putString(
                TOKEN_DATA,
                Base64.encodeToString(
                    encryptedData,
                    Base64.NO_WRAP
                )
            )
            .apply()
    }

    fun getToken(): String? {

        val ivString = preferences.getString(TOKEN_IV, null)
            ?: return null

        val encryptedString =
            preferences.getString(TOKEN_DATA, null)
                ?: return null

        return try {

            val iv = Base64.decode(
                ivString,
                Base64.NO_WRAP
            )

            val encryptedData = Base64.decode(
                encryptedString,
                Base64.NO_WRAP
            )

            val cipher = Cipher.getInstance(
                "AES/GCM/NoPadding"
            )

            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(128, iv)
            )

            String(
                cipher.doFinal(encryptedData),
                Charsets.UTF_8
            )

        } catch (e: Exception) {

            clearToken()
            null
        }
    }

    fun saveUser(
        id: String,
        name: String,
        email: String,
        mobile: String
    ) {
        preferences.edit()
            .putString("user_id", id)
            .putString("user_name", name)
            .putString("user_email", email)
            .putString("user_mobile", mobile)
            .apply()
    }

    fun getUserId(): String? {
        preferences.getString("user_id", null)?.let { return it }
        val payload = getToken()?.split('.')?.getOrNull(1) ?: return null
        return try {
            val decoded = String(
                Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
                Charsets.UTF_8
            )
            val id = org.json.JSONObject(decoded).optString("sub").takeIf { it.isNotBlank() }
                ?: return null
            preferences.edit().putString("user_id", id).apply()
            id
        } catch (_: Exception) {
            null
        }
    }

    fun getUserName(): String =
        preferences.getString("user_name", "Customer") ?: "Customer"

    fun getUserEmail(): String =
        preferences.getString("user_email", "") ?: ""

    fun getUserMobile(): String =
        preferences.getString("user_mobile", "") ?: ""

    fun clearSession() {
        clearToken()

        preferences.edit()
            .remove("user_name")
            .remove("user_id")
            .remove("user_email")
            .remove("user_mobile")
            .apply()
    }

    fun clearToken() {
        preferences.edit()
            .remove(TOKEN_IV)
            .remove(TOKEN_DATA)
            .apply()
    }
}
