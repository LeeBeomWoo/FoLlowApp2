package com.example.followapp

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.Fragment
import cn.gavinliu.android.lib.scale.config.ScaleConfig

class MainActivity : AppCompatActivity() {

    var camera: Fragment? = null
    var s:String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ScaleConfig.create(this,
            1080, // Design Width
            1920, // Design Height
            (3).toFloat(),    // Design Density
            (3).toFloat(),    // Design FontScale
            ScaleConfig.DIMENS_UNIT_DP)
        setContentView(R.layout.activity_main)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.root_layout, Camera.newInstance(), "play")
            .commit()
    }
}
