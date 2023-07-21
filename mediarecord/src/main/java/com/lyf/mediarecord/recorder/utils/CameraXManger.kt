package com.lyf.mediarecord.recorder.utils

import android.content.Context
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.common.util.concurrent.ListenableFuture
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraXManger private constructor(context: Context) {
    //构造方法的内容
    companion object {
        private const val TAG = "CameraXManger"
        var PREVIEW_WIDTH = 640
        var PREVIEW_HEIGHT = 480
        private var mCameraInstance: CameraXManger? = null

        fun getInstance(context: Context): CameraXManger {
            if (mCameraInstance == null) {
                mCameraInstance = CameraXManger(context)
                // 从CameraX库中获取摄像头提供程序对象
            }
            return mCameraInstance!!
        }
    }

    private lateinit var cameraProvider: ProcessCameraProvider  //相机信息
    private var cameraExecutor: ExecutorService   //开启相机的线程
    private var previewView: PreviewView? = null    //预览视图
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>  //开启相机管理器
    private var mPreview: Preview? = null
    private lateinit var imageAnalyzer: ImageAnalysis
    private lateinit var handler: Executor
    private var context: FragmentActivity? = null

    //初始化
    init {
        //初始化线程
        cameraExecutor = Executors.newSingleThreadExecutor()
        // 从CameraX库中获取摄像头提供程序对象
        cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        handler = ContextCompat.getMainExecutor(context)

    }

    fun setPreviewView(previewView: PreviewView, context: FragmentActivity) {
        this.previewView = previewView
        this.context = context
    }

    fun startPreview() {
        Log.d(TAG, "startPreview...")
        // 为相机预览创建一个Preview对象
        try {
            mPreview = Preview.Builder()
                .setTargetResolution(Size(PREVIEW_WIDTH, PREVIEW_HEIGHT))
                .build()
                .also {
                    it.setSurfaceProvider(previewView?.surfaceProvider)
                }
        } catch (e: IOException) {
            Log.d(TAG, "fail is ---->$e")
        }
    }

    fun openCamera() {
        Log.d(TAG, "openCamera...")
        // 创建相机选择器，选择使用后置摄像头
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()


        // 在后台线程中启动摄像头
        cameraProviderFuture.addListener(
            {
                // 获取摄像头提供程序对象
                cameraProvider = cameraProviderFuture.get()
                // 绑定相机生命周期和预览
                cameraProvider.bindToLifecycle(context!!, cameraSelector, mPreview, imageAnalyzer)
            }, handler
        )
    }

    fun stopCamera() {
        // 停止自定义生命周期所有者
        cameraProvider.unbindAll()
        cameraExecutor.shutdown()
    }

    fun stopPreview() {
        mPreview?.setSurfaceProvider(null) // 与PreviewView解绑
        mPreview = null // 将Preview对象设置为null
    }

    fun setImageAnalysis(imageAnalyzer: ImageAnalysis) {
        this.imageAnalyzer = imageAnalyzer
    }
}