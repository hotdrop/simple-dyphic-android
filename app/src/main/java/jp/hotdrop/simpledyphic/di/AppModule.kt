package jp.hotdrop.simpledyphic.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import jp.hotdrop.simpledyphic.data.local.RoomRecordLocalDataSource
import jp.hotdrop.simpledyphic.data.remote.FirebaseAuthRemoteDataSource
import jp.hotdrop.simpledyphic.data.remote.FirestoreRecordRemoteDataSource

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindRecordLocalDataSource(impl: RoomRecordLocalDataSource): RoomRecordLocalDataSource

    @Binds
    @Singleton
    abstract fun bindAuthRemoteDataSource(impl: FirebaseAuthRemoteDataSource): FirebaseAuthRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindRecordRemoteDataSource(impl: FirestoreRecordRemoteDataSource): FirestoreRecordRemoteDataSource
}
