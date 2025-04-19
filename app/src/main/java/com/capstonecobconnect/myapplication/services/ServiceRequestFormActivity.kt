package com.capstonecobconnect.myapplication.services

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstonecobconnect.myapplication.R
import com.google.android.material.bottomsheet.BottomSheetBehavior

class ServiceRequestFormActivity : AppCompatActivity() {

    private lateinit var listViewLayout: LinearLayout
    private lateinit var bottomLayout: LinearLayout
    private lateinit var recyclerViewListService: RecyclerView
    private lateinit var priceTextView: TextView
    private lateinit var sheet: FrameLayout
    private lateinit var firstFragment: Service_Request_FirstFragment
    private lateinit var btnCancel: Button
    private lateinit var btnReview: Button

    private lateinit var adapter: ListItemAdapter

    // ✅ Use ViewModel delegate
    private val serviceRequestViewModel: ServiceRequestViewModel by viewModels()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_service_request_form)

        val userId = intent.getStringExtra("USER_ID")

        // View Initialization
        sheet = findViewById(R.id.sheet)
        priceTextView = findViewById(R.id.price_tv)
        listViewLayout = findViewById(R.id.linearLayout_listView)
        bottomLayout = findViewById(R.id.linearLayout_bottom)
        btnCancel = findViewById(R.id.cancel_btn)
        btnReview = findViewById(R.id.review_btn)


        recyclerViewListService = findViewById(R.id.recyclerList)



        // Setup BottomSheet

        val bottomSheet = findViewById<FrameLayout>(R.id.sheet)
        val behavior = BottomSheetBehavior.from(bottomSheet)

        // Set expanded height to 500dp programmatically
        val displayMetrics = resources.displayMetrics
        behavior.peekHeight = (170 * displayMetrics.density).toInt()
        bottomSheet.layoutParams.height = (500 * displayMetrics.density).toInt()
        bottomSheet.requestLayout()

        behavior.state = BottomSheetBehavior.STATE_COLLAPSED



        firstFragment = Service_Request_FirstFragment()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentSR, firstFragment, "firstFragment")
                .commitAllowingStateLoss()
        }

        // RecyclerView setup
        recyclerViewListService.layoutManager = LinearLayoutManager(this)
        adapter = ListItemAdapter(emptyList(), serviceRequestViewModel)
        recyclerViewListService.adapter = adapter

        // Observe LiveData from ViewModel
        serviceRequestViewModel.selectedServiceInfoList.observe(this) { list ->
            adapter.updateData(list)

            val totalPrice = list.sumOf { info ->
                info.services.sumOf { it.price } * info.quantity
            }

            priceTextView.text = "₱%.2f".format(totalPrice)

            btnReview.isEnabled = list.isNotEmpty()

            btnReview.setOnClickListener {
                val intent = Intent(this, Service_Request::class.java)
                intent.putExtra("selected_service_info", ArrayList(list))
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
            }
        }

        btnCancel.setOnClickListener {
            val hasSelectedItems = serviceRequestViewModel.selectedServiceInfoList.value?.isNotEmpty() == true

            if (hasSelectedItems) {
                // Show confirmation dialog
                AlertDialog.Builder(this)
                    .setTitle("Discard Changes?")
                    .setMessage("You have selected services. Going back will discard your changes. Are you sure you want to continue?")
                    .setPositiveButton("Yes") { _, _ ->
                        finish()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                finish()
            }
        }

    }


}
