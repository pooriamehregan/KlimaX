package com.example.airboyz.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.airboyz.MainActivity
import com.example.airboyz.R
import com.example.airboyz.databinding.FragmentSettingsBinding
import com.example.airboyz.dataclasses.MapType
import com.example.airboyz.ui.map.func.CustomSpinnerAdapter

class SettingsFragment : Fragment() , AdapterView.OnItemSelectedListener{

    // bind layout of this fragment to it
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var mApp: MainActivity
    private lateinit var spinner: Spinner
    private lateinit var sharedPref: SharedPreferences
    private var defaultValue : Int = 0// a default value to return if the key doesn't exist
    private var defaultMapTypePos: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Declare class variables
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val view = binding.root
        mApp = activity as MainActivity
        initTheme()
        initSpinner()
        initResetBtn()
        // Inflate the layout for this fragment
        return view
    }

    private fun initTheme(){
        sharedPref = mApp.getPreferences(Context.MODE_PRIVATE) ?: return
        val isNightModeOn : Boolean = sharedPref.getBoolean("NightMode", false)
        val sharedPrefEdit : SharedPreferences.Editor = sharedPref.edit()
        val btn : Button = binding.themeBtn

        btn.setOnClickListener{
            if(isNightModeOn){
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                sharedPrefEdit.putBoolean("NightMode", false)
                sharedPrefEdit.apply()
            }
            else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                sharedPrefEdit.putBoolean("NightMode", true)
                sharedPrefEdit.apply()
            }
        }
    }

    private fun initSpinner() {
        spinner = binding.defaultMapTypeSpinner
        spinner.onItemSelectedListener = this
        val array = resources.getStringArray(R.array.mapTypes)
        val mapTypelist = listOf(
            MapType(getString(R.string.air_zone_map), getString(R.string.air_zone_map), array[0]),
            MapType(getString(R.string.no2_concentration_annual_mean), getString(R.string.annual_mean), array[1]),
            MapType(getString(R.string.pm10_concentration_annual_mean), getString(R.string.annual_mean), array[2]),
            MapType(getString(R.string.pm25_concentration_annual_mean), getString(R.string.annual_mean), array[3]),
            MapType(getString(R.string.no2_concentration_annual_mean), getString(R.string.nhhiny), array[4]),
            MapType(getString(R.string.pm10_concentration_annual_mean), getString(R.string.tohdiny), array[5])
        )

        val arrayAdapter = CustomSpinnerAdapter(mApp, R.layout.map_item_layout , mapTypelist )
        spinner.adapter = arrayAdapter

        // set spinner default value
        defaultMapTypePos = sharedPref.getInt(getString(R.string.saved_default_map_type_key), defaultValue)
        spinner.setSelection(defaultMapTypePos)
    }

    private fun initResetBtn() {
        binding.settingsResetBtn.setOnClickListener {
            val builder: AlertDialog.Builder = AlertDialog.Builder(mApp)
            builder.setMessage(getString(R.string.places_reset_msg))

            builder.setPositiveButton(getString(R.string.reset)) {
                    dialog, _ -> // Do nothing but close the dialog
                mApp.resetMyPlaces()
                Toast.makeText(mApp, getString(R.string.reset), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }

            builder.setNegativeButton(getString(R.string.cancel)) {
                    dialog, _ -> // Do nothing
                dialog.dismiss()
            }
            val alert: AlertDialog = builder.create()
            alert.show()
        }
    }


    override fun onNothingSelected(parent: AdapterView<*>?) {
        TODO("Not yet implemented")
    }

    /** Change the spinner text to the selected item and store the selected map type in persistent memory.
     * Finally update the universal Default Map Type variable so that other spinner in KartFragment shows that. * */
    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        parent.setSelection(pos)
        defaultMapTypePos = sharedPref.getInt(getString(R.string.saved_default_map_type_key), defaultValue)

        with(sharedPref.edit()){
            if(defaultMapTypePos != defaultValue){ // if mode already exist
                remove(getString(R.string.saved_default_map_type_key)) // remove old value
            }
            putInt(getString(R.string.saved_default_map_type_key), pos) // put new value
            apply()
        }
    }
}
