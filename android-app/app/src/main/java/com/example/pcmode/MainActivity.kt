package com.example.pcmode

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * 主界面
 * 负责请求权限和启动虚拟显示服务
 */
class MainActivity : AppCompatActivity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var statusText: TextView
    private lateinit var activateButton: Button
    private lateinit var deactivateButton: Button

    companion object {
        private const val REQUEST_MEDIA_PROJECTION = 1001
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化 MediaProjectionManager
        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        // 初始化 UI
        statusText = findViewById(R.id.statusText)
        activateButton = findViewById(R.id.activateButton)
        deactivateButton = findViewById(R.id.deactivateButton)

        // 设置按钮点击事件
        activateButton.setOnClickListener {
            requestMediaProjection()
        }

        deactivateButton.setOnClickListener {
            stopVirtualDisplay()
        }

        updateUI()
    }

    /**
     * 请求 MediaProjection 权限
     */
    private fun requestMediaProjection() {
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(captureIntent, REQUEST_MEDIA_PROJECTION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                // 用户授予权限,启动虚拟显示服务
                startVirtualDisplayService(resultCode, data)
            } else {
                // 用户拒绝权限
                Toast.makeText(this, "需要屏幕录制权限才能激活 PC 模式", Toast.LENGTH_LONG).show()
                updateUI()
            }
        }
    }

    /**
     * 启动虚拟显示服务
     */
    private fun startVirtualDisplayService(resultCode: Int, data: Intent) {
        val serviceIntent = Intent(this, VirtualDisplayService::class.java).apply {
            putExtra("resultCode", resultCode)
            putExtra("data", data)
        }
        
        startForegroundService(serviceIntent)
        
        statusText.text = "状态: PC 模式已激活"
        updateUI()
        
        Toast.makeText(this, "虚拟显示器已创建,尝试激活 PC 模式", Toast.LENGTH_SHORT).show()
    }

    /**
     * 停止虚拟显示服务
     */
    private fun stopVirtualDisplay() {
        val serviceIntent = Intent(this, VirtualDisplayService::class.java)
        stopService(serviceIntent)
        
        statusText.text = "状态: 未激活"
        updateUI()
        
        Toast.makeText(this, "已停止虚拟显示器", Toast.LENGTH_SHORT).show()
    }

    /**
     * 更新 UI 状态
     */
    private fun updateUI() {
        val isServiceRunning = VirtualDisplayService.isRunning
        activateButton.isEnabled = !isServiceRunning
        deactivateButton.isEnabled = isServiceRunning
        
        if (!isServiceRunning) {
            statusText.text = "状态: 未激活"
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }
}
