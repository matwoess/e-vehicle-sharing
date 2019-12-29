package com.mathias.android.carcass

import android.net.Uri
import android.util.Log
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.mathias.android.carcass.model.AnimalType
import com.mathias.android.carcass.model.Carcass
import java.util.*
import kotlin.collections.HashMap

class FireDBHelper(map: GoogleMap) {
    private lateinit var mDBCarcassRef: DatabaseReference
    private lateinit var mDBAnimalTypeRef: DatabaseReference
    private lateinit var mDBStorage: FirebaseStorage
    private lateinit var mDBStorageRef: StorageReference
    private val mMap: GoogleMap = map

    fun initFirebaseDB() {
        Log.i(TAG, "initialize Firebase DB")
        mDBAnimalTypeRef = FirebaseDatabase.getInstance().reference.child("animalTypes")
        mDBCarcassRef = FirebaseDatabase.getInstance().reference.child("carcasses")
        mDBStorage = FirebaseStorage.getInstance()
        mDBStorageRef = mDBStorage.reference.child("images")
        mDBAnimalTypeRef.addChildEventListener(AnimalTypeListener())
        mDBCarcassRef.addChildEventListener(CarcassListener())
    }

    fun pushCarcass(carcass: Carcass, uri: Uri?): String {
        val ref = mDBCarcassRef.push()
        ref.setValue(carcass)
        val key = ref.key!!
        if (uri != null) storeImage(key, uri)
        return key
    }

    fun storeImage(key: String, image: Uri) {
        Log.i(TAG, "carcass has image, uploading to storage")
        mDBStorageRef.child(key).child(image.lastPathSegment!!).putFile(image)
            .addOnSuccessListener { t ->
                t.task.result.metadata!!.reference!!.downloadUrl
                    .addOnSuccessListener { r ->
                        carcasses[key]!!.url = r.toString()
                        updateCarcass(key, carcasses[key]!!)
                    }
            }
    }

    fun updateCarcass(key: String, carcass: Carcass): String {
        val ref = mDBCarcassRef.child(key)
        ref.setValue(carcass)
        return ref.key!!
    }

    fun removeCarcass(carcass: Carcass): Boolean {
        val entry = carcasses.entries.stream()
            .filter { e -> e.value == carcass }
            .findFirst()
            .orElse(null)
            ?: return false
        if (carcass.url != null) deleteImage(entry.key)
        return mDBCarcassRef.child(entry.key).removeValue().isSuccessful
    }

    fun deleteImage(key: String): Boolean {
        Log.d(TAG, "delete image at images/$key")
        val c = carcasses[key]!!
        if (c.url == null) return false
        val imgRef = mDBStorage.getReferenceFromUrl(c.url!!)
        Log.d(TAG, "imgRef: $imgRef")
        return imgRef.delete().isSuccessful
    }

    fun addAnimalType(animalType: AnimalType): String {
        val ref = mDBAnimalTypeRef.push()
        ref.setValue(animalType)
        return ref.key!!
    }

    fun addMarker(
        ref: String,
        carcass: Carcass
    ): Marker {
        val marker =
            mMap.addMarker(MarkerOptions().position(carcass.latLng()).title(carcass.type!!.name))
        marker.tag = ref
        markers[marker] = ref
        if (carcass.flagged) {
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
            for (j in 1..4) {
                var addLat = rand.nextDouble() * scale
                var addLng = rand.nextDouble() * scale
                if (i % 2 == 0) addLat *= -1
                if (j % 2 == 0) addLng *= -1
                val loc = LatLng(
                    userPos.latitude + addLat,
                    userPos.longitude + addLng
                )
                val type = animalTypes.values.elementAt(j - 1)
                val c = Carcass(type, "a dead ${type.name}", Date().time, loc)
                val ref = pushCarcass(c, null) // TODO
            }
        }
    }

    inner class AnimalTypeListener : ChildEventListener {

        override fun onCancelled(error: DatabaseError) {
            Log.w("Failed to read value.", error.toException())
        }

        override fun onChildMoved(snapshot: DataSnapshot, prevChild: String?) {
            Log.d(TAG, "onChildMoved: $snapshot, $prevChild")
        }

        override fun onChildChanged(snapshot: DataSnapshot, prevChild: String?) {
            Log.d(TAG, "onChildChanged: $snapshot, $prevChild")
            if (animalTypes.containsKey(snapshot.key)) {
                Log.i(TAG, "update entry")
                val c = snapshot.getValue(AnimalType::class.java)!!
                animalTypes[snapshot.key!!]?.updateValues(c)
            }
        }

        override fun onChildAdded(snapshot: DataSnapshot, prevChild: String?) {
            Log.d(TAG, "onChildAdded: $snapshot, $prevChild")
            if (!animalTypes.containsKey(snapshot.key)) {
                Log.i(TAG, "add new entry")
                val c = snapshot.getValue(AnimalType::class.java)!!
                animalTypes[snapshot.key!!] = c
            }
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {
            Log.d(TAG, "onChildRemoved: $snapshot")
            if (animalTypes.containsKey(snapshot.key)) {
                Log.i(TAG, "remove entry")
                animalTypes.remove(snapshot.key)
            }
        }
    }

    inner class CarcassListener : ChildEventListener {

        override fun onCancelled(error: DatabaseError) {
            Log.w("Failed to read value.", error.toException())
        }

        override fun onChildMoved(snapshot: DataSnapshot, prevChild: String?) {
            Log.d(TAG, "onChildMoved: $snapshot, $prevChild")
        }

        override fun onChildChanged(snapshot: DataSnapshot, prevChild: String?) {
            Log.d(TAG, "onChildChanged: $snapshot, $prevChild")
            if (carcasses.containsKey(snapshot.key)) {
                Log.i(TAG, "update entry")
                val c = snapshot.getValue(Carcass::class.java)!!
                carcasses[snapshot.key!!]!!.updateValues(c)
                updateMarker(snapshot.key!!)
            }
        }

        override fun onChildAdded(snapshot: DataSnapshot, prevChild: String?) {
            Log.d(TAG, "onChildAdded: $snapshot, $prevChild")
            if (!carcasses.containsKey(snapshot.key)) {
                Log.i(TAG, "add new entry")
                val c = snapshot.getValue(Carcass::class.java)!!
                carcasses[snapshot.key!!] = c
                addMarker(snapshot.key!!, c)
            }
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {
            Log.d(TAG, "onChildRemoved: $snapshot")
            if (carcasses.containsKey(snapshot.key)) {
                Log.i(TAG, "remove entry")
                val c = snapshot.getValue(Carcass::class.java)!!
                carcasses.remove(snapshot.key)
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
        Log.i(TAG, "flagged = ${carcasses[key]!!.flagged}")
        if (carcasses[key]!!.flagged) {
            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.yellow_dot))
        } else {
            marker.setIcon(null) // reset to default icon
        }
    }

    companion object {
        private const val TAG = "FireDBHelper"
        var carcasses: HashMap<String, Carcass> = HashMap()
        var markers: HashMap<Marker, String> = HashMap()
        var animalTypes: HashMap<String, AnimalType> = HashMap()
    }

}