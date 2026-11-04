package com.brasa.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.brasa.app.ui.BrasaApp

/** AppCompatActivity: necesaria para cambiar el idioma con AppCompatDelegate.setApplicationLocales. */
class MainActivity : AppCompatActivity() {
  private val vm: BrasaViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
    setContent { BrasaApp(vm) }
  }
}
