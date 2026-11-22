package com.benclawbot.cmfflow.backup

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.benclawbot.cmfflow.FlowApplication
import com.benclawbot.cmfflow.ui.FlowTheme
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Separate activity so backup/decryption never happens automatically on app
 * launch. Android's Storage Access Framework provides a user-selected URI.
 */
class BackupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = LocalBackupStore((application as FlowApplication).database)
        setContent {
            FlowTheme {
                BackupScreen(store, applicationContext, onClose = ::finish)
            }
        }
    }
}

private fun readBounded(input: InputStream): ByteArray {
    val output = ByteArrayOutputStream()
    val chunk = ByteArray(8192)
    while (true) {
        val n = input.read(chunk)
        if (n < 0) break
        require(output.size() + n <= EncryptedBackupCodec.MAX_ARCHIVE_BYTES) {
            "Selected backup exceeds the supported size"
        }
        output.write(chunk, 0, n)
    }
    return output.toByteArray()
}

@Composable
private fun BackupScreen(store: LocalBackupStore, context: Context, onClose: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("No data leaves the device unless you choose an export destination.") }
    var exportBytes by remember { mutableStateOf<ByteArray?>(null) }
    var stagedRestore by remember { mutableStateOf<BackupPreview?>(null) }
    val scope = rememberCoroutineScope()

    val savePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val bytes = exportBytes
        exportBytes = null
        if (uri != null && bytes != null) {
            scope.launch {
                busy = true
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                            stream.write(bytes)
                            stream.flush()
                        } ?: error("Cannot open selected export location")
                    }
                    message = "Encrypted backup saved to your selected location."
                } catch (_: Exception) {
                    message = "Backup write failed. Delete any incomplete file in the selected location."
                } finally {
                    bytes.fill(0)
                    busy = false
                }
            }
        } else {
            bytes?.fill(0)
        }
    }
    val openPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                val pass = password.toCharArray()
                password = ""
                confirmation = ""
                try {
                    val preview = withContext(Dispatchers.IO) {
                        val archive = context.contentResolver.openInputStream(uri)?.use(::readBounded)
                            ?: error("Cannot open selected backup")
                        try {
                            store.previewEncryptedArchive(archive, pass)
                        } finally {
                            archive.fill(0)
                        }
                    }
                    stagedRestore = preview
                    message = "Backup decrypted and validated. Review counts before replacing local data."
                } catch (_: Exception) {
                    stagedRestore = null
                    message = "Unable to open backup: wrong passphrase, corruption, or incompatible schema."
                } finally {
                    pass.fill('\u0000')
                    busy = false
                }
            }
        }
    }

    stagedRestore?.let { preview ->
        AlertDialog(
            onDismissRequest = { stagedRestore = null },
            title = { Text("Replace all local CMF Flow data?") },
            text = {
                Text(
                    "This permanently replaces current tasks, check-ins, health aggregates, " +
                        "sessions, recommendations and experiments. The restored backup contains " +
                        preview.recordCounts.values.sum() + " records. There is no undo. " +
                        "The existing database will be preserved if the import fails." +
                        "\n\nPreview by category:\n" +
                        preview.recordCounts.entries.joinToString("\n") { entry ->
                            entry.key.replace('_', ' ') + ": " + entry.value
                        }
                )
            },
            confirmButton = {
                Button(onClick = {
                    stagedRestore = null
                    scope.launch {
                        busy = true
                        try {
                            withContext(Dispatchers.IO) { store.restoreFromPreview(preview) }
                            message = "Local data restored successfully."
                        } catch (_: Exception) {
                            message = "Restore failed and was rolled back; current data was preserved."
                        } finally {
                            busy = false
                        }
                    }
                }) { Text("Replace local data") }
            },
            dismissButton = {
                TextButton(onClick = { stagedRestore = null }) { Text("Cancel") }
            },
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Encrypted backup", style = MaterialTheme.typography.headlineMedium)
        Text("Backups may include sensitive health summaries, tasks, experiments and personal check-ins. " +
            "Choose a private storage location. Do not share the backup or passphrase.")
        Text("A lost passphrase cannot be recovered. Use at least 12 characters; longer is better.")
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Backup passphrase") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = confirmation,
            onValueChange = { confirmation = it },
            label = { Text("Confirm passphrase for export") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            enabled = !busy && password.length >= 12 && password == confirmation,
            onClick = {
                scope.launch {
                    busy = true
                    val pass = password.toCharArray()
                    try {
                        val bytes = withContext(Dispatchers.IO) { store.createEncryptedArchive(pass) }
                        exportBytes = bytes
                        password = ""
                        confirmation = ""
                        savePicker.launch("cmf-flow-backup.cmfbak")
                    } catch (_: Exception) {
                        message = "Could not create backup. Check available storage and try again."
                    } finally {
                        pass.fill('\u0000')
                        busy = false
                    }
                }
            },
        ) { Text("Export encrypted backup") }
        OutlinedButton(
            enabled = !busy && password.length >= 12,
            onClick = { openPicker.launch(arrayOf("application/octet-stream", "*/*")) },
        ) { Text("Open encrypted backup and preview restore") }
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row { TextButton(onClick = onClose) { Text("Back to Settings") } }
    }
}
