package com.example.tidynest

import android.app.AlertDialog
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

class History : ComponentActivity() {

    private lateinit var database: Database
    private var userId = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        applySystemBarPadding()

        database = Database(this)
        userId = Session.userId(this)

        findViewById<View>(R.id.historyBack).setOnClickListener { finish() }
        findViewById<View>(R.id.historyClear).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear history?")
                .setMessage("This permanently deletes all completed chores.")
                .setPositiveButton("Clear") { _, _ ->
                    database.clearCompleted(userId)
                    load()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        val list = findViewById<LinearLayout>(R.id.historyList)
        val done = database.getChores(userId)
            .filter { it.done }
            .sortedByDescending { it.completedAt }

        list.removeAllViews()
        for (chore in done) {
            val row = layoutInflater.inflate(R.layout.item_history, list, false)
            row.findViewById<TextView>(R.id.historyTitle).text = chore.title
            val whenDone = if (chore.completedAt > 0) {
                DateUtils.getRelativeTimeSpanString(
                    chore.completedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
                ).toString()
            } else "earlier"
            row.findViewById<TextView>(R.id.historyDetails).text =
                "${chore.room} • ${chore.timeLabel} • done $whenDone"
            row.setOnClickListener { showChoreDetails(chore, fromHistory = true) { load() } }
            row.findViewById<View>(R.id.historyUndo).setOnClickListener {
                database.setChoreDone(chore.id, false)
                load()
            }
            list.addView(row)
        }

        findViewById<TextView>(R.id.historySummary).text = "${done.size} completed"
        findViewById<View>(R.id.historyEmpty).visibility = if (done.isEmpty()) View.VISIBLE else View.GONE
        findViewById<View>(R.id.historyClear).visibility = if (done.isEmpty()) View.INVISIBLE else View.VISIBLE
    }
}
