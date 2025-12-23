package com.songdosamgyeop.order.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.functions.FirebaseFunctions
import com.songdosamgyeop.order.BuildConfig
import com.songdosamgyeop.order.Env
import com.songdosamgyeop.order.data.remote.HqFunctionsDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides @Singleton
    fun provideFirestore(): FirebaseFirestore =
        FirebaseFirestore.getInstance().apply {
            if (BuildConfig.EMULATOR) {
                useEmulator(BuildConfig.EMULATOR_HOST, 8080)
                firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(false) // 🔥 중요
                    .build()
            }
        }

    @Provides @Singleton
    fun provideFunctions(): FirebaseFunctions =
        FirebaseFunctions.getInstance(Env.FUNCTIONS_REGION).apply {
            if (BuildConfig.EMULATOR) {
                useEmulator(BuildConfig.EMULATOR_HOST, /* port = */ 5001)
            }
        }

    // (권장) Auth 바인딩도 같이 제공 — MissingBinding 예방
    @Provides @Singleton
    fun provideAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideHqFunctionsDS(functions: FirebaseFunctions): HqFunctionsDataSource {
        val functionsEnabled = Env.FUNCTIONS_ENABLED || BuildConfig.EMULATOR
        return HqFunctionsDataSource(functions, functionsEnabled)
    }
}