package com.brasa.app.ui.screens

import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Text
import com.brasa.app.R
import com.brasa.app.ui.components.EdgeTitle
import com.brasa.app.ui.brasa.Brasa
import com.brasa.app.ui.brasa.Emotion
import com.brasa.app.ui.brasa.Stage
import com.brasa.app.ui.brasa.EmotionBackground
import com.brasa.app.ui.brasa.emotionName
import com.brasa.app.ui.brasa.stageName
import com.brasa.app.ui.theme.BrasaTheme
import com.brasa.app.ui.theme.BrasaType
import com.brasa.app.ui.theme.Tokens
import kotlin.math.cos
import kotlin.math.sin

/** Una página de la galería: qué emoción y etapa mostrar, y qué regla la activa. */
data class GalleryPage(val emotion: Emotion, val stage: Stage, val isStage: Boolean, val detail: Int)

val galleryPages = listOf(
  GalleryPage(Emotion.DIM, Stage.FLAME, false, R.string.rule_dim),
  GalleryPage(Emotion.RESTLESS, Stage.FLAME, false, R.string.rule_restless),
  GalleryPage(Emotion.CALM, Stage.FLAME, false, R.string.rule_calm),
  GalleryPage(Emotion.HAPPY, Stage.FLAME, false, R.string.rule_happy),
  GalleryPage(Emotion.ECSTATIC, Stage.FLAME, false, R.string.rule_ecstatic),
  GalleryPage(Emotion.HAPPY, Stage.SPARK, true, R.string.levels_spark),
  GalleryPage(Emotion.HAPPY, Stage.FLAME, true, R.string.levels_flame),
  GalleryPage(Emotion.HAPPY, Stage.BONFIRE, true, R.string.levels_bonfire),
  GalleryPage(Emotion.HAPPY, Stage.LEGEND, true, R.string.levels_legend),
)

/** Pantalla de prueba de la fase 1: desliza para ver cada emoción y etapa; toca a Brasa para que reaccione. */
@Composable
fun GalleryScreen() {
  val state = rememberPagerState { galleryPages.size }
  Box(Modifier.fillMaxSize()) {
    HorizontalPager(state, Modifier.fillMaxSize()) { i ->
      GalleryContent(galleryPages[i])
    }
    PageIndicator(total = galleryPages.size, current = state.currentPage)
  }
}

@Composable
fun GalleryContent(page: GalleryPage, fixedTime: Float? = null) {
  val colors = BrasaTheme.colors
  val accent = Tokens.palette(page.emotion, colors.dark).accent
  val haptics = LocalHapticFeedback.current
  var taps by remember { mutableIntStateOf(0) }

  EmotionBackground(page.emotion) {
    EdgeTitle(stringResource(if (page.isStage) R.string.gallery_evolution else R.string.gallery_emotions))
    Column(
      Modifier.fillMaxSize().padding(horizontal = Tokens.Spacing.xl),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(Modifier.weight(0.24f))
      Brasa(
        emotion = page.emotion,
        stage = page.stage,
        reaction = taps,
        fixedTime = fixedTime,
        modifier = Modifier
          .fillMaxWidth(0.86f)
          .weight(1f)
          .clickable(remember { MutableInteractionSource() }, indication = null) {
            taps++
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          },
      )
      Text(
        stringResource(if (page.isStage) stageName(page.stage) else emotionName(page.emotion)),
        style = BrasaType.title, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
      )
      Text(
        stringResource(page.detail),
        style = BrasaType.data, color = accent, maxLines = 2, overflow = TextOverflow.Ellipsis,
        // Más margen abajo: el círculo se estrecha y el texto debe quedar dentro.
        modifier = Modifier.padding(horizontal = Tokens.Spacing.l),
      )
      Spacer(Modifier.height(Tokens.Spacing.xl))
    }
  }
}

/** Indicador de página en arco, abrazando el borde inferior. */
@Composable
fun PageIndicator(total: Int, current: Int) {
  val colors = BrasaTheme.colors
  val description = androidx.compose.ui.res.stringResource(com.brasa.app.R.string.page_of, current + 1, total)
  Canvas(Modifier.fillMaxSize().semantics { contentDescription = description; traversalIndex = 2f }) {
    val radius = size.minDimension / 2f - 7.dp.toPx()
    val stride = 5.5f
    val start = 90f + stride * (total - 1) / 2f
    for (i in 0 until total) {
      val angle = Math.toRadians((start - i * stride).toDouble())
      val c = Offset(center.x + (cos(angle) * radius).toFloat(), center.y + (sin(angle) * radius).toFloat())
      val active = i == current
      drawCircle(
        if (active) colors.fire else colors.secondaryText.copy(alpha = 0.4f),
        radius = (if (active) 3.2f else 2.2f).dp.toPx(),
        center = c,
      )
    }
  }
}
