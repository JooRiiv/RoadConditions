package com.example.roadconditions

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var googleMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

        }
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        addCustomMarker()
    }


    private fun addCustomMarker() {
        // Mock data
        val locations = listOf(LatLng(61.462087, 23.843748),
        LatLng(61.465286, 23.812305),
        LatLng(61.471189, 23.861733)
        )
        for (location in locations) {
            val markerOptions = MarkerOptions()
                .position(location)
                .title("Possible Bump Detected")
                .snippet("Testi")
            googleMap.addMarker(markerOptions)
        }
        val tampere = LatLng(61.497234,23.759126)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tampere, 12f))
    }

}