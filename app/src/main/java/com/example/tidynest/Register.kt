package com.example.tidynest

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.widget.doAfterTextChanged

class Register : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        applySystemBarPadding()

        val fullName = findViewById<EditText>(R.id.fullName)
        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)
        val confirmPassword = findViewById<EditText>(R.id.confirmPassword)
        val showPassword = findViewById<ImageView>(R.id.showPassword)
        val agree = findViewById<CheckBox>(R.id.agree)
        val signUp = findViewById<Button>(R.id.signUp)
        val logIn = findViewById<TextView>(R.id.logIn)

        fun showInfo(titleRes: Int, bodyRes: Int) {
            AlertDialog.Builder(this)
                .setTitle(titleRes)
                .setMessage(bodyRes)
                .setPositiveButton(R.string.close, null)
                .show()
        }

        fun link(textRes: Int, onClick: () -> Unit): SpannableString {
            val s = SpannableString(getString(textRes))
            s.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) = onClick()
                override fun updateDrawState(ds: TextPaint) {
                    ds.isUnderlineText = true
                    ds.isFakeBoldText = true
                }
            }, 0, s.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            return s
        }

        agree.text = SpannableStringBuilder()
            .append(getString(R.string.terms_prefix))
            .append(link(R.string.terms_guidelines_link) {
                showInfo(R.string.guidelines_title, R.string.guidelines_body)
            })
            .append(getString(R.string.terms_and))
            .append(link(R.string.terms_tos_link) {
                showInfo(R.string.tos_title, R.string.tos_body)
            })
        agree.movementMethod = LinkMovementMethod.getInstance()

        fun setFieldError(field: EditText, hasError: Boolean) {
            (field.parent as? View)?.setBackgroundResource(
                if (hasError) R.drawable.bg_field_error else R.drawable.bg_field
            )
        }

        fullName.doAfterTextChanged { setFieldError(fullName, false) }
        email.doAfterTextChanged { setFieldError(email, false) }
        password.doAfterTextChanged { setFieldError(password, false) }
        confirmPassword.doAfterTextChanged { setFieldError(confirmPassword, false) }

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

        signUp.setOnClickListener {
            val name = fullName.text.toString().trim()
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString()
            val confirmText = confirmPassword.text.toString()

            var hasError = false
            if (name.isEmpty()) {
                setFieldError(fullName, true)
                hasError = true
            }
            if (emailText.isEmpty()) {
                setFieldError(email, true)
                hasError = true
            }
            if (passwordText.isEmpty()) {
                setFieldError(password, true)
                hasError = true
            }
            if (confirmText.isEmpty()) {
                setFieldError(confirmPassword, true)
                hasError = true
            }

            if (hasError) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
                setFieldError(email, true)
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (passwordText.length < 6) {
                setFieldError(password, true)
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (passwordText != confirmText) {
                setFieldError(password, true)
                setFieldError(confirmPassword, true)
                Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!agree.isChecked) {
                Toast.makeText(this, "Please agree to the terms", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = Database(this).registerUser(name, emailText, passwordText)
            if (userId == -1L) {
                setFieldError(email, true)
                Toast.makeText(this, "This email is already registered", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Account created. Please log in", Toast.LENGTH_SHORT).show()
            finish()
        }

        logIn.setOnClickListener {
            finish()
        }
    }
}
