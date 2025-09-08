package com.songdosamgyeop.order.di

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.functions.FirebaseFunctions
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
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        }

    @Provides @Singleton
    fun provideFunctions(): FirebaseFunctions =
        FirebaseFunctions.getInstance(Env.FUNCTIONS_REGION)

    @Provides @Singleton
    fun provideHqFunctionsDS(
        functions: FirebaseFunctions
    ): HqFunctionsDataSource = HqFunctionsDataSource(functions, Env.FUNCTIONS_ENABLED)
}