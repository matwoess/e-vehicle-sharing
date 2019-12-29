package com.mathias.android.carcass

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.location.Address
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.google.android.gms.maps.model.LatLng
import com.mathias.android.carcass.ActivityMaps.Companion.geocoder
import com.mathias.android.carcass.FireDBHelper.Companion.animalTypes
import com.mathias.android.carcass.FireDBHelper.Companion.carcasses
import com.mathias.android.carcass.model.AnimalType
import com.mathias.android.carcass.sheets.BottomSheetAnimalType
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min


class ActivityEdit : AppCompatActivity(), IBottomSheetAnimalTypeListener {
    private lateinit var spnType: Spinner
    private lateinit var txtDescription: TextView
    private lateinit var txtTime: TextView
    private lateinit var txtLocation: TextView
    private lateinit var btnTakePicture: Button
    private lateinit var btnDone: Button
    private lateinit var imageView: ImageView

    private lateinit var location: LatLng
    private var reportedAt: Long = 0L
    private var description: String? = null
    private var animalType: AnimalType? = null
    private var existingKey: String? = null
    private var imageUrl: String? = null

    private var currentPhotoPath: String = ""

    private var previousIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        existingKey = intent.getStringExtra(EXISTING_KEY)
        if (existingKey == null) {
            Log.i(TAG, "new entry")
            val lat = intent.getDoubleExtra(CARCASS_LOCATION_LAT, .0)
            val lng = intent.getDoubleExtra(CARCASS_LOCATION_LNG, .0)
            Log.i(TAG, lat.toString())
            Log.i(TAG, lng.toString())
            location = LatLng(lat, lng)
            reportedAt = Date().time
        } else {
            Log.i(TAG, "edit entry")
            val c = carcasses[existingKey!!]!!
            this.description = c.description
            this.reportedAt = c.reportedAt!!
            this.animalType = c.type
            this.location = c.latLng()
            this.imageUrl = c.url
        }
        initUI()
        initButtons()
    }

    private fun initUI() {
        spnType = findViewById(R.id.spn_animal_type)
        txtDescription = findViewById(R.id.txt_animal_description)
        txtTime = findViewById(R.id.txt_current_time)
        txtLocation = findViewById(R.id.txt_current_location)
        initSpinner()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        txtTime.text = dateFormat.format(reportedAt)
        Log.i(TAG, geocoder.toString())
        val addresses: List<Address> = geocoder.getFromLocation(
            location.latitude,
            location.longitude,
            1
        )
        txtLocation.text = if (addresses.isNotEmpty()) addresses[0].thoroughfare else "N/A"
        txtDescription.text = description
        txtDescription.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
            if (!hasFocus) {
                val imm: InputMethodManager =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.applicationWindowToken, 0)
            }
        }
        imageView = findViewById(R.id.img_view_report)
        if (imageUrl != null) {
            Glide.with(this).load(imageUrl).into(imageView)
        }
    }

    private fun initSpinner() {
        val adapter =
            ArrayAdapter<CharSequence>(this, R.layout.support_simple_spinner_dropdown_item)
        animalTypes.values.forEach { t -> adapter.add(t.name) }
        adapter.add("Other...")
        spnType.adapter = adapter
        if (animalType != null) {
            val idx = adapter.getPosition(animalType?.name)
            spnType.setSelection(idx)
        }
        spnType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>?,
                selectedItemView: View,
                position: Int,
                id: Long
            ) {
                if (parentView!!.selectedItem.toString() == "Other...") {
                    addAnimalType()
                } else {
                    previousIndex = position
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {}
        }
    }

    private fun addAnimalType() {
        Log.i(TAG, "add animal type")
        val sheet = BottomSheetAnimalType().newInstance()
        sheet.show(supportFragmentManager, "New animal type")
    }

    override fun onAnimalTypeSaved(name: String) {
        Log.i(TAG, "received new type")
        ActivityMaps.fireDBHelper.addAnimalType(AnimalType(name))
        val adapter: ArrayAdapter<CharSequence> = spnType.adapter as ArrayAdapter<CharSequence>
        adapter.insert(name, 0)
        spnType.setSelection(0)
    }

    override fun onDismiss(added: Boolean) {
        if (!added) {
            spnType.setSelection(previousIndex)
        }
    }


    private fun initButtons() {
        btnTakePicture = findViewById(R.id.btn_take_picture)
        btnDone = findViewById(R.id.btn_done)
        btnTakePicture.text = if (imageUrl != null) "Edit picture" else "Take picture"
        btnTakePicture.setOnClickListener { dispatchTakePictureIntent() }
        btnDone.setOnClickListener { finishAndReturn() }
    }

    private fun finishAndReturn() {
        val type = spnType.selectedItem.toString()
        val description = txtDescription.text.toString()
        val returnIntent = Intent()
        val bundle = Bundle()
        bundle.putString(CARCASS_TYPE, type)
        bundle.putString(CARCASS_DESCRIPTION, description)
        bundle.putLong(CARCASS_TIME, reportedAt)
        bundle.putDouble(CARCASS_LOCATION_LAT, location.latitude)
        bundle.putDouble(CARCASS_LOCATION_LNG, location.longitude)
        bundle.putString(CARCASS_IMAGE_PATH, currentPhotoPath)
        Log.i(TAG, "putting string extra $existingKey")
        bundle.putString(EXISTING_KEY, existingKey)
        returnIntent.putExtra(CARCASS_BUNDLE, bundle)
        setResult(RESULT_OK, returnIntent)
        finish()
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    Log.e(TAG, "error creating image file")
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.mathias.android.carcass",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.i(TAG, "picture taken")
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) setPic()
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun setPic() {
        val targetW: Int = imageView.width
        val targetH: Int = imageView.height
        val bmOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            val photoW: Int = outWidth
            val photoH: Int = outHeight
            val scaleFactor: Int = min(photoW / targetW, photoH / targetH)
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
        }
        BitmapFactory.decodeFile(currentPhotoPath, bmOptions)?.also { bitmap ->
            imageView.setImageBitmap(bitmap)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        private const val TAG = "AddActivity"
        private const val REQUEST_TAKE_PHOTO = 220
        const val EXISTING_KEY = "KEY"
        const val CARCASS_BUNDLE = "BUNDLE"
        const val CARCASS_TYPE = "TYPE"
        const val CARCASS_DESCRIPTION = "DESCRIPTION"
        const val CARCASS_TIME = "TIME"
        const val CARCASS_LOCATION_LAT = "LOCATION_LAT"
        const val CARCASS_LOCATION_LNG = "LOCATION_LNG"
        const val CARCASS_IMAGE_PATH = "IMAGE_PATH"
    }
}
