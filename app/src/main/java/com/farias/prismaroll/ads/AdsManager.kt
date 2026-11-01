package com.farias.prismaroll.ads

import android.app.Activity
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.farias.prismaroll.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AdMob + UMP consent. Banner at the bottom, interstitial between games (capped),
 * rewarded video for the "continue" second chance.
 */
class AdsManager(private val activity: Activity) {

    var adsReady by mutableStateOf(false)
        private set
    var rewardedReady by mutableStateOf(false)
        private set
    var privacyOptionsRequired by mutableStateOf(false)
        private set

    private val consentInfo: ConsentInformation = UserMessagingPlatform.getConsentInformation(activity)
    private val initStarted = AtomicBoolean(false)
    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    private var gamesSinceInterstitial = 0
    private var lastInterstitialAt = SystemClock.elapsedRealtime()
    private var loadingInterstitial = false
    private var loadingRewarded = false

    val bannerId: String get() = BuildConfig.ADMOB_BANNER_ID

    fun start() {
        val params = ConsentRequestParameters.Builder().build()
        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                    updatePrivacyFlag()
                    if (consentInfo.canRequestAds()) initAds()
                }
            },
            { _ ->
                if (consentInfo.canRequestAds()) initAds()
            },
        )
        // Consent from a previous session lets us start right away.
        if (consentInfo.canRequestAds()) initAds()
    }

    private fun updatePrivacyFlag() {
        privacyOptionsRequired = consentInfo.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    fun showPrivacyOptions() {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ -> updatePrivacyFlag() }
    }

    private fun initAds() {
        if (!initStarted.compareAndSet(false, true)) return
        Thread {
            MobileAds.initialize(activity.applicationContext)
            activity.runOnUiThread {
                adsReady = true
                loadInterstitial()
                loadRewarded()
            }
        }.start()
    }

    private fun loadInterstitial() {
        if (!adsReady || interstitial != null || loadingInterstitial) return
        loadingInterstitial = true
        InterstitialAd.load(
            activity,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    loadingInterstitial = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                    loadingInterstitial = false
                }
            },
        )
    }

    private fun loadRewarded() {
        if (!adsReady || rewarded != null || loadingRewarded) return
        loadingRewarded = true
        RewardedAd.load(
            activity,
            BuildConfig.ADMOB_REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewarded = ad
                    rewardedReady = true
                    loadingRewarded = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewarded = null
                    rewardedReady = false
                    loadingRewarded = false
                }
            },
        )
    }

    /**
     * Called when the player starts a new game. Shows an interstitial at most every
     * 2 games and 90 seconds, so ads don't drive players away. [onDone] always runs.
     */
    fun onNewGame(onDone: () -> Unit) {
        gamesSinceInterstitial++
        val ad = interstitial
        val enoughTime = SystemClock.elapsedRealtime() - lastInterstitialAt > 90_000L
        // Never in the first levels: new players must fall in love with the game first.
        val veteran = com.farias.prismaroll.data.Progress.levelsWon >= 4
        if (ad == null || gamesSinceInterstitial < 2 || !enoughTime || !veteran) {
            loadInterstitial()
            onDone()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                loadInterstitial()
                onDone()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitial = null
                loadInterstitial()
                onDone()
            }
        }
        gamesSinceInterstitial = 0
        lastInterstitialAt = SystemClock.elapsedRealtime()
        ad.show(activity)
    }

    /** Shows the rewarded video. [onReward] runs after the ad closes, only if the reward was earned. */
    fun showRewarded(onReward: () -> Unit) {
        val ad = rewarded ?: run {
            loadRewarded()
            return
        }
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewarded = null
                rewardedReady = false
                loadRewarded()
                if (earned) onReward()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewarded = null
                rewardedReady = false
                loadRewarded()
            }
        }
        ad.show(activity) { earned = true }
        // An interstitial right after a rewarded video feels bad; push the next one back.
        lastInterstitialAt = SystemClock.elapsedRealtime()
    }
}
