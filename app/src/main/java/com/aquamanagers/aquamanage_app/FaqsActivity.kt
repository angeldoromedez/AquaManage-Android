package com.aquamanagers.aquamanage_app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton

class FaqsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faqs)

        val faq1: LinearLayout = findViewById(R.id.faq1)
        val faq2: LinearLayout = findViewById(R.id.faq2)
        val faq3: LinearLayout = findViewById(R.id.faq3)
        val faq4: LinearLayout = findViewById(R.id.faq4)

        faq1.setOnClickListener { showFaqDialog(R.layout.faqs_a) }
        faq2.setOnClickListener { showFaqDialog(R.layout.faqs_b) }
        faq3.setOnClickListener { showFaqDialog(R.layout.faqs_c) }
        faq4.setOnClickListener { showFaqDialog(R.layout.faqs_d) }
    }

    private fun showFaqDialog(layoutResId: Int) {
        // Inflate the selected FAQ layout
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(layoutResId, null)

        // Create and show the dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.show()

        // Set up the OK button to close the dialog
        val okButton: AppCompatButton? = dialogView.findViewById(R.id.okButton)
        okButton?.setOnClickListener {
            dialog.dismiss()
        } ?: run {
            // Log or handle the case where okButton is not found
            dialog.dismiss()
        }
    }
}
