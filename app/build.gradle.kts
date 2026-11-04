import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.roborazzi)
}

val localProps = Properties().apply {
  val f = rootProject.file("local.properties")
  if (f.exists()) f.inputStream().use { load(it) }
}

android {
  namespace = "com.brasa.app"
  compileSdk = 37

  defaultConfig {
    applicationId = "com.brasa.app"
    minSdk = 30
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"
    buildConfigField("String", "SERVER_URL", "\"${localProps.getProperty("SERVER_URL", "")}\"")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  androidResources {
    // Genera LocaleConfig (idiomas por app en Ajustes del sistema, API 33+).
    generateLocaleConfig = true
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      // Robolectric necesita acceder a DirectByteBuffer (el texto curvo de Wear usa PathIterator).
      all { it.jvmArgs("--add-opens=java.base/java.nio=ALL-UNNAMED", "--add-opens=java.base/java.lang=ALL-UNNAMED") }
    }
  }
  packaging {
    resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
  }
}

kotlin { jvmToolchain(17) }

roborazzi {
  outputDir.set(rootProject.file("docs/capturas/roborazzi"))
}

dependencies {
  implementation(platform(libs.compose.bom))
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.compose.ui)
  implementation(libs.compose.ui.graphics)
  implementation(libs.compose.foundation)
  implementation(libs.compose.ui.tooling.preview)
  implementation(libs.wear.compose.material3)
  implementation(libs.wear.compose.foundation)
  implementation(libs.wear.compose.navigation)
  implementation(libs.wear.compose.ui.tooling)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.serialization.json)
  debugImplementation(libs.compose.ui.tooling)
  debugImplementation(libs.compose.ui.test.manifest)

  testImplementation(libs.junit)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.rule)
  testImplementation(platform(libs.compose.bom))
  testImplementation(libs.compose.ui.test.junit4)
}
