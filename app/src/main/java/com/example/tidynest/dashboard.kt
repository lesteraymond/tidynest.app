package com.example.tidynest

import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Paint
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.widget.doAfterTextChanged

class Dashboard : ComponentActivity() {

    private lateinit var database: Database
    private var userId = -1L

    private lateinit var choreList: LinearLayout
    private lateinit var emptyText: TextView
    private lateinit var search: EditText
    private lateinit var filters: List<TextView>

    private var currentFilter = "All"
    private var currentStatus = "Pending"
    private lateinit var statusChips: List<TextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        applySystemBarPadding()

        database = Database(this)
        userId = Session.userId(this)

        choreList = findViewById(R.id.choreList)
        emptyText = findViewById(R.id.emptyText)
        search = findViewById(R.id.search)
        filters = listOf(
            findViewById(R.id.filterAll),
            findViewById(R.id.filterUrgent),
            findViewById(R.id.filterHigh),
            findViewById(R.id.filterMedium),
            findViewById(R.id.filterLow)
        )

        val name = database.getUserName(userId)
        val firstName = name.split(" ").first()
        findViewById<TextView>(R.id.greeting).text = "Hello, $firstName!"
        findViewById<TextView>(R.id.avatar).text = name.take(1).uppercase()
        findViewById<TextView>(R.id.topAvatar).text = name.take(1).uppercase()

        statusChips = listOf(findViewById(R.id.statusPending), findViewById(R.id.statusDone))
        for (chip in statusChips) {
            chip.setOnClickListener {
                currentStatus = chip.tag.toString()
                loadChores()
            }
        }

        for (filter in filters) {
            filter.setOnClickListener {
                currentFilter = filter.tag.toString()
                loadChores()
            }
        }

        search.doAfterTextChanged { loadChores() }

        val openAddChore = View.OnClickListener {
            startActivity(Intent(this, AddChore::class.java))
        }
                findViewById<View>(R.id.navAdd).setOnClickListener(openAddChore)

        findViewById<View>(R.id.bellButton).setOnClickListener { showNotifications() }
        findViewById<View>(R.id.navHistory).setOnClickListener {
            startActivity(Intent(this, History::class.java))
        }

        // Home and Tasks: reset search/filter and jump back to the top
        val goHome = View.OnClickListener {
            search.setText("")
            currentFilter = "All"
            currentStatus = "Pending"
            loadChores()
            findViewById<View>(R.id.scroll).scrollTo(0, 0)
        }
        findViewById<View>(R.id.homeButton).setOnClickListener(goHome)
        findViewById<View>(R.id.navTasks).setOnClickListener(goHome)

