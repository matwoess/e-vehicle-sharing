package com.mathias.android.hitchhike

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mathias.android.hitchhike.FireDBHelper.Companion.markers
import com.mathias.android.hitchhike.FireDBHelper.Companion.vehicleTypes
import com.mathias.android.hitchhike.sheets.BottomSheetInfo
import java.util.*

class ActivityMaps : AppCompatActivity(), OnMapReadyCallback, IRentVehicle {
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var fabInfo: FloatingActionButton
    private var currentVehicle: String? = null
    private var requestingLocationUpdates: Boolean = true
    private var lastLocation: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateValuesFromBundle(savedInstanceState)
        setContentView(R.layout.activity_maps)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fabInfo = findViewById(R.id.fab_info)
        fabInfo.setOnClickListener { showInfo() }
        // for location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geocoder = Geocoder(applicationContext, Locale.getDefault())
        locationRequest = createLocationRequest()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                updateLocation(locationResult)
            }
        }
    }

    private fun updateValuesFromBundle(savedInstanceState: Bundle?) {
        savedInstanceState ?: return
        if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
            requestingLocationUpdates = savedInstanceState.getBoolean(
                REQUESTING_LOCATION_UPDATES_KEY
            )
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.i(TAG, "map ready")
        mMap = googleMap
        fireDBHelper = FireDBHelper(mMap)
        fireDBHelper.initFirebaseDB()
        mMap.uiSettings.isZoomControlsEnabled = true
        initLocation()
        mMap.setOnMarkerClickListener { latLng -> handleMarkerClick(latLng) }
    }

    private fun handleMarkerClick(marker: Marker): Boolean {
        showBottomSheet(markers[marker]!!)
        return true
    }

    @SuppressLint("MissingPermission")
    private fun initLocation() {
        Log.i(TAG, "init location")
        if (checkPermissions()) {
            requestingLocationUpdates = true
            mMap.isMyLocationEnabled = true
            startLocationUpdates()
        }
    }

    private fun createLocationRequest(): LocationRequest {
        Log.i(TAG, "creating location request")
        return LocationRequest.create().apply {
            interval = 15000
            fastestInterval = 10000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun updateLocation(locationResult: LocationResult?) {
        Log.i(TAG, "updateLocation")
        locationResult ?: return
        Log.i(TAG, "got %d location(s)".format(locationResult.locations.size))
        if (locationResult.lastLocation != null) {
            val location = LatLng(
                locationResult.lastLocation.latitude,
                locationResult.lastLocation.longitude
            )
            if (lastLocation == null) {
                Toast.makeText(applicationContext, "location received", Toast.LENGTH_SHORT).show()
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 12f))
            }
            lastLocation = location
        }
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "ask for permissions")
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERM_LOCATION
            )
            return false
        }
        Log.i(TAG, "permissions exist")
        return true
    }

    private fun showBottomSheet(key: String) {
        val sheet = BottomSheetInfo().newInstance(key, this)
        sheet.show(this.supportFragmentManager, "Vehicle Info")
    }

    private fun showInfo() {
        showBottomSheet(currentVehicle!!)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_map, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.demo_data -> {
                if (vehicleTypes.isNotEmpty()) fireDBHelper.insertDemoData(lastLocation!!)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        if (requestingLocationUpdates) startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, requestingLocationUpdates)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val TAG = "ActivityMaps"

        lateinit var geocoder: Geocoder
        lateinit var fireDBHelper: FireDBHelper

        private const val REQUEST_PERM_LOCATION = 100
        private const val REQUESTING_LOCATION_UPDATES_KEY = "req-loc-upd"
    }

    override fun onVehicleRented(key: String) {
        currentVehicle = key
        fabInfo.isVisible = true
        markers.keys.forEach { m -> if (m.tag != key) m.isVisible = false }
    }

    override fun onVehicleReleased(key: String) {
        currentVehicle = null
        fabInfo.isVisible = false
        markers.keys.forEach { m -> m.isVisible = true }
    }
}
