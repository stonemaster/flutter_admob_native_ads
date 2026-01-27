package com.tqc.ads.flutter_admob_native_ads.banner

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerAdView
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView

/**
 * Platform view for displaying banner ads.
 *
 * This view creates its own AdView and loads the ad independently,
 * rather than relying on a shared loader. This ensures the ad is always
 * available when the platform view is created.
 */
class BannerAdPlatformView(
    private val context: Context,
    private val viewId: Int,
    private val creationParams: Map<String, Any?>,
    private val messenger: BinaryMessenger
) : PlatformView {

    companion object {
        private const val TAG = "BannerAdPlatformView"
        private const val BANNER_CHANNEL_NAME = "flutter_admob_banner_ads"
    }

    private val container: FrameLayout = FrameLayout(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
    }

    private var adView: AdManagerAdView? = null
    private val enableDebugLogs: Boolean
    private val controllerId: String?
    private val channel: MethodChannel

    init {
        enableDebugLogs = creationParams["enableDebugLogs"] as? Boolean ?: false
        controllerId = creationParams["controllerId"] as? String
        channel = MethodChannel(messenger, BANNER_CHANNEL_NAME)

        log("Initializing banner platform view for controller: $controllerId")

        createAndLoadAdView()
    }

    /**
     * Creates a new AdView and loads the ad.
     */
    private fun createAndLoadAdView() {
        val adUnitId = creationParams["adUnitId"] as? String
        val sizeIndex = creationParams["size"] as? Int ?: 5

        if (adUnitId.isNullOrEmpty()) {
            log("Invalid adUnitId")
            return
        }

        log("Creating AdView with unitId: $adUnitId, sizeIndex: $sizeIndex")

        // Create AdView
        adView = AdManagerAdView(context).apply {
            val adSize = BannerAdSizeExtensions.getAdSize(sizeIndex, context, null)
            setAdSize(adSize)
            this.adUnitId = adUnitId

            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    log("Ad loaded successfully")
                    sendEvent("onAdLoaded", mapOf("controllerId" to (controllerId ?: "")))
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    log("Ad failed to load: ${error.message} (code: ${error.code})")
                    sendEvent("onAdFailedToLoad", mapOf(
                        "controllerId" to (controllerId ?: ""),
                        "error" to (error.message ?: "Unknown error"),
                        "errorCode" to error.code
                    ))
                }

                override fun onAdClicked() {
                    log("Ad clicked")
                    sendEvent("onAdClicked", mapOf("controllerId" to (controllerId ?: "")))
                }

                override fun onAdImpression() {
                    log("Ad impression recorded")
                    sendEvent("onAdImpression", mapOf("controllerId" to (controllerId ?: "")))
                }

                override fun onAdOpened() {
                    log("Ad opened")
                    sendEvent("onAdOpened", mapOf("controllerId" to (controllerId ?: "")))
                }

                override fun onAdClosed() {
                    log("Ad closed")
                    sendEvent("onAdClosed", mapOf("controllerId" to (controllerId ?: "")))
                }
            }
        }

        // Add to container
        container.addView(adView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        log("AdView added to container, loading ad...")

        // Load ad
        adView?.loadAd(AdRequest.Builder().build())
    }

    /**
     * Sends an event to Flutter via method channel.
     */
    private fun sendEvent(method: String, arguments: Map<String, Any?>) {
        try {
            channel.invokeMethod(method, arguments)
        } catch (e: Exception) {
            log("Error sending event $method: ${e.message}")
        }
    }

    override fun getView(): View = container

    override fun dispose() {
        log("Disposing banner platform view")
        container.removeAllViews()
        adView?.destroy()
        adView = null
    }

    private fun log(message: String) {
        if (enableDebugLogs) {
            Log.d(TAG, "[$viewId] $message")
        }
    }
}
