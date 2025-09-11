package com.songdosamgyeop.order.di

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

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore =
        FirebaseFirestore.getInstance().apply {
            // 🔹 에뮬레이터 분기 (로컬 PC 또는 10.0.2.2)
            if (BuildConfig.EMULATOR) {
                useEmulator(BuildConfig.EMULATOR_HOST, /* port = */ 8080)
            }
            // 🔹 오프라인 퍼시스턴스
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        }

    @Provides
    @Singleton
    fun provideFunctions(): FirebaseFunctions =
        FirebaseFunctions.getInstance(Env.FUNCTIONS_REGION).apply {
            // 🔹 에뮬레이터 분기
            if (BuildConfig.EMULATOR) {
                useEmulator(BuildConfig.EMULATOR_HOST, /* port = */ 5001)
            }
        }

    @Provides
    @Singleton
    fun provideHqFunctionsDS(
        functions: FirebaseFunctions
    ): HqFunctionsDataSource {
        // 🔹 Functions 호출 활성 여부:
        // - 실서버가 켜졌거나(Env.FUNCTIONS_ENABLED)
        // - 에뮬레이터를 쓰는 경우(BuildConfig.EMULATOR)
        val functionsEnabled = Env.FUNCTIONS_ENABLED || BuildConfig.EMULATOR
        return HqFunctionsDataSource(functions, functionsEnabled)
    }
}