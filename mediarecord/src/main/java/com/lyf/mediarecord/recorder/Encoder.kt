package com.lyf.mediarecord.recorder

import android.media.*
import android.util.Log
import com.lyf.mediarecord.recorder.data.EncoderParams
import com.lyf.mediarecord.recorder.utils.CameraXEncoder
import kotlinx.coroutines.delay
import java.lang.ref.WeakReference

class Encoder : Thread() {
    private var mParamsRef: WeakReference<EncoderParams>? = null
    private var mColorFormat = 0
    private var mMuxer: MediaMuxer? = null
    private var mMediaCodec: MediaCodec? = null
    private var isRecording = false
    private var videoTrack = 0

    override fun run() {
        if (!isRecording) {
            startRecoding()
        }
        while (mMediaCodec != null) {
            while (true) {
                //获得输出缓冲区 (编码后的数据从输出缓冲区获得)
                val bufferInfo = MediaCodec.BufferInfo()
                val encoderStatus = mMediaCodec!!.dequeueOutputBuffer(bufferInfo, 10000)
                //稍后重试
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    //输出格式发生改变  第一次总会调用，所以在这里开启混合器
                    val newFormat = mMediaCodec!!.outputFormat
                    videoTrack = mMuxer!!.addTrack(newFormat)
                    mMuxer!!.start()
                } else {
                    //正常则 encoderStatus 获得缓冲区下标
                    val encodedData = mMediaCodec!!.getOutputBuffer(encoderStatus)
                    //如果当前的buffer是配置信息，不管它 不用写出去
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size != 0) {
                        //设置从哪里开始读数据(读出来就是编码后的数据)
                        encodedData!!.position(bufferInfo.offset)
                        //设置能读数据的总长度
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        //写出为mp4
                        mMuxer!!.writeSampleData(videoTrack, encodedData, bufferInfo)
                    }
                    // 释放这个缓冲区，后续可以存放新的编码后的数据啦
                    mMediaCodec!!.releaseOutputBuffer(encoderStatus, false)
                }
            }
        }
        stopRecording()
    }

    fun queueEncode(data: ByteArray) {
        if (!isRecording && mMediaCodec == null) return
        val index = mMediaCodec!!.dequeueInputBuffer(0)
        if (index >= 0) {
            val inputBuffer = mMediaCodec!!.getInputBuffer(index)
            inputBuffer?.clear()
            inputBuffer?.put(data, 0, data.size)
            mMediaCodec?.queueInputBuffer(index, 0, data.size, System.nanoTime() / 1000, 0)
        }
    }

    companion object {
        const val TAG = "VideoCodec"
    }

    fun setEncoderParam(params: EncoderParams) {
        mParamsRef = WeakReference<EncoderParams>(params)
    }

    private fun startRecoding() {
        if (mParamsRef?.get() == null) {
            Log.e(TAG, "This mParamsRef is null")
            return
        }
        val mParams = mParamsRef!!.get()
        try {
            val mCodecInfo = selectSupportCodec(MediaFormat.MIMETYPE_VIDEO_AVC)
            mColorFormat = selectSupportColorFormat(mCodecInfo!!, MediaFormat.MIMETYPE_VIDEO_AVC)
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            mMuxer = MediaMuxer(mParams!!.videoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (e: Exception) {
            Log.e(TAG, "[create MediaCodec、ColorFormat、Muxer fail]----->cause is $e")
            e.printStackTrace()
        }
        val format = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            mParams!!.videoWidth, mParams.videoHeight
        )
        //色彩空间
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, mColorFormat)
        //码率
        format.setInteger(MediaFormat.KEY_BIT_RATE, mParams.bitRate)
        //帧率 fps
        format.setInteger(MediaFormat.KEY_FRAME_RATE, mParams.frameRate)
        //关键帧间隔
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        mMediaCodec!!.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mMediaCodec!!.start()
        isRecording = true
    }

    private fun stopRecording(){
        isRecording=true
        mMediaCodec?.apply {
            stop()
            release()
        }
        mMediaCodec=null
    }

    fun stopMuxer(){
        mMuxer?.apply {
            stop()
            release()
        }
        mMuxer=null
    }

    /**
     * 遍历所有编解码器，返回第一个与指定MIME类型匹配的编码器
     * 判断是否有支持指定mime类型的编码器
     */
    private fun selectSupportCodec(mimeType: String): MediaCodecInfo? {
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        val codecInfos = codecList.codecInfos
        for (codecInfo in codecInfos) {
            // 判断是否为编码器，否则直接进入下一次循环
            if (!codecInfo.isEncoder) {
                continue
            }
            // 如果是编码器，判断是否支持Mime类型
            val types = codecInfo.supportedTypes
            for (j in types.indices) {
                if (types[j].equals(mimeType, ignoreCase = true)) {
                    Log.d(CameraXEncoder.TAG, "使用的编码器----->$codecInfo")
                    return codecInfo
                }
            }
        }
        return null
    }

    /**
     * 根据mime类型匹配编码器支持的颜色格式
     */
    private fun selectSupportColorFormat(mCodecInfo: MediaCodecInfo, mimeType: String): Int {
        val capabilities = mCodecInfo.getCapabilitiesForType(mimeType)
        for (i in capabilities.colorFormats.indices) {
            val colorFormat = capabilities.colorFormats[i]
            if (isCodecRecognizedFormat(colorFormat)) {
                Log.d(CameraXEncoder.TAG, "支持的颜色格式$colorFormat")
                return colorFormat
            }
        }
        return 0
    }

    private fun isCodecRecognizedFormat(colorFormat: Int): Boolean {
        return when (colorFormat) {
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar, MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar -> true
            else -> false
        }
    }
}