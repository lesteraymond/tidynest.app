package com.example.tidynest

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.text.format.DateUtils
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

fun Activity.showChoreDetails(chore: Chore, fromHistory: Boolean = false, onChanged: () -> Unit) {
    val database = Database(this)
    val view = layoutInflater.inflate(R.layout.dialog_chore_details, null)
    view.findViewById<TextView>(R.id.detailTitle).text = chore.title
    val badge = view.findViewById<TextView>(R.id.detailBadge)
    val (bg, fg) = when (chore.priority) {
        "Urgent" -> R.color.urgent_bg to R.color.priority_urgent
        "High" -> R.color.high_bg to R.color.high_text
        "Medium" -> R.color.lavender to R.color.priority_medium
        else -> R.color.light_green to R.color.green
    }
    badge.text = "• ${chore.priority.uppercase()}"
    badge.backgroundTintList = ColorStateList.valueOf(getColor(bg))
    badge.setTextColor(getColor(fg))
    view.findViewById<TextView>(R.id.detailRoom).text = chore.room
    val dayRow = view.findViewById<LinearLayout>(R.id.detailDays)
    val dayNames = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    for (i in dayNames.indices) {
        dayRow.getChildAt(i).isSelected = chore.days.contains(dayNames[i])
    }
    view.findViewById<TextView>(R.id.detailDuration).text = "${chore.timeLabel} · ${chore.duration}"
    view.findViewById<TextView>(R.id.detailAssigned).text = chore.assignedTo
    view.findViewById<TextView>(R.id.detailReminder).text = if (chore.reminder) "On" else "Off"
    view.findViewById<TextView>(R.id.detailNotes).text = chore.notes.ifEmpty { "No notes" }

    val done = fromHistory || chore.doneToday
    val ago = if (chore.completedAt > 0) {
        DateUtils.getRelativeTimeSpanString(
            chore.completedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
        ).toString()
    } else null
    view.findViewById<TextView>(R.id.detailStatus).text = when {
        done -> if (ago != null) "Done · $ago" else "Done"
        chore.done && ago != null -> "Pending · last done $ago"
        else -> "Pending"
    }

    val dialog = AlertDialog.Builder(this).setView(view).create()
    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    val toggle = view.findViewById<TextView>(R.id.detailToggle)
    toggle.text = if (done) "Mark pending" else "Mark done"
    toggle.setOnClickListener {
        database.setChoreDone(chore.id, !done)
        onChanged()
        dialog.dismiss()
    }
    view.findViewById<View>(R.id.detailEdit).setOnClickListener {
        dialog.dismiss()
        startActivity(Intent(this, AddChore::class.java).putExtra("choreId", chore.id))
    }
    view.findViewById<View>(R.id.detailClose).setOnClickListener { dialog.dismiss() }
    dialog.show()
}
