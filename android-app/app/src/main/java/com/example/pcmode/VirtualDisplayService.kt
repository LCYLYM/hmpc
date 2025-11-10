package com.example.pcmode

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

/**
 * 虚拟显示服务
 * 负责创建和管理虚拟显示器
 */
class VirtualDisplayService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var mediaProjectionCallback: MediaProjection.Callback? = null
    private var imageThread: HandlerThread? = null
    private var imageHandler: Handler? = null

    @Volatile private var previewEnabled: Boolean = false
    private var lastPreviewTimestamp = 0L
    private var notificationCreated = false

    private var displayWidth = DEFAULT_WIDTH
    private var displayHeight = DEFAULT_HEIGHT
    private var displayDpi = DEFAULT_DPI
    private var displayFlags = DEFAULT_FLAGS
    private var currentProfileId: String = ""

    companion object {
        private const val TAG = "VirtualDisplayService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "virtual_display_channel"

        private const val VIRTUAL_DISPLAY_NAME = "PC_Mode_Display"
        private const val DEFAULT_WIDTH = 1920
        private const val DEFAULT_HEIGHT = 1080
        private const val DEFAULT_DPI = 160
        private const val DEFAULT_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION or
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR

        private const val PREVIEW_MIN_INTERVAL_MS = 120L
        private const val PREVIEW_TARGET_WIDTH = 960
        private const val PREVIEW_TARGET_HEIGHT = 540

        const val ACTION_STATE_CHANGED = "com.example.pcmode.ACTION_VIRTUAL_DISPLAY_STATE"
        const val ACTION_PREVIEW_FRAME = "com.example.pcmode.ACTION_VIRTUAL_DISPLAY_PREVIEW"
        const val ACTION_START = "com.example.pcmode.ACTION_START_VIRTUAL_DISPLAY"
        const val ACTION_ENABLE_PREVIEW = "com.example.pcmode.ACTION_ENABLE_PREVIEW"
        const val ACTION_DISABLE_PREVIEW = "com.example.pcmode.ACTION_DISABLE_PREVIEW"

        const val EXTRA_IS_RUNNING = "extra_is_running"
        const val EXTRA_STATUS_MESSAGE = "extra_status_message"
        const val EXTRA_ERROR_MESSAGE = "extra_error_message"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_PROFILE_ID = "extra_profile_id"
        const val EXTRA_PROFILE_WIDTH = "extra_profile_width"
        const val EXTRA_PROFILE_HEIGHT = "extra_profile_height"
        const val EXTRA_PROFILE_DPI = "extra_profile_dpi"
        const val EXTRA_PROFILE_FLAGS = "extra_profile_flags"
        const val EXTRA_PREVIEW_FRAME = "extra_preview_frame"

        var isRunning = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        when (action) {
            ACTION_START -> handleStartIntent(intent)
            ACTION_ENABLE_PREVIEW -> updatePreviewState(true)
            ACTION_DISABLE_PREVIEW -> updatePreviewState(false)
            else -> Log.w(TAG, "Unknown action: $action")
        }
        return START_NOT_STICKY
    }

    private fun handleStartIntent(intent: Intent?) {
        Log.d(TAG, "handleStartIntent")

        if (isRunning) {
            Log.i(TAG, "Existing session detected, restarting")
            cleanupResources(resetPreview = false)
        }

        if (!notificationCreated) {
            createNotificationChannel()
            val notification = createNotification()
            startForeground(NOTIFICATION_ID, notification)
            notificationCreated = true
        }

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED
        val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
        }

        displayWidth = intent?.getIntExtra(EXTRA_PROFILE_WIDTH, DEFAULT_WIDTH) ?: DEFAULT_WIDTH
        displayHeight = intent?.getIntExtra(EXTRA_PROFILE_HEIGHT, DEFAULT_HEIGHT) ?: DEFAULT_HEIGHT
        displayDpi = intent?.getIntExtra(EXTRA_PROFILE_DPI, DEFAULT_DPI) ?: DEFAULT_DPI
        displayFlags = intent?.getIntExtra(EXTRA_PROFILE_FLAGS, DEFAULT_FLAGS) ?: DEFAULT_FLAGS
        currentProfileId = intent?.getStringExtra(EXTRA_PROFILE_ID) ?: ""

        if (displayWidth <= 0 || displayHeight <= 0) {
            displayWidth = DEFAULT_WIDTH
            displayHeight = DEFAULT_HEIGHT
        }

        if (resultCode == Activity.RESULT_OK && data != null) {
            startMediaProjection(resultCode, data)
        } else {
            Log.e(TAG, "Invalid MediaProjection parameters")
            sendStateBroadcast(
                isRunning = false,
                statusMessage = getString(R.string.status_inactive),
                errorMessage = getString(R.string.toast_media_projection_required)
            )
            stopSelf()
        }
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "虚拟显示服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于激活平板 PC 模式"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 创建通知
     */
    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_content))
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    /**
     * 启动 MediaProjection
     */
    private fun startMediaProjection(resultCode: Int, data: Intent) {
        val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        
        try {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            
            if (mediaProjection == null) {
                Log.e(TAG, "MediaProjection is null after authorization")
                sendStateBroadcast(
                    isRunning = false,
                    statusMessage = getString(R.string.status_inactive),
                    errorMessage = getString(R.string.toast_media_projection_required)
                )
                stopSelf()
                return
            }

            mediaProjectionCallback = object : MediaProjection.Callback() {
                override fun onStop() {
                    super.onStop()
                    Log.d(TAG, "MediaProjection stopped")
                    cleanupResources(getString(R.string.toast_media_projection_lost))
                    stopSelf()
                }
            }

            mediaProjectionCallback?.let { callback ->
                mediaProjection?.registerCallback(callback, null)
            }
            
            createVirtualDisplay()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MediaProjection", e)
            cleanupResources(e.localizedMessage?.let {
                getString(R.string.toast_virtual_display_failed, it)
            })
            stopSelf()
        }
    }

    /**
     * 创建虚拟显示器
     */
    private fun createVirtualDisplay() {
        // 使用配置的虚拟显示器参数
        val width = displayWidth
        val height = displayHeight
        val dpi = displayDpi
        
        Log.d(TAG, "Creating virtual display: ${width}x${height}@${dpi}dpi")

        // 创建 ImageReader 作为 Surface
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        configureImageReaderListener()

        // 虚拟显示器标志
        val flags = displayFlags
        
        try {
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                width,
                height,
                dpi,
                flags,
                imageReader?.surface,
                object : VirtualDisplay.Callback() {
                    override fun onPaused() {
                        super.onPaused()
                        Log.d(TAG, "VirtualDisplay paused")
                    }

                    override fun onResumed() {
                        super.onResumed()
                        Log.d(TAG, "VirtualDisplay resumed")
                    }

                    override fun onStopped() {
                        super.onStopped()
                        Log.d(TAG, "VirtualDisplay stopped")
                    }
                },
                null
            )
            
            if (virtualDisplay != null) {
                Log.i(TAG, "Virtual display created successfully")
                logDisplayInfo()
                isRunning = true
                sendStateBroadcast(true, getString(R.string.status_active))
            } else {
                Log.e(TAG, "Failed to create virtual display")
                cleanupResources(getString(R.string.toast_virtual_display_failed, "unknown"))
                stopSelf()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating virtual display", e)
            cleanupResources(e.localizedMessage?.let {
                getString(R.string.toast_virtual_display_failed, it)
            })
            stopSelf()
        }
    }

    private fun updatePreviewState(enable: Boolean) {
        if (previewEnabled == enable) {
            if (!enable) {
                sendPreviewBroadcast(null)
            }
            return
        }

        previewEnabled = enable

        if (!enable) {
            lastPreviewTimestamp = 0L
            imageReader?.setOnImageAvailableListener(null, null)
            sendPreviewBroadcast(null)
            stopImageThread()
            return
        }

        if (!ensureImageThread()) {
            previewEnabled = false
            Log.w(TAG, "Unable to start preview thread")
            return
        }

        configureImageReaderListener()
    }

    private fun ensureImageThread(): Boolean {
        if (imageThread != null && imageHandler != null) {
            return true
        }

        return try {
            val thread = HandlerThread("VirtualDisplayPreview").apply { start() }
            imageThread = thread
            imageHandler = Handler(thread.looper)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start preview thread", e)
            stopImageThread()
            false
        }
    }

    private fun stopImageThread() {
        val thread = imageThread
        if (thread != null) {
            thread.quitSafely()
            try {
                thread.join(500)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        imageThread = null
        imageHandler = null
    }

    private fun configureImageReaderListener() {
        val reader = imageReader ?: return

        if (!previewEnabled) {
            reader.setOnImageAvailableListener(null, null)
            return
        }

        if (!ensureImageThread()) {
            previewEnabled = false
            reader.setOnImageAvailableListener(null, null)
            return
        }

        val handler = imageHandler
        if (handler == null) {
            Log.w(TAG, "Preview handler unavailable")
            previewEnabled = false
            reader.setOnImageAvailableListener(null, null)
            return
        }

        reader.setOnImageAvailableListener({ availableReader ->
            handleImageAvailable(availableReader)
        }, handler)
    }

    private fun handleImageAvailable(reader: ImageReader) {
        val image = try {
            reader.acquireLatestImage()
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Failed to acquire preview image", e)
            null
        } ?: return

        if (!previewEnabled) {
            image.close()
            return
        }

        val now = SystemClock.elapsedRealtime()
        if (now - lastPreviewTimestamp < PREVIEW_MIN_INTERVAL_MS) {
            image.close()
            return
        }

        lastPreviewTimestamp = now

        var rawBitmap: Bitmap? = null
        var croppedBitmap: Bitmap? = null
        var scaledBitmap: Bitmap? = null

        try {
            val planes = image.planes
            if (planes.isEmpty()) {
                return
            }

            val plane = planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * image.width
            val bitmapWidth = image.width + rowPadding / pixelStride

            rawBitmap = Bitmap.createBitmap(bitmapWidth, image.height, Bitmap.Config.ARGB_8888)
            buffer.rewind()
            rawBitmap!!.copyPixelsFromBuffer(buffer)

            croppedBitmap = if (rowPadding != 0) {
                Bitmap.createBitmap(rawBitmap!!, 0, 0, image.width, image.height)
            } else {
                rawBitmap
            }

            scaledBitmap = resizeBitmap(croppedBitmap!!, PREVIEW_TARGET_WIDTH, PREVIEW_TARGET_HEIGHT)

            val previewBitmap = scaledBitmap ?: croppedBitmap
            val bytes = compressBitmapToJpeg(previewBitmap!!)
            if (bytes != null) {
                sendPreviewBroadcast(bytes)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error processing preview frame", e)
        } finally {
            scaledBitmap?.let { bitmap ->
                if (bitmap !== croppedBitmap && !bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }

            croppedBitmap?.let { bitmap ->
                if (bitmap !== rawBitmap && !bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }

            rawBitmap?.let { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }

            image.close()
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        if (bitmap.width <= targetWidth && bitmap.height <= targetHeight) {
            return bitmap
        }

        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        var desiredWidth = targetWidth
        var desiredHeight = (desiredWidth / aspectRatio).roundToInt().coerceAtLeast(1)

        if (desiredHeight > targetHeight) {
            desiredHeight = targetHeight
            desiredWidth = (desiredHeight * aspectRatio).roundToInt().coerceAtLeast(1)
        }

        if (desiredWidth == bitmap.width && desiredHeight == bitmap.height) {
            return bitmap
        }

        return Bitmap.createScaledBitmap(bitmap, desiredWidth, desiredHeight, true)
    }

    private fun compressBitmapToJpeg(bitmap: Bitmap): ByteArray? {
        return try {
            ByteArrayOutputStream().use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)) {
                    Log.w(TAG, "Bitmap compression returned false")
                    return null
                }
                stream.toByteArray()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to compress preview bitmap", e)
            null
        }
    }

    /**
     * 记录显示器信息
     */
    private fun logDisplayInfo() {
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val displays = displayManager.displays
        
        Log.d(TAG, "Current displays count: ${displays.size}")
        displays.forEachIndexed { index, display ->
            Log.d(TAG, "Display $index: ${display.name}, ${display.width}x${display.height}")
            if (display.flags and android.view.Display.FLAG_PRESENTATION != 0) {
                Log.d(TAG, "  -> This is a PRESENTATION display")
            }
        }
    }

    /**
     * 清理资源
     */
    private fun cleanupResources(message: String? = null, resetPreview: Boolean = true) {
        val wasRunning = isRunning
        virtualDisplay?.release()
        virtualDisplay = null
        
        imageReader?.setOnImageAvailableListener(null, null)
        imageReader?.close()
        imageReader = null

        if (resetPreview) {
            previewEnabled = false
            stopImageThread()
        }

        lastPreviewTimestamp = 0L
        sendPreviewBroadcast(null)
        
        mediaProjectionCallback?.let { callback ->
            try {
                mediaProjection?.unregisterCallback(callback)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unregister MediaProjection callback", e)
            }
        }
        mediaProjectionCallback = null

        try {
            mediaProjection?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop MediaProjection", e)
        }
        mediaProjection = null
        
        isRunning = false
        
        Log.d(TAG, "Resources cleaned up")

        if (wasRunning || message != null) {
            sendStateBroadcast(
                isRunning = false,
                statusMessage = getString(R.string.status_inactive),
                errorMessage = message
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        cleanupResources()
    }

    private fun sendPreviewBroadcast(frame: ByteArray?) {
        val intent = Intent(ACTION_PREVIEW_FRAME).apply {
            setPackage(packageName)
            frame?.let { putExtra(EXTRA_PREVIEW_FRAME, it) }
        }
        sendBroadcast(intent)
    }

    private fun sendStateBroadcast(isRunning: Boolean, statusMessage: String, errorMessage: String? = null) {
        val intent = Intent(ACTION_STATE_CHANGED).apply {
            setPackage(packageName)
            putExtra(EXTRA_IS_RUNNING, isRunning)
            putExtra(EXTRA_STATUS_MESSAGE, statusMessage)
            errorMessage?.let { putExtra(EXTRA_ERROR_MESSAGE, it) }
        }
        sendBroadcast(intent)
    }
}
