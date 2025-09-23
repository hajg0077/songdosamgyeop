package com.songdosamgyeop.order.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 주의: 중복 바인딩 방지를 위해 바인딩 메서드 없음.
 * PaymentRepository / OrderRepository 바인딩은 RepositoryModule에서만 처리.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PaymentModule