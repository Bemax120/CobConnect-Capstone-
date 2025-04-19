package com.capstonecobconnect.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.capstonecobconnect.myapplication.customers.activity.Activity_Page_Fragment
import com.capstonecobconnect.myapplication.customers.message.Message_Page_Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class Home_Page : AppCompatActivity() {

    private lateinit var homeFragment: Home_Page_Fragment
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        // Retrieve the userId from the intent
        userId = intent.getStringExtra("USER_ID")

        // Set up the BottomNavigationView
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        homeFragment = Home_Page_Fragment()

        // Set initial fragment
        replaceFragment(homeFragment)

        // Set up navigation item selection
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(homeFragment)
                    true
                }
                R.id.nav_history -> {
                    replaceFragment(Activity_Page_Fragment.newInstance(userId!!))
                    true
                }
                R.id.nav_message -> {
                    replaceFragment(Message_Page_Fragment.newInstance(userId!!))
                    true
                }
                R.id.nav_account -> {
                    replaceFragment(Account_Page_Fragment.newInstance(userId!!))
                    true
                }
                else -> false
            }
        }
    }

    // Function to replace the fragment
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
