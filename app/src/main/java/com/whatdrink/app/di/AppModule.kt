package com.whatdrink.app.di

import com.google.firebase.firestore.FirebaseFirestore
import com.whatdrink.app.data.repository.DrinkRepository
import com.whatdrink.app.data.repository.DrinkRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideDrinkRepository(
        firestore: FirebaseFirestore
    ): DrinkRepository {
        return DrinkRepositoryImpl(firestore)
    }
}