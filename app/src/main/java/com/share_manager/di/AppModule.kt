package com.share_manager.di

import android.content.Context
import androidx.room.Room
import com.google.gson.GsonBuilder
import com.share_manager.data.db.AccountDao
import com.share_manager.data.db.MeroShareDatabase
import com.share_manager.network.IpoApiService
import com.share_manager.network.MeroShareApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // ── Public IPO result checker ─────────────────────────────────────────────

    @Provides
    @Singleton
    @Named("ipo")
    fun provideIpoRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://iporesult.cdsc.com.np/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
            .build()

    @Provides
    @Singleton
    fun provideIpoApiService(@Named("ipo") retrofit: Retrofit): IpoApiService =
        retrofit.create(IpoApiService::class.java)

    // ── Authenticated MeroShare API ───────────────────────────────────────────

    @Provides
    @Singleton
    @Named("meroshare")
    fun provideMeroShareRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://webbackend.cdsc.com.np/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
            .build()

    @Provides
    @Singleton
    fun provideMeroShareApiService(@Named("meroshare") retrofit: Retrofit): MeroShareApiService =
        retrofit.create(MeroShareApiService::class.java)

    // ── Room ──────────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MeroShareDatabase =
        Room.databaseBuilder(context, MeroShareDatabase::class.java, "meroshare.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideAccountDao(db: MeroShareDatabase): AccountDao = db.accountDao()
}
