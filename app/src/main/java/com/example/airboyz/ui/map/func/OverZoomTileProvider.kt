package com.example.airboyz.ui.map.func

import android.graphics.*
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import java.io.ByteArrayOutputStream


// Provides images for googlemaps tileOverlay when user passes maxZoom
// Divides bitmaps into quadrants and uses these as tiles

class OverZoomTileProvider(private val mTileProvider: TileProvider) : TileProvider {
    private val tilePainter = Paint()

    override fun getTile(x: Int, y: Int, zoom: Int): Tile {
        if (zoom <= MAX_ZOOM) return mTileProvider.getTile(x, y, zoom)
        // Draw tile if overzoom
        val image = Bitmap.createBitmap(
            TILE_SIZE, TILE_SIZE,
            Bitmap.Config.ARGB_8888
        )
        image.eraseColor(Color.TRANSPARENT)
        val canvas = Canvas(image)
        drawTile(canvas, zoom, x, y)
        val data = bitmapToByteArray(image)
        image.recycle()
        return Tile(
            TILE_SIZE,
            TILE_SIZE,
            data
        )
    }

    private fun drawTile(canvas: Canvas, zoom: Int, x: Int, y: Int) {
        val bitmap = getTileAsBitmap(x, y, zoom)
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, 0f, 0f, tilePainter)
            bitmap.recycle()
        }
    }

    private fun getTileAsBitmap(x: Int, y: Int, zoom: Int): Bitmap? {
        if (zoom <= MAX_ZOOM) {
            val tile = mTileProvider.getTile(x, y, zoom)
            return if (tile == TileProvider.NO_TILE) {
                null
            } else BitmapFactory.decodeByteArray(tile.data, 0, tile.data.size)
        }
        val leftColumn = x % 2 == 0
        val topRow = y % 2 == 0
        var bitmap = getTileAsBitmap(x / 2, y / 2, zoom - 1)
        val quadrant: Int
        quadrant = if (leftColumn && topRow) {
            1
        } else if (!leftColumn && topRow) {
            2
        } else if (leftColumn) {
            3
        } else {
            4
        }
        when (quadrant) {
            1 -> bitmap = Bitmap.createBitmap(
                bitmap!!,
                0,
                0,
                HALF_TILE_SIZE,
                HALF_TILE_SIZE
            )
            2 -> bitmap = Bitmap.createBitmap(
                bitmap!!,
                HALF_TILE_SIZE,
                0,
                HALF_TILE_SIZE,
                HALF_TILE_SIZE
            )
            3 -> bitmap = Bitmap.createBitmap(
                bitmap!!,
                0,
                HALF_TILE_SIZE,
                HALF_TILE_SIZE,
                HALF_TILE_SIZE
            )
            4 -> bitmap = Bitmap.createBitmap(
                bitmap!!,
                HALF_TILE_SIZE,
                HALF_TILE_SIZE,
                HALF_TILE_SIZE,
                HALF_TILE_SIZE
            )
        }
        return Bitmap.createScaledBitmap(
            bitmap!!,
            TILE_SIZE,
            TILE_SIZE,
            false
        )

    }

    private fun bitmapToByteArray(bm: Bitmap): ByteArray {
        val bos = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.PNG, 100, bos)
        val data = bos.toByteArray()
        try {
            bos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return data
    }

    companion object {
        const val MAX_ZOOM = 11
        private const val TILE_SIZE = 256
        private const val HALF_TILE_SIZE = TILE_SIZE / 2
    }

}