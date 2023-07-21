package com.lyf.mediarecord.sw

import android.content.Context
import android.text.TextUtils
import java.io.File
class TxtOverlay(private val context: Context) {
    private var ctx: Long = 0

    init {
        System.loadLibrary("TxtOverlay")
    }

    fun init(width: Int, height: Int, fonts: String) {
        require(!TextUtils.isEmpty(fonts)) { "the font file must be valid!" }
        require(File(fonts).exists()) { "the font file must be exists!" }
        ctx = txtOverlayInit(width, height, fonts)
    }

    fun overlay(data: ByteArray, txt: String) {
        if (ctx == 0L) return
        txtOverlay(ctx, data, txt)
    }

    fun release() {
        if (ctx == 0L) return
        txtOverlayRelease(ctx)
        ctx = 0
    }

    private external fun txtOverlayInit(width: Int, height: Int, fonts: String): Long

    private external fun txtOverlay(ctx: Long, data: ByteArray, txt: String)

    private external fun txtOverlayRelease(ctx: Long)
}