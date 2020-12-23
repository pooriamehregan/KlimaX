package com.example.airboyz.ui.map.func

import com.example.airboyz.MainActivity

interface FragmentHelper {
    // Interface to enable SeeMoreFragment transition
    var mApp: MainActivity
    fun removeSeeMore()
}