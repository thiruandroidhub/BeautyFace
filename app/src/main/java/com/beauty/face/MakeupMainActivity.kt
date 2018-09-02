package com.beauty.face

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.tzutalin.dlib.Constants
import com.tzutalin.dlib.FaceDet

class MakeupMainActivity : AppCompatActivity() {

    internal var mFaceDet: FaceDet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFaceDet = FaceDet(Constants.getFaceShapeModelPath())
    }
}