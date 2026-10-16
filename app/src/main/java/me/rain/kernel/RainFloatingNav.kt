package me.rain.kernel

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarMode
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import java.util.function.IntConsumer

class RainFloatingNav @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    private var selectedPage by mutableIntStateOf(0)
    private var onNavigate: IntConsumer? = null

    init {
        val composeView = ComposeView(context)
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        composeView.setContent {
            val controller = remember { ThemeController(ColorSchemeMode.System) }
            val pages = remember {
                listOf(
                    NavigationItem(context.getString(R.string.tab_overview), outline("overview", "M3,10.5 L12,3 L21,10.5 L21,20 L14.5,20 L14.5,14 L9.5,14 L9.5,20 L3,20 Z")),
                    NavigationItem(context.getString(R.string.tab_drivers), outline("drivers", "M4,6 L12,2 L20,6 L12,10 Z M4,12 L12,16 L20,12 M4,18 L12,22 L20,18")),
                    NavigationItem(context.getString(R.string.tab_settings), outline("settings", "M12,3 A9,9 0,1 1,12 21 A9,9 0,1 1,12 3 M12,9 A3,3 0,1 1,12 15 A3,3 0,1 1,12 9")),
                )
            }
            val bounce = remember { Animatable(1f) }
            var ready by remember { mutableStateOf(false) }
            LaunchedEffect(selectedPage) {
                if (ready) {
                    bounce.animateTo(0.984f, tween(durationMillis = 90))
                    bounce.animateTo(1f, spring(dampingRatio = 0.64f, stiffness = 320f))
                } else ready = true
            }
            MiuixTheme(controller = controller) {
                FloatingNavigationBar(
                    modifier = Modifier.graphicsLayer {
                        scaleX = bounce.value
                        scaleY = bounce.value
                    },
                    items = pages,
                    selected = selectedPage,
                    onClick = { index -> if (index != selectedPage) onNavigate?.accept(index) },
                    mode = FloatingNavigationBarMode.IconOnly,
                )
            }
        }
        addView(composeView, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    fun setOnNavigate(listener: IntConsumer?) {
        onNavigate = listener
    }

    fun select(index: Int) {
        if (index in 0..2) selectedPage = index
    }

    private fun outline(name: String, data: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = PathParser().parsePathString(data).toNodes(),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }.build()
}
