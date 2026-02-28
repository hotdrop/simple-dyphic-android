package jp.hotdrop.simpledyphic.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import jp.hotdrop.simpledyphic.data.local.db.AppDatabase
import jp.hotdrop.simpledyphic.data.local.db.AppDatabaseMigrations
import jp.hotdrop.simpledyphic.data.local.db.RecordDao
import jp.hotdrop.simpledyphic.data.local.db.WeeklyGoalDao

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(AppDatabaseMigrations.MIGRATION_1_2)
            .build()
    }

    @Provides
    @Singleton
    fun provideRecordDao(database: AppDatabase): RecordDao = database.recordDao()

    @Provides
    @Singleton
    fun provideWeeklyGoalDao(database: AppDatabase): WeeklyGoalDao = database.weeklyGoalDao()
}
