package com.example.airboyz.ui.map.func

import android.content.res.AssetManager
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import com.google.android.gms.maps.model.TileProvider.NO_TILE

class KartTileProvider(
    private val assets: AssetManager,
    private var mapType: String,
    private var timeLable: String
) :
    TileProvider {

    override fun getTile(x: Int, y: Int, zoom: Int): Tile {
        val inn = assets.open(getTileFilename(x, fixYCoordinate(y, zoom), zoom)).readBytes()
        return if (inn.isEmpty()) {
            NO_TILE
        } else {
            Tile(256, 256, inn)
        }
    }


    private fun getTileFilename(x: Int, y: Int, zoom: Int): String {
        return "maps/$timeLable/$mapType/$zoom/$x/$y.png"
    }

    private fun fixYCoordinate(y: Int, zoom: Int): Int {
        val size = 1 shl zoom // size = 2^zoom
        return size - 1 - y
    }
}