        val topAvatar = findViewById<View>(R.id.topAvatar)
        topAvatar.setOnClickListener { showProfileMenu(topAvatar) }
    }

    private fun showProfileMenu(anchor: View) {
        val view = layoutInflater.inflate(R.layout.popup_profile, null)

        val popup = PopupWindow(
            view,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popup.setBackgroundDrawable(getDrawable(R.drawable.bg_popup))
        popup.elevation = 0f

        view.findViewById<View>(R.id.popupLogOut).setOnClickListener {
            popup.dismiss()
            Session.logout(this)
            startActivity(Intent(this, Login::class.java))
            finish()
        }

        val gap = (8 * resources.displayMetrics.density).toInt()
        popup.showAsDropDown(anchor, 0, gap, Gravity.END)
    }

    /** Pending chores scheduled for today, so reminders have something real to show. */
    private fun dueToday(): List<Chore> {
        val today = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")[
            java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) - 1
        ]
        return database.getChores(userId).filter { !it.done && it.days.contains(today) }
    }

    private fun showNotifications() {
        val due = dueToday()
        val message = if (due.isEmpty()) {
            "You're all caught up. No chores due today."
        } else {
            due.joinToString("\n") { "• ${it.title} (${it.room}, ${it.priority})" }
        }
        AlertDialog.Builder(this)
            .setTitle(if (due.isEmpty()) "Notifications" else "Due today (${due.size})")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showDetails(chore: Chore) {
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
        view.findViewById<TextView>(R.id.detailDuration).text = chore.duration
        view.findViewById<TextView>(R.id.detailAssigned).text = chore.assignedTo
        view.findViewById<TextView>(R.id.detailReminder).text = if (chore.reminder) "On" else "Off"
        view.findViewById<TextView>(R.id.detailNotes).text =
            chore.notes.ifEmpty { "No notes" }
        view.findViewById<TextView>(R.id.detailStatus).text =
            if (chore.done) "Done" else "Pending"

        val dialog = AlertDialog.Builder(this).setView(view).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val toggle = view.findViewById<TextView>(R.id.detailToggle)
        toggle.text = if (chore.done) "Mark pending" else "Mark done"
        toggle.setOnClickListener {
            database.setChoreDone(chore.id, !chore.done)
            loadChores()
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.detailClose).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun confirmDelete(chore: Chore) {
        AlertDialog.Builder(this)
            .setTitle("Delete chore?")
            .setMessage("\"${chore.title}\" will be removed.")
            .setPositiveButton("Delete") { _, _ ->
                database.deleteChore(chore.id)
                loadChores()
                Toast.makeText(this, "Chore deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadChores()
    }

    private fun loadChores() {
        val allChores = database.getChores(userId)
        val done = allChores.count { it.done }
        val total = allChores.size
        val percent = if (total == 0) 0 else done * 100 / total

        findViewById<View>(R.id.bellDot).visibility =
            if (dueToday().isEmpty()) View.GONE else View.VISIBLE
        findViewById<TextView>(R.id.choresLeft).text = "${total - done} ${if (total - done == 1) "chore" else "chores"} left today"
        findViewById<TextView>(R.id.activeCount).text = total.toString()
        findViewById<TextView>(R.id.doneCount).text = "$done Done"
        findViewById<ProgressBar>(R.id.progressRing).progress = percent
        findViewById<TextView>(R.id.progressText).text = "$percent%"
        findViewById<TextView>(R.id.progressMessage).text = when {
            total == 0 -> "Add your first chore"
            percent == 100 -> "All done!"
            percent >= 50 -> "Halfway there!"
            else -> "Keep going!"
        }

        for (chip in statusChips) {
            val status = chip.tag.toString()
            chip.text = "$status (${if (status == "Done") done else total - done})"
            chip.isSelected = status == currentStatus
        }

        val inStatus = allChores.filter { it.done == (currentStatus == "Done") }
        for (filter in filters) {
            val priority = filter.tag.toString()
            val count = if (priority == "All") inStatus.size else inStatus.count { it.priority == priority }
            filter.text = "$priority ($count)"
            filter.isSelected = priority == currentFilter
        }

        val query = search.text.toString().trim()
        val shown = inStatus.filter { chore ->
            (currentFilter == "All" || chore.priority == currentFilter) &&
                (chore.title.contains(query, ignoreCase = true) ||
                    chore.room.contains(query, ignoreCase = true))
        }

        choreList.removeAllViews()
        for (chore in shown) {
            choreList.addView(makeChoreCard(chore))
        }

        emptyText.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
        emptyText.text = if (total == 0) {
            "No chores yet.\nTap + Add New Chore to add your first one."
        } else {
            "No chores match your search."
        }
    }

    private fun makeChoreCard(chore: Chore): View {
        val card = layoutInflater.inflate(R.layout.item_chore, choreList, false)
        val check = card.findViewById<ImageView>(R.id.check)
        val title = card.findViewById<TextView>(R.id.title)
        val badge = card.findViewById<TextView>(R.id.badge)
        val days = card.findViewById<LinearLayout>(R.id.days)
        val details = card.findViewById<TextView>(R.id.details)

        title.text = chore.title
        details.text = "${chore.room} • ${chore.duration}"

        val (background, textColor) = when (chore.priority) {
            "Urgent" -> R.color.urgent_bg to R.color.priority_urgent
            "High" -> R.color.high_bg to R.color.high_text
            "Medium" -> R.color.lavender to R.color.priority_medium
            else -> R.color.light_green to R.color.green
        }
        badge.text = "• ${chore.priority.uppercase()}"
        badge.backgroundTintList = ColorStateList.valueOf(getColor(background))
        badge.setTextColor(getColor(textColor))

        val dayNames = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
        for (i in dayNames.indices) {
            days.getChildAt(i).isSelected = chore.days.contains(dayNames[i])
        }

        if (chore.done) {
            check.isSelected = true
            title.paintFlags = title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            card.alpha = 0.6f
        }

        card.findViewById<View>(R.id.delete).setOnClickListener { confirmDelete(chore) }
        card.setOnClickListener { showDetails(chore) }
        card.setOnLongClickListener {
            confirmDelete(chore)
            true
        }

        check.setOnClickListener {
            database.setChoreDone(chore.id, !chore.done)
            loadChores()
        }

        return card
    }
}
