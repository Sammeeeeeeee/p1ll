package io.github.sammeeeeeeee.p1ll.ui.remind

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.sammeeeeeeee.p1ll.ui.theme.P1ll
import kotlinx.coroutines.launch

/**
 * The sheet a reminder's action opens. [content] gets a close function that slides the sheet away;
 * [onDismiss] runs once it's gone, however it was closed, and only once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSheet(onDismiss: () -> Unit, content: @Composable (close: () -> Unit) -> Unit) {
    val c = P1ll.colors
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val done = remember { BooleanArray(1) }
    fun dismiss() {
        if (!done[0]) {
            done[0] = true
            onDismiss()
        }
    }
    ModalBottomSheet(
        onDismissRequest = ::dismiss,
        sheetState = sheet,
        containerColor = c.surface,
        contentColor = c.ink,
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 6.dp).size(36.dp, 4.dp).clip(CircleShape).background(c.line))
        },
    ) {
        content { scope.launch { sheet.hide() }.invokeOnCompletion { dismiss() } }
    }
}
