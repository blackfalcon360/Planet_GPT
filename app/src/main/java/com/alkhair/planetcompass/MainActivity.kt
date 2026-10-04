package com.alkhair.planetcompass

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.*
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale
import kotlin.math.*

data class Planet(
    val name: String,
    val azimuth: Double,
    val altitude: Double,
    val visible: Boolean
)

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var rotation = FloatArray(9)
    private var orientation = FloatArray(3)
    private var heading by mutableStateOf(0f)
    private var location by mutableStateOf(Location("default").apply {
        latitude = 34.0
        longitude = 71.5
    })
    private var hasLocation by mutableStateOf(false)

    private val locationPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (it[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                it[Manifest.permission.ACCESS_COARSE_LOCATION] == true) getLocation()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        startCompass()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) getLocation()
        else locationPermission.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF7DD3FC),
                    background = Color(0xFF050B14),
                    surface = Color(0xFF0B1625)
                )
            ) {
                PlanetCompassScreen(heading, location, hasLocation)
            }
        }
    }

    private fun startCompass() {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        sensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    private fun getLocation() {
        val client = LocationServices.getFusedLocationProviderClient(this)
        try {
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        location = loc
                        hasLocation = true
                    }
                }
        } catch (_: SecurityException) {}
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        SensorManager.getRotationMatrixFromVector(rotation, event.values)
        SensorManager.remapCoordinateSystem(
            rotation, SensorManager.AXIS_X, SensorManager.AXIS_Z, rotation
        )
        SensorManager.getOrientation(rotation, orientation)
        var deg = Math.toDegrees(orientation[0].toDouble()).toFloat()
        if (deg < 0) deg += 360f
        heading = deg
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }
}

