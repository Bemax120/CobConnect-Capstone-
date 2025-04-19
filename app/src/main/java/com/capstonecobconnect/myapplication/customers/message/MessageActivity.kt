package com.capstonecobconnect.myapplication.customers.message

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.capstonecobconnect.myapplication.R

class MessageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        val cobblerId = intent.getStringExtra("COBBLER_ID") ?: return
        val customerId = intent.getStringExtra("CUSTOMER_ID") ?: return

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.message_fragment_container,
                    MessageFragment.newInstance(cobblerId, customerId)
                )
                .commit()
        }
    }
}
