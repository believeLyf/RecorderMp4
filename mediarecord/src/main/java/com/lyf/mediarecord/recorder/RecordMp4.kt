package com.lyf.mediarecord.recorder

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.lyf.mediarecord.recorder.data.EncoderParams
import com.lyf.mediarecord.recorder.utils.CameraXEncoder
import com.lyf.mediarecord.recorder.utils.CameraXManger

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
    private var mCameraXEncoder: CameraXEncoder? = null
    private var mParams: EncoderParams?=null

    init {
        mCameraXManger = CameraXManger.getInstance(context)
    }

    fun startCamera(previewView: PreviewView,context: FragmentActivity) {
        mCameraXManger?.apply {
            setPreviewView(previewView,context)
            startPreview()
            openCamera()
        }
    }

    fun stopCamera() {
        mCameraXManger?.stopPreview()
        mCameraXManger?.stopCamera()
    }

    fun startRecord(){
        if(mParams==null) throw IllegalStateException("EncoderParams can not be null,need call setEncodeParams method!")
        mCameraXEncoder=CameraXEncoder()
        mCameraXEncoder?.run {
            setEncoderParam(mParams!!)
            startRecoding()
        }
    }

    fun stopRecord(){
        mCameraXEncoder?.stopRecord()
    }

    fun setEncoderParams(encoderParams: EncoderParams){
        this.mParams=encoderParams
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun setImageAnalysis(imageAnalyzer: ImageAnalysis,context: Context) {
        mCameraXManger?.setImageAnalysis(imageAnalyzer)
        // 设置ImageAnalysis用例的分析器，获取预览帧数据并传递给编码器
        imageAnalyzer.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
            val buffer = imageProxy.planes[0].buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)
            mCameraXEncoder?.queueEncode(data)
            imageProxy.close()
        }
    }
}