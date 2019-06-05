package com.example.buttonkit

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.mparticle.AttributionError
import com.mparticle.AttributionListener
import com.mparticle.AttributionResult
import com.mparticle.MParticle
import com.mparticle.MParticleOptions

class SampleApplication : Application() , AttributionListener{

    private val TAG = "ButtonKitSample"

    override fun onCreate() {
        super.onCreate()

        val options = MParticleOptions.builder(this)
                .credentials(BuildConfig.MP_KEY, BuildConfig.MP_SECRET)
                .environment(MParticle.Environment.AutoDetect)
                .attributionListener(this@SampleApplication)
                .logLevel(MParticle.LogLevel.VERBOSE)
                .build()
        MParticle.start(options)
    }

    override fun onResult(result: AttributionResult?) {
        if (result?.serviceProviderId == MParticle.ServiceProviders.BUTTON) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.link))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    override fun onError(error: AttributionError?) {
        if (error?.serviceProviderId == MParticle.ServiceProviders.BUTTON) {
            Log.e(TAG, "Attribution error: " + error.message)
        }
    }
}
