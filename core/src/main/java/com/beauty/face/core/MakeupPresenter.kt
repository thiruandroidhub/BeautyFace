package com.beauty.face.core

import io.reactivex.Single

class MakeupPresenter {

    fun attach() {}

    fun getFaceLandmarkForiLps() : Single<String> = Single.just("")

    fun detach() {}
}