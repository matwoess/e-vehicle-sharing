package com.mathias.android.hitchhike

import android.util.Log
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*
import com.mathias.android.hitchhike.model.Specifications
import com.mathias.android.hitchhike.model.VehicleType
import com.mathias.android.hitchhike.model.Vehicle
import java.util.*
import kotlin.collections.HashMap

class FireDBHelper(map: GoogleMap) {
    private lateinit var mDBVehiclesRef: DatabaseReference
    private lateinit var mDBVehicleTypesRef: DatabaseReference
    private val mMap: GoogleMap = map

    fun initFirebaseDB() {
        Log.i(TAG, "initialize Firebase DB")
        mDBVehicleTypesRef = FirebaseDatabase.getInstance().reference.child("vehicleTypes")
        mDBVehiclesRef = FirebaseDatabase.getInstance().reference.child("vehicles")
        mDBVehicleTypesRef.addChildEventListener(VehicleTypeListener())
        mDBVehiclesRef.addChildEventListener(VehicleListener())
    }

    fun pushVehicle(vehicle: Vehicle): String {
        val ref = mDBVehiclesRef.push()
        ref.setValue(vehicle)
        return ref.key!!
    }

    fun updateVehicle(key: String, vehicle: Vehicle): String {
        val ref = mDBVehiclesRef.child(key)
        ref.setValue(vehicle)
        return ref.key!!
    }

    fun removeVehicle(vehicle: Vehicle): Boolean {
        val entry = vehicles.entries.stream()
            .filter { e -> e.value == vehicle }
            .findFirst()
            .orElse(null)
            ?: return false
        return mDBVehiclesRef.child(entry.key).removeValue().isSuccessful
    }

    fun addVehicleType(vehicleType: VehicleType): String {
        val ref = mDBVehicleTypesRef.push()
        ref.setValue(vehicleType)
        return ref.key!!
    }

    fun addMarker(
        ref: String,
        vehicle: Vehicle
    ): Marker {
        val marker =
            mMap.addMarker(MarkerOptions().position(vehicle.latLng()).title(vehicle.type!!.name))
        marker.tag = ref
        markers[marker] = ref
        if (vehicle.rented) {
            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.yellow_dot))
        }
        return marker
    }

    private fun removeMarker(key: String) {
        val marker = markers.keys.stream()
            .filter { m -> key == m.tag }
            .findFirst()
            .orElse(null)
        markers.remove(marker)
        marker.remove()
    }

    fun insertDemoData(userPos: LatLng) {
        Log.i(TAG, "insertDemoData")

        val rand = Random()
        val scale = 1 / 80.0
        for (i in 1..2) {
            for (j in 1..3) {
                var addLat = rand.nextDouble() * scale
                var addLng = rand.nextDouble() * scale
                if (i % 2 == 0) addLat *= -1
                if (j % 2 == 0) addLng *= -1
                val loc = LatLng(
                    userPos.latitude + addLat,
                    userPos.longitude + addLng
                )
                val type = vehicleTypes.values.elementAt(j - 1)
                val size = Specifications(1.0f, 1.0f, 1.0f, 1.0f)
                val v = Vehicle(type, size, "an ${type.name}", rand.nextInt(100), loc)
                val ref = pushVehicle(v)
            }
        }
    }

    inner class VehicleTypeListener : ChildEventListener {

        override fun onCancelled(error: DatabaseError) {
            Log.w("Failed to read value.", error.toException())
        }

        override fun onChildMoved(snapshot: DataSnapshot, prevChild: String?) {
            Log.d(TAG, "onChildMoved: $snapshot, $prevChild")
        }

        override fun onChildChanged(snapshot: DataSnapshot, prevChild: String?) {
            Log.d(TAG, "onChildChanged: $snapshot, $prevChild")
            if (vehicleTypes.containsKey(snapshot.key)) {
                Log.i(TAG, "update entry")
                val c = snapshot.getValue(VehicleType::class.java)!!
                vehicleTypes[snapshot.key!!]?.updateValues(c)
            }
        }

        override fun onChildAdded(snapshot: DataSnapshot, prevChild: String?) {
            Log.d(TAG, "onChildAdded: $snapshot, $prevChild")
            if (!vehicleTypes.containsKey(snapshot.key)) {
                Log.i(TAG, "add new entry")
                val c = snapshot.getValue(VehicleType::class.java)!!
                vehicleTypes[snapshot.key!!] = c
            }
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {
            Log.d(TAG, "onChildRemoved: $snapshot")
            if (vehicleTypes.containsKey(snapshot.key)) {
                Log.i(TAG, "remove entry")
                vehicleTypes.remove(snapshot.key)
            }
        }
    }

    inner class VehicleListener : ChildEventListener {

        override fun onCancelled(error: DatabaseError) {
            Log.w("Failed to read value.", error.toException())
        }

        override fun onChildMoved(snapshot: DataSnapshot, prevChild: String?) {
            Log.d(TAG, "onChildMoved: $snapshot, $prevChild")
        }

        override fun onChildChanged(snapshot: DataSnapshot, prevChild: String?) {
            Log.d(TAG, "onChildChanged: $snapshot, $prevChild")
            if (vehicles.containsKey(snapshot.key)) {
                Log.i(TAG, "update entry")
                val c = snapshot.getValue(Vehicle::class.java)!!
                vehicles[snapshot.key!!]!!.updateValues(c)
                updateMarker(snapshot.key!!)
            }
        }

        override fun onChildAdded(snapshot: DataSnapshot, prevChild: String?) {
            Log.d(TAG, "onChildAdded: $snapshot, $prevChild")
            if (!vehicles.containsKey(snapshot.key)) {
                Log.i(TAG, "add new entry")
                val c = snapshot.getValue(Vehicle::class.java)!!
                vehicles[snapshot.key!!] = c
                addMarker(snapshot.key!!, c)
            }
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {
            Log.d(TAG, "onChildRemoved: $snapshot")
            if (vehicles.containsKey(snapshot.key)) {
                Log.i(TAG, "remove entry")
                val c = snapshot.getValue(Vehicle::class.java)!!
                vehicles.remove(snapshot.key)
                removeMarker(snapshot.key!!)
            }
        }
    }

    private fun updateMarker(key: String) {
        Log.i(TAG, "update marker")
        val marker = markers.keys.stream()
            .filter { m -> key == m.tag }
            .findFirst()
            .orElse(null)
            ?: return
        Log.i(TAG, "rented = ${vehicles[key]!!.rented}")
        var drawable = -1
        val vehicle = vehicles[key]!!
        if (vehicle.rented) {
            drawable = if (vehicle.locked) R.drawable.blue_dot else R.drawable.yellow_dot
            if (vehicle.alarm) drawable = R.drawable.purple_dot
        }
        if (drawable != -1) {
            marker.setIcon(BitmapDescriptorFactory.fromResource(drawable))
        } else {
            marker.setIcon(null) // reset to default icon
        }
    }

    companion object {
        private const val TAG = "FireDBHelper"
        var vehicles: HashMap<String, Vehicle> = HashMap()
        var markers: HashMap<Marker, String> = HashMap()
        var vehicleTypes: HashMap<String, VehicleType> = HashMap()
    }

}