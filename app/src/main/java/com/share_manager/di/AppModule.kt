package com.share_manager.di

import android.content.Context
import androidx.room.Room
import com.google.gson.GsonBuilder
import com.share_manager.data.db.AccountDao
import com.share_manager.data.db.MeroShareDatabase
import com.share_manager.network.IpoApiService
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
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Accept", "application/json, text/plain, */*")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "null")
                .build()
            chain.proceed(request)
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        val lenientGson = GsonBuilder().setLenient().create()
        return Retrofit.Builder()
            .baseUrl("https://iporesult.cdsc.com.np/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(lenientGson))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): IpoApiService =
        retrofit.create(IpoApiService::class.java)

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
