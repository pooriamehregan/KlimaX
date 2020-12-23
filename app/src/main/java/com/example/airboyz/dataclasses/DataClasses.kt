package com.example.airboyz.dataclasses

import com.google.android.gms.maps.model.LatLng
import com.google.gson.annotations.SerializedName

// Made with json2kotlin
data class Times (
    @SerializedName("reftimes") val reftimes : List<String>
)

// Mega class
// Contains nulls on api calls
// Beware
data class Variables(

    @SerializedName("AQI_pm25") val aQI_pm25: AQI,
    @SerializedName("o3_local_fraction_shipping") val o3_local_fraction_shipping: CValue,
    @SerializedName("no2_local_fraction_shipping") val no2_local_fraction_shipping: CValue,
    @SerializedName("no2_nonlocal_fraction_seasalt") val no2_nonlocal_fraction_seasalt: CValue,
    @SerializedName("pm25_nonlocal_fraction_seasalt") val pm25_nonlocal_fraction_seasalt: CValue,
    @SerializedName("no2_local_fraction_industry") val no2_local_fraction_industry: CValue,
    @SerializedName("AQI_pm10") val aQI_pm10: AQI,
    @SerializedName("pm10_local_fraction_traffic_exhaust") val pm10_local_fraction_traffic_exhaust: CValue,
    @SerializedName("pm25_local_fraction_traffic_exhaust") val pm25_local_fraction_traffic_exhaust: CValue,
    @SerializedName("pm10_concentration") val pm10_concentration: CValue,
    @SerializedName("o3_local_fraction_traffic_nonexhaust") val o3_local_fraction_traffic_nonexhaust: CValue,
    @SerializedName("AQI") val aQI: AQI,
    @SerializedName("pm10_nonlocal_fraction") val pm10_nonlocal_fraction: CValue,
    @SerializedName("no2_local_fraction_traffic_exhaust") val no2_local_fraction_traffic_exhaust: CValue,
    @SerializedName("o3_nonlocal_fraction_seasalt") val o3_nonlocal_fraction_seasalt: CValue,
    @SerializedName("o3_concentration") val o3_concentration: CValue,
    @SerializedName("pm25_local_fraction_industry") val pm25_local_fraction_industry: CValue,
    @SerializedName("o3_local_fraction_industry") val o3_local_fraction_industry: CValue,
    @SerializedName("pm10_local_fraction_industry") val pm10_local_fraction_industry: CValue,
    @SerializedName("o3_local_fraction_traffic_exhaust") val o3_local_fraction_traffic_exhaust: CValue,
    @SerializedName("o3_nonlocal_fraction") val o3_nonlocal_fraction: CValue,
    @SerializedName("pm25_concentration") val pm25_concentration: CValue,
    @SerializedName("pm10_local_fraction_shipping") val pm10_local_fraction_shipping: CValue,
    @SerializedName("pm25_local_fraction_heating") val pm25_local_fraction_heating: CValue,
    @SerializedName("AQI_o3") val aQI_o3: AQI,
    @SerializedName("no2_local_fraction_heating") val no2_local_fraction_heating: CValue,
    @SerializedName("pm25_local_fraction_traffic_nonexhaust") val pm25_local_fraction_traffic_nonexhaust: CValue,
    @SerializedName("pm10_nonlocal_fraction_seasalt") val pm10_nonlocal_fraction_seasalt: CValue,
    @SerializedName("pm10_local_fraction_traffic_nonexhaust") val pm10_local_fraction_traffic_nonexhaust: CValue,
    @SerializedName("pm25_nonlocal_fraction") val pm25_nonlocal_fraction: CValue,
    @SerializedName("pm10_local_fraction_heating") val pm10_local_fraction_heating: CValue,
    @SerializedName("o3_local_fraction_heating") val o3_local_fraction_heating: CValue,
    @SerializedName("no2_nonlocal_fraction") val no2_nonlocal_fraction: CValue,
    @SerializedName("no2_local_fraction_traffic_nonexhaust") val no2_local_fraction_traffic_nonexhaust: CValue,
    @SerializedName("AQI_no2") val aQI_no2: AQI,
    @SerializedName("pm25_local_fraction_shipping") val pm25_local_fraction_shipping: CValue,
    @SerializedName("no2_concentration") val no2_concentration: CValue
)

