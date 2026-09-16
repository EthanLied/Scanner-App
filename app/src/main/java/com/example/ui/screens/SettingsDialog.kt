package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ScanColorMode
import com.example.data.model.ScanDpi
import com.example.data.model.ScanPageSize
import com.example.data.model.ScanSettings

@Composable
fun SettingsDialog(
    currentSettings: ScanSettings,
    onDismiss: () -> Unit,
    onSave: (ScanSettings) -> Unit
) {
    var selectedDpi by remember { mutableStateOf(currentSettings.dpi) }
    var selectedColor by remember { mutableStateOf(currentSettings.colorMode) }
    var selectedSize by remember { mutableStateOf(currentSettings.pageSize) }

    val previewSettings = remember(selectedDpi, selectedColor, selectedSize) {
        ScanSettings(dpi = selectedDpi, colorMode = selectedColor, pageSize = selectedSize)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Hardware Scan Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // DPI
                Text("Resolution (DPI)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ScanDpi.values().forEach { dpi ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { selectedDpi = dpi }
                                .padding(4.dp)
                                .testTag("dpi_option_${dpi.value}")
                        ) {
                            RadioButton(
                                selected = selectedDpi == dpi,
                                onClick = { selectedDpi = dpi }
                            )
                            Text("${dpi.value}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Color Mode
                Text("Color Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ScanColorMode.values().forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedColor = mode }
                                .padding(vertical = 2.dp)
                                .testTag("color_option_${mode.name}")
                        ) {
                            RadioButton(
                                selected = selectedColor == mode,
                                onClick = { selectedColor = mode }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(mode.label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Page Size
                Text("Page Size", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ScanPageSize.values().forEach { size ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSize = size }
                                .padding(vertical = 2.dp)
                                .testTag("size_option_${size.name}")
                        ) {
                            RadioButton(
                                selected = selectedSize == size,
                                onClick = { selectedSize = size }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(size.label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Calculated Info Card
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Scan Dimensions: ${previewSettings.widthPx} × ${previewSettings.heightPx} px",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Byte 0x21: JPEG (0x82) | Format: ${previewSettings.colorMode.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(previewSettings)
                    onDismiss()
                },
                modifier = Modifier.testTag("save_settings_button")
            ) {
                Text("Save Settings", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
