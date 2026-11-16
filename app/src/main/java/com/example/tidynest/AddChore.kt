package com.example.tidynest

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity

class AddChore : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_chore)
        applySystemBarPadding()

        val database = Database(this)
        val userId = Session.userId(this)
        val userName = database.getUserName(userId)

        val avatar = findViewById<TextView>(R.id.avatar)
        val choreTitle = findViewById<AutoCompleteTextView>(R.id.choreTitle)
        val daysActive = findViewById<TextView>(R.id.daysActive)
        val memberName = findViewById<EditText>(R.id.memberName)
        val notes = findViewById<EditText>(R.id.notes)
        val reminder = findViewById<Switch>(R.id.reminder)

        val rooms = listOf(
            findViewById<TextView>(R.id.roomKitchen),
            findViewById<TextView>(R.id.roomBathroom),
            findViewById<TextView>(R.id.roomLiving),
            findViewById<TextView>(R.id.roomBedroom),
            findViewById<TextView>(R.id.roomLaundry)
        )
        val priorities = listOf(
            findViewById<View>(R.id.priorityLow),
            findViewById<View>(R.id.priorityMedium),
            findViewById<View>(R.id.priorityHigh),
            findViewById<View>(R.id.priorityUrgent)
        )
        val durations = listOf(
            findViewById<TextView>(R.id.duration15),
            findViewById<TextView>(R.id.duration30),
            findViewById<TextView>(R.id.duration45),
            findViewById<TextView>(R.id.duration60)
        )
        val days = listOf(
            findViewById<TextView>(R.id.dayMon),
            findViewById<TextView>(R.id.dayTue),
            findViewById<TextView>(R.id.dayWed),
            findViewById<TextView>(R.id.dayThu),
            findViewById<TextView>(R.id.dayFri),
            findViewById<TextView>(R.id.daySat),
            findViewById<TextView>(R.id.daySun)
        )

        avatar.text = userName.take(1).uppercase()
        memberName.setText(userName)

        pickOne(rooms)
        pickOne(priorities)
        pickOne(durations)
        rooms[0].isSelected = true
        priorities[1].isSelected = true
        durations[1].isSelected = true

        for (day in days) {
            day.setOnClickListener {
                day.isSelected = !day.isSelected
                daysActive.text = "${days.count { it.isSelected }} days active"
            }
        }

        // Common chores to pick from; anything else can still be typed
        val commonChores = listOf(
            "Wash dishes", "Take out the trash", "Sweep the floor", "Mop the floor",
            "Vacuum", "Do the laundry", "Fold clothes", "Clean the bathroom",
            "Wipe kitchen counters", "Water the plants", "Cook dinner", "Grocery shopping",
            "Change bed sheets", "Dust the shelves", "Clean the fridge"
        )
        choreTitle.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, commonChores))
        findViewById<View>(R.id.titleDropdown).setOnClickListener {
            choreTitle.requestFocus()
            choreTitle.showDropDown()
        }
        choreTitle.setOnClickListener { choreTitle.showDropDown() }

        findViewById<View>(R.id.clearTitle).setOnClickListener { choreTitle.setText("") }


        findViewById<View>(R.id.topBack).setOnClickListener { finish() }
        findViewById<View>(R.id.back).setOnClickListener { finish() }
        findViewById<View>(R.id.cancel).setOnClickListener { finish() }

        findViewById<View>(R.id.save).setOnClickListener {
            val title = choreTitle.text.toString().trim()
            val pickedDays = days.filter { it.isSelected }

            if (title.isEmpty()) {
                Toast.makeText(this, "Please enter a chore title", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pickedDays.isEmpty()) {
                Toast.makeText(this, "Please pick at least one day", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            database.addChore(
                userId = userId,
                title = title,
                room = rooms.first { it.isSelected }.text.toString(),
                priority = priorities.first { it.isSelected }.tag.toString(),
                days = pickedDays.joinToString(", ") { it.text },
                assignedTo = memberName.text.toString().trim().ifEmpty { userName },
                duration = durations.first { it.isSelected }.text.toString(),
                notes = notes.text.toString().trim(),
                reminder = reminder.isChecked
            )
            Toast.makeText(this, "Chore added", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun pickOne(options: List<View>) {
        for (option in options) {
            option.setOnClickListener {
                for (other in options) other.isSelected = (other == option)
            }
        }
    }
}
