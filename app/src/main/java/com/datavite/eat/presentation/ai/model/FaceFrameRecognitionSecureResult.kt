package com.datavite.eat.presentation.ai.model

data class FaceFrameRecognitionSecureResult(
    val faceFrame: FaceFrame,
    val faceRecognitionSecureResults: List<FaceRecognitionSecureResult>
)
