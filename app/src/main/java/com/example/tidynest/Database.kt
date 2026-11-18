package com.example.tidynest

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest

data class Chore(
    val id: Long,
    val title: String,
    val room: String,
    val priority: String,
    val days: String,
    val assignedTo: String,
    val duration: String,
    val done: Boolean,
    val completedAt: Long = 0,
    val reminder: Boolean = false,
    val notes: String = "",
    val timeMinutes: Int = DEFAULT_TIME
) {
    val doneToday: Boolean get() = done && completedAt >= startOfToday()

    val isScheduledToday: Boolean get() = days.contains(todayName())

    val timeLabel: String get() = formatTime(timeMinutes)

    companion object {
        const val DEFAULT_TIME = 8 * 60

        fun formatTime(minutes: Int): String {
            val h = minutes / 60
            val m = minutes % 60
            val h12 = if (h % 12 == 0) 12 else h % 12
            return "%d:%02d %s".format(h12, m, if (h < 12) "AM" else "PM")
        }

        fun startOfToday(): Long {
            val c = java.util.Calendar.getInstance()
            c.set(java.util.Calendar.HOUR_OF_DAY, 0)
            c.set(java.util.Calendar.MINUTE, 0)
            c.set(java.util.Calendar.SECOND, 0)
            c.set(java.util.Calendar.MILLISECOND, 0)
            return c.timeInMillis
        }

        fun todayName(): String = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")[
            java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) - 1
        ]
    }
}

class Database(context: Context) : SQLiteOpenHelper(context, "tidynest.db", null, 4) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL
            )"""
        )
        createChoresTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("DROP TABLE IF EXISTS chores")
            createChoresTable(db)
        } else {
            if (oldVersion < 3) {
                db.execSQL("ALTER TABLE chores ADD COLUMN completed_at INTEGER NOT NULL DEFAULT 0")
            }
            if (oldVersion < 4) {
                db.execSQL("ALTER TABLE chores ADD COLUMN time_minutes INTEGER NOT NULL DEFAULT ${Chore.DEFAULT_TIME}")
            }
        }
    }

    private fun createChoresTable(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE chores (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                title TEXT NOT NULL,
                room TEXT NOT NULL,
                priority TEXT NOT NULL,
                days TEXT NOT NULL,
                assigned_to TEXT NOT NULL,
                duration TEXT NOT NULL,
                notes TEXT NOT NULL,
                reminder INTEGER NOT NULL,
                done INTEGER NOT NULL DEFAULT 0,
                completed_at INTEGER NOT NULL DEFAULT 0,
                time_minutes INTEGER NOT NULL DEFAULT ${Chore.DEFAULT_TIME}
            )"""
        )
    }

    fun registerUser(name: String, email: String, password: String): Long {
        val values = ContentValues()
        values.put("name", name)
        values.put("email", email.lowercase())
        values.put("password", hash(password))
        return writableDatabase.insert("users", null, values)
    }

    fun login(email: String, password: String): Long {
        val cursor = readableDatabase.rawQuery(
            "SELECT id FROM users WHERE email = ? AND password = ?",
            arrayOf(email.lowercase(), hash(password))
        )
        val id = if (cursor.moveToFirst()) cursor.getLong(0) else -1
        cursor.close()
        return id
    }

    fun getUserName(userId: Long): String {
        val cursor = readableDatabase.rawQuery(
            "SELECT name FROM users WHERE id = ?",
            arrayOf(userId.toString())
        )
        val name = if (cursor.moveToFirst()) cursor.getString(0) else ""
        cursor.close()
        return name
    }

    fun addChore(
        userId: Long,
        title: String,
        room: String,
        priority: String,
        days: String,
        assignedTo: String,
        duration: String,
        notes: String,
        reminder: Boolean,
        timeMinutes: Int
    ) {
        val values = ContentValues()
        values.put("user_id", userId)
        values.put("title", title)
        values.put("room", room)
        values.put("priority", priority)
        values.put("days", days)
        values.put("assigned_to", assignedTo)
        values.put("duration", duration)
        values.put("notes", notes)
        values.put("reminder", if (reminder) 1 else 0)
        values.put("time_minutes", timeMinutes)
        writableDatabase.insert("chores", null, values)
    }

    fun getChores(userId: Long): List<Chore> =
        getChoresWhere("user_id = ?", arrayOf(userId.toString()))

    private fun getChoresWhere(where: String, args: Array<String>): List<Chore> {
        val chores = mutableListOf<Chore>()
        val cursor = readableDatabase.rawQuery(
            "SELECT id, title, room, priority, days, assigned_to, duration, done, completed_at, reminder, notes, time_minutes FROM chores WHERE $where",
            args
        )
        while (cursor.moveToNext()) {
            chores.add(
                Chore(
                    id = cursor.getLong(0),
                    title = cursor.getString(1),
                    room = cursor.getString(2),
                    priority = cursor.getString(3),
                    days = cursor.getString(4),
                    assignedTo = cursor.getString(5),
                    duration = cursor.getString(6),
                    done = cursor.getInt(7) == 1,
                    completedAt = cursor.getLong(8),
                    reminder = cursor.getInt(9) == 1,
                    notes = cursor.getString(10),
                    timeMinutes = cursor.getInt(11)
                )
            )
        }
        cursor.close()
        return chores
    }

    fun getChore(choreId: Long): Chore? =
        getChoresWhere("id = ?", arrayOf(choreId.toString())).firstOrNull()

    fun updateChore(
        choreId: Long,
        title: String,
        room: String,
        priority: String,
        days: String,
        assignedTo: String,
        duration: String,
        notes: String,
        reminder: Boolean,
        timeMinutes: Int
    ) {
        val values = ContentValues()
        values.put("title", title)
        values.put("room", room)
        values.put("priority", priority)
        values.put("days", days)
        values.put("assigned_to", assignedTo)
        values.put("duration", duration)
        values.put("notes", notes)
        values.put("reminder", if (reminder) 1 else 0)
        values.put("time_minutes", timeMinutes)
        writableDatabase.update("chores", values, "id = ?", arrayOf(choreId.toString()))
    }

    fun setChoreDone(choreId: Long, done: Boolean) {
        val values = ContentValues()
        values.put("done", if (done) 1 else 0)
        values.put("completed_at", if (done) System.currentTimeMillis() else 0)
        writableDatabase.update("chores", values, "id = ?", arrayOf(choreId.toString()))
    }

    fun deleteChore(choreId: Long) {
        writableDatabase.delete("chores", "id = ?", arrayOf(choreId.toString()))
    }

    fun clearCompleted(userId: Long) {
        writableDatabase.delete("chores", "user_id = ? AND done = 1", arrayOf(userId.toString()))
    }

    private fun hash(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
