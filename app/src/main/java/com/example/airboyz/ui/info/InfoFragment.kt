package com.example.airboyz.ui.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.airboyz.R


class InfoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_info, container, false)
        // Values are added here
        // Must also be created in layout xml
        val exp: MutableList<Triple<Int, Int, Triple<Int, Int, Int>>> = mutableListOf()
        exp.add(
            Triple(
                R.string.head_desc, R.string.info_desc,
                Triple(R.id.header1, R.id.expand_txt_1, R.id.expand_btn_1)
            )
        )
        exp.add(
            Triple(
                R.string.head_what, R.string.info_what,
                Triple(R.id.header2, R.id.expand_txt_2, R.id.expand_btn_2)
            )
        )
        exp.add(
            Triple(
                R.string.head_why, R.string.info_why,
                Triple(R.id.header3, R.id.expand_txt_3, R.id.expand_btn_3)
            )
        )
        exp.add(
            Triple(
                R.string.head_who, R.string.info_who,
                Triple(R.id.header4, R.id.expand_txt_4, R.id.expand_btn_4)
            )
        )
        exp.add(
            Triple(
                R.string.head_where, R.string.info_where,
                Triple(R.id.header5, R.id.expand_txt_5, R.id.expand_btn_5)
            )
        )
        exp.forEach {
            val expHead: TextView = root.findViewById(it.third.first)
            val expDesc: TextView = root.findViewById(it.third.second)
            val descHead = getString(it.first)
            val descFill = getString(it.second)
            val btn: ImageButton = root.findViewById(it.third.third)

            expHead.text = HtmlCompat.fromHtml(descHead, HtmlCompat.FROM_HTML_MODE_LEGACY)
            expDesc.text = ""
            btn.setOnClickListener {
                if (!expDesc.isVisible) {
                    btn.setImageResource(R.drawable.ic_collapse_large_holo_light)
                    expDesc.visibility = View.VISIBLE
                    expDesc.text =  HtmlCompat.fromHtml(descFill, HtmlCompat.FROM_HTML_MODE_LEGACY)
                } else {
                    btn.setImageResource(R.drawable.ic_expand_large_holo_light)
                    expDesc.text = ""
                    expDesc.visibility = View.GONE

                }

            }
        }
        return root
    }


}
