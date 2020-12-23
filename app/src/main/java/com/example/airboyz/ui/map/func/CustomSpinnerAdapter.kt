package com.example.airboyz.ui.map.func

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import com.example.airboyz.R
import com.example.airboyz.dataclasses.MapType

class CustomSpinnerAdapter(context: Context,
                           private val layoutResource: Int,
                           private val mapTypes: List<MapType>)
    : ArrayAdapter<MapType>(context, layoutResource, mapTypes) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        return createViewFromResource(position, convertView, parent)
    }

    private fun createViewFromResource(position: Int, convertView: View?, parent: ViewGroup?): View {
        val layout: LinearLayout = convertView as LinearLayout? ?:
        LayoutInflater.from(context).inflate(layoutResource, parent, false) as LinearLayout

        val titleTextview = layout.findViewById<TextView>(R.id.map_title_text_view)
        val fullnameTextview = layout.findViewById<TextView>(R.id.map_full_text_view)
        val descriptionTextview = layout.findViewById<TextView>(R.id.map_description_text_view)

        titleTextview.text = mapTypes[position].title
        descriptionTextview.text = mapTypes[position].description
        fullnameTextview.text = mapTypes[position].fullName
        return layout
    }


}