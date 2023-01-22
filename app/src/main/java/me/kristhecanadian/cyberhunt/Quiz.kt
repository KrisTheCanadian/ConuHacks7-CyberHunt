package me.kristhecanadian.cyberhunt

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import me.kristhecanadian.cyberhunt.databinding.ActivityLandingBinding

class Quiz : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // create a binding
        val binding = ActivityLandingBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }


}