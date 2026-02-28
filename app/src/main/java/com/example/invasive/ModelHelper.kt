package com.example.invasive

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class ModelHelper(context: Context) {

    private val interpreter: Interpreter
    private val inputSize: Int

    private val labels = listOf(
        "lantana",
        "neltuma",
        "non_invasive",
        "parthenium"
    )

    init {
        val assetFileDescriptor = context.assets.openFd("plant_model_fixed.tflite")
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel

        val modelBuffer = fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            assetFileDescriptor.startOffset,
            assetFileDescriptor.declaredLength
        )

        interpreter = Interpreter(modelBuffer)

        val inputShape = interpreter.getInputTensor(0).shape()
        inputSize = inputShape[1]

        Log.d("MODEL_INFO", "Input size: $inputSize x $inputSize")
    }

    fun predict(bitmap: Bitmap): Pair<String, Float> {

        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        val inputBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        inputBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputSize * inputSize)
        resized.getPixels(
            intValues,
            0,
            inputSize,
            0,
            0,
            inputSize,
            inputSize
        )

        // 🔥 Debug: first raw RGB pixel
        val firstPixel = intValues[0]
        val r0 = (firstPixel shr 16 and 0xFF)
        val g0 = (firstPixel shr 8 and 0xFF)
        val b0 = (firstPixel and 0xFF)
        Log.d("PIXEL_DEBUG", "First raw RGB: $r0, $g0, $b0")

        for (pixelValue in intValues) {

            val r = (pixelValue shr 16 and 0xFF)
            val g = (pixelValue shr 8 and 0xFF)
            val b = (pixelValue and 0xFF)

            // 🔥 NO normalization here
            inputBuffer.putFloat(r.toFloat())
            inputBuffer.putFloat(g.toFloat())
            inputBuffer.putFloat(b.toFloat())
        }

        // 🔥 DEBUG: Print first 10 normalized floats
        inputBuffer.rewind()
        Log.d("FLOAT_DEBUG", "First 10 floats:")
        for (i in 0 until 10) {
            Log.d("FLOAT_DEBUG", inputBuffer.float.toString())
        }

        // Rewind again before inference
        inputBuffer.rewind()

        val output = Array(1) { FloatArray(labels.size) }

        interpreter.run(inputBuffer, output)

        for (i in output[0].indices) {
            Log.d("MODEL_DEBUG", "Class $i (${labels[i]}) = ${output[0][i]}")
        }

        var maxIndex = 0
        var maxConfidence = Float.MIN_VALUE

        for (i in output[0].indices) {
            if (output[0][i] > maxConfidence) {
                maxConfidence = output[0][i]
                maxIndex = i
            }
        }

        return Pair(labels[maxIndex], maxConfidence)
    }
}