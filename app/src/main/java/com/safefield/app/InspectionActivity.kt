package com.safefield.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class InspectionActivity : Activity() {
    private lateinit var inspectionModule: InspectionModule
    private var currentBack: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?): Unit {
        super.onCreate(savedInstanceState)
        inspectionModule = InspectionModule(this) { finish() }
        inspectionModule.show()
    }

    fun setBackHandler(handler: (() -> Unit)?): Unit {
        currentBack = handler
    }

    override fun onBackPressed(): Unit {
        val back = currentBack
        if (back != null) back() else finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Unit {
        if (::inspectionModule.isInitialized && inspectionModule.handleActivityResult(requestCode, resultCode, data)) return
        super.onActivityResult(requestCode, resultCode, data)
    }
}
