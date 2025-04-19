package com.capstonecobconnect.myapplication.services

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstonecobconnect.myapplication.R

class Service_Request_ListItemFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ListItemAdapter
    private lateinit var swipeDwonTextView: TextView
    private lateinit var noteTextView: TextView
    private lateinit var iconDownView: ImageView

    private var doubleBackToExitPressedOnce = false

    // ✅ Use shared ViewModel
    private val viewModel: ServiceRequestViewModel by activityViewModels()

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.fragment_service_request_list_item, container, false)

        recyclerView = rootView.findViewById(R.id.recyclerViewSelectedServices)
        swipeDwonTextView = rootView.findViewById(R.id.swipeDown)
        iconDownView = rootView.findViewById(R.id.iconDownView)
        noteTextView = rootView.findViewById(R.id.note)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initial empty adapter
        adapter = ListItemAdapter(emptyList(), viewModel)
        recyclerView.adapter = adapter

        // Observe the LiveData and update adapter
        viewModel.selectedServiceInfoList.observe(viewLifecycleOwner) { list ->
            adapter.updateData(list)
        }

        animateIconGlow()
        handleBackPress()


        return rootView
    }

    private fun handleBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (doubleBackToExitPressedOnce) {
                        requireActivity().finishAffinity() // Close the application
                    } else {
                        doubleBackToExitPressedOnce = true
                        Toast.makeText(
                            requireContext(),
                            "The back button is disable",
                            Toast.LENGTH_SHORT
                        ).show()

                        Handler(Looper.getMainLooper()).postDelayed({
                            doubleBackToExitPressedOnce = false
                        }, 2000)
                    }
                }
            })
    }

    private fun animateIconGlow() {

        // Glow effect (light blue color)
        val glowColor = Color.parseColor("#562069A8")  // Light Blue color

        // Create ObjectAnimator to move the light upwards
        val moveUp = ObjectAnimator.ofFloat(iconDownView, "translationY", 0f, -80f) // Moves up by 100 pixels
        moveUp.duration = 1000  // 1 second for one movement cycle
        moveUp.repeatCount = ObjectAnimator.INFINITE  // Repeats forever
        moveUp.repeatMode = ObjectAnimator.REVERSE  // Reverses the direction after each animation cycle

        // Create an ObjectAnimator to change the color to light blue (glow effect)
        val colorChange = ObjectAnimator.ofObject(
            iconDownView,
            "colorFilter",
            ArgbEvaluator(),
            Color.TRANSPARENT,
            glowColor
        )
        colorChange.duration = 1000  // 1 second for color change (pulse effect)
        colorChange.repeatCount = ObjectAnimator.INFINITE  // Repeats forever
        colorChange.repeatMode = ObjectAnimator.REVERSE  // Reverses after each cycle

        // Create an animation for pulsing the light (changing alpha)
        val pulseGlow = ObjectAnimator.ofFloat(iconDownView, "alpha", 0.5f, 1f) // Fade in/out effect
        pulseGlow.duration = 1000  // 1 second for pulsing
        pulseGlow.repeatCount = ObjectAnimator.INFINITE  // Repeats forever
        pulseGlow.repeatMode = ObjectAnimator.REVERSE  // Reverse to create a fade-in/fade-out effect

        // Combine the animations (move up + pulsing glow)
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(moveUp, pulseGlow)
        animatorSet.start()
    }
}
