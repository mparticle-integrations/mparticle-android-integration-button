package com.example.buttonkit

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import com.mparticle.MParticle
import com.mparticle.kits.ButtonKit
import kotlinx.android.synthetic.main.activity_main.bt_attribution_refresh
import kotlinx.android.synthetic.main.activity_main.tv_attribution_token

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = MParticle.getInstance().getKitInstance(
                MParticle.ServiceProviders.BUTTON) as ButtonKit?

        bt_attribution_refresh.setOnClickListener {
            val token = button?.attributionToken

            if (!TextUtils.isEmpty(token)) {
                tv_attribution_token.text = token
            } else {
                tv_attribution_token.text = getString(R.string.label_default_token)
            }
        }

    }
}
