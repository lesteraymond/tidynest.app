package com.example.tidynest

import android.content.Context

object Session {

    private fun prefs(context: Context) =
        context.getSharedPreferences("session", Context.MODE_PRIVATE)

    fun login(context: Context, userId: Long) {
        prefs(context).edit().putLong("userId", userId).apply()
    }

    fun logout(context: Context) {
        prefs(context).edit().remove("userId").apply()
    }

    fun userId(context: Context): Long = prefs(context).getLong("userId", -1)
}
