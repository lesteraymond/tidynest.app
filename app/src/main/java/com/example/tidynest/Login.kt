package com.example.tidynest

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.widget.doAfterTextChanged

class Login : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        applySystemBarPadding()

        if (Session.userId(this) != -1L) {
            openDashboard()
            return
        }

        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)
        val showPassword = findViewById<ImageView>(R.id.showPassword)
        val forgotPassword = findViewById<TextView>(R.id.forgotPassword)
        val logIn = findViewById<Button>(R.id.logIn)
        val signUp = findViewById<TextView>(R.id.signUp)

        fun setFieldError(field: EditText, hasError: Boolean) {
            (field.parent as? View)?.setBackgroundResource(
                if (hasError) R.drawable.bg_field_error else R.drawable.bg_field
            )
        }

        email.doAfterTextChanged { setFieldError(email, false) }
        password.doAfterTextChanged { setFieldError(password, false) }

        var passwordVisible = false
        showPassword.setOnClickListener {
            passwordVisible = !passwordVisible
            password.inputType = if (passwordVisible) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            password.setSelection(password.text.length)
        }

        logIn.setOnClickListener {
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString()

            val emailEmpty = emailText.isEmpty()
            val passwordEmpty = passwordText.isEmpty()

            if (emailEmpty || passwordEmpty) {
                if (emailEmpty) setFieldError(email, true)
                if (passwordEmpty) setFieldError(password, true)
                Toast.makeText(this, "Please enter your email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = Database(this).login(emailText, passwordText)
            if (userId == -1L) {
                setFieldError(email, true)
                setFieldError(password, true)
                Toast.makeText(this, "Wrong email or password", Toast.LENGTH_SHORT).show()
            } else {
                Session.login(this, userId)
                openDashboard()
            }
        }

        forgotPassword.setOnClickListener {
            Toast.makeText(this, "Password reset coming soon", Toast.LENGTH_SHORT).show()
        }

        signUp.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }
    }

    private fun openDashboard() {
        startActivity(Intent(this, Dashboard::class.java))
        finish()
    }
}
