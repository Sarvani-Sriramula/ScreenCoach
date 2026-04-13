package com.example.screencoach

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var statusText: TextView? = null
    private var impactText: TextView? = null
    
    private var prevImpactText: TextView? = null
    private var prevActionText: TextView? = null

    private var currentImpact: String? = null
    private var currentAction: String? = null

    private val uiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val newImpact = intent.getStringExtra("impact")
            val newAction = intent.getStringExtra("action")
            Log.d("ScreenCoachUI", "Broadcast received in Fragment: $newImpact")

            if (newImpact != null && newAction != null) {
                updateUI(newImpact, newAction)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        statusText = view.findViewById(R.id.tvStatus)
        impactText = view.findViewById(R.id.tvImpact)
        
        prevImpactText = view.findViewById(R.id.tvPrevImpact)
        prevActionText = view.findViewById(R.id.tvPrevAction)

        val buildInfoText = view.findViewById<TextView>(R.id.tvBuildInfo)
        
        val versionName = BuildConfig.VERSION_NAME
        val versionCode = BuildConfig.VERSION_CODE
        
        // Use the build time captured by Gradle during compilation
        val releasedDateTime = BuildConfig.BUILD_TIME

        buildInfoText?.text = "Build: $versionName ($versionCode) | Released: $releasedDateTime"
        statusText?.text = "Screen Coach Ready"

        loadSavedMessages()
    }

    private fun loadSavedMessages() {
        val prefs = requireContext().getSharedPreferences("ScreenCoachPrefs", Context.MODE_PRIVATE)
        val savedImpact = prefs.getString("current_impact", null)
        val savedAction = prefs.getString("current_action", null)
        
        if (savedImpact != null && savedAction != null) {
            impactText?.text = savedImpact
            currentImpact = savedImpact
            currentAction = savedAction
        }

        val savedPrevImpact = prefs.getString("prev_impact", null)
        val savedPrevAction = prefs.getString("prev_action", null)
        if (savedPrevImpact != null && savedPrevAction != null) {
            prevImpactText?.text = savedPrevImpact
            prevActionText?.text = savedPrevAction
            prevImpactText?.visibility = View.VISIBLE
            prevActionText?.visibility = View.VISIBLE
        }
    }

    private fun updateUI(newImpact: String, newAction: String) {
        // If current impact is already what's being shown, don't shift to previous yet
        // This prevents double-shifting if the broadcast and manual load overlap
        if (newImpact == currentImpact) return

        if (currentImpact != null) {
            prevImpactText?.text = currentImpact
            prevActionText?.text = currentAction
            prevImpactText?.visibility = View.VISIBLE
            prevActionText?.visibility = View.VISIBLE
        }

        impactText?.text = newImpact
        
        currentImpact = newImpact
        currentAction = newAction
    }

    override fun onResume() {
        super.onResume()
        Log.d("ScreenCoachUI", "HomeFragment resumed")

        val filter = IntentFilter("SCREEN_TIME_EVENT")

        try {
            requireContext().unregisterReceiver(uiReceiver)
        } catch (_: Exception) {}

        // Standard registration for internal broadcasts
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Use RECEIVER_EXPORTED to ensure internal service can reach it reliably
            requireContext().registerReceiver(uiReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            requireContext().registerReceiver(uiReceiver, filter)
        }
        
        loadSavedMessages()
    }

    override fun onPause() {
        super.onPause()
        try {
            requireContext().unregisterReceiver(uiReceiver)
        } catch (_: Exception) {}
    }
}
