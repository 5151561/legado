package io.legado.app.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import androidx.compose.ui.unit.dp
import android.view.View
import android.view.ViewGroup

data class MainNavigationItem(
    val menuId: Int,
    val fragmentId: Int,
    @param:DrawableRes val selectedIconRes: Int,
    @param:DrawableRes val unselectedIconRes: Int,
    @param:StringRes val labelRes: Int
)

@Composable
fun MainActivityScreen(
    items: List<MainNavigationItem>,
    selectedMenuId: Int,
    bookshelfBadgeCount: Int,
    fragmentContainerId: Int,
    onContainerReady: (View) -> Unit,
    onItemClick: (Int) -> Unit
) {
    val containerBackground = MaterialTheme.colorScheme.background.toArgb()
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface
        )
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                Surface(
                    modifier = Modifier.clip(
                        RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    ),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp
                ) {
                    NavigationBar(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        tonalElevation = 0.dp
                    ) {
                        items.forEach { item ->
                            val selected = selectedMenuId == item.menuId
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onItemClick(item.menuId) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                icon = {
                                    val icon = @Composable {
                                        Icon(
                                            painter = painterResource(
                                                if (selected) item.selectedIconRes else item.unselectedIconRes
                                            ),
                                            contentDescription = null
                                        )
                                    }
                                    if (item.menuId == io.legado.app.R.id.menu_bookshelf && bookshelfBadgeCount > 0) {
                                        BadgedBox(
                                            badge = { Badge { Text(bookshelfBadgeCount.toString()) } }
                                        ) {
                                            icon()
                                        }
                                    } else {
                                        icon()
                                    }
                                },
                                label = { Text(text = stringResource(item.labelRes)) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        FragmentContainerView(context).apply {
                            id = fragmentContainerId
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(containerBackground)
                            post { onContainerReady(this) }
                        }
                    },
                    update = {
                        it.setBackgroundColor(containerBackground)
                        onContainerReady(it)
                    }
                )
            }
        }
    }
}
