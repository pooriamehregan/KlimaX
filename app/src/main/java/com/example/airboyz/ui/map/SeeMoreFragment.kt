package com.example.airboyz.ui.map

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import com.example.airboyz.R
import com.example.airboyz.dataclasses.*
import com.example.airboyz.ui.map.func.CustomPercentageFormatter
import com.example.airboyz.ui.map.func.CustomPieChartRenderer
import com.example.airboyz.ui.map.func.FragmentHelper
import com.example.airboyz.ui.map.func.round
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.gson.responseObject
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.salomonbrys.kotson.fromJson
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import kotlinx.android.synthetic.main.colored_box.view.*
import kotlinx.android.synthetic.main.see_more_fragment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

class SeeMoreFragment : Fragment() {

    val scope = MainScope()
    private lateinit var point: LatLng
    private lateinit var parentFrg: FragmentHelper
    private lateinit var areaClass: String
    private lateinit var pos: String
    private val colors = listOf(
        Color.parseColor("#488f31"),
        Color.parseColor("#88a037"),
        Color.parseColor("#c0af4a"),
        Color.parseColor("#f4bd6a"),
        Color.parseColor("#ef9556"),
        Color.parseColor("#e56b4e"),
        Color.parseColor("#de425b")
    )
    // Capitalize

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.see_more_fragment, container, false)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    @ExperimentalStdlibApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        see_more_back_btn?.setOnClickListener {
            parentFrg.removeSeeMore()
        }

        see_more_add_place?.setOnClickListener {
            showAddPlaceDialog()
        }
        val f: ScrollView? = see_more_forecast_sv
        val s: ScrollView? = see_more_sources_sv
        val a: ScrollView? = see_more_annual_sv

        val tx1: TextView? = see_more_sources_btn
        val tx2: TextView? = see_more_forecast_btn
        val tx3: TextView? = see_more_annual_btn
        tx1?.setOnClickListener {
            if (s?.visibility == View.GONE) {
                f?.visibility = View.GONE
                a?.visibility = View.GONE
                s.visibility = View.VISIBLE
                tx1.setBackgroundColor(Color.WHITE)
                tx2?.setBackgroundColor(Color.TRANSPARENT)
                tx3?.setBackgroundColor(Color.TRANSPARENT)
            }
        }
        tx2?.setOnClickListener {
            if (f?.visibility == View.GONE) {
                s?.visibility = View.GONE
                a?.visibility = View.GONE
                f.visibility = View.VISIBLE
                tx2.setBackgroundColor(Color.WHITE)
                tx1?.setBackgroundColor(Color.TRANSPARENT)
                tx3?.setBackgroundColor(Color.TRANSPARENT)
            }
        }
        tx3?.setOnClickListener {
            if (a?.visibility == View.GONE) {
                s?.visibility = View.GONE
                f?.visibility = View.GONE
                a.visibility = View.VISIBLE
                tx2?.setBackgroundColor(Color.TRANSPARENT)
                tx1?.setBackgroundColor(Color.TRANSPARENT)
                tx3.setBackgroundColor(Color.WHITE)
            }
        }
        sources_nearest_time_btn?.setOnClickListener {
            selectedTime = nearestTime
            sources_seek.progress = nearestTime
            populateSources(master_data[0])
            updateDataSet()
        }

        if (::point.isInitialized) {
            scope.launch(Dispatchers.IO) {
                populateChart()
                fillAnnualMean()

                scope.launch(Dispatchers.Main) {
                    val txt = forecast_chart_ylable
                    txt?.text = getString(R.string.unit_concentration)
                    txt?.visibility = View.VISIBLE

                    if (pos == "") {
                        pos =
                            "${(point.latitude * 1000).toInt() / 1000.0}, ${(point.longitude * 1000).toInt() / 1000.0}"
                    }
                    val classes = listOf("grunnkrets", "delomrade", "kommune")
                    val classesString = listOf(
                        getString(R.string.basic_statistical_unit),
                        getString(R.string.sub_area),
                        getString(R.string.municipality)
                    )
                    val i = classes.indexOf(areaClass)


                    val str =
                        "${getString(R.string.position)}: $pos \n${getString(R.string.areaclass)}: ${classesString[i]}"
                    see_more_info?.text = str

                    see_more_info2?.text = str
                    sources_seek?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                        override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                            if (::master_data.isInitialized) {
                                selectedTime = p1
                                populateSources(master_data[0])
                                updateDataSet()
                            }


                        }

                        override fun onStartTrackingTouch(p0: SeekBar?) {}
                        override fun onStopTrackingTouch(p0: SeekBar?) {}
                    })
                }

            }

        }

        super.onViewCreated(view, savedInstanceState)
    }

    private fun showAddPlaceDialog(){
        val builder: AlertDialog.Builder = AlertDialog.Builder(parentFrg.mApp)
        val input: EditText? = EditText(parentFrg.mApp)
        input?.hint = getString(R.string.name_of_place)
        input?.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)


        builder.setTitle(getString(R.string.add_place))
        builder.setMessage(getString(R.string.add_custom_place))

        builder.setPositiveButton(
            getString(R.string.add_place)
        ) { dialog, _ -> // Do nothing but close the dialog
            val name = input?.text.toString()
            if (name == "") {
                dialog.dismiss()
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(
                        parentFrg.mApp,
                        getString(R.string.invalid_name),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            } else {
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(
                        parentFrg.mApp,
                        "$name ${getString(R.string.added)}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                parentFrg.mApp.addPlace(areaClass, name, point)
                dialog.dismiss()
            }

        }

        builder.setNegativeButton(
            getString(
                R.string.cancel
            )
        ) { dialog, _ -> // Do nothing
            dialog.dismiss()
        }

        val alert: AlertDialog = builder.create()
        alert.show()
    }

    private lateinit var master_data: MutableList<PointData>

    // Custom format
    @SuppressLint("SimpleDateFormat")
    private var nearestTime = 0
    private fun populateChart() {
        scope.launch(Dispatchers.IO) {
        // Get last two available reftimes
        val (_, _, result) = Fuel.get("https://in2000-apiproxy.ifi.uio.no/weatherapi/airqualityforecast/0.1/reftimes")
            .timeout(5000).responseObject<Times>()
        val (refParent: Times?, error) = result
        if (error != null) {
            Log.e("Airboiz", "oof1")
            scope.launch(Dispatchers.Main) {
                Toast.makeText(
                    parentFrg.mApp,
                    getString(R.string.error_get_data),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        // Get 0 8 and combine to one set

        val data = mutableListOf<PointData>()
            refParent?.reftimes?.let { refList ->
                mutableListOf(0, 4).forEach { i ->

                    val url =
                        "https://in2000-apiproxy.ifi.uio.no/weatherapi/airqualityforecast/0.1/?lat=${point.latitude}&lon=${point.longitude}&areaclass=$areaClass&reftime=${refList[i].replace(
                            ":",
                            "%3A"
                        )}"
                val (_, _, res) = Fuel.get(url).timeout(5000).responseObject<PointData>()
                val (pointData, err) = res
                if (err != null) {
                    Log.e("Airboiz", "Oof2 : ${err.message}")
                }

                pointData?.let {
                    data.add(pointData)
                }
                }
                scope.launch(Dispatchers.Main) {

                }
            if (data.size == 2) {
                var diff = 0f
                var i = 0
                data[0].data.time.forEach { time ->
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
                selectedTime = nearestTime
                master_data = data



                val date =
                    SimpleDateFormat("yy-MM-dd'T'HH:mm:ss'Z'").parse(data[1].data.time[0].from)
                date?.let { d ->

                    val diffMill = (Date().time - d.time).toFloat()
                    val seconds = diffMill / 1000
                    val minutes = seconds / 60
                    val hours = minutes / 60
                    // String
                    val l = LimitLine(hours, "<- ${getString(R.string.now)}")
                    l.lineWidth = 1f
                    val c = ResourcesCompat.getColor(
                        resources,
                        R.color.navigationTabs,
                        null
                    )
                    l.textColor = c
                    l.lineColor = c
                    l.textSize = 14f
                    l.labelPosition = LimitLine.LimitLabelPosition.RIGHT_BOTTOM
                    forecast_chart?.xAxis?.limitLines?.add(l)
                }


                // Statisk tekst
                val set1 = LineDataSet(createEntriesPM10(data), "PM10")
                val set2 = LineDataSet(createEntriesNO2(data), "NO2")
                val set3 = LineDataSet(createEntriesPM25(data), "PM2.5")
                val set4 = LineDataSet(createEntriesO3(data), "O3")
                setOption(set1, Color.BLUE)
                setOption(set2, Color.RED)
                setOption(set3, Color.CYAN)
                setOption(set4, Color.GREEN)

                scope.launch(Dispatchers.IO) {
                    populateSources(data[0])
                    updateDataSet()
                }


                val desc = Description()
                desc.text = "${getString(R.string.last_updated)}: ${data[0].meta.reftime}"
                forecast_chart?.description = desc

                forecast_chart?.let { forecast_chart.data = LineData(set1, set2, set3) }
                var ll = LimitLine(30f, getString(R.string.pm25_low_pollution))
                forecast_chart?.axisLeft?.addLimitLine(ll)
                ll = LimitLine(50f, getString(R.string.pm10_low_pollution))
                forecast_chart?.axisLeft?.addLimitLine(ll)
                ll = LimitLine(100f, getString(R.string.no2_low_pollution))
                forecast_chart?.axisLeft?.addLimitLine(ll)


                // Input earliest date
                forecast_chart?.xAxis?.valueFormatter = DateAxisFormatter(data[1].data.time[0].from)
                // Se bedre ut
                forecast_chart?.xAxis?.granularity = 1f
                forecast_chart?.xAxis?.labelCount = 10
                forecast_chart?.extraRightOffset = 15f
                forecast_chart?.extraLeftOffset = 15f
                forecast_chart?.xAxis?.labelRotationAngle = -30f
                forecast_chart?.xAxis?.position = XAxis.XAxisPosition.TOP_INSIDE
                forecast_chart?.axisLeft?.isEnabled = false
                forecast_chart?.axisRight?.spaceTop = 10f


                // Vise nå-tid


                see_more_show_03?.setOnCheckedChangeListener { _, b ->
                    forecast_chart?.let { toggleO3(forecast_chart, set4, b) }
                    }

                scope.launch(Dispatchers.Main) {
                    see_more_show_03?.visibility = View.VISIBLE
                    see_more_progress?.visibility = View.GONE
                    forecast_chart?.visibility = View.VISIBLE
                    forecast_chart?.invalidate()
                    sources_seek?.progress = nearestTime

                }


            }


        }
        }
    }


    // Auto indentation makes this very long :/
    private fun pieChartPm25(v: Variables): MutableList<PieEntry> {
        val l = mutableListOf<PieEntry>()
        l.add(
            PieEntry(
                v.pm25_local_fraction_heating.value?.toFloat() ?: 0f,
                getString(R.string.fraction_heating)
            )
        )
        l.add(
            PieEntry(
                v.pm25_local_fraction_industry.value?.toFloat() ?: 0f,
                getString(R.string.fraction_industry)
            )
        )
        l.add(
            PieEntry(
                v.pm25_local_fraction_shipping.value?.toFloat() ?: 0f,
                getString(R.string.fraction_shipping)
            )
        )
        l.add(
            PieEntry(
                v.pm25_local_fraction_traffic_exhaust.value?.toFloat() ?: 0f,
                getString(R.string.fraction_traffic_exhaust)
            )
        )
        l.add(
            PieEntry(
                v.pm25_local_fraction_traffic_nonexhaust.value?.toFloat() ?: 0f,
                getString(R.string.fraction_traffic_nonexhaust)
            )
        )
        l.add(
            PieEntry(
                v.pm25_nonlocal_fraction.value?.toFloat() ?: 0f,
                getString(R.string.fraction_nonlocal)
            )
        )
        l.add(
            PieEntry(
                v.pm25_nonlocal_fraction_seasalt.value?.toFloat() ?: 0f,
                getString(R.string.fraction_nonlocal_seasalt)
            )
        )
        val tmp: MutableList<PieEntry> = mutableListOf()
        l.forEach {
            if (it.value != 0f) {
                tmp.add(it)
            }
        }
        return tmp
    }

    private fun pieChartPm10(v: Variables): MutableList<PieEntry> {
        val l = mutableListOf<PieEntry>()
        l.add(
            PieEntry(
                v.pm10_local_fraction_heating.value?.toFloat() ?: 0f,
                getString(R.string.fraction_heating)
            )
        )
        l.add(
            PieEntry(
                v.pm10_local_fraction_industry.value?.toFloat() ?: 0f,
                getString(R.string.fraction_industry)
            )
        )
        l.add(
            PieEntry(
                v.pm10_local_fraction_shipping.value?.toFloat() ?: 0f,
                getString(R.string.fraction_shipping)
            )
        )
        l.add(
            PieEntry(
                v.pm10_local_fraction_traffic_exhaust.value?.toFloat() ?: 0f,
                getString(R.string.fraction_traffic_exhaust)
            )
        )
        l.add(
            PieEntry(
                v.pm10_local_fraction_traffic_nonexhaust.value?.toFloat() ?: 0f,
                getString(R.string.fraction_traffic_nonexhaust)
            )
        )
        l.add(
            PieEntry(
                v.pm10_nonlocal_fraction.value?.toFloat() ?: 0f,
                getString(R.string.fraction_nonlocal)
            )
        )
        l.add(
            PieEntry(
                v.pm10_nonlocal_fraction_seasalt.value?.toFloat() ?: 0f,
                getString(R.string.fraction_nonlocal_seasalt)
            )
        )
        val tmp: MutableList<PieEntry> = mutableListOf()
        l.forEach {
            if (it.value != 0f) {
                tmp.add(it)
            }
        }
        return tmp
    }

    private fun pieChartNo2(v: Variables): MutableList<PieEntry> {
        val l = mutableListOf<PieEntry>()
        l.add(
            PieEntry(
                v.no2_local_fraction_heating.value?.toFloat() ?: 0f,
                getString(R.string.fraction_heating)
            )
        )
        l.add(
            PieEntry(
                v.no2_local_fraction_industry.value?.toFloat() ?: 0f,
                getString(R.string.fraction_industry)
            )
        )
        l.add(
            PieEntry(
                v.no2_local_fraction_shipping.value?.toFloat() ?: 0f,
                getString(R.string.fraction_shipping)
            )
        )
        l.add(
            PieEntry(
                v.no2_local_fraction_traffic_exhaust.value?.toFloat() ?: 0f,
                getString(R.string.fraction_traffic_exhaust)
            )
        )
        l.add(
            PieEntry(
                v.no2_local_fraction_traffic_nonexhaust.value?.toFloat() ?: 0f,
                getString(R.string.fraction_traffic_nonexhaust)
            )
        )
        l.add(
            PieEntry(
                v.no2_nonlocal_fraction.value?.toFloat() ?: 0f,
                getString(R.string.fraction_nonlocal)
            )
        )
        l.add(
            PieEntry(
                v.no2_nonlocal_fraction_seasalt.value?.toFloat() ?: 0f,
                getString(R.string.fraction_nonlocal_seasalt)
            )
        )
        val tmp: MutableList<PieEntry> = mutableListOf()
        l.forEach {
            if (it.value != 0f) {
                tmp.add(it)
            }
        }
        return tmp
    }


    private fun formatSet(d: PieDataSet): PieData {
        d.colors = colors
        val data = PieData(d)
        data.setValueTextSize(13f)
        data.setValueFormatter(CustomPercentageFormatter())
        d.valueLinePart1Length = 0.6f
        d.valueLinePart2Length = 0.3f
        d.valueLineWidth = 2f
        d.valueLinePart1OffsetPercentage = 115f  // Line starts outside of chart
        d.isUsingSliceColorAsValueLineColor = true
        d.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        d.valueTextSize = 16f
        d.valueTypeface = Typeface.DEFAULT_BOLD
        d.selectionShift = 3f

        return data
    }

    private var selectedTime = 0
    private fun populateSources(data: PointData) {
        sources_chart?.setUsePercentValues(true)


        val pm25: MutableList<PieEntry> = pieChartPm25(data.data.time[selectedTime].variables)
        val pm25data = formatSet(PieDataSet(pm25, ""))
        pm25dataset = pm25data
        pm25string =
            "${((data.data.time[selectedTime].variables.pm25_concentration.value ?: 0.0 * 10).toInt()) / 10f}\n ${data.data.time[selectedTime].variables.pm25_concentration.units ?: 0.0}"

        val pm10: MutableList<PieEntry> = pieChartPm10(data.data.time[selectedTime].variables)
        val pm10data = formatSet(PieDataSet(pm10, ""))
        pm10dataset = pm10data
        pm10string =
            "${((data.data.time[selectedTime].variables.pm10_concentration.value ?: 0.0 * 10).toInt()) / 10f}\n ${data.data.time[selectedTime].variables.pm10_concentration.units ?: 0.0}"

        val no2: MutableList<PieEntry> = pieChartNo2(data.data.time[selectedTime].variables)
        val no2data = formatSet(PieDataSet(no2, ""))

        no2dataset = no2data
        no2string =
            "${((data.data.time[selectedTime].variables.no2_concentration.value ?: 0.0 * 10).toInt()) / 10f}\n ${data.data.time[selectedTime].variables.no2_concentration.units ?: 0.0}"

        sources_chart?.description = null




        sources_chart?.isDrawHoleEnabled = true

        sources_chart?.setExtraOffsets(40f, 0f, 40f, 5f)
        sources_chart?.setEntryLabelColor(Color.BLACK)
        sources_chart?.setDrawMarkers(false) // To remove markers when click
        sources_chart?.setDrawEntryLabels(false) // To remove labels from piece of pie

        sources_chart?.let { it.renderer = CustomPieChartRenderer(it, 10f) }
        sources_chart?.setHoleColor(Color.TRANSPARENT)


        sources_chart?.setDrawCenterText(true)
        sources_chart?.setCenterTextSize(20f)
        sources_chart?.setCenterTextTypeface(Typeface.DEFAULT_BOLD)
        sources_chart?.setCenterTextColor(Color.BLACK)


        val l: Legend? = sources_chart?.legend // get legend of pie

        l?.verticalAlignment =
            Legend.LegendVerticalAlignment.BOTTOM // set vertical alignment for legend
        l?.horizontalAlignment =
            Legend.LegendHorizontalAlignment.CENTER // set horizontal alignment for legend
        l?.orientation = Legend.LegendOrientation.HORIZONTAL // set orientation for legend
        l?.textSize = 15f
        sources_chart?.legend?.isWordWrapEnabled = true
        l?.setDrawInside(true) // set if legend should be drawn inside or not


        sources_spinner?.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedValue = position
                updateDataSet()
            }
        }

    }


    private fun toggleO3(chart: LineChart?, set: LineDataSet, value: Boolean) {
        chart?.let {
            scope.launch(Dispatchers.Main) {
                if (value) {
                    if (chart.data.dataSetCount == 3) {
                        chart.data.addDataSet(set)
                    }
                } else {
                    if (chart.data.dataSetCount == 4) {
                        chart.data.removeDataSet(set)
                    }
                }
                chart.invalidate()
                chart.notifyDataSetChanged()
            }
        }
    }

    private fun setOption(set: LineDataSet, color: Int) {
        set.color = color
        set.setDrawCircles(false)
        set.setDrawValues(false)
    }

    private fun createEntriesPM10(data: List<PointData>): MutableList<Entry> {
        val l = mutableListOf<Entry>()
        var i = 0f

        // [1]  = i går
        var j = 0
        data[1].data.time.forEach {
            if (j == 24) return@forEach
            l.add(Entry(i, it.variables.pm10_concentration.value?.toFloat() ?: 0f))

            j++
            i += 1f
        }
        j = 0
        data[0].data.time.forEach {
            if (j == 55) return@forEach
            l.add(Entry(i, it.variables.pm10_concentration.value?.toFloat() ?: 0f))

            j++
            i += 1f
        }


        return  l
    }

    private fun createEntriesO3(data: List<PointData>): MutableList<Entry> {
        val l = mutableListOf<Entry>()
        var i = 0f

        var j = 0
        data[1].data.time.forEach {
            if (j == 24) return@forEach
            l.add(Entry(i, it.variables.o3_concentration.value?.toFloat() ?: 0f))
            j++
            i += 1f
        }
        j = 0
        data[0].data.time.forEach {
            if (j == 55) return@forEach
            l.add(Entry(i, it.variables.o3_concentration.value?.toFloat() ?: 0f))
            j++
            i += 1f
        }


        return l
    }



    private fun createEntriesNO2(data: List<PointData>): MutableList<Entry> {
        val l = mutableListOf<Entry>()
        var i = 0f

        // [1]  = i går
        var j = 0
        data[1].data.time.forEach {
            if (j == 24) return@forEach
            l.add(Entry(i, it.variables.no2_concentration.value?.toFloat() ?: 0f))

            j++
            i += 1f
        }
        j = 0
        data[0].data.time.forEach {
            if (j == 55) return@forEach
            l.add(Entry(i, it.variables.no2_concentration.value?.toFloat() ?: 0f))

            j++
            i += 1f
        }


        return  l
    }
    private fun createEntriesPM25(data: List<PointData>): MutableList<Entry> {
        val l = mutableListOf<Entry>()
        var i = 0f

        // [1]  = i går
        var j = 0
        data[1].data.time.forEach {
            if (j == 24) return@forEach
            l.add(Entry(i, it.variables.pm25_concentration.value?.toFloat() ?: 0f))
            j++
            i += 1f
        }
        j = 0
        data[0].data.time.forEach {
            if (j == 55) return@forEach
            l.add(Entry(i, it.variables.pm25_concentration.value?.toFloat() ?: 0f))
            j++
            i += 1f
        }


        return  l
    }




    private class DateAxisFormatter(val dateString: String): ValueFormatter() {


        override fun getFormattedValue(value: Float): String {
            val s = SimpleDateFormat("dd-MM HH:00")
            val date = SimpleDateFormat("yy-MM-dd'T'HH:mm:ss'Z'").parse(dateString)
            date?.let { d->
                // Starts at 0, every step is one hour 3600*1000 * value
                val hours = 3600000*value
                d.time = d.time+hours.toLong()

                return s.format(d)
            }
            Log.e("Airboiz", "Error parsing date")
            return super.getFormattedValue(value)
        }

    }


    companion object {
        @JvmStatic
        fun newInstance(p0: LatLng, ac: String, p: String, frg: FragmentHelper) =
            SeeMoreFragment().apply {
                parentFrg = frg
                point = p0
                areaClass = ac
                pos = p

            }
    }

    private var pm25dataset: PieData? = null
    private var pm10dataset: PieData? = null
    private var no2dataset: PieData? = null

    private var pm25string: String = ""
    private var pm10string: String = ""
    private var no2string: String = ""

    private var selectedValue = 0
    private fun updateDataSet() {

        scope.launch(Dispatchers.Main) {
            sources_selected_time?.text = master_data[0].data.time[selectedTime].from

            when (selectedValue) {
                0 -> pm25dataset?.let {
                    sources_chart?.data = pm25dataset; sources_chart?.centerText = pm25string
                }
                1 -> pm10dataset?.let {
                    sources_chart?.data = pm10dataset; sources_chart?.centerText = pm10string
                }
                2 -> no2dataset?.let {
                    sources_chart?.data = no2dataset; sources_chart?.centerText = no2string
                }
            }
            sources_chart?.notifyDataSetChanged()
            sources_chart?.invalidate()
        }
    }

    /**
     * Converts a LatLng object to a list of {kommune, fylke, land}
     * @param latLng is the latlng to be converted.
     * @return a List<String> with 3 values: {kommune, fylke, Land}
     * Blocking
     */

    private fun latLngToRegionList(latLng: LatLng): List<String> {
        val list: MutableList<String> = mutableListOf()
        val geoKey: String = parentFrg.mApp.getString(R.string.geocoding_key)
        val url =
            "https://maps.googleapis.com/maps/api/geocode/json?latlng=${latLng.latitude},${latLng.longitude}&key=${geoKey}"
        val tag = "latLngToFylke(): "
        var kommune = ""
        var fylke = ""
        var land = ""
            val (_, response, result)
                    = Fuel.get(url).timeout(5000).responseObject<GeeCoding>()
            if (response.statusCode == 200) {
                val (geo, error) = result
                if (error != null) {
                    Log.w(tag, "status code NOT OK!")
                    scope.launch(Dispatchers.Main) {
                        Toast.makeText(
                            parentFrg.mApp,
                            getString(R.string.error_get_data),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } else {
                    geo?.results?.forEach {
                        it.address_components.forEach { res ->
                            when {
                                res.types[0] == "administrative_area_level_2" -> {
                                    if (kommune == "") {
                                        kommune = res.long_name
                                        kommune = kommune.replace("kommune", "")

                                    }
                                }
                                res.types[0] == "administrative_area_level_1" -> {
                                    if (fylke == "") fylke = res.long_name
                                }
                                res.types[0] == "country" -> {
                                    if (land == "") land = res.long_name
                                }
                            }
                        }
                    }

                }
            } else {
                Log.w(tag, "status code NOT OK!")
            }
        list.add(kommune.trim())
        list.add(fylke.trim())
        list.add(land.trim())

        return list
    }

    private fun getAreaCode(): String {
        val region = latLngToRegionList(point)
        val file = parentFrg.mApp.assets.open("Available.json")
        val avP: AvParent = Gson().fromJson(InputStreamReader(file))

        val av = avP.available.find { av ->
            av.timelabel == "2018" && av.areaname == region[0]
        }
        av ?: return "NULL"
        return av.areacode
    }


    private fun fillAnnualMean() {
        scope.launch(Dispatchers.IO) {
            val mApp = parentFrg.mApp
            val areaCode = getAreaCode()
            if (areaCode != "NULL") {
                // Get annualmean
                val cats = listOf("pm25", "pm10", "no2")
                val name = hashMapOf<String, String>()
                name["pm25"] = getString(R.string.pm25)
                name["pm10"] = getString(R.string.pm10)
                name["no2"] = getString(R.string.no2)

                scope.launch(Dispatchers.Main) {

                    var headerRow = TableRow(mApp)
                    headerRow.addView(newHeaderTv("Type"))
                    headerRow.addView(newHeaderTv(getString(R.string.max_annualmean)))
                    val tv1 = newHeaderTv(getString(R.string.annual_mean))
                    tv1.measure(0, 0)
                    headerRow.addView(tv1)
                    headerRow.addView(newHeaderTv(getString(R.string.period)))
                    headerRow.addView(Space(mApp))
                    formatRow(headerRow)

                    see_more_annualmean_table?.addView(headerRow)

                    headerRow = TableRow(mApp)
                    headerRow.addView(newHeaderTv("Type"))
                    headerRow.addView(newHeaderTv(getString(R.string.max_annualmean)))
                    val tv2 = newHeaderTv(getString(R.string.max_over))
                    tv2.width = tv1.measuredWidth
                    headerRow.addView(tv2)
                    headerRow.addView(newHeaderTv(getString(R.string.period)))
                    headerRow.addView(Space(mApp))
                    headerRow.addView(Space(mApp))
                    formatRow(headerRow)

                    see_more_shortterm_table?.addView(headerRow)
                }
                var url = ""
                var error404 = false
                cats.forEach { cat ->
                    scope.launch(Dispatchers.IO) {
                        url =
                            "https://airquality-expert-ifi.met.no/airqualityexpert/0.2/point_dataset/$areaCode/2018/${cat}_concentration_annualmean?lat=${point.latitude}&lon=${point.longitude}"
                        val (_, _, res) = Fuel.get(url).timeout(5000)
                            .responseObject<AnnualmeanPoint>()
                        val (data, err) = res
                        if (err != null) {
                            Log.e("Airboiz", "Oof : ${err.message}")
                                error404 = true
                        }
                        scope.launch(Dispatchers.Main) {

                            val table: TableLayout? = see_more_annualmean_table
                            data?.data?.let { data ->
                                val row: TableRow? = TableRow(mApp)
                                row?.addView(newTv(name[cat] ?: ""))
                                val value: CValue
                                var maxValue = 0

                                when (cat) {
                                    "pm25" -> {
                                        value = data.variables.pm25_concentration

                                        maxValue = 15

                                    }
                                    "pm10" -> {
                                        value = data.variables.Pm10_concentration
                                        maxValue = 25

                                    }
                                    else -> {
                                        value = data.variables.no2_concentration
                                        maxValue = 40
                                    }
                                }
                                row?.addView(newTv("$maxValue ${value.units ?: 0.0}"))
                                row?.addView(newTv("${round(value.value ?: 0.0)} ${value.units ?: 0.0}"))
                                row?.addView(newTv(getString(R.string.one_year)))

                                val img = if (value.value ?: 0.0 < maxValue) {
                                    newColor(Color.GREEN)
                                } else {
                                    newColor(Color.RED)
                                }

                                row?.addView(img)

                                formatRow(row)
                                table?.addView(row)
                            }
                            table?.visibility = View.VISIBLE
                            // Peak
                        }
                    }


                }
                scope.launch(Dispatchers.IO) {
                    url =
                        "https://airquality-expert-ifi.met.no/airqualityexpert/0.2/point_dataset/$areaCode/2018/pm10_concentration_31_highest_daily_value_inyear?lat=${point.latitude}&lon=${point.longitude}"
                    val (_, _, respm10) = Fuel.get(url).timeout(5000)
                        .responseObject<AnnualmeanPoint>()
                    val (datapm10, error) = respm10
                    if (error != null) {
                        Log.e("Airboiz", "Oof : ${error.message}")
                    }
                    scope.launch(Dispatchers.Main) {
                        val table: TableLayout? = see_more_shortterm_table
                        // PM10
                        datapm10?.data?.let { data ->
                            val row = TableRow(mApp)
                            row.addView(newTv("PM10"))
                            row.addView(newTv("50 ${data.variables.pm10_concentration_31_highest_daily_value_inyear.units ?: 0.0}"))
                            row.addView(newTv(getString(R.string.pm10_max_days_yearly)))
                            row.addView(newTv(getString(R.string.one_day)))
                            row.addView(Space(mApp))
                            val img =
                                if (data.variables.pm10_concentration_31_highest_daily_value_inyear.value ?: 0.0 < 50.0) {
                                    newColor(Color.GREEN)
                                } else {
                                    newColor(Color.RED)
                                }
                            row.addView(img)
                            formatRow(row)

                            table?.addView(row)


                        }

                        // NO2
                    }
                }

                // Get peak
                url =
                    "https://airquality-expert-ifi.met.no/airqualityexpert/0.2/point_dataset/$areaCode/2018/no2_concentration_19_highest_hourly_value_inyear?lat=${point.latitude}&lon=${point.longitude}"
                val (_, _, resno2) = Fuel.get(url).timeout(5000).responseObject<AnnualmeanPoint>()
                val (datano2, err) = resno2
                if (err != null) {
                    Log.e("Airboiz", "Oof : ${err.message}")
                    mApp.runOnUiThread {
                        Toast.makeText(mApp, getString(R.string.error_get_data), Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                scope.launch(Dispatchers.Main) {
                    val table: TableLayout? = see_more_shortterm_table
                    // PM10
                    datano2?.data?.let { data ->
                        val row = TableRow(mApp)
                        row.addView(newTv("NO2"))
                        row.addView(newTv("200 ${data.variables.no2_concentration_19_highest_hourly_value_inyear.units ?: 0.0}"))
                        row.addView(newTv(getString(R.string.no2_max_hours_year)))
                        row.addView(newTv(getString(R.string.one_hour)))
                        row.addView(Space(mApp))
                        val img =
                            if (data.variables.no2_concentration_19_highest_hourly_value_inyear.value ?: 0.0 < 200.0) {
                                newColor(Color.GREEN)
                            } else {
                                newColor(Color.RED)
                            }
                        row.addView(img)
                        formatRow(row)
                        table?.addView(row)
                    }
                    // NO2
                }
                // If areaCode was wrong
                if (error404) {
                    scope.launch(Dispatchers.Main) {
                        val ll = annual_ll
                        val tv = TextView(mApp)
                        tv.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                        tv.text = getString(R.string.error_occured)
                        tv.textSize = 24f
                        ll.addView(tv)
                    }
                }
            } else {
                scope.launch(Dispatchers.Main) {
                    val ll = annual_ll
                    val tv = TextView(mApp)
                    tv.text = getString(R.string.could_not_determine_location)
                    tv.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                    tv.textSize = 24f
                    ll.addView(tv)
                }
            }
        }
    }

    private fun newTv(text: String): TextView {
        val tv = TextView(parentFrg.mApp)
        tv.text = text
        tv.textSize = 14f
        tv.setPadding(10, 0, 10, 0)
        return tv
    }

    private fun newColor(c: Int): View {
        val img = layoutInflater.inflate(R.layout.colored_box, null)
        img.textView15.background = c.toDrawable()
        return img
    }
    private fun newHeaderTv(text: String): TextView {
        val tv = TextView(parentFrg.mApp)
        tv.text = text
        tv.textSize = 18f
        tv.movementMethod = ScrollingMovementMethod()
        tv.setPadding(10, 0, 10, 0)
        tv.setTypeface(null, Typeface.BOLD)
        return tv
    }

    fun reset() {
        forecast_chart?.clear()
    }
    private fun formatRow(row: TableRow?) {
        row?.let {
            row.gravity = Gravity.CENTER_HORIZONTAL
            row.showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
            row.dividerDrawable = Color.BLACK.toDrawable()
            row.setPadding(5, 5, 5, 5)
        }

    }

}
