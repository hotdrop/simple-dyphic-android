package jp.hotdrop.simpledyphic.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import jp.hotdrop.simpledyphic.core.log.AppLogger
import jp.hotdrop.simpledyphic.core.log.LogcatAppLogger
import jp.hotdrop.simpledyphic.data.local.source.RecordLocalDataSource
import jp.hotdrop.simpledyphic.data.local.source.RoomRecordLocalDataSource
import jp.hotdrop.simpledyphic.data.remote.auth.AuthRemoteDataSource
import jp.hotdrop.simpledyphic.data.remote.auth.FirebaseAuthRemoteDataSource
import jp.hotdrop.simpledyphic.data.remote.firestore.FirestoreRecordRemoteDataSource
import jp.hotdrop.simpledyphic.data.remote.firestore.RecordRemoteDataSource
import jp.hotdrop.simpledyphic.data.repository.impl.DefaultAccountRepository
import jp.hotdrop.simpledyphic.data.repository.impl.DefaultHealthConnectRepository
import jp.hotdrop.simpledyphic.data.repository.impl.LocalRecordRepository
import jp.hotdrop.simpledyphic.domain.repository.AccountRepository
import jp.hotdrop.simpledyphic.domain.repository.HealthConnectRepository
import jp.hotdrop.simpledyphic.domain.repository.RecordRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindAppLogger(impl: LogcatAppLogger): AppLogger

    @Binds
    @Singleton
    abstract fun bindRecordLocalDataSource(impl: RoomRecordLocalDataSource): RecordLocalDataSource

    @Binds
    @Singleton
    abstract fun bindRecordRepository(impl: LocalRecordRepository): RecordRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: DefaultAccountRepository): AccountRepository

    @Binds
    @Singleton
    abstract fun bindAuthRemoteDataSource(impl: FirebaseAuthRemoteDataSource): AuthRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindRecordRemoteDataSource(impl: FirestoreRecordRemoteDataSource): RecordRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindHealthConnectRepository(
        impl: DefaultHealthConnectRepository
    ): HealthConnectRepository
}
