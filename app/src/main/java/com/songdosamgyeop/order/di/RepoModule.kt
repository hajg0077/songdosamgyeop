package com.songdosamgyeop.order.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.songdosamgyeop.order.data.repo.AuthRepository
import com.songdosamgyeop.order.data.repo.RegistrationRepository
import com.songdosamgyeop.order.data.repo.RegistrationRepositoryImpl
import dagger.Binds
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

    /**
     * AuthRepository 인스턴스를 싱글톤으로 제공한다.
     *
     * @param auth FirebaseAuth 주입
     * @param firestore FirebaseFirestore 주입
     */
    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): AuthRepository = AuthRepository(auth, firestore)
}