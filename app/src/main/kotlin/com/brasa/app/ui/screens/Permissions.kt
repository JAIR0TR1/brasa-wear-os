package com.brasa.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Text
import com.brasa.app.R
import com.brasa.app.ui.brasa.Brasa
import com.brasa.app.ui.brasa.Emotion
import com.brasa.app.ui.brasa.Stage
import com.brasa.app.ui.brasa.EmotionBackground
import com.brasa.app.ui.components.BrasaButton
import com.brasa.app.ui.theme.BrasaTheme
import com.brasa.app.ui.theme.BrasaType
import com.brasa.app.ui.theme.Tokens

/**
 * Antes de pedir permisos, Brasa explica para qué los usa (pedir en contexto) y aclara que no es
 * un dispositivo médico. "Ahora no" deja la app en modo manual, totalmente usable.
 */
@Composable
fun Permissions(onAllow: () -> Unit, onNotNow: () -> Unit, fixedTime: Float? = null) {
  val colors = BrasaTheme.colors
  val accent = Tokens.palette(Emotion.HAPPY, colors.dark).accent
  EmotionBackground(Emotion.HAPPY, intensity = 0.8f) {
    Column(
      Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 26.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(Modifier.height(Tokens.Spacing.s))
      Brasa(Emotion.HAPPY, Stage.SPARK, Modifier.fillMaxWidth(0.5f).height(44.dp), fixedTime = fixedTime)
      Text(stringResource(R.string.permissions_title), style = BrasaType.title, color = colors.text)
      Spacer(Modifier.height(Tokens.Spacing.xs))
      Text(stringResource(R.string.permissions_text), style = BrasaType.voice, color = accent)
      Spacer(Modifier.height(Tokens.Spacing.xs))
      // La acción principal, visible sin desplazar.
      BrasaButton(stringResource(R.string.allow), accent, onAllow)
      Box(
        Modifier.defaultMinSize(minHeight = Tokens.Spacing.touchTarget).fillMaxWidth()
          .clickable(role = Role.Button, onClick = onNotNow),
        contentAlignment = Alignment.Center,
      ) {
        Text(stringResource(R.string.not_now), style = BrasaType.label, color = colors.secondaryText)
      }
      Text(stringResource(R.string.privacy_notice), style = BrasaType.data, color = colors.secondaryText)
      Spacer(Modifier.height(Tokens.Spacing.s))
      Text(
        stringResource(R.string.medical_notice), style = BrasaType.data.copy(textAlign = TextAlign.Center),
        color = colors.secondaryText,
      )
      Spacer(Modifier.height(36.dp))
    }
  }
}