data class Time(

    @SerializedName("to") val to: String,
    @SerializedName("variables") val variables: Variables,
    @SerializedName("reason") val reason: Reason,
    @SerializedName("from") val from: String
)

data class Superlocation(

    @SerializedName("name") val name: String,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("path") val path: String,
    @SerializedName("areaclass") val areaclass: String,
    @SerializedName("areacode") val areacode: Int,
    @SerializedName("superareacode") val superareacode: Int,
    @SerializedName("latitude") val latitude: Double
)

data class Reason(

    @SerializedName("variables") val variables: List<String>,
    @SerializedName("sources") val sources: List<String>
)

data class Sublocations(

    @SerializedName("areacode") val areacode: Int,
    @SerializedName("superareacode") val superareacode: Int,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("path") val path: String,
    @SerializedName("areaclass") val areaclass: String,
    @SerializedName("name") val name: String,
    @SerializedName("longitude") val longitude: Double
)

data class Meta(

    @SerializedName("location") val location: Location,
    @SerializedName("reftime") val reftime: String,
    @SerializedName("sublocations") val sublocations: List<Sublocations>,
    @SerializedName("superlocation") val superlocation: Superlocation
)

data class Location(

    @SerializedName("name") val name: String,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("path") val path: String,
    @SerializedName("areaclass") val areaclass: String,
    @SerializedName("areacode") val areacode: Int,
    @SerializedName("superareacode") val superareacode: Int,
    @SerializedName("latitude") val latitude: Double
)

data class PointData(

    @SerializedName("meta") val meta: Meta,
    @SerializedName("data") val data: Data
)

data class Data(

    @SerializedName("time") val time: List<Time>
)

data class AQI(

    @SerializedName("units") val units: Int?,
    @SerializedName("value") val value: Double?
)

data class CValue(

    @SerializedName("units") val units: String?,
    @SerializedName("value") val value: Double?
)
data class Place (

    @SerializedName("area") val area: String,
    @SerializedName("name") val name : String,
    @SerializedName("LatLng") val latLng : LatLng
)

data class MapType(val title: String, val description: String, val fullName: String)


// AreaCode classes


data class AvParent(

    @SerializedName("areadefinition") val areadefinition: Areadefinition,
    @SerializedName("last_updated_utc") val last_updated_utc: String,
    @SerializedName("available") val available: List<Available>
)

data class Areadefinition(

    @SerializedName("name") val name: String,
    @SerializedName("url") val url: String
)

data class Available(

    @SerializedName("areacode") val areacode: String,
    @SerializedName("areaclass") val areaclass: String,
    @SerializedName("areaname") val areaname: String,
    @SerializedName("timelabel") val timelabel: String
)


// NILU Classes

data class NiluStation(

    @SerializedName("id") val id: Int,
    @SerializedName("zone") val zone: String,
    @SerializedName("municipality") val municipality: String,
    @SerializedName("area") val area: String,
    @SerializedName("station") val station: String,
    @SerializedName("eoi") val eoi: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("owner") val owner: String,
    @SerializedName("status") val status: String,
    @SerializedName("description") val description: String,
    @SerializedName("firstMeasurment") val firstMeasurment: String,
    @SerializedName("lastMeasurment") val lastMeasurment: String,
    @SerializedName("components") val components: String,
    @SerializedName("isVisible") val isVisible: Boolean
)

data class NiluStationComp(

    @SerializedName("zone") val zone: String,
    @SerializedName("municipality") val municipality: String,
    @SerializedName("area") val area: String,
    @SerializedName("station") val station: String,
    @SerializedName("eoi") val eoi: String,
    @SerializedName("component") val component: String,
    @SerializedName("fromTime") val fromTime: String,
    @SerializedName("toTime") val toTime: String,
    @SerializedName("value") val value: Double,
    @SerializedName("unit") val unit: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("timestep") val timestep: Int,
    @SerializedName("isValid") val isValid: Boolean
)

// Met point data

data class AnnualmeanPoint(

    @SerializedName("meta") val meta: MeanMeta,
    @SerializedName("data") val data: MeanData
)

data class MeanData(

    @SerializedName("variables") val variables: MeanVariables
)

data class MeanMeta(

    @SerializedName("timelabel") val timelabel: Int,
    @SerializedName("areaname") val areaname: String,
    @SerializedName("areacode") val areacode: Int,
    @SerializedName("dataset") val dataset: String,
    @SerializedName("location") val location: MeanLocation
)

