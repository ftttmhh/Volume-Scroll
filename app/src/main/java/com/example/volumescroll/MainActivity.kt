package com.example.volumescroll

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.media.MediaPlayer


class MainActivity : AppCompatActivity() {

    private lateinit var serviceToggle: Switch
    private lateinit var scrollSpeedSeekBar: SeekBar
    private lateinit var scrollSpeedLabel: TextView
    private lateinit var enableServiceButton: Button
    private lateinit var statusText: TextView
    private var mediaPlayer: MediaPlayer? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        mediaPlayer = MediaPlayer.create(this, R.raw.click_snap_sound)
        setupListeners()
        updateServiceStatus()
    }

    private fun initViews() {
        serviceToggle = findViewById(R.id.serviceToggle)
        scrollSpeedSeekBar = findViewById(R.id.scrollSpeedSeekBar)
        scrollSpeedLabel = findViewById(R.id.scrollSpeedLabel)
        enableServiceButton = findViewById(R.id.enableServiceButton)
        statusText = findViewById(R.id.statusText)

        // Set initial scroll speed (default 600px)
        scrollSpeedSeekBar.max = 1000
        scrollSpeedSeekBar.progress = 600
        updateScrollSpeedLabel(600)
    }


    private fun setupListeners() {
        serviceToggle.setOnCheckedChangeListener { _, isChecked ->
            mediaPlayer?.start() // ← this plays the sound

            if (isChecked) {
                if (!isAccessibilityServiceEnabled()) {
                    serviceToggle.isChecked = false
                    openAccessibilitySettings()
                } else {
                    MyAccessibilityService.setServiceEnabled(true)
                    updateServiceStatus()
                }
            } else {
                MyAccessibilityService.setServiceEnabled(false)
                updateServiceStatus()
            }
        }


        scrollSpeedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val speed = progress + 200 // Min 200px, Max 1200px
                updateScrollSpeedLabel(speed)
                MyAccessibilityService.setScrollDistance(speed)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        enableServiceButton.setOnClickListener {
            openAccessibilitySettings()
        }
    }

    private fun updateScrollSpeedLabel(speed: Int) {
        scrollSpeedLabel.text = "Scroll Distance: ${speed}px"
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityEnabled = Settings.Secure.getInt(
            contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        )

        if (accessibilityEnabled == 1) {
            val services = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return services?.contains("${packageName}/${MyAccessibilityService::class.java.name}") == true
        }
        return false
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(this, "Please enable Volume Scroll Service", Toast.LENGTH_LONG).show()
    }

    private fun updateServiceStatus() {
        val isEnabled = isAccessibilityServiceEnabled()
        val isActive = MyAccessibilityService.isServiceActive()

        when {
            !isEnabled -> {
                statusText.text = "Status: Service not enabled in Accessibility Settings"
                statusText.setTextColor(getColor(android.R.color.holo_red_dark))
                serviceToggle.isEnabled = false
            }
            !isActive -> {
                statusText.text = "Status: Service enabled but inactive"
                statusText.setTextColor(getColor(android.R.color.holo_orange_dark))
                serviceToggle.isEnabled = true
                serviceToggle.isChecked = false
            }
            else -> {
                statusText.text = "Status: Service active and running"
                statusText.setTextColor(getColor(android.R.color.holo_green_dark))
                serviceToggle.isEnabled = true
                serviceToggle.isChecked = true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

}