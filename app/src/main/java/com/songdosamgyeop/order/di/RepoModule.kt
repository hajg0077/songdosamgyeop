package com.songdosamgyeop.order.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.songdosamgyeop.order.data.repo.AuthRepository
import com.songdosamgyeop.order.data.repo.BranchRegistrationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository 객체들을 Hilt로 주입하기 위한 DI 모듈.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepoModule {

    /** AuthRepository 인스턴스를 싱글톤으로 제공한다. */
    @Provides @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): AuthRepository = AuthRepository(auth, firestore)

    /** BranchRegistrationRepository 인스턴스를 싱글톤으로 제공한다. */
    @Provides @Singleton
    fun provideBranchRegistrationRepository(
        firestore: FirebaseFirestore
    ): BranchRegistrationRepository = BranchRegistrationRepository(firestore)
}