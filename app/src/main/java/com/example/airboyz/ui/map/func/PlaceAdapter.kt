package com.example.airboyz.ui.map.func

import android.graphics.Typeface
import android.text.style.CharacterStyle
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.airboyz.R
import com.google.android.libraries.places.api.model.AutocompletePrediction


interface OnItemClickListener{
    fun onItemClicked(prediction: AutocompletePrediction)
}


class TextViewHolder(private val textView: TextView) : RecyclerView.ViewHolder(textView){
    // The type of the primary text to be returned by predictions
    private val styleBold: CharacterStyle = StyleSpan(Typeface.BOLD)

    fun bind(prediction: AutocompletePrediction, clickListener: OnItemClickListener)
    {
        // - replace the contents of the view with that element
        textView.text = prediction.getPrimaryText(styleBold)

        // if user selects a prediction, pass the selected rediction to the KartFragment to be handled by onItemClicked()
        textView.setOnClickListener {
            clickListener.onItemClicked(prediction)
        }
    }
}


class PlaceAdapter(
    private var predictions: MutableList<AutocompletePrediction>,
    private val itemClickListener: OnItemClickListener
    )
        : RecyclerView.Adapter<TextViewHolder>()
{
    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.autocomplete, parent, false) as TextView
        return TextViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: TextViewHolder, position: Int) {
        // - get element from prediction list at this position
        val autocompletePrediction: AutocompletePrediction = predictions[position]
        holder.bind(autocompletePrediction, itemClickListener)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = predictions.size

}
