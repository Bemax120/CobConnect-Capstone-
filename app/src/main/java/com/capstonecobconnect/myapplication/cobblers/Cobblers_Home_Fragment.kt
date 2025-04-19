package com.capstonecobconnect.myapplication.cobblers

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import com.capstonecobconnect.myapplication.R
import com.capstonecobconnect.myapplication.cobblers.HomeService.Cobblers_Home_Service
import com.capstonecobconnect.myapplication.store.cobblers.CobblerStore_MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class Cobblers_Home_Fragment : Fragment() {

    private var userSignedIn: Boolean = true
    private var doubleBackToExitPressedOnce = false
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var jobCountListener: ListenerRegistration? = null
    private var walletBalanceListener: ListenerRegistration? = null

    private var userId: String? = null

    companion object {
        private const val ARG_USER_ID = "userId"

        fun newInstance(userId: String): Cobblers_Home_Fragment {
            val fragment = Cobblers_Home_Fragment()
            val args = Bundle()
            args.putString(ARG_USER_ID, userId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString(ARG_USER_ID)
        }

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_cobblers__home, container, false)

        val jobComLayout = view.findViewById<LinearLayout>(R.id.jobCompletedtxt)
        val btnHomeService = view.findViewById<LinearLayout>(R.id.btn_HomeServiceCob)
        val btnMarketPlace = view.findViewById<LinearLayout>(R.id.btn_marketPlaceCob)
        val walletLayout = view.findViewById<LinearLayout>(R.id.wallet_Cob)

        btnMarketPlace.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            val dialogView = layoutInflater.inflate(R.layout.custom_dialog_create_store, null)
            val dialog = builder.setView(dialogView).create()

            // Close button
            val btnClose = dialogView.findViewById<Button>(R.id.btnClose)
            btnClose.setOnClickListener {
                dialog.dismiss()
            }

            // Submit button
            val btnSubmit = dialogView.findViewById<Button>(R.id.btnSubmit)
            btnSubmit.setOnClickListener {
                val shopName = dialogView.findViewById<EditText>(R.id.ET1).text.toString()
                val shopAddress = dialogView.findViewById<EditText>(R.id.ET2).text.toString()
                val phoneNumber = dialogView.findViewById<EditText>(R.id.ET3).text.toString()

                if (shopName.isNotEmpty() && shopAddress.isNotEmpty() && phoneNumber.isNotEmpty()) {
                    val intent = Intent(requireContext(), CobblerStore_MainActivity::class.java)
                    intent.putExtra("shopName", shopName)
                    intent.putExtra("shopAddress", shopAddress)
                    intent.putExtra("phoneNumber", phoneNumber)
                    startActivity(intent)
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            }

            dialog.show()
        }

        btnHomeService.setOnClickListener {
            val intent = Intent(requireContext(), Cobblers_Home_Service::class.java)
            intent.putExtra("USER_ID", userId)
            startActivity(intent)
        }

        walletLayout.setOnClickListener {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            val intent = Intent(requireContext(), com.capstonecobconnect.myapplication.wallet.Wallet_View::class.java)
            intent.putExtra("USER_ID", currentUserId)
            startActivity(intent)
        }

        // Fetch and display the count of completed jobs in real-time
        setUpRealTimeJobCountListener(jobComLayout)

        // Handle back press
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (userSignedIn) {
                    if (doubleBackToExitPressedOnce) {
                        requireActivity().finishAffinity()
                    } else {
                        doubleBackToExitPressedOnce = true
                        Toast.makeText(requireContext(), "Please click BACK again to exit", Toast.LENGTH_SHORT).show()

                        Handler(Looper.getMainLooper()).postDelayed({
                            doubleBackToExitPressedOnce = false
                        }, 2000)
                    }
                } else {
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
            }
        })

        return view
    }

    private fun setUpRealTimeJobCountListener(jobComLayout: LinearLayout) {
        val jobCountTextView = jobComLayout.getChildAt(0) as? TextView
        userId?.let { uid ->
            jobCountListener = firestore.collection("job_requests")
                .whereEqualTo("cobblerId", uid)
                .whereEqualTo("status", "completed")
                .addSnapshotListener { snapshot, _ ->
                    val completedCount = snapshot?.size() ?: 0
                    jobCountTextView?.text = completedCount.toString()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        jobCountListener?.remove()
        walletBalanceListener?.remove()
    }
}
