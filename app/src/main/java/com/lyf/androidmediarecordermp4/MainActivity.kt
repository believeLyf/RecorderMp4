package com.lyf.androidmediarecordermp4

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.lyf.mediarecord.recorder.RecordMp4
import com.lyf.mediarecord.recorder.data.EncoderParams


class MainActivity : AppCompatActivity() {
    private lateinit var imageAnalyzer: ImageAnalysis
    private var isRecording: Boolean = false
    private lateinit var mp4: RecordMp4
    private lateinit var previewView:PreviewView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermission()
        previewView = findViewById<PreviewView>(R.id.previewView)
        // 配置相机用例
        val recordMp4 = RecordMp4.getInstance(this)
        recordMp4.startCamera(previewView, this)
        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        recordMp4.setImageAnalysis(imageAnalyzer, this)

        mp4 = RecordMp4.getInstance(this)
        mp4.setEncoderParams(EncoderParams())
        val button = findViewById<Button>(R.id.record)
        button.setOnClickListener {
            if (isRecording) {
                mp4.stopRecord()
                isRecording = false
                button.text="开始录制"
            } else {
               mp4.startRecord()
                isRecording=true
                button.text="停止录制"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mp4.stopCamera()
    }

    private fun checkPermission() {
        var targetSdkVersion = 0
        val PermissionString = arrayOf<String>(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        try {
            val info = this.packageManager.getPackageInfo(this.packageName, 0)
            targetSdkVersion = info.applicationInfo.targetSdkVersion //获取应用的Target版本
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            Log.i("err", "检查权限_err0")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Build.VERSION.SDK_INT是获取当前手机版本 Build.VERSION_CODES.M为6.0系统
            //如果系统>=6.0
            if (targetSdkVersion >= Build.VERSION_CODES.M) {
                //第 1 步: 检查是否有相应的权限
                val isAllGranted = checkPermissionAllGranted(PermissionString)
                if (isAllGranted) {
                    Log.i("success", "所有权限已经授权！")
                    return
                }
                // 一次请求多个权限, 如果其他有权限是已经授予的将会自动忽略掉
                ActivityCompat.requestPermissions(
                    this,
                    PermissionString, 1
                )
            }
        }
    }

    /**
     * 检查是否拥有指定的所有权限
     */
    private fun checkPermissionAllGranted(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // 只要有一个权限没有被授予, 则直接返回 false
                Log.e("err", "权限" + permission + "没有授权")
                return false
            }
        }
        return true
    }

    //申请权限结果返回处理
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            var isAllGranted = true
            // 判断是否所有的权限都已经授予了
            for (grant in grantResults) {
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    isAllGranted = false
                    break
                }
            }
            if (isAllGranted) {
                // 所有的权限都授予了
                Log.e("err", "权限都授权了")
            } else {
                // 弹出对话框告诉用户需要权限的原因, 并引导用户去应用权限管理中手动打开权限按钮
                //容易判断错
                //MyDialog("提示", "某些权限未开启,请手动开启", 1) ;
            }
        }
    }
}