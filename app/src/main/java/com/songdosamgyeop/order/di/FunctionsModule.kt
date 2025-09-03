package com.songdosamgyeop.order.di

import com.google.firebase.functions.BuildConfig
import com.google.firebase.functions.FirebaseFunctions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FunctionsModule {

    @Provides @Singleton
    fun provideFunctions(): FirebaseFunctions {
        val f = FirebaseFunctions.getInstance("asia-northeast3") // 서버 리전과 일치
        if (BuildConfig.DEBUG) {
            // 에뮬레이터: Android 에뮬레이터에서 로컬호스트는 10.0.2.2
            f.useEmulator("10.0.2.2", 5001)
        }
        return f
    }
}