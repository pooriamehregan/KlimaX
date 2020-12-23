package com.example.airboyz.ui.map.func

import com.example.airboyz.dataclasses.NiluStation
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

// Custom cluster class
class ClusterMarker(val station: NiluStation) : ClusterItem {

    override fun getSnippet(): String {
        return ""
    }

    override fun getTitle(): String {
        return station.station
    }

    override fun getPosition(): LatLng {
        return LatLng(station.latitude, station.longitude)
    }

}