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
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
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
            // Allow OkHttp to follow HTTP→HTTPS redirects
            .followRedirects(true)
            .followSslRedirects(true)
            // Interceptor: force every request to HTTPS so we never
            // start on HTTP and trigger a redirect chain
            .addInterceptor { chain ->
                val original: Request = chain.request()
                val url = original.url.toString()

                // If the URL is accidentally HTTP, upgrade it to HTTPS
                val secureRequest = if (url.startsWith("http://")) {
                    original.newBuilder()
                        .url(url.replace("http://", "https://"))
                        .build()
                } else {
                    original
                }

                chain.proceed(secureRequest)
            }

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
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideAccountDao(db: MeroShareDatabase): AccountDao = db.accountDao()
}