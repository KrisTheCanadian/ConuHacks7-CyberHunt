package me.kristhecanadian.cyberhunt.ui.ar

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.Location.distanceBetween
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.ar.core.Pose
import com.google.ar.sceneform.AnchorNode
import me.kristhecanadian.cyberhunt.Quiz
import me.kristhecanadian.cyberhunt.R
import me.kristhecanadian.cyberhunt.api.NearbyPlacesResponse
import me.kristhecanadian.cyberhunt.api.PlacesService
import me.kristhecanadian.cyberhunt.ar.ClueNode
import me.kristhecanadian.cyberhunt.ar.PlacesArFragment
import me.kristhecanadian.cyberhunt.databinding.FragmentArBinding
import me.kristhecanadian.cyberhunt.model.Clues
import me.kristhecanadian.cyberhunt.model.GeometryLocation
import me.kristhecanadian.cyberhunt.model.getPositionVector
import me.kristhecanadian.cyberhunt.ui.home.HomeFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ArFragment : Fragment(), SensorEventListener {

    private val TAG = "AR_Activity"

    private lateinit var placesService: PlacesService
    private lateinit var arFragment: PlacesArFragment
    private lateinit var mapFragment: SupportMapFragment

    // Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Sensor
    private lateinit var sensorManager: SensorManager
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var anchorNode: AnchorNode? = null
    private var markers: MutableList<Marker> = emptyList<Marker>().toMutableList()
    private var totalClues: List<Clues>? = null
    private var currentLocation: Location? = null
    private var map: GoogleMap? = null

    private var _binding: FragmentArBinding? = null

    private var hasBeenInitialized = false

    private var scores = arrayListOf<Int>()

    private var quizNumber = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {



        val viewOfLayout = inflater.inflate(R.layout.fragment_ar, container, false)

        // AR PANE
        arFragment = childFragmentManager.findFragmentById(R.id.ar_fragment) as PlacesArFragment
        // MAP
        mapFragment =
            childFragmentManager.findFragmentById(R.id.maps_fragment) as SupportMapFragment

        sensorManager =
            (requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager)
        placesService = PlacesService.create()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        readClues()

        // check for user movement and update the ar scene
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            // ask for permission
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
            return viewOfLayout
        }
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000,
            0f,
            object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    currentLocation = location
                    updateArScene()
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                    // do nothing
                }

                override fun onProviderEnabled(provider: String) {
                    // do nothing
                }

                override fun onProviderDisabled(provider: String) {
                    // do nothing
                }
            })



        setUpAr()
        setUpMaps()

        if (currentLocation != null) {
            updateArScene()
        }

        return viewOfLayout
    }

    private fun updateArScene() {
        // update the ar scene
        val anchor = arFragment.arSceneView.session?.createAnchor(
            Pose.makeTranslation(
                currentLocation!!.latitude.toFloat(),
                currentLocation!!.longitude.toFloat(),
                0f
            )
        )

        anchorNode = AnchorNode(anchor)
        anchorNode?.setParent(arFragment.arSceneView.scene)

        addClues(anchorNode!!)
        hasBeenInitialized = true

        Log.d(TAG, "Current location: ${currentLocation!!.latitude}, ${currentLocation!!.longitude}")
    }

    override fun onResume() {
        super.onResume()
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            val score = data?.getIntExtra("SCORE", 0)
            Log.d(TAG, "Score: $score")

            // remove the clue from the list
            markers.remove(markers[0])

            // remove the node from the scene
            anchorNode?.removeChild(anchorNode?.children?.get(0))

            // if there are no more clues, end the game
            if (markers.isEmpty()) {
                Log.d(TAG, "No more clues")
                val intent = Intent(requireContext(), HomeFragment::class.java)
                // create a toast saying the game is over
                Toast.makeText(
                    requireContext(),
                    "Game Over! You got completed all the challenges! Try again later!",
                    Toast.LENGTH_LONG
                ).show()
                startActivity(intent)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    private fun setUpAr() {
        arFragment.setOnTapArPlaneListener { hitResult, _, _ ->
            if(hasBeenInitialized) {
                Log.d(TAG, "Clues already planted")
                // create a toast saying that the clues have already been planted
                Toast.makeText(
                    requireContext(),
                    "Clues are already placed!",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnTapArPlaneListener
            }

            val anchor = hitResult.createAnchor()
            anchorNode = AnchorNode(anchor)
            anchorNode?.setParent(arFragment.arSceneView.scene)

            addClues(anchorNode!!)
            hasBeenInitialized = true
        }
    }

    private fun addClues(anchorNode: AnchorNode) {
        val currentLocation = currentLocation
        if (currentLocation == null) {
            Log.w(TAG, "Location has not been determined yet")
            return
        }

        val cluesList = totalClues
        if (cluesList == null) {
            Log.w(TAG, "No clues to put")
            return
        }

        for (clues in cluesList ) {

            val userLocation = getUserPosition()

            if (userLocation == null) {
                Log.w(TAG, "User location has not been determined yet")
                return
            }

            val distance = distanceBetween(userLocation, clues.geometry.location)

            if (distance < 100) {
                // log the clue
                Log.d(TAG, "Clue planted: ${clues.name}")
                // Add the place in AR
                val node = ClueNode(requireActivity().applicationContext, clues)
                node.setParent(anchorNode)

                node.localPosition = clues.getPositionVector(orientationAngles[0], currentLocation.latLng)
                node.setOnTapListener { _, _ ->
                    showInfoWindow(clues)
                    // TODO: add the clue to the list of found clues
                    Toast.makeText(context, "Opening Challenge!", Toast.LENGTH_SHORT).show()

                    // create an intent to go to to the quiz activity
                    val intent = Intent(context, Quiz::class.java)
                    startActivity(intent)
                }
            }

            Log.d(TAG, "Clue too far but added to map: ${clues.name}")

            // Add the place in maps
            map?.let {
                val marker = it.addMarker(
                    MarkerOptions()
                        .position(clues.geometry.location.latLng)
                        .title(clues.name)
                )
                marker?.tag = clues
                if (marker != null) {
                    markers.add(marker)
                }
            }
        }
    }

    private fun readClues() {
        val sharedPref = this.activity?.getSharedPreferences("cyberhunt", Context.MODE_PRIVATE)
        val size = sharedPref?.getInt("size", 0)
        val score = sharedPref?.getInt("quiz$quizNumber", -1)
        if (size != null) {
            if((score != -1) && (score != null) && (size > quizNumber)) {
                scores.add(score)
                quizNumber = scores.size
                // create a toast saying congrats on completing the quiz
                Toast.makeText(
                    requireContext(),
                    "Congratulations! You passed the quiz! You got $score out of 5!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        if(score == null){
            Log.d(TAG, "Score is null")
        }
    }

    private fun distanceBetween(userLocation: Location, cluesLocation: GeometryLocation): Float {
        val distance = FloatArray(1)
        distanceBetween(
            userLocation.latitude,
            userLocation.longitude,
            cluesLocation.lat,
            cluesLocation.lng,
            distance
        )
        return distance[0]
    }

    private fun getUserPosition(): Location? {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            // request the permission
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )

            return null
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location : Location? ->
                currentLocation = location
                Log.d(TAG, "Location: $location")
            }

        return currentLocation;
    }

    private fun showInfoWindow(place: Clues) {
        // Show in AR
        val matchingPlaceNode = anchorNode?.children?.filterIsInstance<ClueNode>()?.first {
            val otherPlace = (it as ClueNode).place ?: return@first false
            return@first otherPlace == place
        } as? ClueNode
        matchingPlaceNode?.showInfoWindow()

        // Show as marker
        val matchingMarker = markers.firstOrNull {
            val placeTag = (it.tag as? Clues) ?: return@firstOrNull false
            return@firstOrNull placeTag == place
        }
        matchingMarker?.showInfoWindow()
    }

    private fun setUpMaps() {
        mapFragment.getMapAsync { googleMap ->
            if (ActivityCompat.checkSelfPermission(
                    requireActivity().applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireActivity().applicationContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return@getMapAsync
            }
            googleMap.isMyLocationEnabled = true

            getCurrentLocation {
                val pos = CameraPosition.fromLatLngZoom(it.latLng, 13f)
                googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos))
                getNearbyPlaces(it)
            }
            googleMap.setOnMarkerClickListener { marker ->
                val tag = marker.tag
                if (tag !is Clues) {
                    return@setOnMarkerClickListener false
                }
                showInfoWindow(tag)
                return@setOnMarkerClickListener true
            }
            map = googleMap
        }
    }

    private fun getCurrentLocation(onSuccess: (Location) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                requireActivity().applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireActivity().applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            currentLocation = location
            onSuccess(location)
        }.addOnFailureListener {
            Log.e(TAG, "Could not get location")
        }
    }

    private fun getNearbyPlaces(location: Location) {
        val apiKey = resources.getString(R.string.googleMapApiKey)
        placesService.nearbyPlaces(
            apiKey = apiKey,
            location = "${location.latitude},${location.longitude}",
            radiusInMeters = 500,
            placeType = "park"
        ).enqueue(
            object : Callback<NearbyPlacesResponse> {
                override fun onFailure(call: Call<NearbyPlacesResponse>, t: Throwable) {
                    Log.e(TAG, "Failed to get nearby places", t)
                }

                override fun onResponse(
                    call: Call<NearbyPlacesResponse>,
                    response: Response<NearbyPlacesResponse>
                ) {
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Failed to get nearby places")
                        return
                    }

                    val places = response.body()?.results ?: emptyList()
                    this@ArFragment.totalClues = places
                }
            }
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) {
            return
        }
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
        }

        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

val Location.latLng: LatLng
    get() = LatLng(this.latitude, this.longitude)