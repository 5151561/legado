package io.legado.app.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import androidx.compose.ui.unit.dp

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
    onContainerReady: () -> Unit,
    onItemClick: (Int) -> Unit
) {
    val containerBackground = MaterialTheme.colorScheme.background.toArgb()
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                items.forEach { item ->
                    val selected = selectedMenuId == item.menuId
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onItemClick(item.menuId) },
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
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    FragmentContainerView(context).apply {
                        id = fragmentContainerId
                        setBackgroundColor(containerBackground)
                        post(onContainerReady)
                    }
                },
                update = {
                    it.setBackgroundColor(containerBackground)
                }
            )
        }
    }
}
