package io.legado.app.ui.compose.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LegadoDetailSummaryScaffold(
    title: String,
    subtitle: String,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
    emptyTitle: String? = null,
    emptySummary: String? = null,
    isEmpty: Boolean = false,
    content: LazyListScope.() -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            LegadoPageHeader(
                title = title,
                subtitle = subtitle,
                onBackClick = onBackClick,
                actions = { actions() }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = LegadoPageDefaults.HorizontalPadding,
                end = LegadoPageDefaults.HorizontalPadding,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(LegadoPageDefaults.SectionSpacing)
        ) {
            if (isEmpty && !emptyTitle.isNullOrBlank() && !emptySummary.isNullOrBlank()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LegadoEmptyState(
                            title = emptyTitle,
                            summary = emptySummary
                        )
                    }
                }
            } else {
                content()
            }
        }
    }
}

@Composable
fun LegadoSectionGroup(
    title: String? = null,
    contentPadding: PaddingValues = PaddingValues(vertical = 4.dp),
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!title.isNullOrBlank()) {
            LegadoSectionLabel(text = title)
        }
        LegadoSectionCard(contentPadding = contentPadding) {
            content()
        }
    }
}
