package cat.dam.andy.googlemaps_kt

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //Inicialitza fragment
        val mapFragment: Fragment = MapFragment(this)
        //Obre fragment al frame del layout
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.map_frame, mapFragment)
            .commit()
    }
}