package cat.dam.andy.googlemaps_kt

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale

class MapFragment(private val context: Context) : Fragment() {

    //Members
    private val UPDATE_INTERVAL: Long = 10000 /* 10 segons */
    private val FASTEST_INTERVAL: Long = 5000 /* 5 segons */
    private val DEFAULT_LAT = 42.1152668
    private val DEFAULT_LONG = 2.7656192 //Ubicació per defecte (Banyoles)
    private val MAP_ZOOM = 10 //ampliació de zoom al marcador (més gran, més zoom)
    private val MAP_LOCATION_ZOOM = 17 //ampliació de zoom al marcador ubicació

    private var map: GoogleMap? = null
    private var supportMapFragment: SupportMapFragment? = null
    private var myLocation: Location? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var locationMarker: Marker? = null
    private var locationFound = false
    private var locationCallback: LocationCallback? = null

    private var tvLatitude: TextView? = null
    private var tvLongitude: TextView? = null
    private var btnFindMe: Button? = null

    private var permissionManager = PermissionManager(context)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_map, container, false)
        initViews()
        initPermissions()
        initMap()
        initListeners()
        showMap()
        //Retorna view del fragment
        return view
    }

    private fun initViews() {
        tvLatitude = requireActivity().findViewById<TextView>(R.id.tv_latitude)
        tvLongitude = requireActivity().findViewById<TextView>(R.id.tv_longitude)
        btnFindMe = requireActivity().findViewById<Button>(R.id.btn_find_me)
    }

    private fun initPermissions() {
        permissionManager.addPermission(
            Manifest.permission.ACCESS_FINE_LOCATION,
            getString(R.string.locationPermissionInfo),
            getString(R.string.locationPermissionNeeded),
            getString(R.string.locationPermissionDenied),
            getString(R.string.locationPermissionThanks),
            getString(R.string.locationPermissionSettings)
        )
    }

    private fun initMap() {
        //Initialitza fragment mapa
        supportMapFragment =
            childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment?

        //Mapa asíncron
        if (supportMapFragment != null) {
            supportMapFragment?.getMapAsync { googleMap: GoogleMap ->
                configureMap(googleMap)
                addMarkers(googleMap)
            }
        }
    }

    private fun configureMap(googleMap: GoogleMap) {
//Configuració i paràmetres del mapa
        map = googleMap
        googleMap.mapType =
            GoogleMap.MAP_TYPE_NORMAL //Representa un típic mapa de carretera amb noms de carrers i etiquetes
        /* MAP_TYPE_SATELLITE); //Representa una vista satèl·lit de l'àrea sense nom de carrers ni etiquetes
MAP_TYPE_TERRAIN); //Dades topogràfiques. El mapa inclou colors, línies de nivells i etiquetes, i perspectiva ombrejada. Alguns carrers i etiquetes poden també ser visibles.
MAP_TYPE_HYBRID); //Combina una vista de satèl·lit i la normal amb totes les etiquetes*/
        googleMap.uiSettings.isZoomControlsEnabled = true //mostrem botons zoom
        googleMap.uiSettings.isZoomGesturesEnabled = true //possibilitat d'ampliar amb dits
        googleMap.uiSettings.isCompassEnabled = true //mostrem bruixola
        // googleMap.setTrafficEnabled(true); //podriem habilitar visió trànsit
    }

    private fun addMarkers(googleMap: GoogleMap) {
        // Afegeix marcadors un cop configurat el mapa
        val latLngGirona =
            LatLng(41.9802474, 2.78356)
        val markerOptionsGirona =
            MarkerOptions().position(latLngGirona).title("Girona")
                .snippet(getString(R.string.jewishGirona))
        markerOptionsGirona.icon(
            BitmapDescriptorFactory.defaultMarker(
                BitmapDescriptorFactory.HUE_CYAN
            )
        )
        googleMap.addMarker(markerOptionsGirona)
        val latLngBesalu =
            LatLng(42.1998706, 2.6890259)
        val markerOptionsBesalu =
            MarkerOptions().position(latLngBesalu).title("Besalú")
                .snippet(getString(R.string.jewishBesalu))
        markerOptionsBesalu.icon(
            BitmapDescriptorFactory.defaultMarker(
                BitmapDescriptorFactory.HUE_CYAN
            )
        )
        googleMap.addMarker(markerOptionsBesalu)
        val latLngDefault =
            LatLng(DEFAULT_LAT, DEFAULT_LONG)
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLngDefault)) //es situa a la posició per defecte
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(MAP_ZOOM.toFloat())) //ampliació extra d'aproximació
        // Podem afegir listeners al mapa
        googleMap.setOnMapClickListener { latLng: LatLng ->
            // Quan es clica el mapa inicialitza el marcador a on ha clicat
            val markerOptions =
                MarkerOptions() //podem canviar la icona o el color
            //markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.custom_marker))
            //googleMap.clear(); //esborrem tots els marcadors
            googleMap.addMarker(
                markerOptions.position(latLng).title(
                    getString(R.string.clickedHere) + " (LAT:" + String.format(
                        Locale.getDefault(),
                        "%.4f",
                        latLng.latitude
                    ) + " LONG:" + String.format(
                        Locale.getDefault(),
                        "%.4f",
                        latLng.longitude
                    ) + ")"
                )
            )
        }
    }

    private fun initListeners() {
        btnFindMe?.setOnClickListener { showMap() }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        // Obté la posició actual
        // mentre cerca la localització no es permet clicar de nou el botó
        btnFindMe?.setText(R.string.waitingLocation)
        btnFindMe?.isEnabled = false
        //Inicialitza l'objecte necessari per conèixer la ubicació
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(
            requireContext()
        )
        //Configura l'actualització de les peticions d'ubicació
        val locationRequest = LocationRequest.Builder(UPDATE_INTERVAL)
        //Aquest mètode estableix la velocitat en mil·lisegons en què l'aplicació prefereix rebre actualitzacions d'ubicació. Tingueu en compte que les actualitzacions d'ubicació poden ser una mica més ràpides o més lentes que aquesta velocitat per optimitzar l'ús de la bateria, o pot ser que no hi hagi actualitzacions (si el dispositiu no té connectivitat, per exemple).
        locationRequest.setMinUpdateIntervalMillis(FASTEST_INTERVAL)
        //Aquest mètode estableix la taxa més ràpida en mil·lisegons en què la vostra aplicació pot gestionar les actualitzacions d'ubicació gràcies a peticions d'altres apps. A menys que la vostra aplicació es beneficiï de rebre actualitzacions més ràpidament que la taxa especificada a setInterval (), no cal que toqueu a aquest mètode.
        locationRequest.setPriority(Priority.PRIORITY_HIGH_ACCURACY)
        /*
           PRIORITY_BALANCED_POWER_ACCURACY - Utilitzeu aquest paràmetre per sol·licitar la precisió de la ubicació a un bloc de la ciutat, que té una precisió aproximada de 100 metres. Es considera un nivell aproximat de precisió i és probable que consumeixi menys energia. Amb aquesta configuració, és probable que els serveis d’ubicació utilitzin el WiFi i el posicionament de la torre cel·lular. Tingueu en compte, però, que l'elecció del proveïdor d'ubicació depèn de molts altres factors, com ara quines fonts estan disponibles.
           PRIORITY_HIGH_ACCURACY - Utilitzeu aquesta configuració per sol·licitar la ubicació més precisa possible. Amb aquesta configuració, és més probable que els serveis d’ubicació utilitzin el GPS per determinar la ubicació i consumeixi molta més energia.
           PRIORITY_LOW_POWER - Utilitzeu aquest paràmetre per sol·licitar una precisió a nivell de ciutat, que té una precisió d'aproximadament 10 quilòmetres. Es considera un nivell aproximat de precisió i és probable que consumeixi menys energia.
           PRIORITY_NO_POWER - Utilitzeu aquesta configuració si necessiteu un impacte insignificant en el consum d'energia, però voleu rebre actualitzacions d'ubicació quan estiguin disponibles. Amb aquesta configuració, l'aplicació no activa cap actualització d'ubicació, sinó que rep ubicacions activades per altres aplicacions.
            */
        //Crea un objecte de petició d'ubicació
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                for (location in locationResult.locations) {
                    if (location != null) {
                        // Hem rebut una nova localització
                        // Optem per actualitzar el marcador sense fer zoom per si l'usuari marca altres punts
                        showLocation(location, false)
                        // Però podriem optar per fer zoom sempre o en determinades condicions
                        // per exemple si està fora de la zona visible del mapa o ha canviat una distància determinada
                        // val latLng = LatLng(location.latitude, location.longitude) ... if (!isLocationVisible(latLng))
                        // if (location.distanceTo(myLocation!!) > 20)
                        // Si volguessim aturar les actualitzacions, també podem fer-ho amb aquesta línia
                        // fusedLocationProviderClient.removeLocationUpdates(locationCallback);
                    }
                }
            }
        }
        // Farem actualitzacions periodiques sempre i quan tinguem permisos, sinó els demanem i retornem
        if (!permissionManager.hasAllNeededPermissions()
        ) { //Si manquen permisos els demanem
            permissionManager.askForPermissions(
                permissionManager.getRejectedPermissions()
            )
            return
        }
        fusedLocationProviderClient?.requestLocationUpdates(
            locationRequest.build(),
            locationCallback as LocationCallback,
            Looper.getMainLooper()
        )
    }

    private fun isLocationVisible(latLng: LatLng): Boolean {
        val bounds = map?.projection?.visibleRegion?.latLngBounds
        return bounds?.contains(latLng) == true
    }

    fun showLocation(location: Location?, zoom: Boolean) {
        //mostra posició
        tvLatitude?.text = String.format(Locale.getDefault(), "%.4f", location!!.latitude)
        tvLongitude?.text =
            String.format(Locale.getDefault(), "%.4f", location.longitude)
        //Sincronitza mapa
        supportMapFragment?.getMapAsync { googleMap: GoogleMap ->
            //Inicialitza Lat i Long
            val latLng =
                LatLng(
                    location.latitude, location.longitude
                )
            //Crea el marcador
            val markerOptions =
                MarkerOptions().position(latLng).title(getString(R.string.currentLocation))
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
            if (locationFound) {
                locationMarker!!.remove() //eliminem antic marcador si s'havia trobat Ubicació
            } else {
                locationFound = true //cert per indicar que ja s'ha trobat una Ubicació
            }
            locationMarker = googleMap.addMarker(markerOptions)
            btnFindMe?.setText(R.string.get_location)
            btnFindMe?.isEnabled = true
            myLocation = location
            if (zoom) { //en cas de prèmer botó o altres casos necessaris fem zoom
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng)) //es situa a la posició
                googleMap.animateCamera(CameraUpdateFactory.zoomTo(MAP_LOCATION_ZOOM.toFloat())) //ampliació extra d'aproximació
            }
        }
    }

    private fun showMap() {
        if (!permissionManager.hasAllNeededPermissions()
        ) { //Si manquen permisos els demanem
            permissionManager.askForPermissions(
                permissionManager.getRejectedPermissions()
            )
        } else {
            //Si tenim permisos demanem la posició o la mostrem si ja la tenim
            if (!locationFound) {
                //Demanem la posició (per defecte ja es mostrarà el mapa)
                requestLocationUpdates()
            } else {
                //mostrem localització
                showLocation(myLocation, true)
            }
        }
    }
}