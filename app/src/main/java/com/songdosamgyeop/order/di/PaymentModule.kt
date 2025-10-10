package com.songdosamgyeop.order.di

import com.google.firebase.ktx.Firebase
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.songdosamgyeop.order.Env
import com.songdosamgyeop.order.payment.data.PaymentRepository
import com.songdosamgyeop.order.payment.data.PaymentRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PaymentBindModule {
    @Binds
    @Singleton
    abstract fun bindPaymentRepository(impl: PaymentRepositoryImpl): PaymentRepository
}

@Module
@InstallIn(SingletonComponent::class)
object PaymentProvideModule {
    @Provides
    @Singleton
    fun provideFunctions(): FirebaseFunctions =
        Firebase.functions(Env.FUNCTIONS_REGION)
}