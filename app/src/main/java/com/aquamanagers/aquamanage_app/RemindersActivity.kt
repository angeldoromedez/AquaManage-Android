package com.aquamanagers.aquamanage_app

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.aquamanagers.aquamanage_app.adapters.ReminderAdapter

class RemindersActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: ImageButton

    private val layouts = arrayOf(
        R.layout.reminder_a,
        R.layout.reminder_b,
        R.layout.reminder_c,
        R.layout.reminder_d,
        R.layout.reminder_e,
        R.layout.reminder_f
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminders)

        viewPager = findViewById(R.id.viewPager)
        btnNext = findViewById(R.id.btnNext)

        val adapter = ReminderAdapter(this, layouts)
        viewPager.adapter = adapter

        btnNext.setOnClickListener {
            val currentItem = viewPager.currentItem
            if (currentItem < layouts.size - 1) {
                viewPager.currentItem = currentItem + 1
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if(position == layouts.lastIndex)
                    btnNext.setImageResource(R.drawable.ic_done_symbol)
                else
                    btnNext.setImageResource(R.drawable.ic_arrow_right)
            }
        })
    }
}
