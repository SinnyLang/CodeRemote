package xyz.sl.coderemote

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

import xyz.sl.coderemote.ui.StartProjectActivity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, StartProjectActivity::class.java))
        finish()
    }
}


