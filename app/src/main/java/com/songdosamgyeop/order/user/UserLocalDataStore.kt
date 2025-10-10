package com.songdosamgyeop.order.user

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import com.songdosamgyeop.order.common.datastore.userPrefsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserLocalDataStore @Inject constructor(
    private val appContext: Context
) {
    private object Keys {
        val UID = stringPreferencesKey("uid")
        val EMAIL = stringPreferencesKey("email")
        val NAME = stringPreferencesKey("name")
        val ROLE = stringPreferencesKey("role")            // "HQ" | "BRANCH"
        val BRANCH_ID = stringPreferencesKey("branchId")
        val BRANCH_NAME = stringPreferencesKey("branchName")
    }

    fun getProfileFlow(): Flow<UserProfile?> =
        appContext.userPrefsDataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { p ->
                val uid = p[Keys.UID] ?: return@map null
                UserProfile(
                    uid = uid,
                    email = p[Keys.EMAIL].orEmpty(),
                    name = p[Keys.NAME].orEmpty(),
                    role = p[Keys.ROLE].orEmpty(),
                    branchId = p[Keys.BRANCH_ID],
                    branchName = p[Keys.BRANCH_NAME]
                )
            }

    suspend fun saveProfile(profile: UserProfile) {
        appContext.userPrefsDataStore.edit { p: Preferences ->
            p[Keys.UID] = profile.uid
            p[Keys.EMAIL] = profile.email
            p[Keys.NAME] = profile.name
            p[Keys.ROLE] = profile.role
            profile.branchId?.let { p[Keys.BRANCH_ID] = it } ?: p.remove(Keys.BRANCH_ID)
            profile.branchName?.let { p[Keys.BRANCH_NAME] = it } ?: p.remove(Keys.BRANCH_NAME)
        }
    }

    suspend fun clear() {
        appContext.userPrefsDataStore.edit { it.clear() }
    }
}