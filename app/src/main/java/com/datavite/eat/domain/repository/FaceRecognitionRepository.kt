package com.datavite.eat.domain.repository

import FaceRecognitionAnalyzer
import androidx.lifecycle.LiveData
import com.datavite.eat.domain.model.DomainFaceRecognition

interface FaceRecognitionRepository {
    fun getFaceAnalyzer(): FaceRecognitionAnalyzer
    fun getFaceResults(): LiveData<List<DomainFaceRecognition>>
}