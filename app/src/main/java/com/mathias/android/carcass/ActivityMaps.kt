package com.mathias.android.carcass

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mathias.android.carcass.ActivityEdit.Companion.CARCASS_BUNDLE
import com.mathias.android.carcass.ActivityEdit.Companion.CARCASS_DESCRIPTION
import com.mathias.android.carcass.ActivityEdit.Companion.CARCASS_IMAGE_PATH
import com.mathias.android.carcass.ActivityEdit.Companion.CARCASS_LOCATION_LAT
import com.mathias.android.carcass.ActivityEdit.Companion.CARCASS_LOCATION_LNG
import com.mathias.android.carcass.ActivityEdit.Companion.CARCASS_TIME
import com.mathias.android.carcass.ActivityEdit.Companion.CARCASS_TYPE
import com.mathias.android.carcass.ActivityEdit.Companion.EXISTING_KEY
import com.mathias.android.carcass.FireDBHelper.Companion.animalTypes
import com.mathias.android.carcass.FireDBHelper.Companion.markers
import com.mathias.android.carcass.model.AnimalType
import com.mathias.android.carcass.model.Carcass
import com.mathias.android.carcass.sheets.BottomSheetInfo
import java.io.File
import java.util.*

class ActivityMaps : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var requestingLocationUpdates: Boolean = true
    private lateinit var mFab: FloatingActionButton
    private var lastLocation: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateValuesFromBundle(savedInstanceState)
        setContentView(R.layout.activity_maps)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        mFab = findViewById(R.id.floatingActionButton)
        mFab.setOnClickListener { handleFabClick() }
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
        mMap.setOnMarkerClickListener { latLng -> handleMarkerClick(mMap, latLng) }
    }

    private fun handleFabClick() {
        val intent = Intent(this, ActivityEdit::class.java).apply {
            putExtra(CARCASS_LOCATION_LAT, lastLocation?.latitude)
            putExtra(CARCASS_LOCATION_LNG, lastLocation?.longitude)
        }
        startActivityForResult(intent, ADD_REQUEST_CODE)
    }

    private fun handleMarkerClick(mMap: GoogleMap, marker: Marker): Boolean {
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
        val sheet = BottomSheetInfo().newInstance(key)
        sheet.show(this.supportFragmentManager, "Carcass Info")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_map, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.demo_data -> {
                if (animalTypes.isNotEmpty()) fireDBHelper.insertDemoData(lastLocation!!)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.i(TAG, "onActivityResult")
        if (requestCode == ADD_REQUEST_CODE && resultCode == RESULT_OK) {
            Log.i(TAG, "result is OK")
            if (data == null) return
            val bundle = data.getBundleExtra(CARCASS_BUNDLE) ?: return
            Log.i(TAG, "get carcass data")
            val new = getCarcassFromBundle(bundle)
            val img = bundle.getString(CARCASS_IMAGE_PATH)
            var uri: Uri? = null
            if (img != null && img.isNotEmpty()) {
                uri = File(img).toUri()
            }
            Log.i(TAG, "create new entry in DB")
            fireDBHelper.pushCarcass(new, uri)
        } else if (requestCode == EDIT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Log.i(TAG, "result is OK")
            if (data == null) return
            val bundle = data.getBundleExtra(CARCASS_BUNDLE) ?: return
            Log.i(TAG, "get carcass data")
            val updated = getCarcassFromBundle(bundle)
            Log.i(TAG, "update existing entry in DB")
            val key = bundle.getString(EXISTING_KEY)
            Log.i(TAG, "key = $key")
            updated.flagged = false // reset flagged state after editing
            fireDBHelper.updateCarcass(key!!, updated)
            val img = bundle.getString(CARCASS_IMAGE_PATH)
            if (img != null && img.isNotEmpty()) {
                Log.i(TAG, "update image")
                fireDBHelper.deleteImage(key)
                val uri = File(img).toUri()
                fireDBHelper.storeImage(key, uri)
            }
        }
    }

    private fun getCarcassFromBundle(bundle: Bundle): Carcass {
        val type = bundle.getString(CARCASS_TYPE)
        val description = bundle.getString(CARCASS_DESCRIPTION)
        val time = bundle.getLong(CARCASS_TIME)
        val lat = bundle.getDouble(CARCASS_LOCATION_LAT)
        val lng = bundle.getDouble(CARCASS_LOCATION_LNG)
        var animalType = animalTypes.values.stream()
            .filter { t -> t.name == type }
            .findFirst()
            .orElse(null)
        if (animalType == null) animalType = AnimalType(type!!)
        return Carcass(animalType, description, time, LatLng(lat, lng))
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
        private const val ADD_REQUEST_CODE = 200
        private const val REQUESTING_LOCATION_UPDATES_KEY = "req-loc-upd"
        const val EDIT_REQUEST_CODE = 210
    }
}
