package com.erik.despertar.di

import android.content.Context
import androidx.room.Room
import com.erik.despertar.data.AppDatabase
import com.erik.despertar.data.FavoriteDao
import com.erik.despertar.data.AlarmDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "despertar_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideFavoriteDao(db: AppDatabase): FavoriteDao {
        return db.favoriteDao()
    }

    @Provides
    fun provideAlarmDao(db: AppDatabase): AlarmDao {
        return db.alarmDao()
    }
}
