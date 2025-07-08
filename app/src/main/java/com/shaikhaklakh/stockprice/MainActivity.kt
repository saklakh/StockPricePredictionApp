package com.shaikhaklakh.stockprice

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class MainActivity : AppCompatActivity() {
    private lateinit var lineChart: LineChart
    private lateinit var tflite: Interpreter
    private val min = 13.12  // Replace with your real scaler min
    private val max = 702.1  // Replace with your real scaler max
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }





        lineChart = findViewById(R.id.lineChart)
        val predictButton = findViewById<Button>(R.id.predictButton)

        tflite = Interpreter(loadModelFile("stock_model.tflite"))


        val markerView = CustomMarkerView(this, R.layout.marker_view_layout)
        lineChart.marker = markerView

        predictButton.setOnClickListener {
            val symbol = findViewById<EditText>(R.id.symbolEditText).text.toString().uppercase()
            fetchStockData(symbol) { data ->
                runOnUiThread {
                    if (data != null && data.size == 60) {
                        val normalizedInput = data.map { (it - min) / (max - min) }
                        val input = Array(1) { Array(60) { FloatArray(1) } }
                        for (i in 0 until 60) {
                            input[0][i][0] = normalizedInput[i].toFloat()
                        }

                        val output = Array(1) { FloatArray(1) }
                        tflite.run(input, output)

                        val predictedNorm = output[0][0]
                        val predictedPrice = predictedNorm * (max - min) + min

                        showChart(data, predictedPrice)
                    }
                }
            }

            /*
            val inputSequence = generateSampleData() // simulate last 60 days
            val normalizedInput = inputSequence.map { (it - min) / (max - min) }

            val input = Array(1) { Array(60) { FloatArray(1) } }
            for (i in 0 until 60) {
                input[0][i][0] = normalizedInput[i].toFloat()
            }

            val output = Array(1) { FloatArray(1) }
            tflite.run(input, output)

            val predictedNorm = output[0][0]
            val predictedPrice = predictedNorm * (max - min) + min

            showChart(inputSequence, predictedPrice)

             */
        }



    }



    private fun generateSampleData(): List<Double> {
        // Replace with real data later
        return List(60) { index -> (130 + (index % 10)).toDouble() }  // sample close prices
    }

    private fun showChart(pastData: List<Double>, predicted: Double) {
        val actualEntries = mutableListOf<Entry>()
        val predictedEntries = mutableListOf<Entry>()

        // Plot past data (Actual)
        for (i in pastData.indices) {
            actualEntries.add(Entry(i.toFloat(), pastData[i].toFloat()))
        }

        // Plot predicted data
        predictedEntries.add(Entry(pastData.size.toFloat(), predicted.toFloat()))

        // Create datasets
        val actualDataSet = LineDataSet(actualEntries, "Actual Price")
        actualDataSet.color = Color.BLUE
        actualDataSet.setCircleColor(Color.BLUE)
        actualDataSet.lineWidth = 2f

        val predictedDataSet = LineDataSet(predictedEntries, "Predicted Price")
        predictedDataSet.color = Color.RED
        predictedDataSet.setCircleColor(Color.RED)
        predictedDataSet.lineWidth = 2f
        predictedDataSet.setDrawValues(true)

        // Combine both
        val lineData = LineData(actualDataSet, predictedDataSet)
        lineChart.data = lineData

        // Axis customization
        val xAxis = lineChart.xAxis
        xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = -45f
        xAxis.setDrawGridLines(false)

        val leftAxis = lineChart.axisLeft
        leftAxis.setDrawGridLines(true)

        lineChart.axisRight.isEnabled = false

        // Description and legend
        lineChart.description.text = "Actual vs Predicted Stock Price"
        lineChart.legend.isEnabled = true

        // Interaction
        lineChart.setTouchEnabled(true)
        lineChart.setPinchZoom(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(true)

        // Refresh
        lineChart.invalidate()
    }





    private fun loadModelFile(modelFileName: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(modelFileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }




    fun fetchStockData(symbol: String, callback: (List<Double>?) -> Unit) {
        val url = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=$symbol&apikey=CK01Y9PXEXN8VORG&datatype=csv"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { body ->
                    val lines = body.lines().drop(1).take(60) // Skip header, take latest 60
                    val closes = lines.mapNotNull {
                        val cols = it.split(",")
                        if (cols.size > 4) cols[4].toDoubleOrNull() else null
                    }.reversed()
                    callback(closes)
                } ?: callback(null)
            }
        })
    }




}