package com.example.buttonkit

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import com.mparticle.MParticle
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.kits.ButtonKit
import kotlinx.android.synthetic.main.activity_main.bt_activity_add_to_cart
import kotlinx.android.synthetic.main.activity_main.bt_activity_cart_view
import kotlinx.android.synthetic.main.activity_main.bt_activity_product_view
import kotlinx.android.synthetic.main.activity_main.bt_attribution_refresh
import kotlinx.android.synthetic.main.activity_main.tv_attribution_token

class MainActivity : AppCompatActivity() {

    private val product: Product
        get() {
            val sku = (100000000000..999999999999).random().toString()
            return Product.Builder("Product ${(0..10).random()}", sku, (10..99).random().toDouble())
                    .category("samples")
                    .quantity((1..10).random().toDouble())
                    .customAttributes(mapOf(Pair("test-attr", "yes")))
                    .build()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = MParticle.getInstance()
                ?.getKitInstance(MParticle.ServiceProviders.BUTTON) as ButtonKit?

        bt_attribution_refresh.setOnClickListener {
            val token = button?.attributionToken

            if (!TextUtils.isEmpty(token)) {
                tv_attribution_token.text = token
            } else {
                tv_attribution_token.text = getString(R.string.label_default_token)
            }
        }

        bt_activity_product_view.setOnClickListener {
            val event = CommerceEvent.Builder(Product.DETAIL, product).build()
            MParticle.getInstance()?.logEvent(event) ?: logKitUnavailable()
        }

        bt_activity_add_to_cart.setOnClickListener {
            val event = CommerceEvent.Builder(Product.ADD_TO_CART, product).build()
            MParticle.getInstance()?.logEvent(event) ?: logKitUnavailable()
        }

        bt_activity_cart_view.setOnClickListener {
            val event = CommerceEvent.Builder(Product.CHECKOUT, product).build()
            MParticle.getInstance()?.logEvent(event) ?: logKitUnavailable()
        }
    }

    private fun logKitUnavailable() {
        Log.e("ButtonKitSample", "mParticle / ButtonKit is unavailable!")
    }
}
