package com.mathias.android.hitchhike.sheets

import android.app.AlertDialog
import android.location.Address
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mathias.android.hitchhike.ActivityMaps.Companion.fireDBHelper
import com.mathias.android.hitchhike.ActivityMaps.Companion.geocoder
import com.mathias.android.hitchhike.FireDBHelper.Companion.vehicles
import com.mathias.android.hitchhike.R
import com.mathias.android.hitchhike.model.Vehicle


class BottomSheetInfo : BottomSheetDialogFragment() {
    private lateinit var txtType: TextView
    private lateinit var txtDescription: TextView
    private lateinit var txtCharge: TextView
    private lateinit var txtLocation: TextView
    private lateinit var btnRent: Button
    private lateinit var btnUnlock: Button
    private lateinit var btnLock: Button
    private lateinit var btnAlarm: Button

    private lateinit var key: String
    private lateinit var vehicle: Vehicle

    internal fun newInstance(key: String): BottomSheetInfo {
        return BottomSheetInfo().apply {
            this.key = key
            this.vehicle = vehicles[key]!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.sheet_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI(view)
        initButtons(view)
    }

    private fun initUI(view: View) {
        txtType = view.findViewById(R.id.txt_type)
        txtDescription = view.findViewById(R.id.txt_description)
        txtCharge = view.findViewById(R.id.txt_charge)
        txtLocation = view.findViewById(R.id.txt_location)
        btnRent = view.findViewById(R.id.btn_rent)
        txtType.text = vehicle.type?.name
        txtDescription.text = vehicle.description
        txtCharge.text = String.format("%s%s",vehicle.charge.toString(), "%")
        Log.i(TAG, geocoder.toString())
        val addresses: List<Address> = geocoder.getFromLocation(
            vehicle.location.lat,
            vehicle.location.lng,
            1
        )
        txtLocation.text = if (addresses.isNotEmpty()) addresses[0].thoroughfare else "N/A"
    }

    private fun updateUI() {
        btnRent.text = if (!vehicle.rented) "Rent" else "Finish"
        btnUnlock.isEnabled = vehicle.locked
        btnLock.isEnabled = !vehicle.locked
        btnAlarm.text = if (vehicle.alarm) "Cancel" else "Alarm"
        btnAlarm.isEnabled = vehicle.rented
    }

    private fun initButtons(view: View) {
        btnLock = view.findViewById(R.id.btn_lock)
        btnUnlock = view.findViewById(R.id.btn_unlock)
        btnAlarm = view.findViewById(R.id.btn_alarm)
        btnRent.setOnClickListener { if (!vehicle.rented) onRentVehicle() else finishLease() }
        btnUnlock.setOnClickListener { unlockVehicle() }
        btnLock.setOnClickListener { lockVehicle() }
        btnAlarm.setOnClickListener { if (!vehicle.alarm) onTriggerAlarm() else cancelAlarm() }
        updateUI()
    }

    private fun onRentVehicle() {
        AlertDialog.Builder(context)
            .setTitle("Rent")
            .setMessage("Do you want to rent this vehicle?")
            .setPositiveButton(android.R.string.yes) { _, _ -> rentVehicle() }
            .setNegativeButton(android.R.string.no, null).show()
    }

    private fun rentVehicle() {
        vehicle.rented = true
        fireDBHelper.updateVehicle(key, vehicle)
        updateUI()
    }

    private fun unlockVehicle() {
        vehicle.locked = false
        fireDBHelper.updateVehicle(key, vehicle)
        updateUI()
    }

    private fun lockVehicle() {
        vehicle.locked = true
        fireDBHelper.updateVehicle(key, vehicle)
        updateUI()
    }

    private fun onTriggerAlarm() {
        AlertDialog.Builder(context)
            .setTitle("Alarm")
            .setMessage("Do you want to trigger the emergency signal?")
            .setPositiveButton(android.R.string.yes) { _, _ -> triggerAlarm() }
            .setNegativeButton(android.R.string.no, null).show()
    }

    private fun triggerAlarm() {
        vehicle.alarm = true
        fireDBHelper.updateVehicle(key, vehicle)
        updateUI()
    }

    private fun cancelAlarm() {
        vehicle.alarm = false
        fireDBHelper.updateVehicle(key, vehicle)
        updateUI()
    }

    private fun finishLease() {
        vehicle.rented = false
        vehicle.locked = true
        vehicle.alarm = false
        fireDBHelper.updateVehicle(key, vehicle)
        this.dismiss()
    }

    companion object {
        private const val TAG = "BottomSheetInfo"
    }
}