package com.datavite.eat.data.local.datasource.ai

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import com.datavite.eat.domain.model.DomainFaceRecognition

interface FaceRecognitionDataSource {
    fun processFaceRecognitions(bitmap: Bitmap, rotation:Int)
    fun FaceRecognitions(): LiveData<List<DomainFaceRecognition>>
}