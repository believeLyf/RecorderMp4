package com.lyf.androidmediarecordermp4

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.lyf.mediarecord.recorder.Encoder
import com.lyf.mediarecord.recorder.data.EncoderParams
import com.lyf.mediarecord.recorder.utils.CameraHelper


class TestActivity : AppCompatActivity(), View.OnClickListener,
    Camera.PreviewCallback {
    private var mCameraHelper: CameraHelper? = null
    private var videoCodec: Encoder? = null
    private var isRecording:Boolean=false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        checkPermission()
        mCameraHelper = CameraHelper(640, 480)
        mCameraHelper!!.setPreviewCallback(this)
        videoCodec = Encoder()
        videoCodec!!.setEncoderParam(EncoderParams("/storage/emulated/0/DCIM/Camera/aaa.mp4"))
        val textureView = findViewById<TextureView>(R.id.textureView)
        textureView.surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                mCameraHelper!!.startPreview(surface)
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                mCameraHelper!!.stopPreview()
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
        findViewById<View>(R.id.btn_record).setOnClickListener(this)
        findViewById<View>(R.id.button).setOnClickListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        videoCodec!!.queueEncode(data!!)
    }

    @SuppressLint("SdCardPath")
    override fun onClick(v: View) {
        val button: Button = v as Button
        if (v.getId() === R.id.btn_record) {
            if(!isRecording){
                videoCodec?.start()
                isRecording=true
            }else{
                isRecording=false
                videoCodec?.stopMuxer()
                videoCodec?.exit()
                val t1=videoCodec
                t1?.interrupt()
                t1?.join()
            }
        } else {
            checkPermission()
        }
    }

    fun checkPermission() {
        var targetSdkVersion = 0
        val PermissionString = arrayOf<String>(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
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
        permissions: Array<String>,
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