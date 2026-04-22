package com.share_manager.di

import android.content.Context
import androidx.room.Room
import com.share_manager.BuildConfig
import com.share_manager.data.db.AccountDao
import com.share_manager.data.db.MeroShareDatabase
import com.share_manager.data.db.MeroShareDatabase.Companion.MIGRATION_1_2
import com.share_manager.network.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /** MeroShare portal backend — all authenticated API calls go here. */
    private const val MERO_SHARE_BASE_URL = "https://backend.cdsc.com.np/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // ── In-memory CookieJar ───────────────────────────────────────────
            // Cloudflare sets __cf_bm and cf_clearance cookies on the first
            // HTML page load (the warm-up GET in MeroShareRepository).
            // Without a CookieJar those cookies are discarded and every
            // subsequent API request is treated as a new bot probe.
            // This simple store groups cookies by host and replays them on
            // every matching request for the lifetime of the process.
            .cookieJar(object : CookieJar {
                // CopyOnWriteArrayList is safe for concurrent reads/writes
                // without explicit synchronisation.
                private val store = HashMap<String, CopyOnWriteArrayList<Cookie>>()

                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    val host = url.host
                    val list = store.getOrPut(host) { CopyOnWriteArrayList() }
                    cookies.forEach { incoming ->
                        // Replace any existing cookie with the same name so we
                        // always hold the freshest value (CF rotates __cf_bm).
                        list.removeAll { it.name == incoming.name }
                        list.add(incoming)
                    }
                }

                override fun loadForRequest(url: HttpUrl): List<Cookie> {
                    // Return all stored cookies whose domain matches the host.
                    return store[url.host]
                        ?.filter { it.matches(url) }
                        ?: emptyList()
                }
            })
            // ── Follow redirects ──────────────────────────────────────────────
            // Cloudflare sometimes issues a 301/302 redirect before settling
            // on the real response; OkHttp follows these by default but we
            // make it explicit for clarity.
            .followRedirects(true)
            .followSslRedirects(true)

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
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(MERO_SHARE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MeroShareDatabase =
        Room.databaseBuilder(context, MeroShareDatabase::class.java, "meroshare.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    @Singleton
    fun provideAccountDao(db: MeroShareDatabase): AccountDao = db.accountDao()
}
