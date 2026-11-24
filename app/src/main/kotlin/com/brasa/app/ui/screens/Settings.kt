package com.brasa.app.ui.screens

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Text
import com.brasa.app.DataSource
import com.brasa.app.R
import com.brasa.app.data.AppSettings
import com.brasa.app.ui.brasa.Emotion
import com.brasa.app.ui.brasa.Stage
import com.brasa.app.ui.brasa.EmotionBackground
import com.brasa.app.ui.brasa.emotionName
import com.brasa.app.ui.brasa.stageName
import com.brasa.app.ui.components.SettingRow
import com.brasa.app.ui.theme.BrasaTheme
import com.brasa.app.ui.theme.BrasaType
import com.brasa.app.ui.theme.Tokens

/** Acciones de Ajustes (las implementa el ViewModel). */
data class SettingsActions(
  val theme: (Boolean) -> Unit,
  val demo: (Boolean) -> Unit,
  val emotion: (Emotion?) -> Unit,
  val stage: (Stage?) -> Unit,
  val permissions: () -> Unit = {},
)

private fun <T> next(current: T?, values: List<T>): T? {
  val i = if (current == null) -1 else values.indexOf(current)
  return if (i + 1 >= values.size) null else values[i + 1]
}

/** Idioma: Sistema → Español → English → Sistema. AppCompat recrea la actividad y lo guarda (autoStoreLocales). */
private fun cycleLanguage() {
  val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
  val newValue = when {
    current.isEmpty() -> "es"
    current.startsWith("es") -> "en"
    else -> ""
  }
  AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(newValue))
}

@Composable
fun SettingsScreen(
  settings: AppSettings,
  actions: SettingsActions,
  currentLanguage: String = AppCompatDelegate.getApplicationLocales().toLanguageTags(),
  source: DataSource = DataSource.SENSORS,
) {
  val colors = BrasaTheme.colors
  val accent = colors.fire
  val state = rememberScalingLazyListState(initialCenterItemIndex = 0)
  val language = when {
    currentLanguage.isEmpty() -> stringResource(R.string.language_system)
    currentLanguage.startsWith("es") -> "Español"
    else -> "English"
  }
  EmotionBackground(Emotion.CALM, intensity = 0.5f) {
    ScalingLazyColumn(
      Modifier.fillMaxSize(),
      state = state,
      autoCentering = null,
      // Márgenes laterales: las filas no tocan el borde del círculo.
      contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 30.dp, bottom = 44.dp),
    ) {
      item {
        Text(
          stringResource(R.string.settings), style = BrasaType.title, color = colors.text,
          modifier = Modifier.padding(bottom = Tokens.Spacing.xs).semantics { heading() },
        )
      }
      item {
        SettingRow(
          stringResource(R.string.setting_theme),
          stringResource(if (settings.dark) R.string.theme_dark else R.string.theme_light),
          accent, { actions.theme(!settings.dark) }, Modifier.fillMaxWidth(),
        )
      }
      item { SettingRow(stringResource(R.string.setting_language), language, accent, ::cycleLanguage, Modifier.fillMaxWidth()) }
      item {
        SettingRow(
          stringResource(R.string.setting_demo),
          stringResource(if (settings.demo) R.string.enabled else R.string.disabled),
          accent, { actions.demo(!settings.demo) }, Modifier.fillMaxWidth(),
        )
      }
      if (settings.demo) {
        item {
          val e = settings.demoEmotion
          SettingRow(
            stringResource(R.string.setting_emotion),
            if (e == null) stringResource(R.string.automatic) else stringResource(emotionName(e)),
            e?.let { Tokens.palette(it, colors.dark).accent } ?: accent,
            { actions.emotion(next(e, Emotion.entries)) }, Modifier.fillMaxWidth(),
          )
        }
        item {
          val e = settings.demoStage
          SettingRow(
            stringResource(R.string.setting_stage),
            if (e == null) stringResource(R.string.automatic) else stringResource(stageName(e)),
            accent, { actions.stage(next(e, Stage.entries)) }, Modifier.fillMaxWidth(),
          )
        }
      }
      item {
        SettingRow(
          stringResource(R.string.setting_permissions),
          stringResource(
            when (source) {
              DataSource.SENSORS -> R.string.permissions_active
              DataSource.MANUAL -> R.string.permissions_manual
              DataSource.DEMO -> R.string.permissions_demo
            },
          ),
          accent, actions.permissions, Modifier.fillMaxWidth(),
        )
      }
      item {
        Box(Modifier.padding(top = Tokens.Spacing.s, start = 6.dp, end = 6.dp)) {
          Text(stringResource(R.string.medical_notice), style = BrasaType.voice, color = colors.secondaryText)
        }
      }
      item {
        Text(stringResource(R.string.privacy_notice), style = BrasaType.data, color = colors.secondaryText, modifier = Modifier.padding(horizontal = 6.dp))
      }
    }
  }
}
