package com.capstonecobconnect.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class Account_Page_Fragment : Fragment() {

    private lateinit var mFirestore: FirebaseFirestore
    private lateinit var userId: String
    private var userListener: ListenerRegistration? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account__page, container, false)

        mFirestore = FirebaseFirestore.getInstance()

        val changePassTV = view.findViewById<TextView>(R.id.changePassTxt)
        val tectSupportTV = view.findViewById<TextView>(R.id.techSupportTxt)
        val logout = view.findViewById<Button>(R.id.btn_logout)
        val editBtn = view.findViewById<Button>(R.id.edit_btn)

        userId = arguments?.getString("USER_ID") ?: ""

        editBtn.setOnClickListener {
            val intent = Intent(activity, Edit_Profile::class.java)
            intent.putExtra("USER_ID", userId)
            startActivityForResult(intent, REQUEST_CODE_EDIT_PROFILE)
        }

        logout.setOnClickListener {
            val intent = Intent(activity, login::class.java)
            startActivity(intent)
            activity?.finish()
            Toast.makeText(activity, "Logout Successfully", Toast.LENGTH_SHORT).show()
        }

        changePassTV.setOnClickListener {
            val intent = Intent(activity, Change_Password::class.java)
            intent.putExtra("USER_ID", userId)
            startActivityForResult(intent, REQUEST_CODE_EDIT_PROFILE)
        }

        tectSupportTV.setOnClickListener {
            val intent = Intent(activity, Technical_Support::class.java)
            startActivity(intent)
        }

        if (userId.isNotEmpty()) {
            listenToUserData(userId, view)
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        userListener?.remove()
    }

    private fun listenToUserData(userId: String, view: View) {
        val userRef = mFirestore.collection("customers").document(userId)
        userListener = userRef.addSnapshotListener { document, e ->
            if (e != null) {
                Toast.makeText(activity, "Failed to retrieve user data: ${e.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            if (document != null && document.exists()) {
                val fName = document.getString("fName") ?: "N/A"
                val lName = document.getString("lName") ?: "N/A"
                val email = document.getString("email") ?: "N/A"
                val address = document.getString("address") ?: "N/A"
                val phone = document.getString("phone") ?: "N/A"
                val profileImage = document.getString("profileImg") ?: ""

                val fNameTextView = view.findViewById<TextView>(R.id.fullNameTxt)
                val emailTextView = view.findViewById<TextView>(R.id.emailtxt)
                val profileImageView = view.findViewById<ImageView>(R.id.profileImgCus)
                val addressTextView = view.findViewById<TextView>(R.id.addressTxt)
                val phoneTextView = view.findViewById<TextView>(R.id.phoneTxt)

                fNameTextView.text = "$fName $lName"
                emailTextView.text = email
                addressTextView.text = address
                phoneTextView.text = phone

                if (profileImage.isNotEmpty()) {
                    Glide.with(this).load(profileImage).circleCrop().into(profileImageView)
                } else {
                    profileImageView.setImageResource(R.drawable.default_picture)
                }
            } else {
                Toast.makeText(activity, "User data not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_EDIT_PROFILE = 1001

        fun newInstance(userId: String): Account_Page_Fragment {
            val fragment = Account_Page_Fragment()
            val args = Bundle()
            args.putString("USER_ID", userId)
            fragment.arguments = args
            return fragment
        }
    }
}
