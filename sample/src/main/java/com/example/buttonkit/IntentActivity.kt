package com.example.buttonkit

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.mparticle.MParticle
import com.mparticle.kits.ButtonKit
import kotlinx.android.synthetic.main.activity_intent.tv_attribution_token

class IntentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intent)

        actionBar?.setDisplayShowHomeEnabled(true)

        val button = MParticle.getInstance().getKitInstance(
                MParticle.ServiceProviders.BUTTON) as ButtonKit?

        tv_attribution_token.text = button?.attributionToken
    }
}
