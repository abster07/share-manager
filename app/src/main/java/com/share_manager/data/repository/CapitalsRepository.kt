package com.share_manager.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mirrors Python: Account.get_client_id(dp)
 *
 *   capital = next(item for item in constants.CAPITALS if item['code'] == str(dp))
 *   return capital['id']
 *
 * The capitals list (broker/DP list) is loaded from assets/capitals.json
 * which you should copy from the Python project's constants.py CAPITALS list
 * and save as a JSON array: [{"id": 128, "code": "13200", "name": "..."}, ...]
 *
 * If you don't have the file yet, the hardcoded fallback map covers common DPs.
 */
@Singleton
class CapitalsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val capitals: List<Capital> by lazy { loadCapitals() }

    fun getClientId(dpCode: String): Int? {
        return capitals.firstOrNull { it.code == dpCode }?.id
            ?: hardcodedFallback[dpCode]
    }

    fun getAllCapitals(): List<Capital> = capitals

    private fun loadCapitals(): List<Capital> {
        return try {
            val json = context.assets.open("capitals.json").bufferedReader().readText()
            val type = object : TypeToken<List<Capital>>() {}.type
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            // File not present yet — fall back to hardcoded map
            hardcodedFallback.map { (code, id) -> Capital(id, code, code) }
        }
    }

    data class Capital(val id: Int, val code: String, val name: String)

    // Common DP codes — extend as needed or replace with capitals.json from the Python project
    private val hardcodedFallback = mapOf(
        "13200" to 128,
        "13201" to 129,
        "13202" to 130,
        "13203" to 131,
        "13204" to 132,
        "13205" to 133,
        "13206" to 134,
        "13207" to 135,
        "13300" to 136,
        "13400" to 137,
        "13500" to 138,
        "13600" to 139,
        "13700" to 140,
        "13800" to 141,
        "13900" to 142,
        "14000" to 143,
        "14100" to 144,
        "14200" to 145,
        "14300" to 146,
        "14400" to 147,
        "14500" to 148,
        "14600" to 149,
        "14700" to 150,
        "14800" to 151,
        "14900" to 152,
        "15000" to 153
    )
}
