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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ScannedPage

enum class ExportFormat {
    COMBINED_PDF,
    SEPARATE_PNG
}

@Composable
fun ExportDialog(
    allPages: List<ScannedPage>,
    onDismiss: () -> Unit,
    onStartExport: (format: ExportFormat, selectedPages: List<ScannedPage>) -> Unit
) {
    var format by remember { mutableStateOf(ExportFormat.COMBINED_PDF) }
    val selectedPageIds = remember {
        mutableStateListOf<String>().apply {
            addAll(allPages.map { it.id })
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Save or Share Pages",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("File Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { format = ExportFormat.COMBINED_PDF }
                            .padding(vertical = 4.dp)
                            .testTag("export_format_pdf")
                    ) {
                        RadioButton(
                            selected = format == ExportFormat.COMBINED_PDF,
                            onClick = { format = ExportFormat.COMBINED_PDF }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("One PDF File", fontWeight = FontWeight.Medium)
                            Text(
                                "Combines all chosen pages into a single PDF document",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { format = ExportFormat.SEPARATE_PNG }
                            .padding(vertical = 4.dp)
                            .testTag("export_format_png")
                    ) {
                        RadioButton(
                            selected = format == ExportFormat.SEPARATE_PNG,
                            onClick = { format = ExportFormat.SEPARATE_PNG }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Separate Picture Files (PNG)", fontWeight = FontWeight.Medium)
                            Text(
                                if (selectedPageIds.size > 1) "Saves all ${selectedPageIds.size} pages as separate picture files" else "Saves page as a picture file",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    "Choose Pages (${selectedPageIds.size} of ${allPages.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(8.dp)) {
                        items(allPages, key = { it.id }) { page ->
                            val isChecked = selectedPageIds.contains(page.id)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) selectedPageIds.remove(page.id)
                                        else selectedPageIds.add(page.id)
                                    }
                                    .padding(vertical = 2.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedPageIds.add(page.id)
                                        else selectedPageIds.remove(page.id)
                                    }
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Page ${page.pageNumber}")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedPageIds.isNotEmpty(),
                onClick = {
                    val pages = allPages.filter { selectedPageIds.contains(it.id) }
                    onStartExport(format, pages)
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_export_button")
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
