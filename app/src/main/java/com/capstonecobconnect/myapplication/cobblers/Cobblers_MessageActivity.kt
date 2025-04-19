package com.capstonecobconnect.myapplication.cobblers

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.capstonecobconnect.myapplication.R

class Cobblers_MessageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_cobblers)

        val cobblerId = intent.getStringExtra("COBBLER_ID") ?: return
        val customerId = intent.getStringExtra("CUSTOMER_ID") ?: return

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.message_fragment_containerCob,
                    Cobblers_MessageFragment.newInstance(cobblerId, customerId)
                )
                .commit()
        }
    }
}
