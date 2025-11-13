package com.datavite.eat.presentation.ai.model

import com.google.mlkit.vision.face.Face

data class FaceRecognitionSecureResult(
    val isSecure:Boolean= false,
    val isGPSMatch:Boolean= false,
    val isFaceMatchWithSelectedItem: Boolean = false,
    val faceRecognitionResult: FaceRecognitionResult,
    val face: Face
)
