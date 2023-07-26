package com.lyf.mediarecord.recorder.utils

import android.graphics.Rect
import android.media.Image

object ImageUtils {
    fun imageToYuv420(image: Image, cropRect: Rect): ByteArray {
        val width = cropRect.width()
        val height = cropRect.height()

        val imagePlanes = image.planes
        val yuvBytes = arrayOf(
            ByteArray(imagePlanes[0].buffer.capacity()),
            ByteArray(imagePlanes[1].buffer.capacity() / 2),
            ByteArray(imagePlanes[2].buffer.capacity() / 2)
        )

        for (i in imagePlanes.indices) {
            val buffer = imagePlanes[i].buffer
            val pixelStride = imagePlanes[i].pixelStride
            val rowStride = imagePlanes[i].rowStride
            val planeWidth = if (i == 0) width else width / 2
            val planeHeight = if (i == 0) height else height / 2

            val bufferOffset = cropRect.top * rowStride + cropRect.left * pixelStride
            val rowOffset = if (i == 0) 0 else 1
            val pixelOffset = if (i == 0) 0 else 2

            for (row in 0 until planeHeight) {
                val rowBytes = ByteArray(rowStride)
                buffer.position(bufferOffset + row * rowStride)
                buffer.get(rowBytes, 0, rowStride)
                for (col in 0 until planeWidth) {
                    val colOffset = col * pixelStride
                    yuvBytes[0][row * width + col + pixelOffset + rowOffset * width] =
                        rowBytes[colOffset + pixelOffset]
                    if (i == 0 && row % 2 == 0 && col % 2 == 0) {
                        val chromaOffset = colOffset + pixelStride
                        yuvBytes[1][row * width / 2 + col / 2] = rowBytes[chromaOffset + pixelOffset]
                    } else if (i == 0 && row % 2 == 1 && col % 2 == 1) {
                        val chromaOffset = colOffset + pixelStride
                        yuvBytes[2][row * width / 2 + col / 2] = rowBytes[chromaOffset + pixelOffset]
                    }
                }
            }
        }

        val yuvBytesLength = yuvBytes[0].size + yuvBytes[1].size + yuvBytes[2].size
        val yuvBytesOutput = ByteArray(yuvBytesLength)

        System.arraycopy(yuvBytes[0], 0, yuvBytesOutput, 0, yuvBytes[0].size)
        System.arraycopy(yuvBytes[1], 0, yuvBytesOutput, yuvBytes[0].size, yuvBytes[1].size)
        System.arraycopy(yuvBytes[2], 0, yuvBytesOutput, yuvBytes[0].size + yuvBytes[1].size, yuvBytes[2].size)

        return yuvBytesOutput
    }
}