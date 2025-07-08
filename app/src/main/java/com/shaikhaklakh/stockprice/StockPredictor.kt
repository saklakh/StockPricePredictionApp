package com.shaikhaklakh.stockprice

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.MappedByteBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.io.FileInputStream
import kotlin.math.max
import kotlin.math.min

class StockPredictor(context: Context) {

    private var interpreter: Interpreter

    init {
        val model = loadModel(context, "stock_model.tflite")
        interpreter = Interpreter(model)
    }

    private fun loadModel(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    // Make a prediction: Provide 60 scaled prices (0-1)
    fun predictNext(scaledInput: FloatArray): Float {
        val inputBuffer = ByteBuffer.allocateDirect(60 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())
        for (value in scaledInput) {
            inputBuffer.putFloat(value)
        }

        inputBuffer.rewind()

        val inputArray = Array(1) { Array(60) { FloatArray(1) } }
        for (i in 0 until 60) {
            inputArray[0][i][0] = scaledInput[i]
        }

        val outputArray = Array(1) { FloatArray(1) }
        interpreter.run(inputArray, outputArray)
        return outputArray[0][0] // Still normalized, needs to be inverse transformed
    }
}
