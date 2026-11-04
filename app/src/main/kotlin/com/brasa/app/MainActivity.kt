package com.brasa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.brasa.app.ui.pantallas.GaleriaPantalla
import com.brasa.app.ui.theme.BrasaTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
    setContent {
      BrasaTheme(oscuro = true) {
        GaleriaPantalla()
      }
    }
  }
}
