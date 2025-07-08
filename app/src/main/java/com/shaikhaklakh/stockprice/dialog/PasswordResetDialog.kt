package com.shaikhaklakh.stockprice.dialog

import android.app.Activity
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.shaikhaklakh.stockprice.R


class PasswordResetDialog(
    private val activity: Activity,
    private val onSendClick: (String) -> Unit
) {

    fun show() {
        val dialog = BottomSheetDialog(activity,R.style.CustomDialogStyle)
        val view = LayoutInflater.from(activity).inflate(R.layout.reset_password_dailog, null)
        dialog.setContentView(view)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.show()

        val edEmail = view.findViewById<EditText>(R.id.etPasswordReset)
        val btnSend = view.findViewById<Button>(R.id.buttonSendResetPassword)
        val btnCancel = view.findViewById<Button>(R.id.buttonCancelResetPassword)

        btnSend.setOnClickListener {
            val email = edEmail.text.toString().trim()
            onSendClick(email)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
    }
}