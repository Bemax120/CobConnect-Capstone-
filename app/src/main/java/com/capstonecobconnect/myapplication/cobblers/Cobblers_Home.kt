package com.capstonecobconnect.myapplication.cobblers

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.capstonecobconnect.myapplication.R
import com.capstonecobconnect.myapplication.cobblers.Activity.Cobbler_History_Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class Cobblers_Home : AppCompatActivity() {

    private lateinit var cobblersHomeFragment: Cobblers_Home_Fragment
    private var userId: String? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cobblers_home)

        // Retrieve the userId from the intent
        userId = intent.getStringExtra("USER_ID")

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation_cob)

        // Initialize fragment with userId
        cobblersHomeFragment = Cobblers_Home_Fragment()

        // Set initial fragment
        replaceFragment(cobblersHomeFragment)

        // Set up navigation item selection
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(Cobblers_Home_Fragment.newInstance(userId!!))
                    true
                }
                R.id.nav_history -> {
                    replaceFragment(Cobbler_History_Fragment())
                    true
                }
                R.id.nav_message -> {
                    replaceFragment(Cobblers_Message_Fragment.newInstance(userId!!))
                    true
                }
                R.id.nav_account -> {
                    replaceFragment(Cobbler_Profile_Fragment.newInstance(userId!!))
                    true
                }
                else -> false
            }
        }
    }

    // Function to replace the fragment
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_Cobblers, fragment)
            .commit()
    }


}
