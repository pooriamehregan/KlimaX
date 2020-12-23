package com.example.airboyz.ui.map


import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.os.Bundle
import android.text.style.CharacterStyle
import android.text.style.StyleSpan
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.airboyz.MainActivity
import com.example.airboyz.R
import com.example.airboyz.databinding.FragmentKartBinding
import com.example.airboyz.dataclasses.MapType
import com.example.airboyz.dataclasses.PointData
import com.example.airboyz.dataclasses.Times
import com.example.airboyz.ui.map.func.*
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.gson.responseObject
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places.createClient
import com.google.android.libraries.places.api.Places.initialize
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.tabs.TabLayout
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import kotlinx.android.synthetic.main.fragment_kart.*
import kotlinx.android.synthetic.main.popup_info_layout.view.*
import kotlinx.android.synthetic.main.popup_live_info_layout.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.set
import kotlin.math.absoluteValue
import kotlin.math.roundToInt


//ZOOM levels
private const val REGION_ZOOM = 14.0f

class KartFragment : Fragment(),
    OnItemClickListener, GoogleMap.OnMapClickListener,
    FragmentHelper {

//// Is used to bind layout to driver classes like Activity or Fragment
    private var _binding: FragmentKartBinding? = null
    private val binding get() = _binding!!
    private lateinit var tabs : TabLayout
////

    override lateinit var mApp: MainActivity // Main Activity
    private var zoom: Float = 0.0f
    private var maxzoom: Float = 20f
    private var minzoom: Float = 4f


//// Variables used for finding user's location, search on map, and providing autocomplete suggestions
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    // Create a new token for the autocomplete session. Pass this to FindAutocompletePredictionsRequest,
    // and once again when the user makes a selection (for example when calling fetchPlace()).
    private var token: AutocompleteSessionToken = AutocompleteSessionToken.newInstance()
    // a rectangular bound that we could restrict our search to.
    private var rectangularBounds: RectangularBounds = RectangularBounds.newInstance(
        LatLng(58.0274, 29.74943),
        LatLng(58.0274, 29.74943)
    )
    // User's location; can be null.
    private var userLocation : LatLng? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var imm : InputMethodManager
    private var currentPrediction : AutocompletePrediction? = null
    // The type of the primary text to be returned by predictions
    private val styleBold: CharacterStyle = StyleSpan(Typeface.BOLD)
    // Variabel som holder på marker, Fjernes om den finnes ved nytt kall
    private var mapObjects = HashMap<String, Any>()
    private var seeMoreFrg: SeeMoreFragment? = null

    private var mapOnClick = true


    private var mapType = "luftsonekart"
////

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Declare class variables
        _binding = FragmentKartBinding.inflate(inflater, container, false)
        val view = binding.root
        mApp = activity as MainActivity
        mApp.kFrg = this
        // Not ready, ugly
        //setStyle()
        initMapType()
        initPlaces()
        initModeSwitch()

        return view
    }


    // When root view has been created add the rest
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        addModelOverlay()
        moveToNorway()
        initSettingsPanel()

        super.onViewCreated(view, savedInstanceState)
    }

    fun dismissPopup() {
        switched = true
        (mapObjects["pointInfoPopup"] as PopupWindow?)?.dismiss()
        (mapObjects["pointInfoPopupMarker"] as Marker?)?.remove()
    }

    private fun initMapType() {
        val sharedPref = mApp.getPreferences(Context.MODE_PRIVATE) ?: return
        val defaultValue =
            resources.getInteger(R.integer.saved_default_key) // a default value to return if the key doesn't exist
        val mapTypePos =
            sharedPref.getInt(getString(R.string.saved_default_map_type_key), defaultValue)
        val array = resources.getStringArray(R.array.mapTypes)
        if (mapTypePos != -1) {
            mapType = array[mapTypePos]
        }
    }

    override fun onResume() {
        GlobalScope.launch(IO) {
            if (!mApp.checkInternet()) {
                mApp.runOnUiThread {
                    Toast.makeText(mApp, getString(R.string.no_internet_map), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
        // Update map on resume
        updateMap(OnMapReadyCallback {
            (mapObjects["tileOverlay"] as TileOverlay?)?.clearTileCache()
        })
        switched = false
        super.onResume()
    }

    private fun moveToNorway() {
        updateMap(OnMapReadyCallback {

            it.setLatLngBoundsForCameraTarget(LatLngBounds(LatLng(57.2075198,
                4.6374903), LatLng(
                70.9242528,
                32.6958158)))
            it.animateCamera(CameraUpdateFactory.newLatLng(LatLng(65.0, 10.757933)))
        })
    }


    // Always init to luftsonekart
    private fun addModelOverlay() {
        removeModelOverlay()
        clearMap()
        updateColorScale()
        updateMap(OnMapReadyCallback {
            val provider = KartTileProvider(
                mApp.assets,
                mapType,
                "2018"
            )
            val providerOverZoom = OverZoomTileProvider(provider)
            val ov = it.addTileOverlay(
                TileOverlayOptions().tileProvider(providerOverZoom)
            )

            ov.transparency = 0.7f
            ov.fadeIn = true
            it.setMaxZoomPreference(maxzoom)
            it.setMinZoomPreference(minzoom)
            mapObjects["tileOverlay"] = ov
            mapObjects["tileProvider"] = provider



            it.setOnMapClickListener(this)

        })
    }

    private fun updateColorScale() {
        if (mapType != "lufsonekart") {
            mApp.runOnUiThread {
                map_color_scale.visibility = View.VISIBLE
            }
        }
        when (mapType) {
            "luftsonekart" -> {
                mApp.runOnUiThread {
                    map_color_scale.visibility = View.GONE
                }
                return
            }
            "no2_concentration_annualmean" -> {
                mApp.runOnUiThread {
                    map_green.text = "0-20"
                    map_yellow.text = "20-40"
                    map_red.text = "40-60"
                    map_purple.text = "60-100"
                }
            }

            "pm10_concentration_annualmean" -> {
                mApp.runOnUiThread {
                    map_green.text = "0-15"
                    map_yellow.text = "15-25"
                    map_red.text = "25-35"
                    map_purple.text = "35-50"
                }
            }
            "pm25_concentration_annualmean" -> {
                mApp.runOnUiThread {
                    map_green.text = "0-8"
                    map_yellow.text = "8-15"
                    map_red.text = "15-25"
                    map_purple.text = "25-35"
                }
            }
            "no2_concentration_19_highest_hourly_value_inyear" -> {
                mApp.runOnUiThread {
                    map_green.text = "0-100"
                    map_yellow.text = "100-200"
                    map_red.text = "200-300"
                    map_purple.text = "300-400"
                }
            }
            "pm10_concentration_31_highest_daily_value_inyear" -> {
                mApp.runOnUiThread {
                    map_green.text = "0-30"
                    map_yellow.text = "30-50"
                    map_red.text = "50-80"
                    map_purple.text = "80-120"
                }
            }
        }

    }

    private fun clearMap() {
        updateMap(OnMapReadyCallback {
            it.clear()
        })
    }

    private fun changeOverlayTransparency(newT: Float) {
        updateMap(OnMapReadyCallback {
            (mapObjects["tileOverlay"] as TileOverlay?)?.transparency = newT
        })
    }

    private fun changeMapType(newMapType: String) {
        mapType = newMapType
        addModelOverlay()
    }


    private fun removeModelOverlay() {
        updateMap(OnMapReadyCallback {
            (mapObjects["tileOverlay"] as TileOverlay?)?.remove()
            mapObjects.remove("tileOverlay")
            mapObjects.remove("tileProvider")
        })
    }



    /**
     * Initiating variables related to map search, adding recycler-view for showing autocomplete,
     * and calling methods that ready and handel user input/interaction.
     */
    private fun initPlaces() {
        initialize(mApp.applicationContext, getString(R.string.google_places_key))
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(mApp)
        placesClient = createClient(mApp)
        viewManager = LinearLayoutManager(mApp)
        recyclerView = binding.placeRecyclerView.apply {
            // use a linear layout manager
            layoutManager = viewManager
            // specify an viewAdapter
            adapter = PlaceAdapter(
                mutableListOf(),
                this@KartFragment
            )
        }

        // Initialize the input manager for handling visibility of onscreen keyboard
        imm = mApp.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        tabs = mApp.findViewById(R.id.sliding_tabs)

        // Start searching when user clicks on the search icon
        binding.searchIcon.setOnClickListener {
            onLocationEntered(currentPrediction)
        }
        activateMyLocationButton()
        inputEnabled()
    }


    private fun updateMap(callback: OnMapReadyCallback) {
        GlobalScope.launch(Main) {
            if (isAdded) {
                val mapsFragment =
                    childFragmentManager.findFragmentById(R.id.mFrag) as MapsFragment?
                mapsFragment?.updateMap(callback)
            }
        }
    }


    /**
     * Updates the user location, if user has given permission to access his/hers location
     * If not, the user will be prompt to give permission, then map will focus and zoom
     * on user's location.
     * User's location will also be stored in a variable during this process, to be used later.
     */
    private fun activateMyLocationButton(){
        binding.myLocationButton.setOnClickListener{
            if (mApp.permissionGranted()) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        updateMap(OnMapReadyCallback { mMap ->
                            userLocation = LatLng(location.latitude, location.longitude)
                            mMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    userLocation,
                                    REGION_ZOOM
                                )
                            )
                            zoom = REGION_ZOOM
                            mMap.isMyLocationEnabled = true

                        })

                    } else {
                        mApp.runOnUiThread {
                            Toast.makeText(mApp, getString(R.string.gps_error), Toast.LENGTH_SHORT)
                                .show()
                        }

                    }
                }.addOnCanceledListener {
                    mApp.runOnUiThread {
                        Toast.makeText(mApp, getString(R.string.gps_error), Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } else {
                mApp.runOnUiThread {
                    Toast.makeText(mApp, getString(R.string.gps_not_enabled), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    /**
     * Sets the serch field on clickListener to observe change. If user presses
     * search/Enter/search-icon then map will be updated based on the input.
     * For each key that user types, that key will be sent to autocomplete method to get list of
     * autocomplete suggstions.
     */
    private fun requestAutoComplete(){
        // send the input key to autoCompletePlaces to get autoComplete suggestion
        binding.inputSearch.setOnKeyListener { _, _, _ ->
            autoCompletePlaces(binding.inputSearch.editableText.toString())
            false
        }
        // Take an action based on what key was entered, e.g. show/hide keyboard
        binding.inputSearch.setOnEditorActionListener { _, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                || actionId == EditorInfo.IME_ACTION_DONE
                || actionId == EditorInfo.IME_ACTION_SEND
                || keyEvent.action == KeyEvent.ACTION_DOWN
                || keyEvent.action == KeyEvent.KEYCODE_ENTER
            ) {
                onLocationEntered(currentPrediction)
            }
            false
        }
    }


    /**
     * finds autocomplete suggestions, then inflates the recycler view with suggestion
     * @param query is a single character passed to this method to get autocomplete based on it.
     */
    private fun autoCompletePlaces(query: String){
        // Use the builder to create a FindAutocompletePredictionsRequest.
        val request: FindAutocompletePredictionsRequest =
            FindAutocompletePredictionsRequest.builder() // Call either setLocationBias() OR setLocationRestriction().
                .setLocationBias(rectangularBounds) //.setLocationRestriction(bounds)
                .setOrigin(LatLng(59.91273, 10.74609)) // origin to start the search from is Oslo
                .setCountries("NO")
                .setSessionToken(token)
                .setQuery(query)
                .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response: FindAutocompletePredictionsResponse ->
                val autocompletes: MutableList<AutocompletePrediction> = response.autocompletePredictions
                // update the recyclerview with predictions
                recyclerView.adapter =
                    PlaceAdapter(
                        response.autocompletePredictions,
                        this
                    )
                // Update the currentPrediction to use if user press enter without choosing autocomplete from suggestions
                if (autocompletes.isNotEmpty()){
                    currentPrediction = response.autocompletePredictions[0]
                }
            }
            .addOnFailureListener { exception: Exception ->
                if (exception is ApiException) {
                    val apiException: ApiException = exception
                    Log.e(TAG, "Place not found: " + apiException.statusCode)
                }
            }
    }


    /**
     * Replaces the edit text with a text view that shows the string that user typed in.
     */
    private fun onLocationEntered(prediction: AutocompletePrediction?) {
        if(prediction != null){
            inputEnabled()
            // drive latitude/longitude from user input, and update map camera
            CoroutineScope(Default).launch {
                val response = placeIDToLatLng(prediction.placeId)
                withContext(Main){
                    if (response != null) {
                        moveCamera(response, REGION_ZOOM) // zoom level 15 is suggested by google for city
                    }
                    else {
                        Log.w("onLocationEntered() :", "no response from placeIDToLatLng().")
                    }
                }
            }
        }
    }


    /**
     * Show and hide on-screen keyboard based on if a view has focus.
     * @param view is the view that determines what to do.
     * @param hasFocus checks if view has the application focus
     */
    private fun changeKeyboardStatus(view: View, hasFocus: Boolean){
        when(hasFocus){
            true -> {
                imm.showSoftInput(view, InputMethodManager.SHOW_FORCED)
            }
            else -> {
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }
    }

    /**
     * Handles selection of autocomplete by user.
     * @param prediction is selected suggestion.
     * @return the area-code of the selected location, to be used for moving camera and updating the map.
     */
    override fun onItemClicked(prediction: AutocompletePrediction) {
        onLocationEntered(prediction)
    }


    /**
     * Converts the placeID of a location to LatLng pointes.
     * @param placeId is the placeId to be converted.
     * @return a LatLng object, which represents the placeId, and can be used to update
     * the map i.g. move the camera.
     */
    private suspend fun placeIDToLatLng(placeId: String): LatLng? {
        var location : LatLng? = null
        val geoKey: String = mApp.getString(R.string.geocoding_key)
        val url = "https://maps.googleapis.com/maps/api/geocode/json?place_id=${placeId}&key=${geoKey}"
        val tag = "placeIDtoLatLng(): "

        try {
            val (_, response, result)
                    = Fuel.get(url).awaitStringResponseResult()
            if(response.statusCode == 200){
                val locationJason = JSONObject("${result.component1()}")
                    .getJSONArray("results") //array
                    .getJSONObject(0) // array has one object
                    .getJSONObject("geometry") // object
                    .getJSONObject("location") // object
                val lat = locationJason.getDouble("lat")
                val lng = locationJason.getDouble("lng")
                location = LatLng(lat, lng)
            } else{
                Log.w(tag, "status code NOT OK!")
            }
        }
        catch (e: Exception){
            Log.e(tag, "Fuel failed fetching LatLng for place ID.")
        }
        return location
    }

    /**
     * Updates the map camera, adds a marker on selected position, and updates the zoom value.
     * @param location is the physical location to move camera to, and add marker on.
     * @param zoom is the zoom level of the displayed map
     */
    private fun moveCamera(location: LatLng, zoom: Float){
        updateMap(OnMapReadyCallback { gMap: GoogleMap ->
            // Check if map already has a Marker, if so: delete the old marker.
            this.zoom = zoom
            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, zoom))
            addMarkerPopup(location)
        })
    }

    private fun inputEnabled(){
        binding.apply {
            if(searchText.visibility == View.VISIBLE){
                searchText.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                inputSearch.visibility = View.VISIBLE

                inputSearch.setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus){
                        requestAutoComplete()
                    }
                    else {
                        inputEnabled()
                    }
                    changeKeyboardStatus(v, hasFocus)
                }
            }
            else {
                inputSearch.visibility = View.GONE
                searchText.text = currentPrediction?.getPrimaryText(styleBold)
                searchText.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                searchText.setOnClickListener {
                    inputEnabled()
                }
            }
        }
    }




    private fun toggleSettingsWindow() {
        (mapObjects["settingsPanel"] as PopupWindow?)?.showAtLocation(view, Gravity.CENTER,0, 0)
    }

    private fun initSettingsPanel() {
        binding.settingsBtn.setOnClickListener { toggleSettingsWindow() }
        val window = PopupWindow(mApp)
        val view = layoutInflater.inflate(R.layout.popup_settings_layout, null)
        window.contentView = view
    /* added by Pooria */
        val spinner: Spinner = view.findViewById(R.id.mapTypeSpinner)
        initSpinner(spinner)
    /**/
        view.findViewById<SeekBar>(R.id.opacity_bar)
            .setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                    GlobalScope.launch(Default) {
                        changeOverlayTransparency(p1.toFloat() / 100f)
                    }
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            })
        view.findViewById<SeekBar>(R.id.opacity_bar).progress = 50
        window.isFocusable = true
        window.isOutsideTouchable = true
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        mapObjects["settingsPanel"] = window
        //window.showAtLocation(view, Gravity.CENTER, 0, 0)
        window.dismiss()
    }


    // Vise dette: https://in2000-apiproxy.ifi.uio.no/weatherapi/airqualityforecast/0.1/documentation#!/data/get_met
    override fun onMapClick(p0: LatLng?) {
        addMarkerPopup(p0)
    }

    private var switched = false
    private fun addMarkerPopup(p0: LatLng?) {
        // TOOD: Sjekke at marker er innefor norges grenser

        if (mapOnClick && p0 != null) {
            if (p0.latitude !in 58.0274..70.66336 && p0.longitude !in 5.0328..29.74943) return

            updateMap(OnMapReadyCallback { gMap ->
                (mapObjects["pointInfoPopup"] as PopupWindow?)?.let {
                    it.dismiss()
                    mapObjects.remove("pointInfoPopup")
                    mapObjects.remove("pointInfoPopupMarker")
                }

                p0.let {
                    val mko = MarkerOptions().position(p0)
                    // Fikse icon til marker
                    // ikke bruke rød vanlig
                    // mko
                    //   .icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(resources, R.mipmap.map_marker_grey_foreground)))
                    val m =
                        gMap.addMarker(mko)
                    mapObjects["pointInfoPopupMarker"] = m
                    val p = gMap.projection.toScreenLocation(p0)
                    val window = PopupWindow(mApp)
                    val viewPopup = layoutInflater.inflate(R.layout.popup_info_layout, null)
                    window.contentView = viewPopup
                    mapObjects["pointInfoPopup"] = window
                    viewPopup.measure(
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    )
                    //window.isFocusable = true
                    window.setOnDismissListener {
                        updateMap(OnMapReadyCallback {
                            m.remove()
                        })
                    }

                    viewPopup.popup_areacode_spinner.onItemSelectedListener =
                        object : AdapterView.OnItemSelectedListener {
                            override fun onNothingSelected(p0: AdapterView<*>?) {}
                            val classes = listOf("grunnkrets", "delomrade", "kommune")
                            override fun onItemSelected(
                                aV: AdapterView<*>?,
                                v: View?,
                                sel: Int,
                                p3: Long
                            ) {
                                mApp.runOnUiThread {
                                    //viewPopup.info_popup.visibility = View.INVISIBLE
                                    viewPopup.loadbar_popup.visibility = View.VISIBLE
                                }
                                addInfoPopup(viewPopup, p0, classes[sel])
                            }
                        }

                    window.isOutsideTouchable = true
                    window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    if (!switched) {
                        window.showAtLocation(
                            view,
                            Gravity.NO_GRAVITY,
                            p.x - viewPopup.measuredWidth / 2,
                            p.y - viewPopup.measuredHeight
                        )
                    } else {
                        m.remove()
                    }




                }
            })
        }
    }

    // Custom format
    @SuppressLint("SimpleDateFormat")
    private fun addInfoPopup(view: View, p0: LatLng, areaclass: String) {
        GlobalScope.launch(IO) {
            val (_, _, result) = Fuel.get("https://in2000-apiproxy.ifi.uio.no/weatherapi/airqualityforecast/0.1/reftimes")
                .timeout(5000).responseObject<Times>()
            val (refParent, error) = result
            if (error != null) {
                Log.e("Airboiz", "oof1 ")
                mApp.runOnUiThread {
                    Toast.makeText(mApp, getString(R.string.error_get_data), Toast.LENGTH_SHORT)
                        .show()
                }

                updateMap(OnMapReadyCallback {
                    (mapObjects["pointInfoPopup"] as PopupWindow?)?.dismiss()
                    mapObjects.remove("pointInfoPopup")
                })
                return@launch
            }

            refParent?.reftimes?.let { refList ->
                val (_, _, result) = Fuel.get(
                    "https://in2000-apiproxy.ifi.uio.no/weatherapi/airqualityforecast/0.1/?lat=${p0.latitude}&lon=${p0.longitude}&areaclass=$areaclass&reftime=${refList[0].replace(
                        ":",
                        "%3A"
                    )}"
                )
                    .timeout(5000).responseObject<PointData>()
                val (data, error) = result
                if (error != null) {
                    updateMap(OnMapReadyCallback {
                        (mapObjects["pointInfoPopup"] as PopupWindow?)?.dismiss()
                        Toast.makeText(
                            mApp,
                            getString(R.string.no_data),
                            Toast.LENGTH_SHORT
                        ).show()
                        mapObjects.remove("pointInfoPopup")
                    })
                    return@launch
                }

                data?.data?.let {
                    var nearestTime = 0
                    // Find nearest time

                    var diff = 0f
                    var i = 0
                    it.time.forEach { time ->
                        val date = SimpleDateFormat("yy-MM-dd'T'HH:mm:ss'Z'").parse(time.from)
                        date?.let { d ->
                            val diffMill = (Date().time - d.time).toFloat()
                            if (diffMill.absoluteValue < diff || diff == 0f) {
                                diff = diffMill.absoluteValue
                                nearestTime = i
                            }
                            i++
                        }
                    }



                    GlobalScope.launch(Main) {

                        val tmp = it.time[nearestTime].variables
                        val strtmp = "Oppdatert: ${it.time[nearestTime].from}"
                        view.findViewById<TextView>(R.id.popup_last_updated).text = strtmp

                        view.findViewById<TextView>(R.id.popup_val2).text =
                            tmp.aQI.units.toString()
                        var pl: String =
                            "${(tmp.pm10_concentration.value ?: 0.0 * 10.0).roundToInt() / 10f} ${tmp.pm10_concentration.units ?: 0.0} (${tmp.aQI_pm10.units ?: 0.0})"
                        view.findViewById<TextView>(R.id.popup_val3).text = pl
                        pl =
                            "${(tmp.pm25_concentration.value ?: 0.0 * 10.0).roundToInt() / 10f} ${tmp.pm25_concentration.units ?: 0.0} (${tmp.aQI_pm25.units ?: 0.0})"
                        view.findViewById<TextView>(R.id.popup_val4).text = pl
                        pl =
                            "${(tmp.no2_concentration.value ?: 0.0 * 10.0).roundToInt() / 10f} ${tmp.no2_concentration.units ?: 0.0} (${tmp.aQI_no2.units ?: 0.0})"
                        view.findViewById<TextView>(R.id.popup_val5).text = pl

                        pl =
                            "${(tmp.o3_concentration.value ?: 0.0 * 10.0).roundToInt() / 10f} ${tmp.o3_concentration.units ?: 0.0} (${tmp.aQI_o3.units ?: 0.0})"
                        view.findViewById<TextView>(R.id.popup_val6).text = pl

                        view.findViewById<ProgressBar>(R.id.loadbar_popup).visibility = View.GONE
                        view.findViewById<LinearLayout>(R.id.info_popup).visibility = View.VISIBLE

                        view.findViewById<Button>(R.id.see_more_btn).setOnClickListener {
                            (mapObjects["pointInfoPopup"] as PopupWindow?)?.dismiss()
                            showSeeMore(p0, areaclass)
                        }
                    }
                }
            }
        }
    }


    private fun showSeeMore(p0: LatLng, areaclass: String){
        val frg: SeeMoreFragment = SeeMoreFragment.newInstance(p0,areaclass, "", this)
        seeMoreFrg = frg
        val trn = childFragmentManager.beginTransaction()
        trn
            .replace(R.id.see_more_container, frg)
            .addToBackStack(null)
            .commit()
        binding.seeMoreContainer.visibility = View.VISIBLE
        binding.mapsContainer.visibility = View.INVISIBLE
    }

    override fun removeSeeMore(){
        seeMoreFrg?.let{frg ->
            frg.reset()
            binding.seeMoreContainer.visibility = View.INVISIBLE
            binding.mapsContainer.visibility = View.VISIBLE
        }

    }




    private fun initSpinner(spinner: Spinner) {
        val sharedPref = mApp.getPreferences(Context.MODE_PRIVATE) ?: return
        val defaultValue = resources.getInteger(R.integer.saved_default_key) // a default value to return if the key doesn't exist
        val mapTypePos = sharedPref.getInt(getString(R.string.saved_default_map_type_key), defaultValue)
        val array = resources.getStringArray(R.array.mapTypes)
        var defaultItemPos = 0

        val mapTypelist = listOf(
            MapType(getString(R.string.luftsonekart), getString(R.string.luftsonekart), array[0]),
            MapType(
                "NO2 ${getString(R.string.concentration)}",
                getString(R.string.annual_mean),
                array[1]
            ),
            MapType(
                "PM10 ${getString(R.string.concentration)}",
                getString(R.string.annual_mean),
                array[2]
            ),
            MapType(
                "PM25 ${getString(R.string.concentration)}",
                getString(R.string.annual_mean),
                array[3]
            ),
            MapType(
                "NO2 ${getString(R.string.concentration)}",
                getString(R.string.nhhiny),
                array[4]
            ),
            MapType(
                "PM10 ${getString(R.string.concentration)}",
                getString(R.string.tohdiny),
                array[5]
            )
        )

        val arrayAdapter = CustomSpinnerAdapter(mApp, R.layout.map_item_layout , mapTypelist )
        spinner.adapter = arrayAdapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                // An item was selected. You can retrieve the selected item using
                (mapObjects["settingsPanel"] as PopupWindow?)?.let {
                    val selected: MapType? =
                        it.contentView.findViewById<Spinner>(R.id.mapTypeSpinner)
                            .getItemAtPosition((pos)) as MapType?
                    selected?.let { changeMapType(selected.fullName) }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Another interface callback
            }

        }

        if(mapTypePos != defaultValue){ // if key exist in shared (persistent) memory
            // get the value of the key
            defaultItemPos = mapTypePos
        }
        spinner.setSelection(defaultItemPos)
    }

    /*
        Live data handling
        Stations n' stuff
    */

    // false = model, true = station
    private lateinit var mClusterManager: ClusterManager<ClusterMarker>
    private var modeSelected = false
    private fun initModeSwitch() {

        GlobalScope.launch(IO) {
            loadAllStations()
            binding.kartModel.setOnClickListener {
                if (modeSelected) {
                    mApp.runOnUiThread {
                        map_color_scale.visibility = View.VISIBLE
                        binding.kartModel.setBackgroundColor(
                            ResourcesCompat.getColor(
                                resources,
                                R.color.navigationTabs,
                                null
                            )
                        )
                        binding.kartStation.setBackgroundColor(
                            ResourcesCompat.getColor(
                                resources,
                                R.color.colorButton,
                                null
                            )
                        )
                        binding.kartModel.setTextColor(
                            ResourcesCompat.getColor(
                                resources,
                                R.color.colorBackgroundDark,
                                null
                            )
                        )
                        binding.kartStation.setTextColor(
                            ResourcesCompat.getColor(
                                resources,
                                R.color.navigationTabs,
                                null
                            )
                        )

                        switchMode(!modeSelected)
                    }
                }

            }
            binding.kartStation.setOnClickListener {
                mApp.runOnUiThread {
                    if (!modeSelected) {
                        map_color_scale.visibility = View.GONE
                        binding.kartStation.setBackgroundColor(
                            ResourcesCompat.getColor(
                                resources,
                                R.color.navigationTabs,
                                null
                            )
                        )
                        binding.kartModel.setBackgroundColor(
                            ResourcesCompat.getColor(
                                resources,
                                R.color.colorButton,
                                null
                            )
                        )
                        binding.kartStation.setTextColor(
                            ResourcesCompat.getColor(
                                resources,
                                R.color.colorBackgroundDark,
                                null
                            )
                        )
                        binding.kartModel.setTextColor(
                            ResourcesCompat.getColor(
                                resources,
                                R.color.navigationTabs,
                                null
                            )
                        )
                        switchMode(!modeSelected)
                    }
                }
            }
            updateMap(OnMapReadyCallback { map ->

                mClusterManager = ClusterManager<ClusterMarker>(mApp, map)
                val r = object : DefaultClusterRenderer<ClusterMarker>(mApp, map, mClusterManager) {

                }
                r.minClusterSize = 3
                mClusterManager.renderer = r

                mClusterManager.setOnClusterItemClickListener { cm ->
                    map.animateCamera(
                        CameraUpdateFactory.newLatLng(cm.position),
                        object : GoogleMap.CancelableCallback {
                            override fun onFinish() {
                                showPopupLive(cm, map)
                            }

                            override fun onCancel() {}
                        })

                    true
                }
                mClusterManager.setOnClusterClickListener { c ->
                    // Find average and display
                    // No button to see more
                    map.animateCamera(
                        CameraUpdateFactory.newLatLng(c.position),
                        object : GoogleMap.CancelableCallback {
                            override fun onFinish() {
                                showPopupLiveCluster(c, map)
                            }

                            override fun onCancel() {}
                        })


                    true
                }

                map.setOnMarkerClickListener(mClusterManager)
                map.setOnCameraIdleListener(mClusterManager)
            })

        }
    }

    private fun showPopupLiveCluster(c: Cluster<ClusterMarker>, map: GoogleMap) {
        val p = map.projection.toScreenLocation(c.position)
        val window = PopupWindow(mApp)
        val viewPopup = layoutInflater.inflate(R.layout.popup_live_info_layout, null)
        window.contentView = viewPopup
        val tv_str = "${c.size} stasjoner"
        var canceled = false
        val str = "${viewPopup.tv_station_measured_v.text}".replace(":", "(gjennomsnittlig):")
        viewPopup.tv_station_measured_v.text = str
        viewPopup.tv_station_name.text = tv_str
        GlobalScope.launch(IO) {
            val lls = getComponentsString(c.items.toList(), mApp)

            mApp.runOnUiThread {
                viewPopup.pb_station.visibility = View.GONE
                formatLayoutLls(lls)
                viewPopup.ll_station_values.addView(lls.first)
                viewPopup.ll_station_values.addView(lls.second)
                viewPopup.tv_station_updated.visibility = View.GONE
                viewPopup.tv_station_updated_head.visibility = View.GONE
                viewPopup.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                window.height = viewPopup.measuredHeight
                if (!canceled) {
                    window.showAtLocation(
                        view,
                        Gravity.NO_GRAVITY,
                        p.x - viewPopup.measuredWidth / 2,
                        p.y - viewPopup.measuredHeight
                    )
                }
            }
        }
        viewPopup.btn_live_seemore.setOnClickListener {
            updateMap(OnMapReadyCallback {
                val b = LatLngBounds.builder()
                c.items.forEach { cm ->
                    b.include(cm.position)
                }
                canceled = true
                it.animateCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 30))
            })
            window.dismiss()
        }

        viewPopup.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        window.isOutsideTouchable = true
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.showAtLocation(
            view,
            Gravity.NO_GRAVITY,
            p.x - viewPopup.measuredWidth / 2,
            p.y - viewPopup.measuredHeight
        )


    }

    private fun formatLayoutLls(lls: Pair<LinearLayout, LinearLayout>) {
        lls.first.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lls.first.orientation = LinearLayout.VERTICAL
        lls.second.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lls.second.orientation = LinearLayout.VERTICAL

        lls.first.setPadding(0, 2, 10, 0)
        lls.second.setPadding(10, 2, 0, 0)
        lls.first.gravity = Gravity.END
        lls.second.gravity = Gravity.END
    }

    private fun showPopupLive(cm: ClusterMarker, map: GoogleMap) {
        val p = map.projection.toScreenLocation(cm.position)
        val window = PopupWindow(mApp)
        val viewPopup = layoutInflater.inflate(R.layout.popup_live_info_layout, null)
        window.contentView = viewPopup

        viewPopup.tv_station_name.text = cm.station.station
        GlobalScope.launch(IO) {
            viewPopup.btn_live_seemore.visibility = View.GONE
            val lls = getComponentsString(cm.station, mApp)

            mApp.runOnUiThread {
                viewPopup.pb_station.visibility = View.GONE
                formatLayoutLls(lls)
                viewPopup.ll_station_values.addView(lls.first)
                viewPopup.ll_station_values.addView(lls.second)
                viewPopup.tv_station_updated.text = cm.station.lastMeasurment
                viewPopup.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                window.height = viewPopup.measuredHeight
            }
        }

        viewPopup.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        window.isOutsideTouchable = true
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.showAtLocation(
            view,
            Gravity.NO_GRAVITY,
            p.x - viewPopup.measuredWidth / 2,
            p.y - viewPopup.measuredHeight
        )

    }


    private fun switchMode(mode: Boolean) {
        // false = model, true = station
        if (mode) {
            // Station
            removeModelOverlay()
            mapOnClick = false

            showStationMarkers(true)
        } else {
            showStationMarkers(false)
            addModelOverlay()
            mapOnClick = true
        }
        modeSelected = mode
    }

    private fun loadAllStations() {
        val stations = getAllStations()
        val clusterList = mutableListOf<ClusterMarker>()
        stations.forEach {
            clusterList.add(ClusterMarker(it))
        }
        mapObjects["stationMarkers"] = clusterList

    }

    private fun showStationMarkers(flag: Boolean) {
        if (mapObjects.containsKey("stationMarkers")) {
            val clusterList = (mapObjects["stationMarkers"] as List<ClusterMarker>)
            if (clusterList.isNotEmpty()) {
                updateMap(OnMapReadyCallback {
                    if (flag) {

                        mClusterManager.addItems(clusterList)
                        mClusterManager.cluster()
                    } else {

                        mClusterManager.clearItems()
                        mClusterManager.cluster()
                    }

                })
            }
        }


    }


}
