package com.mathias.android.carcass.sheets

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.location.Address
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mathias.android.carcass.ActivityEdit
import com.mathias.android.carcass.ActivityEdit.Companion.EXISTING_KEY
import com.mathias.android.carcass.ActivityMaps.Companion.EDIT_REQUEST_CODE
import com.mathias.android.carcass.ActivityMaps.Companion.fireDBHelper
import com.mathias.android.carcass.ActivityMaps.Companion.geocoder
import com.mathias.android.carcass.FireDBHelper.Companion.carcasses
import com.mathias.android.carcass.R
import com.mathias.android.carcass.model.Carcass
import java.text.SimpleDateFormat
import java.util.*


class BottomSheetInfo : BottomSheetDialogFragment() {
    private lateinit var txtType: TextView
    private lateinit var txtDescription: TextView
    private lateinit var txtReported: TextView
    private lateinit var txtLocation: TextView
    private lateinit var btnShowPicture: Button
    private lateinit var btnEdit: Button
    private lateinit var btnRemove: Button
    private lateinit var btnReport: Button

    private lateinit var key: String
    private lateinit var carcass: Carcass

    internal fun newInstance(key: String): BottomSheetInfo {
        return BottomSheetInfo().apply {
            this.key = key
            this.carcass = carcasses[key]!!
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
        txtReported = view.findViewById(R.id.txt_reported)
        txtLocation = view.findViewById(R.id.txt_location)
        btnShowPicture = view.findViewById(R.id.btn_show_picture)
        txtType.text = carcass.type?.name
        txtDescription.text = carcass.description
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        txtReported.text = dateFormat.format(carcass.reportedAt!!)
        Log.i(TAG, geocoder.toString())
        val addresses: List<Address> = geocoder.getFromLocation(
            carcass.location.lat,
            carcass.location.lng,
            1
        )
        txtLocation.text = if (addresses.isNotEmpty()) addresses[0].thoroughfare else "N/A"
    }

    private fun updateUI() {
        btnShowPicture.visibility = if (carcass.url != null) VISIBLE else GONE
        btnRemove.isEnabled = carcass.flagged
        btnReport.isEnabled = !carcass.flagged
    }

    private fun initButtons(view: View) {
        btnRemove = view.findViewById(R.id.btn_remove)
        btnEdit = view.findViewById(R.id.btn_edit)
        btnReport = view.findViewById(R.id.btn_report)
        btnShowPicture.setOnClickListener { showPicture(view) }
        btnRemove.setOnClickListener { onDeleteCarcass() }
        btnEdit.setOnClickListener { editCarcass() }
        btnReport.setOnClickListener { onReportCarcass() }
        updateUI()
    }

    private fun onReportCarcass() {
        AlertDialog.Builder(context)
            .setTitle("Report")
            .setMessage("Do you want to flag this carcass location as out of date or removed?")
            .setPositiveButton(android.R.string.yes) { _, _ -> flagCarcass() }
            .setNegativeButton(android.R.string.no, null).show()
    }

    private fun flagCarcass() {
        carcass.flagged = true
        fireDBHelper.updateCarcass(key, carcass)
        updateUI()
    }

    private fun editCarcass() {
        val intent = Intent(activity, ActivityEdit::class.java).apply {
            putExtra(EXISTING_KEY, key)
        }
        this.dismiss()
        activity?.startActivityForResult(intent, EDIT_REQUEST_CODE)
    }

    private fun onDeleteCarcass() {
        AlertDialog.Builder(context)
            .setTitle("Remove")
            .setMessage("Do you want to permanently delete this location?")
            .setPositiveButton(android.R.string.yes) { _, _ -> deleteCarcass() }
            .setNegativeButton(android.R.string.no, null).show()
    }

    private fun deleteCarcass() {
        fireDBHelper.removeCarcass(carcass)
        this.dismiss()
    }

    private fun showPicture(view: View) {
        val imageDialog = Dialog(view.context)
        imageDialog.window!!.requestFeature(Window.FEATURE_NO_TITLE)
        imageDialog.setContentView(
            layoutInflater.inflate(
                R.layout.image_view
                , null
            )
        )
        imageDialog.show()
        if (carcass.url != null) {
            val imageView = imageDialog.findViewById(R.id.image_view) as ImageView
            Glide.with(this).load(carcass.url).into(imageView)
        }
    }

    companion object {
        private const val TAG = "BottomSheetInfo"
    }
}