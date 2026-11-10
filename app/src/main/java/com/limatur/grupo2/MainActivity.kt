package com.limatur.grupo2

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager

data class TouristPlace(
    val name: String, val district: String, val type: String,
    val description: String, val latitude: Double, val longitude: Double
) : ClusterItem {
    private val positionValue = LatLng(latitude, longitude)
    override fun getPosition() = positionValue
    override fun getTitle() = name
    override fun getSnippet() = "$district • $type"
    override fun getZIndex(): Float? = 0f
}

class MainActivity : AppCompatActivity() {
    private lateinit var map: GoogleMap
    private lateinit var clusterManager: ClusterManager<TouristPlace>
    private lateinit var typeSpinner: Spinner
    private lateinit var districtSpinner: Spinner
    private lateinit var countText: TextView
    private lateinit var searchInput: EditText
    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private var allPlaces: List<TouristPlace> = emptyList()
    private var nightMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        typeSpinner = findViewById(R.id.typeFilter)
        districtSpinner = findViewById(R.id.districtFilter)
        countText = findViewById(R.id.resultsCount)
        searchInput = findViewById(R.id.searchInput)
        val nightSwitch: Switch = findViewById(R.id.nightSwitch)

        allPlaces = samplePlaces()
        setupFilters()
        val mapFragment = SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction().replace(R.id.mapContainer, mapFragment).commit()
        mapFragment.getMapAsync { googleMap ->
            map = googleMap
            map.uiSettings.isZoomControlsEnabled = true
            map.uiSettings.isCompassEnabled = true
            clusterManager = ClusterManager(this, map)
            map.setOnCameraIdleListener(clusterManager)
            map.setOnMarkerClickListener(clusterManager)
            clusterManager.renderer = com.google.maps.android.clustering.view.DefaultClusterRenderer(this, map, clusterManager)
            clusterManager.setOnClusterClickListener { cluster ->
                zoomToCluster(cluster)
                true
            }
            clusterManager.setOnClusterItemClickListener { place ->
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(place.position, 16f))
                false
            }
            applyFilters()
        }

        typeSpinner.onItemSelectedListener = SimpleItemSelected { applyFilters() }
        districtSpinner.onItemSelectedListener = SimpleItemSelected { applyFilters() }
        searchInput.doAfterTextChanged {
            searchRunnable?.let(handler::removeCallbacks)
            searchRunnable = Runnable { applyFilters() }
            handler.postDelayed(searchRunnable!!, 300L)
        }
        nightSwitch.setOnCheckedChangeListener { _, checked ->
            nightMode = checked
            if (::map.isInitialized) applyMapStyle()
        }
    }

    private fun setupFilters() {
        val types = listOf("Todos los tipos") + allPlaces.map { it.type }.distinct().sorted()
        val districts = listOf("Todos los distritos") + allPlaces.map { it.district }.distinct().sorted()
        typeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        districtSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, districts)
    }

    private fun applyFilters() {
        if (!::clusterManager.isInitialized) return
        val type = typeSpinner.selectedItem?.toString() ?: "Todos los tipos"
        val district = districtSpinner.selectedItem?.toString() ?: "Todos los distritos"
        val query = searchInput.text.toString().trim().lowercase()
        val filtered = allPlaces.filter { place ->
            (type == "Todos los tipos" || place.type == type) &&
            (district == "Todos los distritos" || place.district == district) &&
            (query.isBlank() || place.name.lowercase().contains(query) ||
                place.district.lowercase().contains(query) || place.description.lowercase().contains(query))
        }
        clusterManager.clearItems()
        clusterManager.addItems(filtered)
        clusterManager.cluster()
        countText.text = "${filtered.size} lugares encontrados"
        if (filtered.isNotEmpty() && query.isNotBlank()) {
            val bounds = LatLngBounds.builder()
            filtered.forEach { bounds.include(it.position) }
            try { map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 90)) } catch (_: Exception) { }
        }
    }

    private fun zoomToCluster(cluster: Cluster<TouristPlace>) {
        val bounds = LatLngBounds.builder()
        cluster.items.forEach { bounds.include(it.position) }
        try { map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 120)) }
        catch (_: Exception) { map.animateCamera(CameraUpdateFactory.newLatLngZoom(cluster.position, 14f)) }
    }

    private fun applyMapStyle() {
        val style = if (nightMode) NIGHT_STYLE else DAY_STYLE
        map.setMapStyle(MapStyleOptions(style))
    }

    private fun samplePlaces(): List<TouristPlace> = listOf(
        p("Plaza Mayor de Lima","Cercado de Lima","Histórico",-12.0464,-77.0300),
        p("Catedral de Lima","Cercado de Lima","Religioso",-12.0465,-77.0307),
        p("Palacio de Gobierno","Cercado de Lima","Histórico",-12.0468,-77.0306),
        p("Convento de San Francisco","Cercado de Lima","Religioso",-12.0438,-77.0282),
        p("Casa de la Literatura Peruana","Cercado de Lima","Cultural",-12.0447,-77.0297),
        p("Museo de Arte de Lima","Cercado de Lima","Museo",-12.0600,-77.0365),
        p("Parque de la Exposición","Cercado de Lima","Parque",-12.0590,-77.0350),
        p("Circuito Mágico del Agua","Cercado de Lima","Parque",-12.0632,-77.0348),
        p("Parque de la Muralla","Cercado de Lima","Parque",-12.0430,-77.0258),
        p("Barrio Chino","Cercado de Lima","Cultural",-12.0550,-77.0257),
        p("Huaca Pucllana","Miraflores","Arqueológico",-12.1111,-77.0317),
        p("Parque Kennedy","Miraflores","Parque",-12.1211,-77.0297),
        p("Malecón de Miraflores","Miraflores","Mirador",-12.1250,-77.0350),
        p("Larcomar","Miraflores","Comercial",-12.1310,-77.0305),
        p("Parque del Amor","Miraflores","Parque",-12.1300,-77.0360),
        p("Faro de la Marina","Miraflores","Mirador",-12.1175,-77.0480),
        p("Casa Museo Ricardo Palma","Miraflores","Museo",-12.1190,-77.0290),
        p("Museo Amano","Miraflores","Museo",-12.1105,-77.0480),
        p("Parque Reducto No. 2","Miraflores","Histórico",-12.1270,-77.0200),
        p("Playa Waikiki","Miraflores","Playa",-12.1315,-77.0390),
        p("Parque El Olivar","San Isidro","Parque",-12.0960,-77.0360),
        p("Huaca Huallamarca","San Isidro","Arqueológico",-12.0955,-77.0370),
        p("Bosque El Olivar","San Isidro","Parque",-12.0965,-77.0350),
        p("Centro Cultural PUCP","San Isidro","Cultural",-12.0970,-77.0365),
        p("Museo Marina Núñez del Prado","San Isidro","Museo",-12.0990,-77.0370),
        p("Parque Combate de Abtao","San Isidro","Parque",-12.0920,-77.0270),
        p("Parque de la Pera","San Isidro","Parque",-12.1010,-77.0520),
        p("Parque Bustamante","San Isidro","Parque",-12.1040,-77.0270),
        p("Parque Castilla","Lince","Parque",-12.0840,-77.0360),
        p("Parque Mariscal Ramón Castilla","Lince","Parque",-12.0830,-77.0350),
        p("Museo Larco","Pueblo Libre","Museo",-12.0720,-77.0700),
        p("Plaza Bolívar","Pueblo Libre","Histórico",-12.0760,-77.0630),
        p("Museo Nacional de Arqueología","Pueblo Libre","Museo",-12.0770,-77.0620),
        p("Parque El Carmen","Pueblo Libre","Parque",-12.0750,-77.0660),
        p("Fortaleza del Real Felipe","Callao","Histórico",-12.0620,-77.1500),
        p("Plaza Grau del Callao","Callao","Histórico",-12.0600,-77.1480),
        p("Museo Naval del Perú","Callao","Museo",-12.0610,-77.1490),
        p("Monumental Callao","Callao","Cultural",-12.0615,-77.1495),
        p("Islas Palomino","Callao","Naturaleza",-12.0700,-77.1750),
        p("Parque de las Leyendas","San Miguel","Parque",-12.0770,-77.0880),
        p("Plaza San Miguel","San Miguel","Comercial",-12.0775,-77.0820),
        p("Casa de la Cultura de San Miguel","San Miguel","Cultural",-12.0780,-77.0850),
        p("Parque Media Luna","San Miguel","Parque",-12.0770,-77.0950),
        p("Santuario de Pachacamac","Lurín","Arqueológico",-12.2490,-76.8950),
        p("Museo de Sitio de Pachacamac","Lurín","Museo",-12.2495,-76.8955),
        p("Pantanos de Villa","Chorrillos","Naturaleza",-12.2120,-76.9900),
        p("Morro Solar","Chorrillos","Mirador",-12.1850,-77.0220),
        p("Playa Agua Dulce","Chorrillos","Playa",-12.1600,-77.0260),
        p("Parque Fátima","Chorrillos","Parque",-12.1660,-77.0250),
        p("Parque de la Amistad","Santiago de Surco","Parque",-12.1280,-76.9880),
        p("Museo Oro del Perú","Santiago de Surco","Museo",-12.1110,-77.0000)
    )

    private fun p(name: String, district: String, type: String, lat: Double, lng: Double) =
        TouristPlace(name, district, type, "Lugar turístico de $district, Lima.", lat, lng)

    companion object {
        private const val DAY_STYLE = """{"featureType":"all","elementType":"geometry","stylers":[{"color":"#f2f5f3"}]}"""
        private const val NIGHT_STYLE = """{"featureType":"all","elementType":"geometry","stylers":[{"color":"#202c35"}]}"""
    }
}

private class SimpleItemSelected(private val action: () -> Unit) : android.widget.AdapterView.OnItemSelectedListener {
    override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) = action()
    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
}
