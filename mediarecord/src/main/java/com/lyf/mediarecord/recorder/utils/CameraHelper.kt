package com.lyf.mediarecord.recorder.utils

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.Camera.PreviewCallback

class CameraHelper(var width: Int, var height: Int) : PreviewCallback {
    private var cameraId: Int=-1
    private var mCamera: Camera? = null
    private lateinit var buffer: ByteArray
    private var mPreviewCallback: PreviewCallback? = null
    private var mSurfaceTexture: SurfaceTexture? = null
    fun stopPreview() {
        if (mCamera != null) {
            //预览数据回调接口
            mCamera!!.setPreviewCallback(null)
            //停止预览
            mCamera!!.stopPreview()
            //释放摄像头
            mCamera!!.release()
            mCamera = null
        }
    }

    fun startPreview(surfaceTexture: SurfaceTexture?) {
        stopPreview()
        try {
            mSurfaceTexture = surfaceTexture
            //获得camera对象
            mCamera = Camera.open()
            //配置camera的属性
            val parameters = mCamera!!.getParameters()
            //设置预览数据格式为nv21
            parameters.previewFormat = ImageFormat.NV21
            var isSupportSize = false
            val supportedPreviewSizes = parameters.supportedPreviewSizes
            for (supportedPreviewSize in supportedPreviewSizes) {
                if (supportedPreviewSize.width == width && supportedPreviewSize.height == height) {
                    isSupportSize = true
                    break
                }
            }
            if (!isSupportSize) {
                val size = supportedPreviewSizes[0]
                width = size.width
                height = size.height
            }
            //这是摄像头宽、高
            parameters.setPreviewSize(width, height)
            //            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            mCamera!!.setDisplayOrientation(90)
            // 设置摄像头 图像传感器的角度、方向
            mCamera!!.setParameters(parameters)
            buffer = ByteArray(width * height * 3 / 2)
            i420 = ByteArray(width * height * 3 / 2)
            //数据缓存区
            mCamera!!.addCallbackBuffer(buffer)
            mCamera!!.setPreviewCallbackWithBuffer(this)
            //设置预览画面
            mCamera!!.setPreviewTexture(surfaceTexture)
            mCamera!!.startPreview()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun setPreviewCallback(previewCallback: PreviewCallback?) {
        mPreviewCallback = previewCallback
    }

    override fun onPreviewFrame(data: ByteArray, camera: Camera) {
        // data数据依然是倒的
        if (null != mPreviewCallback) {
            nv21ToI420(data)
            mPreviewCallback!!.onPreviewFrame(i420, camera)
        }
        camera.addCallbackBuffer(buffer)
    }

    lateinit var i420: ByteArray
    private fun nv21ToI420(data: ByteArray) {
        //y数据
        System.arraycopy(data, 0, i420, 0, width * height)
        var index = width * height
        run {
            var i = width * height
            while (i < data.size) {
                i420[index++] = data[i + 1]
                i += 2
            }
        }
        var i = width * height
        while (i < data.size) {
            i420[index++] = data[i]
            i += 2
        }
    }

    companion object {
        private const val TAG = "CameraHelper"
    }

    init {
        cameraId = Camera.CameraInfo.CAMERA_FACING_BACK
    }
}