data class MeanLocation(

    @SerializedName("longitude") val longitude: Double,
    @SerializedName("latitude") val latitude: Double
)

data class MeanVariables(

    @SerializedName("no2_concentration") val no2_concentration: CValue,
    @SerializedName("no2_local_contribution_heating") val no2_local_contribution_heating: CValue,
    @SerializedName("no2_local_contribution_industry") val no2_local_contribution_industry: CValue,
    @SerializedName("no2_local_contribution_shipping") val no2_local_contribution_shipping: CValue,
    @SerializedName("no2_local_contribution_traffic_exhaust") val no2_local_contribution_traffic_exhaust: CValue,
    @SerializedName("no2_nonlocal_contribution") val no2_nonlocal_contribution: CValue,
    @SerializedName("pm25_concentration") val pm25_concentration: CValue,
    @SerializedName("pm25_local_contribution_heating") val pm25_local_contribution_heating: CValue,
    @SerializedName("pm25_local_contribution_industry") val pm25_local_contribution_industry: CValue,
    @SerializedName("pm25_local_contribution_shipping") val pm25_local_contribution_shipping: CValue,
    @SerializedName("pm25_local_contribution_traffic_exhaust") val pm25_local_contribution_traffic_exhaust: CValue,
    @SerializedName("pm25_local_contribution_traffic_nonexhaust") val pm25_local_contribution_traffic_nonexhaust: CValue,
    @SerializedName("pm25_nonlocal_contribution") val pm25_nonlocal_contribution: CValue,
    @SerializedName("pm25_nonlocal_contribution_seasalt") val pm25_nonlocal_contribution_seasalt: CValue,
    @SerializedName("pm10_concentration") val Pm10_concentration: CValue,
    @SerializedName("pm10_local_contribution_heating") val Pm10_local_contribution_heating: CValue,
    @SerializedName("pm10_local_contribution_industry") val Pm10_local_contribution_industry: CValue,
    @SerializedName("pm10_local_contribution_shipping") val Pm10_local_contribution_shipping: CValue,
    @SerializedName("pm10_local_contribution_traffic_exhaust") val Pm10_local_contribution_traffic_exhaust: CValue,
    @SerializedName("pm10_local_contribution_traffic_nonexhaust") val Pm10_local_contribution_traffic_nonexhaust: CValue,
    @SerializedName("pm10_nonlocal_contribution") val Pm10_nonlocal_contribution: CValue,
    @SerializedName("pm10_nonlocal_contribution_seasalt") val Pm10_nonlocal_contribution_seasalt: CValue,
    @SerializedName("no2_concentration_19_highest_hourly_value_inyear") val no2_concentration_19_highest_hourly_value_inyear: CValue,
    @SerializedName("pm10_concentration_31_highest_daily_value_inyear") val pm10_concentration_31_highest_daily_value_inyear: CValue
)
// Google goecoding api

data class GeeCoding(

    @SerializedName("plus_code") val plus_code: Plus_code,
    @SerializedName("results") val results: List<Results>,
    @SerializedName("status") val status: String
)

data class Address_components(

    @SerializedName("long_name") val long_name: String,
    @SerializedName("short_name") val short_name: String,
    @SerializedName("types") val types: List<String>
)

data class Bounds(

    @SerializedName("northeast") val northeast: Northeast,
    @SerializedName("southwest") val southwest: Southwest
)

data class Geometry(

    @SerializedName("bounds") val bounds: Bounds,
    @SerializedName("location") val location: LocationGeocode,
    @SerializedName("location_type") val location_type: String,
    @SerializedName("viewport") val viewport: Viewport
)

data class LocationGeocode(

    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double
)

data class Northeast(

    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double
)

data class Plus_code(

    @SerializedName("compound_code") val compound_code: String,
    @SerializedName("global_code") val global_code: String
)


data class Results(

    @SerializedName("access_points") val access_points: List<String>,
    @SerializedName("address_components") val address_components: List<Address_components>,
    @SerializedName("formatted_address") val formatted_address: String,
    @SerializedName("geometry") val geometry: Geometry,
    @SerializedName("place_id") val place_id: String,
    @SerializedName("types") val types: List<String>
)

data class Southwest(

    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double
)

data class Viewport(

    @SerializedName("northeast") val northeast: Northeast,
    @SerializedName("southwest") val southwest: Southwest
)

