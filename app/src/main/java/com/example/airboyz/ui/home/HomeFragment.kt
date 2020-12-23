package com.example.airboyz.ui.home

import android.content.Context
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import com.example.airboyz.MainActivity
import com.example.airboyz.R
import com.example.airboyz.dataclasses.Place
import com.example.airboyz.ui.map.SeeMoreFragment
import com.example.airboyz.ui.map.func.FragmentHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.cardview_home.view.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.my_places_dialog_box.view.*

class HomeFragment : Fragment(), FragmentHelper {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var root: View
    override lateinit var mApp: MainActivity
    private var seeMoreFrg: SeeMoreFragment? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mApp = activity as MainActivity
        fusedLocationClient = mApp.fusedProvider
        root = inflater.inflate(R.layout.fragment_home, container, false)
        row1 = root.findViewById(R.id.home_row1)
        row2 = root.findViewById(R.id.home_row2)
        sv = root.findViewById(R.id.home_sw1)
        errll = root.findViewById(R.id.err_ll)
        addMyPlaces()



        addMyLocation()
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        home_sw1?.let {
            it.setOnTouchListener { view, motionEvent ->
                if (motionEvent.action == MotionEvent.ACTION_UP) {
                    colorAllCards()
                }
                false
            }
        }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        addMyPlaces()
        super.onResume()
    }
    private var row: Int = 1
    private var sv: ViewGroup? = null
    private var row1: ViewGroup? = null
    private var row2: ViewGroup? = null
    private var errll: ViewGroup? = null

    private fun addMyPlaces(){
        row1?.removeAllViewsInLayout()
        row2?.removeAllViewsInLayout()
        errll?.removeAllViewsInLayout()
        row = 1
        cards.clear()
        for (p in mApp.myPlaces){
            addInfoCard(p
            )
        }
        if (mApp.myPlaces.isEmpty()) {
            val tv = TextView(mApp)
            tv.text = getString(R.string.no_places)
            tv.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.colorText,
                    null
                )
            )
            tv.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            errll?.addView(tv)
        }
    }

    private fun addInfoCard(p:Place){
        val p0 = p.latLng
        val label = p.name
        val v = mApp.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val card = v.inflate(R.layout.cardview_home, null)
        card.cardview_home_name.text = label
        val ll = card.cardview_home_ll
        card.setOnClickListener {
            mApp.runOnUiThread {
                showSeeMore(p0, p.area, label)
            }

        }
        card.setOnLongClickListener {
            showChangeDialog(p)
            true

        }
        card.setOnTouchListener { _ , event ->
            when(event.action){
                MotionEvent.ACTION_DOWN -> {
                    ll.setBackgroundColor(
                        ResourcesCompat.getColor(
                            resources,
                            R.color.navigationTabs,
                            null
                        )
                    )
                    card.invalidate()
                    }
                MotionEvent.ACTION_UP -> {
                    ll.setBackgroundColor(
                        ResourcesCompat.getColor(
                            resources,
                            R.color.colorButton,
                            null
                        )
                    )
                    card.invalidate()

                }

            }
            false
        }
        val activeRow = if (row == 1) {
            row = 2
            row1
        } else {
            row = 1
            row2
        }

        // If more than 3 cards, restrict height
        sv?.let {
            sv!!.measure(0, 0)
            card.measure(0, 0)
            sv!!.layoutParams = if (sv!!.measuredHeight >= card.measuredHeight * 3) {
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, card.measuredHeight * 3
                )
            } else {
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

        }



        activeRow?.addView(card)
        cards.add(card)
    }

    private val cards = mutableListOf<View>()
    private fun colorAllCards() {
        cards.forEach lit@{
            val ll: LinearLayout? = it.cardview_home_ll
            ll ?: return@lit
            ll.setBackgroundColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.colorButton,
                    null
                )
            )
            it.invalidate()
        }
    }

    private fun showChangeDialog(p: Place) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(mApp)
        val vg = layoutInflater.inflate(R.layout.my_places_dialog_box, null)
        vg.mpdb_editText.setText(p.name)
        val classes = listOf("grunnkrets", "delomrade", "kommune")
        val i = classes.indexOf(p.area)
        vg.mpdb_spinner.setSelection(i)
        builder.setView(vg)



        builder.setTitle(p.name)
        builder.setMessage("${getString(R.string.edit_or_delete)} '${p.name}'?")

        builder.setPositiveButton(
            getString(R.string.remove_place)
        ) { dialog, _ -> // Do nothing but close the dialog
            val builder2: AlertDialog.Builder = AlertDialog.Builder(mApp)
            builder2.setTitle("${getString(R.string.remove_place)} '${p.name}'?")
            builder2.setNegativeButton(getString(R.string.cancel)) { d, _ ->
                d.dismiss()
            }
            builder2.setPositiveButton(getString(R.string.remove_place)) { d, _ ->
                mApp.delPlace(p)
                addMyPlaces()
                d.dismiss()
            }
            val alert2: AlertDialog = builder2.create()
            alert2.show()
        }


        builder.setNeutralButton(
            getText(R.string.update)
        ) { dialog, _ -> // Do nothing but close the dialog
            val name = vg.mpdb_editText.text.toString()
            if (name != ""){
                mApp.replacePlace(
                    p,
                    Place(classes[vg.mpdb_spinner.selectedItemPosition], name, p.latLng)
                )
                addMyPlaces()
            }

            dialog.dismiss()
        }

        builder.setNegativeButton(
            getString(R.string.cancel)
        ) { dialog, _ -> // Do nothing
            dialog.dismiss()
        }

        val alert: AlertDialog = builder.create()
        alert.show()
    }

    private fun addMyLocation() {
        val card = root.findViewById<View>(R.id.home_my_location)
        val txt = card.findViewById<TextView>(R.id.cardview_home_name)
        txt.text = getString(R.string.my_position)
        card.setOnClickListener {
            if (mApp.permissionGranted()) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        showSeeMore(LatLng(location.latitude, location.longitude), "grunnkrets", "")
                    } else {
                        Toast.makeText(mApp, getString(R.string.gps_error), Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } else {
                Toast.makeText(mApp, getString(R.string.gps_not_enabled), Toast.LENGTH_SHORT).show()
            }
        }
        val ll = card.findViewById<ViewGroup>(R.id.cardview_home_ll)
        card.setOnTouchListener { _ , event ->
            when(event.action){
                MotionEvent.ACTION_DOWN -> {
                    ll.setBackgroundColor(
                        ResourcesCompat.getColor(
                            resources,
                            R.color.navigationTabs,
                            null
                        )
                    )
                    card.invalidate()
                }
                MotionEvent.ACTION_UP -> {
                    ll.setBackgroundColor(
                        ResourcesCompat.getColor(
                            resources,
                            R.color.colorButton,
                            null
                        )
                    )
                    card.invalidate()
                }

            }
            false
        }


    }


    private fun showSeeMore(p0: LatLng, areaclass :String, place: String){
        val frg: SeeMoreFragment = SeeMoreFragment.newInstance(p0,areaclass, place, this)
        seeMoreFrg = frg
        val trn = childFragmentManager.beginTransaction()
        trn
            .replace(R.id.home_frg_container, frg)
            .addToBackStack(null)
            .commit()
        root.findViewById<ImageView>(R.id.home_image).visibility = View.INVISIBLE
        root.findViewById<TextView>(R.id.home_text).visibility = View.INVISIBLE
        root.findViewById<LinearLayout>(R.id.home_container).visibility= View.INVISIBLE
        root.findViewById<FragmentContainerView>(R.id.home_frg_container).visibility = View.VISIBLE
    }

    override fun removeSeeMore(){
        addMyPlaces()
        seeMoreFrg?.let{frg ->
            frg.reset()
            root.findViewById<ImageView>(R.id.home_image).visibility = View.VISIBLE
            root.findViewById<TextView>(R.id.home_text).visibility = View.VISIBLE
            root.findViewById<FragmentContainerView>(R.id.home_frg_container).visibility = View.INVISIBLE
            root.findViewById<LinearLayout>(R.id.home_container).visibility= View.VISIBLE
        }

    }
}
