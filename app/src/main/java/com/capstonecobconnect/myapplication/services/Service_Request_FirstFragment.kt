package com.capstonecobconnect.myapplication.services

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstonecobconnect.myapplication.R

class Service_Request_FirstFragment : Fragment() {

    private lateinit var recyclerViewCleaning: RecyclerView
    private lateinit var recyclerViewShoeRepair: RecyclerView
    private lateinit var recyclerViewSpecialized: RecyclerView
    private lateinit var recyclerViewProductType: RecyclerView
    private lateinit var recyclerViewTypeShoes: RecyclerView
    private lateinit var btnNext: Button

    private lateinit var cleaningAdapter: ServiceItemAdapter
    private lateinit var shoeRepairAdapter: ServiceItemAdapter
    private lateinit var specializedAdapter: ServiceItemAdapter
    private lateinit var productTypeAdapter: SingleSelectServiceItemAdapter
    private lateinit var typeShoesAdapter: SingleSelectServiceItemAdapter

    private var doubleBackToExitPressedOnce = false

    private lateinit var viewModel: ServiceRequestViewModel

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_service__request__first, container, false)

        viewModel = ViewModelProvider(requireActivity())[ServiceRequestViewModel::class.java]

        recyclerViewCleaning = rootView.findViewById(R.id.recyclerCleaning)
        recyclerViewShoeRepair = rootView.findViewById(R.id.recyclerViewShoeRepair)
        recyclerViewSpecialized = rootView.findViewById(R.id.recyclerViewSpecializedServices)
        recyclerViewProductType = rootView.findViewById(R.id.recyclerProductType)
        recyclerViewTypeShoes = rootView.findViewById(R.id.recyclerViewTypeShoes)
        btnNext = rootView.findViewById(R.id.btn_next)
        btnNext.isEnabled = false

        setupRecyclerViews()

        val serviceList = getServiceList()

        val cleaningList = serviceList.filter { it.category == "Cleaning" }
        val shoeRepairList = serviceList.filter { it.category == "Shoe Repair" }
        val specializedList = serviceList.filter { it.category == "Specialized Services" }
        val productTypeList = serviceList.filter { it.category == "Product Type" }
        val typeShoesList = serviceList.filter { it.category == "Type Shoes" }

        cleaningAdapter = ServiceItemAdapter(cleaningList) { updateNextButtonState() }
        shoeRepairAdapter = ServiceItemAdapter(shoeRepairList) { updateNextButtonState() }
        specializedAdapter = ServiceItemAdapter(specializedList) { updateNextButtonState() }
        productTypeAdapter = SingleSelectServiceItemAdapter(productTypeList) { updateNextButtonState() }
        typeShoesAdapter = SingleSelectServiceItemAdapter(typeShoesList) { updateNextButtonState() }

        recyclerViewCleaning.adapter = cleaningAdapter
        recyclerViewShoeRepair.adapter = shoeRepairAdapter
        recyclerViewSpecialized.adapter = specializedAdapter
        recyclerViewProductType.adapter = productTypeAdapter
        recyclerViewTypeShoes.adapter = typeShoesAdapter

        btnNext.setOnClickListener {


            val selectedServices = collectSelectedServices()
            clearSelections()

            val newItem = SelectedServiceInfo(
                services = selectedServices
            )

            viewModel.addServiceInfo(newItem)
            btnNext.isEnabled = false
        }

        handleBackPress()

        return rootView
    }

    private fun setupRecyclerViews() {
        listOf(recyclerViewCleaning, recyclerViewShoeRepair, recyclerViewSpecialized,
            recyclerViewProductType, recyclerViewTypeShoes).forEach { recyclerView ->
            recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun updateNextButtonState() {
        btnNext.isEnabled = productTypeAdapter.hasSelectedItems() &&
                typeShoesAdapter.hasSelectedItems() &&
                (cleaningAdapter.hasSelectedItems() || shoeRepairAdapter.hasSelectedItems() || specializedAdapter.hasSelectedItems())
    }

    private fun collectSelectedServices(): List<ServiceItem> {
        return mutableListOf<ServiceItem>().apply {
            addAll(cleaningAdapter.getSelectedServices())
            addAll(shoeRepairAdapter.getSelectedServices())
            addAll(specializedAdapter.getSelectedServices())
            productTypeAdapter.getSelectedService()?.let { add(it) }
            typeShoesAdapter.getSelectedService()?.let { add(it) }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearSelections() {
        cleaningAdapter.clearSelections()
        shoeRepairAdapter.clearSelections()
        specializedAdapter.clearSelections()
        productTypeAdapter.clearSelections()
        typeShoesAdapter.clearSelections()

        cleaningAdapter.notifyDataSetChanged()
        shoeRepairAdapter.notifyDataSetChanged()
        specializedAdapter.notifyDataSetChanged()
        productTypeAdapter.notifyDataSetChanged()
        typeShoesAdapter.notifyDataSetChanged()
    }

    private fun getServiceList(): List<ServiceItem> {
        return listOf(
            ServiceItem("Cleaning", "Deep Cleaning", 100.0),
            ServiceItem("Cleaning", "Polishing", 120.0),
            ServiceItem("Shoe Repair", "Sole Replacement", 100.0),
            ServiceItem("Shoe Repair", "Heel Repair/Replacement", 100.0),
            ServiceItem("Shoe Repair", "Stitching", 100.0),
            ServiceItem("Shoe Repair", "Leather Repair", 100.0),
            ServiceItem("Specialized Services", "Waterproofing Treatments", 100.0),
            ServiceItem("Specialized Services", "Shoe Refurbishing", 100.0),
            ServiceItem("Specialized Services", "Cushioning and Padding", 100.0),
            ServiceItem("Product Type", "Original", 100.0),
            ServiceItem("Product Type", "High-quality copy", 50.0),
            ServiceItem("Type Shoes", "Running Shoes", 100.0),
            ServiceItem("Type Shoes", "School Shoes", 100.0),
            ServiceItem("Type Shoes", "Rubber Shoes", 50.0)
        )
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
                            "Please click BACK again to exit",
                            Toast.LENGTH_SHORT
                        ).show()

                        Handler(Looper.getMainLooper()).postDelayed({
                            doubleBackToExitPressedOnce = false
                        }, 2000)
                    }
                }
            })
    }
}