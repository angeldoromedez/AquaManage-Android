package com.aquamanagers.aquamanage_app.fragments

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.aquamanagers.aquamanage_app.R

class ReminderFragment : Fragment() {

    companion object {
        private const val ARG_IMAGE_RES = "image_res"

        fun newInstance(imageRes: Int): ReminderFragment {
            val fragment = ReminderFragment()
            val args = Bundle()
            args.putInt(ARG_IMAGE_RES, imageRes)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val imageRes = arguments?.getInt(ARG_IMAGE_RES) ?: R.drawable.reminder_a

        val layout = ConstraintLayout(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            background = ContextCompat.getDrawable(context, imageRes)
        }

        return layout
    }
}
