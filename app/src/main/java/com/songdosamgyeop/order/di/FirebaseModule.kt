package com.songdosamgyeop.order.di

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.functions.FirebaseFunctions
import com.songdosamgyeop.order.Env
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
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        }

    @Provides @Singleton
    fun provideFunctions(): FirebaseFunctions =
        if (Env.FUNCTIONS_ENABLED) FirebaseFunctions.getInstance(Env.FUNCTIONS_REGION)
        else FirebaseFunctions.getInstance() // 호출 자체는 VM에서 막음
}