package com.share_manager.di

import android.content.Context
import androidx.room.Room
import com.share_manager.BuildConfig
import com.share_manager.data.db.AccountDao
import com.share_manager.data.db.MeroShareDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        // Only attach the verbose body logger in debug builds — never in release.
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            )
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MeroShareDatabase =
        Room.databaseBuilder(context, MeroShareDatabase::class.java, "meroshare.db")
            // In production you should supply explicit Migrations instead of
            // destructive migration to avoid data loss on schema updates.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideAccountDao(db: MeroShareDatabase): AccountDao = db.accountDao()
}
