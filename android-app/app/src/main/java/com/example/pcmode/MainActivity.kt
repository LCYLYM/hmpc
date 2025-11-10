package com.example.pcmode

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Build
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

private const val RECEIVER_ACTION = VirtualDisplayService.ACTION_STATE_CHANGED

/**
 * 主界面
 * 负责请求权限和启动虚拟显示服务
 */
class MainActivity : AppCompatActivity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var statusText: TextView
    private lateinit var activateButton: Button
    private lateinit var deactivateButton: Button
    private var lastKnownServiceState = false
    private var isReceiverRegistered = false

    private val serviceStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != RECEIVER_ACTION) return

            val isRunning = intent.getBooleanExtra(VirtualDisplayService.EXTRA_IS_RUNNING, false)
            val statusMessage = intent.getStringExtra(VirtualDisplayService.EXTRA_STATUS_MESSAGE)
            val errorMessage = intent.getStringExtra(VirtualDisplayService.EXTRA_ERROR_MESSAGE)

            statusText.text = statusMessage ?: if (isRunning) {
                getString(R.string.status_active)
            } else {
                getString(R.string.status_inactive)
            }

            activateButton.isEnabled = !isRunning
            deactivateButton.isEnabled = isRunning

            when {
                errorMessage != null -> Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
                isRunning && !lastKnownServiceState -> Toast.makeText(
                    this@MainActivity,
                    getString(R.string.toast_virtual_display_started),
                    Toast.LENGTH_SHORT
                ).show()
                !isRunning && lastKnownServiceState -> Toast.makeText(
                    this@MainActivity,
                    getString(R.string.toast_virtual_display_stopped),
                    Toast.LENGTH_SHORT
                ).show()
            }

            lastKnownServiceState = isRunning
        }
    }

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

    override fun onStart() {
        super.onStart()
        registerServiceStateReceiver()
    }

    override fun onStop() {
        super.onStop()
        unregisterServiceStateReceiver()
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
                Toast.makeText(this, getString(R.string.toast_media_projection_required), Toast.LENGTH_LONG).show()
                updateUI()
            }
        }
    }

    /**
     * 启动虚拟显示服务
     */
    private fun startVirtualDisplayService(resultCode: Int, data: Intent) {
        val serviceIntent = Intent(this, VirtualDisplayService::class.java).apply {
            putExtra(VirtualDisplayService.EXTRA_RESULT_CODE, resultCode)
            putExtra(VirtualDisplayService.EXTRA_RESULT_DATA, data)
        }
        
        ContextCompat.startForegroundService(this, serviceIntent)

        statusText.text = getString(R.string.status_pending)
        activateButton.isEnabled = false
        deactivateButton.isEnabled = false
    }

    /**
     * 停止虚拟显示服务
     */
    private fun stopVirtualDisplay() {
        val serviceIntent = Intent(this, VirtualDisplayService::class.java)
        val wasRunning = VirtualDisplayService.isRunning
        stopService(serviceIntent)

        if (!wasRunning) {
            Toast.makeText(this, getString(R.string.toast_virtual_display_not_running), Toast.LENGTH_SHORT).show()
            updateUI()
        }
    }

    /**
     * 更新 UI 状态
     */
    private fun updateUI() {
        val isServiceRunning = VirtualDisplayService.isRunning
        activateButton.isEnabled = !isServiceRunning
        deactivateButton.isEnabled = isServiceRunning
        statusText.text = if (isServiceRunning) {
            getString(R.string.status_active)
        } else {
            getString(R.string.status_inactive)
        }
        lastKnownServiceState = isServiceRunning
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun registerServiceStateReceiver() {
        if (isReceiverRegistered) return
        val intentFilter = IntentFilter(RECEIVER_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(serviceStateReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(serviceStateReceiver, intentFilter)
        }
        isReceiverRegistered = true
    }

    private fun unregisterServiceStateReceiver() {
        if (!isReceiverRegistered) return
        unregisterReceiver(serviceStateReceiver)
        isReceiverRegistered = false
    }
}
