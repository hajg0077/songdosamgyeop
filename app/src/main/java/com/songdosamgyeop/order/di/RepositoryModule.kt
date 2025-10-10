package com.songdosamgyeop.order.di

import com.songdosamgyeop.order.data.repo.OrderRepository
import com.songdosamgyeop.order.data.repo.OrderRepositoryImpl
import com.songdosamgyeop.order.payment.data.PaymentRepository
import com.songdosamgyeop.order.payment.data.PaymentRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindOrderRepository(impl: OrderRepositoryImpl): OrderRepository

    @Binds @Singleton
    abstract fun bindPaymentRepository(impl: PaymentRepositoryImpl): PaymentRepository
}