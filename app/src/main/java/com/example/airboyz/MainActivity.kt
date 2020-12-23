package com.example.airboyz

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.airboyz.dataclasses.Place
import com.example.airboyz.ui.home.HomeFragment
import com.example.airboyz.ui.info.InfoFragment
import com.example.airboyz.ui.map.KartFragment
import com.example.airboyz.ui.settings.SettingsFragment
import com.github.salomonbrys.kotson.fromJson
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.InputStreamReader
import java.net.InetAddress


open class MainActivity : AppCompatActivity() {
    private lateinit var imageResId : IntArray
    private lateinit var imageResIdOutlined : IntArray
    var kFrg: KartFragment? = null
    lateinit var fusedProvider: FusedLocationProviderClient
    private var hasGPS: Boolean = false
    var myPlaces: MutableList<Place> = mutableListOf()


    override fun onCreate(savedInstanceState: Bundle?) {

        fusedProvider = LocationServices.getFusedLocationProviderClient(this)
        loadMyPlaces()
        initAppTheme() // initialize app theme based on stored user preferences : DARK or LIGHT
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initTabNavigation()
        GlobalScope.launch(Dispatchers.IO) {
            if (!checkInternet()) {
                runOnUiThread {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.no_internet),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Requests GPS permissions, some functions should be disabled if gps not available
    private fun checkPermissions(): Boolean {
        if (checkSelfPermission( Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
        return true
    }

    private fun ensurePermissions() {
        if (!checkPermissions()) {
            val strA = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            requestPermissions(strA, PackageManager.PERMISSION_GRANTED)
            if (checkPermissions()) {
                hasGPS = true
                initGPS()
            }
        } else {
            hasGPS = true
            initGPS()
        }
    }

    private fun initGPS() {
        // Get location once every minute
        val mLocationRequest = LocationRequest.create()
        mLocationRequest.interval = 60000
        mLocationRequest.fastestInterval = 5000
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val mLocationCallback: LocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                Log.i("Airboiz", "Got location")
            }
        }
        fusedProvider.requestLocationUpdates(mLocationRequest, mLocationCallback, null)
        fusedProvider.lastLocation
    }

    open fun permissionGranted() : Boolean {
        if (!hasGPS){
            ensurePermissions()
        }
        return hasGPS
    }

    private fun initTabNavigation() {
        val adapter = ViewPagerAdapter(this)
        viewpager.adapter = adapter
        viewpager.isUserInputEnabled = false
        viewpager.offscreenPageLimit = 6 // preloads 6 fragments
        TabLayoutMediator(sliding_tabs, viewpager) { tab, position ->
        }.attach()

        imageResIdOutlined = intArrayOf(
            R.drawable.ic_home,
            R.drawable.ic_search,
            R.drawable.ic_info,
            R.drawable.ic_settings
        )

        imageResId = intArrayOf(
            R.drawable.ic_home_fill,
            R.drawable.ic_search_fill,
            R.drawable.ic_info_fill,
            R.drawable.ic_settings_fill
        )

        sliding_tabs.getTabAt(0)?.setIcon(imageResId[0]) // use default for home fragment
        for (i in 1 until imageResId.size) {
           sliding_tabs.getTabAt(i)?.setIcon(imageResIdOutlined[i])
        }

        sliding_tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                tab.setIcon(imageResId[tab.position])
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                tab.setIcon(imageResIdOutlined[tab.position])
                kFrg?.dismissPopup()
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    class ViewPagerAdapter(activity: MainActivity) : FragmentStateAdapter(activity) {
        private val fragments: List<Fragment> =
          listOf(HomeFragment(), KartFragment(), InfoFragment(), SettingsFragment())

        override fun createFragment(position: Int): Fragment {
            return fragments[position]
        }

        override fun getItemCount(): Int {
            return fragments.size
        }
    }

    suspend fun checkInternet(): Boolean {
        // Pings usit to check if internet is up
        try {
            val ipAddr: InetAddress = InetAddress.getByName("usit.uio.no")
            return !ipAddr.equals("")
        } catch (e: Exception) {
            // Can give error
        }
        return false
    }

    private fun initAppTheme(){

        val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return
        val isNightModeOn : Boolean = sharedPref.getBoolean("NightMode", false)

        if(isNightModeOn){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }


    private fun loadMyPlaces(){
        // Check if folder with location exists
        val folder = File(filesDir, "myPlaces")
        if (!folder.exists()){
            folder.mkdir()
        }
        val file = File(folder, "myPlacesJson.json")
        if (!file.exists()) {
            file.createNewFile()
            // Init file with def values
            resetMyPlaces()
            val fw = FileWriter(file)
            Gson().toJson(myPlaces, fw)
            fw.close()
            // First init

        }
        myPlaces = Gson().fromJson(FileReader(file))

    }

    fun addPlace(areaclass: String, name: String, pos: LatLng) {
        myPlaces.add(Place(areaclass, name, pos))
        saveMyPlaces()
    }

    private fun saveMyPlaces(){
        // Check if folder with location exists
        val folder = File(filesDir, "myPlaces")
        if (!folder.exists()){
            folder.mkdir()
        }
        val file = File(folder, "myPlacesJson.json")
        if (!file.exists()) {
            file.createNewFile()
        }
        val fw = FileWriter(file)
        Gson().toJson(myPlaces, fw)
        fw.close()
    }
    fun delPlace(p: Place){
        myPlaces.remove(p)
        saveMyPlaces()
    }

    fun replacePlace(p: Place, p2: Place) {
        myPlaces[myPlaces.indexOf(p)] = p2
        saveMyPlaces()
    }

    fun resetMyPlaces() {
        val file = assets.open("my_places_def.json")
        myPlaces = Gson().fromJson(InputStreamReader(file))
        saveMyPlaces()
    }
}