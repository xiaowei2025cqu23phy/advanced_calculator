package com.example.myapplication.di

import android.content.Context
import com.example.myapplication.math.CalculationRepository
import com.example.myapplication.math.ComplexRepository
import com.example.myapplication.math.MatrixRepository
import com.example.myapplication.repository.AppDatabase
import com.example.myapplication.repository.HistoryDao
import com.example.myapplication.repository.HistoryRepository
import com.example.myapplication.repository.SettingsManager
import com.example.myapplication.repository.UnitRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideHistoryDao(database: AppDatabase): HistoryDao {
        return database.historyDao()
    }

    @Provides
    @Singleton
    fun provideHistoryRepository(@ApplicationContext context: Context): HistoryRepository {
        return HistoryRepository(context)
    }

    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager(context)
    }

    @Provides
    @Singleton
    fun provideCalculationRepository(): CalculationRepository {
        return CalculationRepository()
    }

    @Provides
    @Singleton
    fun provideComplexRepository(): ComplexRepository {
        return ComplexRepository()
    }

    @Provides
    @Singleton
    fun provideMatrixRepository(): MatrixRepository {
        return MatrixRepository()
    }

    @Provides
    @Singleton
    fun provideUnitRepository(): UnitRepository {
        return UnitRepository()
    }
}
