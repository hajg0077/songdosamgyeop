package com.songdosamgyeop.order.user

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
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
        val ROLE = stringPreferencesKey("role")
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
        appContext.userPrefsDataStore.edit { p: MutablePreferences -> // ✅ MutablePreferences
            p[Keys.UID] = profile.uid
            p[Keys.EMAIL] = profile.email
            p[Keys.NAME] = profile.name
            p[Keys.ROLE] = profile.role

            if (profile.branchId != null) p[Keys.BRANCH_ID] = profile.branchId else p.remove(Keys.BRANCH_ID) // ✅ remove OK
            if (profile.branchName != null) p[Keys.BRANCH_NAME] = profile.branchName else p.remove(Keys.BRANCH_NAME)
        }
    }

    suspend fun clear() {
        appContext.userPrefsDataStore.edit { p: MutablePreferences -> // ✅ MutablePreferences
            p.clear()
        }
    }
}