package com.example.telebridge_android_app.utils

import android.content.Context

object LocalStorageHelper {
    private const val PREF_NAME = "telebridge_prefs"
    private const val KEY_USER_CODE = "user_code"

    fun saveUserCode(context: Context, code: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_USER_CODE, code).apply()
    }

    fun getUserCode(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_CODE, null)
    }

    fun clearUserCode(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_USER_CODE).apply()
    }
}
