package com.capstonecobconnect.myapplication.services

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

    class ServiceRequestViewModel : ViewModel() {

        private val _selectedServices = MutableLiveData<List<ServiceItem>>()
        val selectedServices: LiveData<List<ServiceItem>> get() = _selectedServices

        private val _selectedServiceInfoList = MutableLiveData<MutableList<SelectedServiceInfo>>(mutableListOf())
        val selectedServiceInfoList: LiveData<MutableList<SelectedServiceInfo>> = _selectedServiceInfoList


        fun setSelectedServices(services: List<ServiceItem>) {
            _selectedServices.value = services
        }

        fun addServiceInfo(newEntry: SelectedServiceInfo) {
            val currentList = _selectedServiceInfoList.value ?: mutableListOf()

            // Normalize both lists for comparison: sort by name to ensure same items == same list
            val newServicesSorted = newEntry.services.sortedBy { it.service }

            // Check if a matching service list already exists
            val existingIndex = currentList.indexOfFirst {
                it.services.sortedBy { s -> s.service } == newServicesSorted
            }

            if (existingIndex != -1) {
                // If exists, just update the quantity
                currentList[existingIndex].quantity += newEntry.quantity
            } else {
                // Else add as new entry
                currentList.add(newEntry)
            }

            _selectedServiceInfoList.value = currentList
        }

        fun updateQuantity(index: Int, newQuantity: Int) {
            _selectedServiceInfoList.value?.get(index)?.quantity = newQuantity
            _selectedServiceInfoList.value = _selectedServiceInfoList.value
        }


        fun addService(service: ServiceItem) {
            val currentList = _selectedServices.value.orEmpty().toMutableList()
            currentList.add(service)
            _selectedServices.value = currentList
        }

        fun clearServices() {
            _selectedServices.value = emptyList()
        }

        fun removeServiceInfo(index: Int) {
            _selectedServiceInfoList.value?.let {
                if (index in it.indices) {
                    it.removeAt(index)
                    _selectedServiceInfoList.value = it
                }
            }
        }


        fun getSelectedServices(): List<ServiceItem> = _selectedServices.value ?: emptyList()
    }
