package org.vicpoo.gasolineproject.ui.common.components

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * Scroll infinito: cuando el usuario llega a los últimos elementos de la lista se llama [onLoadMore]
 * (la pantalla pide los 10 siguientes). Se vuelve a llamar cada vez que la lista crece y sigue
 * viéndose el final, así no se queda atorado si llegaron pocos elementos.
 */
@Composable
fun LoadMoreOnScrollEnd(
    gridState: LazyGridState,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    /** Cuántos elementos antes del final se empieza a pedir. */
    threshold: Int = 2,
) {
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    LaunchedEffect(gridState, hasMore) {
        if (!hasMore) return@LaunchedEffect
        snapshotFlow {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            val atEnd = info.totalItemsCount > 0 && last >= info.totalItemsCount - 1 - threshold
            atEnd to info.totalItemsCount
        }
            .distinctUntilChanged()
            .filter { (atEnd, _) -> atEnd }
            .collect { currentOnLoadMore() }
    }
}
