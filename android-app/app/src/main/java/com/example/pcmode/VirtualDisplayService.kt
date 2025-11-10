package com.example.pcmode

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat

/**
 * 虚拟显示服务
 * 负责创建和管理虚拟显示器
 */
class VirtualDisplayService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    
    companion object {
        private const val TAG = "VirtualDisplayService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "virtual_display_channel"
        
        // 虚拟显示器配置
        private const val VIRTUAL_DISPLAY_NAME = "PC_Mode_Display"
        private const val VIRTUAL_DISPLAY_WIDTH = 1920  // 1080p 宽度
        private const val VIRTUAL_DISPLAY_HEIGHT = 1080 // 1080p 高度
        private const val VIRTUAL_DISPLAY_DPI = 160     // 标准 DPI
        
        var isRunning = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        
        // 创建通知渠道(Android 8.0+)
        createNotificationChannel()
        
        // 启动前台服务
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        // 获取 MediaProjection 参数
        val resultCode = intent?.getIntExtra("resultCode", -1) ?: -1
        val data = intent?.getParcelableExtra<Intent>("data")
        
        if (resultCode != -1 && data != null) {
            startMediaProjection(resultCode, data)
        } else {
            Log.e(TAG, "Invalid intent data")
            stopSelf()
        }
        
        isRunning = true
        return START_NOT_STICKY
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
            .setContentTitle("PC 模式已激活")
            .setContentText("虚拟显示器正在运行")
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
            
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    super.onStop()
                    Log.d(TAG, "MediaProjection stopped")
                    cleanupResources()
                    stopSelf()
                }
            }, null)
            
            createVirtualDisplay()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MediaProjection", e)
            stopSelf()
        }
    }

    /**
     * 创建虚拟显示器
     */
    private fun createVirtualDisplay() {
        // 获取屏幕参数
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        
        // 使用配置的虚拟显示器参数
        val width = VIRTUAL_DISPLAY_WIDTH
        val height = VIRTUAL_DISPLAY_HEIGHT
        val dpi = VIRTUAL_DISPLAY_DPI
        
        Log.d(TAG, "Creating virtual display: ${width}x${height}@${dpi}dpi")
        
        // 创建 ImageReader 作为 Surface
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        
        // 虚拟显示器标志
        val flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION or
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR
        
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
            } else {
                Log.e(TAG, "Failed to create virtual display")
                stopSelf()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating virtual display", e)
            stopSelf()
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
    private fun cleanupResources() {
        virtualDisplay?.release()
        virtualDisplay = null
        
        imageReader?.close()
        imageReader = null
        
        mediaProjection?.stop()
        mediaProjection = null
        
        isRunning = false
        
        Log.d(TAG, "Resources cleaned up")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        cleanupResources()
    }
}