@Composable
fun PlanetCompassScreen(heading: Float, location: Location, hasLocation: Boolean) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    val planets = remember(now, location.latitude, location.longitude) {
        PlanetCalculator.calculateAll(location.latitude, location.longitude, now)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF050B14)).padding(12.dp)
    ) {
        Text(
            "PLANET COMPASS",
            color = Color(0xFF7DD3FC),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            if (hasLocation) "Live sky • GPS location" else "Waiting for GPS location",
            color = Color.LightGray,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(8.dp))

        CompassView(heading, planets, Modifier.fillMaxWidth().weight(1f))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1625))
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    "Planets",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                planets.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(p.name, color = if (p.visible) Color(0xFF7DD3FC) else Color.Gray)
                        Text(
                            "${p.azimuth.roundToInt()}°  ${p.altitude.roundToInt()}°",
                            color = if (p.altitude > 0) Color.White else Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Azimuth / Altitude • Approximate astronomical positions",
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun CompassView(heading: Float, planets: List<Planet>, modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height / 2
            val radius = min(size.width, size.height) * .43f

            drawCircle(Color(0xFF081522), radius)
            drawCircle(Color(0xFF1D4057), radius, style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
            drawCircle(Color(0xFF123047), radius * .72f, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))

            for (i in 0 until 360 step 10) {
                val a = Math.toRadians((i - heading).toDouble())
                val inner = if (i % 30 == 0) radius * .86f else radius * .91f
                val x1 = cx + cos(a).toFloat() * inner
                val y1 = cy + sin(a).toFloat() * inner
                val x2 = cx + cos(a).toFloat() * radius
                val y2 = cy + sin(a).toFloat() * radius
                drawLine(Color(0xFF6B8798), Offset(x1,y1), Offset(x2,y2),
                    strokeWidth = if (i % 30 == 0) 3f else 1f)
            }

            // North pointer
            drawLine(Color(0xFFFF5A5F), Offset(cx, cy), 
                Offset(cx, cy - radius * .78f), strokeWidth = 5f)

            val colors = listOf(
                "Mercury" to Color(0xFFE0B88A),
                "Venus" to Color(0xFFFFE09B),
                "Mars" to Color(0xFFFF765F),
                "Jupiter" to Color(0xFFFFC56E),
                "Saturn" to Color(0xFFE8D09B),
                "Uranus" to Color(0xFF75E6E6),
                "Neptune" to Color(0xFF6EA8FF)
            )

            planets.forEach { p ->
                if (p.altitude < -10) return@forEach
                val relative = Math.toRadians((p.azimuth - heading).toDouble())
                val r = radius * (.64f + .22f * cos(Math.toRadians(p.altitude)).toFloat())
                val x = cx + sin(relative).toFloat() * r
                val y = cy - cos(relative).toFloat() * r * .62f
                val c = colors.firstOrNull { it.first == p.name }?.second ?: Color.White
                drawCircle(c, 10f, Offset(x,y))
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("N", color = Color(0xFFFF5A5F), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("${heading.roundToInt()}°", color = Color.White, fontSize = 12.sp)
        }

        planets.forEach { p ->
            if (p.altitude >= -10) {
                val relative = Math.toRadians((p.azimuth - heading).toDouble())
                // Label positions are approximated in the same horizontal compass frame.
                // A compact list below provides exact azimuth/altitude values.
            }
        }
    }
}

/**
 * Low-dependency planetary position engine.
 * Uses simplified orbital elements and standard transformations.
 * Typical visual error is suitable for a compass/sky-orientation app,
 * but it is NOT intended for precision telescope pointing.
 */
object PlanetCalculator {
    private data class E(
        val N0: Double, val Nd: Double,
        val i0: Double, val id: Double,
        val w0: Double, val wd: Double,
        val a0: Double, val ad: Double,
        val e0: Double, val ed: Double,
        val M0: Double, val Md: Double
    )

    private val elements = mapOf(
        "Mercury" to E(48.3313,3.24587E-5,7.0047,5E-8,29.1241,1.01444E-5,.387098,0, .205635,5.59E-10,168.6562,4.0923344368),
        "Venus" to E(76.6799,2.4659E-5,3.3946,2.75E-8,54.8910,1.38374E-5,.72333,0,.006773,-1.302E-9,48.0052,1.6021302244),
        "Mars" to E(49.5574,2.11081E-5,1.8497,-1.78E-8,286.5016,2.92961E-5,1.523688,0,.093405,2.516E-9,18.6021,.5240207766),
        "Jupiter" to E(100.4542,2.76854E-5,1.3030,-1.557E-7,273.8777,1.64505E-5,5.20256,0,.048498,4.469E-9,19.8950,.0830853001),
        "Saturn" to E(113.6634,2.3898E-5,2.4886,-1.081E-7,339.3939,2.97661E-5,9.55475,0,.055546,-9.499E-9,316.9670,.0334442282),
        "Uranus" to E(74.0005,1.3978E-5,.7733,1.9E-8,96.6612,3.0565E-5,19.18171,-1.55E-8,.047318,7.45E-9,142.5905,.011725806),
        "Neptune" to E(131.7806,3.0173E-5,1.77,-2.55E-7,272.8461,-6.027E-6,30.05826,3.313E-8,.008606,2.15E-9,260.2471,.005995147)
    )

    fun calculateAll(lat: Double, lon: Double, timeMillis: Long): List<Planet> {
        val jd = timeMillis / 86400000.0 + 2440587.5
        val d = jd - 2451543.5
        return elements.map { (name, e) ->
            val xyz = heliocentric(e, d)
            val earth = heliocentric(earthElements(), d)
            val gx = xyz[0] - earth[0]
            val gy = xyz[1] - earth[1]
            val gz = xyz[2] - earth[2]

            // Ecliptic -> equatorial
            val ob = Math.toRadians(23.4393 - 3.563E-7 * d)
            val xeq = gx
            val yeq = gy * cos(ob) - gz * sin(ob)
            val zeq = gy * sin(ob) + gz * cos(ob)

            val ra = atan2(yeq, xeq)
            val dec = atan2(zeq, sqrt(xeq*xeq + yeq*yeq))

            val lst = localSiderealTime(jd, lon)
            var ha = Math.toRadians(lst) - ra
            while (ha < -Math.PI) ha += 2*Math.PI
            while (ha > Math.PI) ha -= 2*Math.PI

            val latR = Math.toRadians(lat)
            val alt = asin(sin(latR)*sin(dec) + cos(latR)*cos(dec)*cos(ha))
            val az = atan2(
                sin(ha),
                cos(ha)*sin(latR) - tan(dec)*cos(latR)
            ) + Math.PI

            Planet(
                name,
                (Math.toDegrees(az) + 360) % 360,
                Math.toDegrees(alt),
                Math.toDegrees(alt) > 0
            )
        }
    }

    private fun earthElements() = E(0.0,0.0,0.0,0.0,282.9404,4.70935E-5,1.0,0.0,.016709,-1.151E-9,356.0470,.9856002585)

    private fun heliocentric(e: E, d: Double): DoubleArray {
        val N = Math.toRadians(e.N0 + e.Nd*d)
        val i = Math.toRadians(e.i0 + e.id*d)
        val w = Math.toRadians(e.w0 + e.wd*d)
        val a = e.a0 + e.ad*d
        val ecc = e.e0 + e.ed*d
        val M = Math.toRadians((e.M0 + e.Md*d) % 360.0)
        var E0 = M
        repeat(8) { E0 -= (E0 - ecc*sin(E0) - M)/(1 - ecc*cos(E0)) }
        val xv = a*(cos(E0)-ecc)
        val yv = a*(sqrt(1-ecc*ecc)*sin(E0))
        val v = atan2(yv,xv)
        val r = hypot(xv,yv)
        val xh = r*(cos(N)*cos(v+w)-sin(N)*sin(v+w)*cos(i))
        val yh = r*(sin(N)*cos(v+w)+cos(N)*sin(v+w)*cos(i))
        val zh = r*(sin(v+w)*sin(i))
        return doubleArrayOf(xh,yh,zh)
    }

    private fun localSiderealTime(jd: Double, lon: Double): Double {
        var t = (jd - 2451545.0) / 36525.0
        var st = 280.46061837 + 360.98564736629*(jd-2451545.0) +
                0.000387933*t*t - t*t*t/38710000.0 + lon
        st %= 360.0
        if (st < 0) st += 360.0
        return st
    }
}
