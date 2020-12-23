package com.example.airboyz.ui.map.func

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.example.airboyz.dataclasses.NiluStation
import com.example.airboyz.dataclasses.NiluStationComp
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.gson.responseObject


fun getAllStations(): List<NiluStation> {
    val url = "https://api.nilu.no/lookup/stations?utd=true"
    val (_, _, result) = Fuel.get(url)
        .responseObject<List<NiluStation>>()
    var (list, error) = result
    if (error != null) {
        Log.e("Airboiz", "oof1 ")
        error.printStackTrace()
    }
    return if (list != null) {
        list
    } else {
        // Empty list to handle exception
        list = mutableListOf()
        list
    }


}

fun getComponentsString(station: NiluStation, mApp: Context): Pair<LinearLayout, LinearLayout> {

    val url = "https://api.nilu.no/obs/utd?stations=${station.station}"
    val (_, _, result) = Fuel.get(url)
        .responseObject<List<NiluStationComp>>()
    val (list, error) = result
    if (error != null) {
        Log.e("Airboiz", "oof1 ")
        error.printStackTrace()
    }
    val lls = Pair(LinearLayout(mApp), LinearLayout(mApp))

    list?.forEach { m ->
        var tv = TextView(mApp)
        tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
        var str = "${m.component}:"
        tv.text = str
        lls.first.addView(tv)

        tv = TextView(mApp)
        tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
        str = "${round(m.value)} ${m.unit}"
        tv.text = str
        lls.second.addView(tv)
    }




    return lls
}

fun getComponentsString(
    stations: List<ClusterMarker>,
    mApp: Context
): Pair<LinearLayout, LinearLayout> {

    val values = hashMapOf<String, Pair<MutableList<Double>, String>>()

    stations.forEach {
        val url = "https://api.nilu.no/obs/utd?stations=${it.station.station}"
        val (_, _, result) = Fuel.get(url)
            .responseObject<List<NiluStationComp>>()
        val (list, error) = result
        if (error != null) {
            Log.e("Airboiz", "oof1 ")
            error.printStackTrace()
        }
        list?.forEach { c ->
            if (values.containsKey(c.component)) {
                (values[c.component] as Pair<MutableList<Double>, String>).first.add(c.value)
            } else {
                values[c.component] = Pair(mutableListOf(c.value), c.unit)
            }
        }
    }

    val lls = Pair(LinearLayout(mApp), LinearLayout(mApp))
    values.forEach { c ->
        var tv = TextView(mApp)
        val value = c.value.first.average()
        tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
        var str = "${c.key}:"
        tv.text = str
        lls.first.addView(tv)

        tv = TextView(mApp)
        tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
        str = "${round(value)} ${c.value.second}"
        tv.text = str
        lls.second.addView(tv)

    }

    return lls
}


fun round(v: Double): Float {
    return ((v * 10).toInt()) / 10f
}