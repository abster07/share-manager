package com.share_manager.di

import android.content.Context
import androidx.room.Room
import com.share_manager.BuildConfig
import com.share_manager.data.db.AccountDao
import com.share_manager.data.db.MeroShareDatabase
import com.share_manager.network.ApiService
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

    // ── OkHttp ────────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // Mimic a real browser — the CDSC backend checks the Origin/Referer headers.
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .addHeader("Accept-Language", "en-US,en;q=0.9")
                    .addHeader("Origin", "https://meroshare.cdsc.com.np")
                    .addHeader("Referer", "https://meroshare.cdsc.com.np/")
                    .addHeader(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    )
                    .build()
                chain.proceed(request)
            }

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            )
        }

        return builder.build()
    }

    // ── Retrofit — backend.cdsc.com.np ────────────────────────────────────────

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://backend.cdsc.com.np/api/meroShare/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)

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
