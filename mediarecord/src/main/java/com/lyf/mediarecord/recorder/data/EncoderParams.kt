package com.lyf.mediarecord.recorder.data

data class EncoderParams(
    var videoPath: String = "/sdcard/aaa.mp4",
    var videoWidth: Int = 640,
    var videoHeight: Int = 480,
    var bitRate: Int = 800000,
    var frameRate: Int = 30,
)