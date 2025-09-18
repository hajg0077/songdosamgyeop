package com.songdosamgyeop.order.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.songdosamgyeop.order.data.repo.BranchOrdersRepository
import com.songdosamgyeop.order.data.repo.BranchOrdersRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * ✅ BranchOrdersRepository 바인딩
 * - 기존 DI가 이미 제공 중이라면 이 모듈은 건너써도 되며, 시그니처 변경만 반영하면 됨.
 */
@Module
@InstallIn(SingletonComponent::class)
object OrdersModule {

    @Provides @Singleton
    fun provideBranchOrdersRepository(
        db: FirebaseFirestore,
        auth: FirebaseAuth
    ): BranchOrdersRepository = BranchOrdersRepositoryImpl(db, auth)
}