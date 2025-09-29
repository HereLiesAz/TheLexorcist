package com.hereliesaz.lexorcist.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.geometry.Rect

internal class DragAndDropState<T> {
    var isDragging: Boolean by mutableStateOf(false)
    var dragPosition by mutableStateOf(Offset.Zero)
    var dragOffset by mutableStateOf(Offset.Zero)
    var draggedComposable by mutableStateOf<(@Composable () -> Unit)?>(null)
    var dataToDrop by mutableStateOf<T?>(null)

    private val dropTargets = mutableMapOf<Any, Pair<Rect, (T) -> Unit>>()

    fun registerDropTarget(key: Any, bounds: Rect, onDrop: (T) -> Unit) {
        dropTargets[key] = Pair(bounds, onDrop)
    }

    fun unregisterDropTarget(key: Any) {
        dropTargets.remove(key)
    }

    fun onDragEnd() {
        val currentPosition = dragPosition + dragOffset
        dataToDrop?.let { data ->
            dropTargets.values.find { (bounds, _) ->
                bounds.contains(currentPosition)
            }?.let { (_, onDrop) ->
                onDrop(data)
            }
        }

        isDragging = false
        dragOffset = Offset.Zero
    }
}

internal val LocalDragAndDropState = compositionLocalOf<DragAndDropState<*>> { error("No DragAndDropState provided.") }

@Composable
fun <T> DragAndDropContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val state = remember { DragAndDropState<T>() }

    CompositionLocalProvider(LocalDragAndDropState provides state) {
        Box(modifier = modifier.fillMaxSize()) {
            content()
            if (state.isDragging) {
                var targetSize by remember { mutableStateOf(IntSize.Zero) }
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            val offset = (state.dragPosition + state.dragOffset)
                            alpha = if (targetSize == IntSize.Zero) 0f else .9f
                            translationX = offset.x - targetSize.width / 2
                            translationY = offset.y - targetSize.height / 2
                        }
                        .onGloballyPositioned {
                            targetSize = it.size
                        }
                ) {
                    state.draggedComposable?.invoke()
                }
            }
        }
    }
}

@Composable
fun <T> DraggableItem(
    modifier: Modifier = Modifier,
    dataToDrop: T,
    content: @Composable () -> Unit
) {
    @Suppress("UNCHECKED_CAST")
    val state = LocalDragAndDropState.current as DragAndDropState<T>
    var currentPosition by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .onGloballyPositioned {
                currentPosition = it.positionInWindow()
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        state.isDragging = true
                        state.dragPosition = currentPosition + offset
                        state.dataToDrop = dataToDrop
                        state.draggedComposable = content
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        state.dragOffset += dragAmount
                    },
                    onDragEnd = {
                        state.onDragEnd()
                    },
                    onDragCancel = {
                        state.dragOffset = Offset.Zero
                        state.isDragging = false
                    }
                )
            }
    ) {
        content()
    }
}


@Composable
fun <T> DropTarget(
    modifier: Modifier = Modifier,
    key: Any,
    onDropped: (T) -> Unit,
    content: @Composable BoxScope.(isHovered: Boolean) -> Unit
) {
    @Suppress("UNCHECKED_CAST")
    val state = LocalDragAndDropState.current as DragAndDropState<T>
    var isHovered by remember { mutableStateOf(false) }
    var bounds by remember { mutableStateOf<Rect?>(null) }

    DisposableEffect(key) {
        onDispose {
            state.unregisterDropTarget(key)
        }
    }

    Box(
        modifier = modifier.onGloballyPositioned {
            bounds = it.boundsInWindow()
            state.registerDropTarget(key, it.boundsInWindow(), onDropped)
        }
    ) {
        isHovered = if (state.isDragging) {
            bounds?.contains(state.dragPosition + state.dragOffset) ?: false
        } else {
            false
        }
        content(isHovered)
    }
}