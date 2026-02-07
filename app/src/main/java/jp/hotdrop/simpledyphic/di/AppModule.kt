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
import jp.hotdrop.simpledyphic.data.repository.impl.LocalRecordRepository
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
}
