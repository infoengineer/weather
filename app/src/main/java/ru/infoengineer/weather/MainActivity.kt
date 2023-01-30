package ru.infoengineer.weather

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException

data class WeatherData(val temperature_out: String, val temperature_in: String, val humidity: String, val pressure: String)

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private val url = BuildConfig.API_URL
    private lateinit var temperatureOutView: TextView
    private lateinit var temperatureInView: TextView
    private lateinit var humidityView: TextView
    private lateinit var pressureView: TextView
    @Suppress("DEPRECATION")
    private var handler = Handler()
    private var runnable: Runnable? = null
    private val delay = 60000
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        temperatureOutView = findViewById(R.id.temperature_out)
        temperatureInView = findViewById(R.id.temperature_in)
        humidityView = findViewById(R.id.humidity)
        pressureView = findViewById(R.id.pressure)
        if (isOnline(this)) {
            run(url)
        }
        else {
            Toast.makeText(this, "Проверьте подключение к интернету", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onResume() {
        handler.postDelayed(Runnable {
            handler.postDelayed(runnable!!, delay.toLong())
            run(url)
        }.also { runnable = it }, delay.toLong())
        super.onResume()
    }
    override fun onPause() {
        handler.removeCallbacks(runnable!!)
        super.onPause()
    }
    private fun run(url: String) {
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val gson = Gson()
                    val wd = gson.fromJson(response.body!!.string(), WeatherData::class.java)
                    runOnUiThread {
                        temperatureOutView.text = wd.temperature_out.replaceFirst(".", ",")
                        try {
                            val tempOut = wd.temperature_out.replace("°", "").toFloat().toInt().toByte()
                            temperatureOutView.setTextColor(Color.parseColor(getTextColor(tempOut)))
                        } catch (_:Exception) {}
                        temperatureInView.text = wd.temperature_in.replaceFirst(".", ",")
                        try {
                            val tempIn = wd.temperature_in.replace("°", "").toFloat().toInt().toByte()
                            temperatureInView.setTextColor(Color.parseColor(getTextColor(tempIn)))
                        } catch (_:Exception) {}
                        try {
                            val hum = wd.humidity.replace("%", "").toFloat().toInt().toByte()
                            humidityView.setTextColor(Color.parseColor(getTextColor2(hum)))
                        } catch (_:Exception) {}
                        humidityView.text = wd.humidity.replaceFirst(".", ",")
                        pressureView.text = wd.pressure.replaceFirst(".", ",")

                    }
                }
            }
        })
    }

    private fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }
    @SuppressLint("ResourceType")
    private fun getTextColor(value: Byte): String {
        return when (value) {
            in -50..4 -> resources.getString(R.color.cold)
            in 5..9 -> resources.getString(R.color.cool)
            in 10..14 -> resources.getString(R.color.lukewarm)
            in 15..19 -> resources.getString(R.color.comfortable)
            in 20..24 -> resources.getString(R.color.warm)
            in 25..29 -> resources.getString(R.color.beach)
            in 30..50 -> resources.getString(R.color.hot)
            else -> resources.getString(R.color.default_color)
        }
    }
    @SuppressLint("ResourceType")
    private fun getTextColor2(value: Byte): String {
        return when (value) {
            in 0..39 -> resources.getString(R.color.low)
            in 40..69 -> resources.getString(R.color.normal)
            in 70..100 -> resources.getString(R.color.high)
            else -> resources.getString(R.color.default_color)
        }
    }
}