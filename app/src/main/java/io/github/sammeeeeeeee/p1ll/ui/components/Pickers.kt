package io.github.sammeeeeeeee.p1ll.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.sammeeeeeeee.p1ll.ui.theme.P1ll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDialog(initial: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val context = LocalContext.current
    val picker = rememberTimePickerState(initial / 60, initial % 60, DateFormat.is24HourFormat(context))
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(radius = 32.dp) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(picker)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    GhostButton("Cancel", onDismiss)
                    Spacer(Modifier.width(8.dp))
                    PrimaryButton("Set", { onPick(picker.hour * 60 + picker.minute) })
                }
            }
        }
    }
}

/** Dates are epoch days; [min] and [max] bound what can be picked. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDialog(initial: Long, onDismiss: () -> Unit, onPick: (Long) -> Unit, min: Long? = null, max: Long? = null) {
    val c = P1ll.colors
    val day = 86_400_000L
    val picker = rememberDatePickerState(
        initialSelectedDateMillis = initial * day,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val d = utcTimeMillis / day
                return (min == null || d >= min) && (max == null || d <= max)
            }
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            PrimaryButton("Set", { picker.selectedDateMillis?.let { onPick(it / day) } ?: onDismiss() }, Modifier.padding(end = 12.dp, bottom = 8.dp))
        },
        dismissButton = { GhostButton("Cancel", onDismiss, Modifier.padding(end = 8.dp, bottom = 8.dp)) },
        shape = RoundedCornerShape(32.dp),
        colors = DatePickerDefaults.colors(containerColor = c.surface),
    ) {
        DatePicker(picker, colors = DatePickerDefaults.colors(containerColor = c.surface), showModeToggle = false, title = null)
    }
}
