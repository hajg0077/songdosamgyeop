package com.songdosamgyeop.order.di

import com.songdosamgyeop.order.ui.payment.data.PaymentRepository
import com.songdosamgyeop.order.ui.payment.data.PaymentRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * PaymentRepository 바인딩 전용 모듈
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PaymentModule {
    @Binds
    @Singleton
    abstract fun bindPaymentRepository(impl: PaymentRepositoryImpl): PaymentRepository
}