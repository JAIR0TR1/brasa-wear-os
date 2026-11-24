package com.brasa.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.brasa.app.BrasaState
import com.brasa.app.R
import com.brasa.app.domain.Rules
import com.brasa.app.ui.brasa.Brasa
import com.brasa.app.ui.brasa.Stage
import com.brasa.app.ui.brasa.EmotionBackground
import com.brasa.app.ui.brasa.stageName
import com.brasa.app.ui.components.EdgeRing
import com.brasa.app.ui.components.EdgeTitle
import com.brasa.app.ui.theme.BrasaTheme
import com.brasa.app.ui.theme.BrasaType
import com.brasa.app.ui.theme.Tokens

/**
 * Evolución: etapa actual, nivel, XP (anillo dorado = progreso al siguiente nivel) y racha de días activos.
 */
@Composable
fun Evolution(state: BrasaState, fixedTime: Float? = null) {
  val colors = BrasaTheme.colors
  val gold = if (colors.dark) Tokens.Colors.Gold else Tokens.Colors.LightThemeFire
  val next = Stage.entries.firstOrNull { it.minLevel > state.level }
  val missingXp = next?.let { Rules.xpForLevel(it.minLevel) - state.xp } ?: 0
  val description = stringResource(R.string.evolution_description, stringResource(stageName(state.stage)), state.level, state.xp, state.streak)

  EmotionBackground(state.emotion, intensity = 0.8f) {
    EdgeRing(state.levelProgress, gold, animated = fixedTime == null)
    EdgeTitle(stringResource(R.string.evolution), margin = 18.dp)
    Column(
      Modifier.fillMaxSize().padding(horizontal = 28.dp).semantics(mergeDescendants = true) { contentDescription = description },
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(Modifier.height(34.dp))
      Brasa(state.emotion, state.stage, Modifier.fillMaxWidth().weight(1f), fixedTime = fixedTime)
      Text(stringResource(stageName(state.stage)), style = BrasaType.title, color = colors.text, maxLines = 1)
      Text(
        stringResource(R.string.level_xp, state.level, java.text.NumberFormat.getIntegerInstance(androidx.compose.ui.platform.LocalConfiguration.current.locales[0]).format(state.xp)),
        style = BrasaType.data, color = gold, maxLines = 1,
      )
      Spacer(Modifier.height(6.dp))
      // Racha: siete brasas; las encendidas son días activos seguidos.
      Row(verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until 7) {
          val lit = i < state.streak.coerceAtMost(7)
          Canvas(Modifier.size(10.dp)) {
            val c = center
            if (lit) {
              drawCircle(Brush.radialGradient(listOf(Tokens.Colors.LightFire, Tokens.Colors.Fire, Color.Transparent), center = c, radius = size.minDimension), size.minDimension, c)
              drawCircle(Tokens.Colors.LightFire, size.minDimension * 0.24f, c)
            } else {
              drawCircle(colors.track, size.minDimension * 0.28f, c)
            }
          }
          if (i < 6) Spacer(Modifier.width(3.dp))
        }
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.streak_days, state.streak), style = BrasaType.data, color = colors.secondaryText)
      }
      Spacer(Modifier.height(2.dp))
      Text(
        if (next != null) stringResource(R.string.next_stage, stringResource(stageName(next)), next.minLevel, java.text.NumberFormat.getIntegerInstance(androidx.compose.ui.platform.LocalConfiguration.current.locales[0]).format(missingXp.coerceAtLeast(0)))
        else stringResource(R.string.max_stage),
        style = BrasaType.voice.copy(fontSize = 12.sp, lineHeight = 14.sp), color = colors.secondaryText, maxLines = 2,
        // Abajo el círculo se estrecha: margen extra para que no se corte.
        modifier = Modifier.padding(horizontal = 10.dp),
      )
      Spacer(Modifier.height(28.dp))
    }
  }
}
