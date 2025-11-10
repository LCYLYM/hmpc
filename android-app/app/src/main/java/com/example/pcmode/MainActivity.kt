package com.example.pcmode

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial

private const val RECEIVER_STATE_ACTION = VirtualDisplayService.ACTION_STATE_CHANGED
private const val RECEIVER_PREVIEW_ACTION = VirtualDisplayService.ACTION_PREVIEW_FRAME
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
    private lateinit var previewSwitch: SwitchMaterial
    private lateinit var previewContainer: LinearLayout
    private lateinit var previewImage: ImageView
    private lateinit var previewPlaceholder: TextView
    private lateinit var profileSpinner: Spinner
    private lateinit var profileAdapter: ArrayAdapter<String>
    private var isReceiverRegistered = false
    private var isPreviewReceiverRegistered = false
    private var previewEnabled = false
    private var currentProfileIndex = 0
    private val displayProfiles = DisplayProfile.defaultProfiles()

    private val serviceStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != RECEIVER_STATE_ACTION) return

            val isRunning = VirtualDisplayService.isRunning
            val statusMessage = intent.getStringExtra(VirtualDisplayService.EXTRA_STATUS_MESSAGE)
            val errorMessage = intent.getStringExtra(VirtualDisplayService.EXTRA_ERROR_MESSAGE)

            statusText.text = statusMessage ?: if (isRunning) {
                getString(R.string.status_active)
            } else {
                getString(R.string.status_inactive)
            }

            activateButton.isEnabled = !isRunning
            deactivateButton.isEnabled = isRunning

            previewSwitch.isEnabled = isRunning
            if (!isRunning) {
                previewSwitch.isChecked = false
                previewEnabled = false
                togglePreviewContainer(false)
            }

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

    private val previewFrameReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != RECEIVER_PREVIEW_ACTION) return

            val frameData = intent.getByteArrayExtra(VirtualDisplayService.EXTRA_PREVIEW_FRAME)
            if (frameData == null || frameData.isEmpty()) {
                previewImage.setImageDrawable(null)
                previewPlaceholder.visibility = View.VISIBLE
                return
            }

            val bitmap = android.graphics.BitmapFactory.decodeByteArray(frameData, 0, frameData.size)
            if (bitmap != null) {
                previewPlaceholder.visibility = View.GONE
                previewImage.setImageBitmap(bitmap)
            } else {
                previewImage.setImageDrawable(null)
                previewPlaceholder.visibility = View.VISIBLE
            }
        }
    }

    companion object {
        private const val REQUEST_MEDIA_PROJECTION = 1001
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
        previewSwitch = findViewById(R.id.previewSwitch)
        previewContainer = findViewById(R.id.previewContainer)
        previewImage = findViewById(R.id.previewImage)
        previewPlaceholder = findViewById(R.id.previewPlaceholder)
        profileSpinner = findViewById(R.id.profileSpinner)

        initProfileSpinner()

        // 设置按钮点击事件
        activateButton.setOnClickListener {
            requestMediaProjection()
        }

        deactivateButton.setOnClickListener {
            stopVirtualDisplay()
        }

        previewSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!VirtualDisplayService.isRunning) {
                previewSwitch.isChecked = false
                Toast.makeText(this, getString(R.string.toast_virtual_display_not_running), Toast.LENGTH_SHORT).show()
                return@setOnCheckedChangeListener
            }

            previewEnabled = isChecked
            togglePreviewContainer(isChecked)
            sendPreviewToggle(isChecked)
            Toast.makeText(
                this,
                if (isChecked) getString(R.string.toast_preview_enabled) else getString(R.string.toast_preview_disabled),
                Toast.LENGTH_SHORT
            ).show()
        }

        updateUI()
    }

    override fun onStart() {
        super.onStart()
        registerServiceStateReceiver()
        registerPreviewReceiver()
    }

    override fun onStop() {
        super.onStop()
        unregisterPreviewReceiver()
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
            action = VirtualDisplayService.ACTION_START
            putExtra(VirtualDisplayService.EXTRA_RESULT_CODE, resultCode)
            putExtra(VirtualDisplayService.EXTRA_RESULT_DATA, data)
            putExtra(VirtualDisplayService.EXTRA_PROFILE_ID, displayProfiles[currentProfileIndex].id)
            putExtra(VirtualDisplayService.EXTRA_PROFILE_WIDTH, displayProfiles[currentProfileIndex].width)
            putExtra(VirtualDisplayService.EXTRA_PROFILE_HEIGHT, displayProfiles[currentProfileIndex].height)
            putExtra(VirtualDisplayService.EXTRA_PROFILE_DPI, displayProfiles[currentProfileIndex].dpi)
            putExtra(VirtualDisplayService.EXTRA_PROFILE_FLAGS, displayProfiles[currentProfileIndex].flags)
        }
        
        ContextCompat.startForegroundService(this, serviceIntent)

        statusText.text = getString(R.string.status_pending)
        activateButton.isEnabled = false
        deactivateButton.isEnabled = false
        previewSwitch.isEnabled = false
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
        previewSwitch.isEnabled = isServiceRunning
        profileSpinner.isEnabled = !isServiceRunning
        statusText.text = if (isServiceRunning) {
            getString(R.string.status_active)
        } else {
            getString(R.string.status_inactive)
        }
        lastKnownServiceState = isServiceRunning
        if (!isServiceRunning) {
            previewSwitch.isChecked = false
            previewEnabled = false
            togglePreviewContainer(false)
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun registerServiceStateReceiver() {
        if (isReceiverRegistered) return
        val intentFilter = IntentFilter(RECEIVER_STATE_ACTION)
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

    private fun registerPreviewReceiver() {
        if (isPreviewReceiverRegistered) return
        val intentFilter = IntentFilter(RECEIVER_PREVIEW_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(previewFrameReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(previewFrameReceiver, intentFilter)
        }
        isPreviewReceiverRegistered = true
    }

    private fun unregisterPreviewReceiver() {
        if (!isPreviewReceiverRegistered) return
        unregisterReceiver(previewFrameReceiver)
        isPreviewReceiverRegistered = false
    }

    private fun sendPreviewToggle(enabled: Boolean) {
        val intent = Intent(this, VirtualDisplayService::class.java).apply {
            action = if (enabled) VirtualDisplayService.ACTION_ENABLE_PREVIEW else VirtualDisplayService.ACTION_DISABLE_PREVIEW
        }
        startService(intent)
    }

    private fun togglePreviewContainer(show: Boolean) {
        previewContainer.visibility = if (show) View.VISIBLE else View.GONE
        if (!show) {
            previewImage.setImageDrawable(null)
            previewPlaceholder.visibility = View.VISIBLE
        }
    }

    private fun initProfileSpinner() {
        val labels = displayProfiles.map { getString(it.labelRes) }
        profileAdapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            labels
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        profileSpinner.adapter = profileAdapter
        profileSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentProfileIndex = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // no-op
            }
        }
    }
}
