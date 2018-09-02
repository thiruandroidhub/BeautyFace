package com.beauty.face

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.tzutalin.dlib.Constants
import com.tzutalin.dlib.FaceDet

class MakeupActivity : AppCompatActivity() {

    private lateinit var mFaceDet: FaceDet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFaceDet = FaceDet(Constants.getFaceShapeModelPath())
    }
}