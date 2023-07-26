package com.lyf.mediarecord.recorder

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.lyf.mediarecord.recorder.data.EncoderParams
import com.lyf.mediarecord.recorder.utils.CameraXManger
import java.nio.ByteBuffer

class RecordMp4 private constructor(context: Context) {
    companion object {
        private const val TAG = "RecordMp4"
        private var recordMp4: RecordMp4? = null
        fun getInstance(context: Context): RecordMp4 {
            if (recordMp4 == null) {
                recordMp4 = RecordMp4(context)
            }
            return recordMp4!!
        }
    }

    private var mCameraXManger: CameraXManger? = null
    private var mEncoder: Encoder? = null
    private var mParams: EncoderParams? = null

    init {
        mCameraXManger = CameraXManger.getInstance(context)
    }

    fun startCamera(previewView: PreviewView, context: FragmentActivity) {
        mCameraXManger?.apply {
            setPreviewView(previewView, context)
            startPreview()
            openCamera()
        }
    }

    fun stopCamera() {
        mCameraXManger?.stopPreview()
        mCameraXManger?.stopCamera()
    }

    fun startRecord() {
        if (mParams == null) throw IllegalStateException("EncoderParams can not be null,need call setEncodeParams method!")
        mEncoder = Encoder()
        mEncoder?.run {
            setEncoderParam(mParams!!)
            start()
        }
    }

    fun stopRecord() {
        mEncoder?.stopMuxer()
        val t = mEncoder
        mEncoder=null
        t?.interrupt()
    }

    fun setEncoderParams(encoderParams: EncoderParams) {
        this.mParams = encoderParams
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun setImageAnalysis(imageAnalyzer: ImageAnalysis, context: Context) {
        mCameraXManger?.setImageAnalysis(imageAnalyzer)
        // 设置ImageAnalysis用例的分析器，获取预览帧数据并传递给编码器
        imageAnalyzer.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
            val buffer: ByteBuffer = imageProxy.planes[0].buffer // 获取 Y 分量数据的 ByteBuffer
            val data = ByteArray(buffer.remaining()) // 创建一个和 ByteBuffer 大小相等的 ByteArray
            buffer.get(data) // 将 ByteBuffer 中的数据拷贝到 ByteArray 中
            // 将 data 传递给您的编码器进行编码
            mEncoder?.queueEncode(data)
            Log.d("test", "data---->" + data.size)
            imageProxy.close()
        }
    }
}