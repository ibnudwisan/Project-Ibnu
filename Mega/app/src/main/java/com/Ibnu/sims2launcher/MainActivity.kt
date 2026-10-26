package com.ibnu.sims2launcher

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val nativeBridge by lazy { NativeBridge() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nativeBridge.initialize()
        setContent { MaterialTheme { LauncherScreen() } }
    }

    @Composable
    private fun LauncherScreen() {
        val scope = rememberCoroutineScope()
        var status by remember { mutableStateOf("Pilih file ISO PS2 untuk mengimpor game.") }
        var progress by remember { mutableFloatStateOf(0f) }
        var copiedBytes by remember { mutableLongStateOf(0L) }
        var totalBytes by remember { mutableLongStateOf(-1L) }
        var importing by remember { mutableStateOf(false) }
        var importedFile by remember { mutableStateOf<File?>(null) }
        var showComplete by remember { mutableStateOf(false) }
        var showController by remember { mutableStateOf(false) }
        var nativeStatus by remember { mutableStateOf(nativeBridge.status()) }

        val picker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            if (uri != null) {
                scope.launch {
                    importing = true
                    progress = 0f
                    copiedBytes = 0L
                    totalBytes = querySize(uri)
                    status = "Mengimpor ISO…"
                    try {
                        val destination = withContext(Dispatchers.IO) {
                            copyIso(uri) { copied, total ->
                                copiedBytes = copied
                                totalBytes = total
                                progress = if (total > 0L) (copied.toDouble() / total).toFloat().coerceIn(0f, 1f) else 0f
                            }
                        }
                        importedFile = destination
                        status = "Impor selesai."
                        showComplete = true
                    } catch (e: Exception) {
                        status = "Impor gagal: ${e.message ?: "kesalahan file"}"
                    } finally {
                        importing = false
                    }
                }
            }
        }

        Surface(Modifier.fillMaxSize(), color = Color(0xFF101826)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("THE SIMS 2 · PS2", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Android launcher prototype", color = Color(0xFFB7C6DC))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2A40))) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(status, color = Color.White)
                        if (importing) {
                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                            Text("${(progress * 100).toInt()}% · ${formatBytes(copiedBytes)} / ${if (totalBytes >= 0) formatBytes(totalBytes) else "ukuran tidak diketahui"}", color = Color(0xFFB7C6DC))
                        }
                        Button(
                            onClick = { picker.launch(arrayOf("*/*")) },
                            enabled = !importing,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Pilih ISO dari penyimpanan") }
                        Text(
                            "File disalin ke penyimpanan privat aplikasi. Tidak memerlukan izin storage luas.",
                            color = Color(0xFFB7C6DC),
                            fontSize = 12.sp
                        )
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2A40))) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Native bridge", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(nativeStatus, color = Color(0xFFB7C6DC), fontSize = 12.sp)
                        Button(onClick = { nativeStatus = nativeBridge.status() }) { Text("Periksa status native") }
                    }
                }
                Button(
                    onClick = { showController = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = importedFile != null
                ) { Text("Buka layar controller") }
                Text(
                    "Catatan: controller ini adalah mock UI. Game belum dijalankan karena emulator PS2 belum ditautkan.",
                    color = Color(0xFFFFD18A),
                    fontSize = 12.sp
                )
            }
        }

        if (showComplete) {
            AlertDialog(
                onDismissRequest = { showComplete = false },
                title = { Text("Impor selesai") },
                text = { Text("The Sims 2 (PS2).iso is in place. The game starts in a moment.") },
                confirmButton = {
                    TextButton(onClick = {
                        showComplete = false
                        showController = true
                    }) { Text("OK") }
                }
            )
        }

        if (showController) {
            AlertDialog(
                onDismissRequest = { showController = false },
                title = { Text("PS2 Controller Overlay") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Overlay demonstrasi. Tombol belum mengirim input ke emulator.")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            ControllerButton("↑") { }
                            ControllerButton("△") { }
                            ControllerButton("○") { }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            ControllerButton("←") { }
                            ControllerButton("✕") { }
                            ControllerButton("□") { }
                            ControllerButton("→") { }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            ControllerButton("L1") { }
                            ControllerButton("L2") { }
                            ControllerButton("SELECT") { }
                            ControllerButton("START") { }
                            ControllerButton("R1") { }
                            ControllerButton("R2") { }
                        }
                        Text("Analog kiri: ◉       Analog kanan: ◉", fontSize = 13.sp)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showController = false }) { Text("Tutup") }
                }
            )
        }
    }

    @Composable
    private fun ControllerButton(label: String, onClick: () -> Unit) {
        FilledTonalButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp)
        ) { Text(label, fontSize = 12.sp) }
    }

    private suspend fun querySize(uri: Uri): Long = withContext(Dispatchers.IO) {
        contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0 && !cursor.isNull(index)) cursor.getLong(index) else -1L
            } else -1L
        } ?: -1L
    }

    private suspend fun copyIso(uri: Uri, onProgress: (Long, Long) -> Unit): File =
        withContext(Dispatchers.IO) {
            val directory = File(filesDir, "games").apply { if (!exists() && !mkdirs()) throw IOException("Tidak bisa membuat folder game") }
            val target = File(directory, "The Sims 2 (PS2).iso")
            val temp = File(directory, "The Sims 2 (PS2).iso.part")
            val total = querySize(uri)
            var copied = 0L
            try {
                val input = contentResolver.openInputStream(uri) ?: throw IOException("Tidak bisa membuka file terpilih")
                input.use { ins ->
                    FileOutputStream(temp).use { out ->
                        val buffer = ByteArray(1024 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = ins.read(buffer)
                            if (count < 0) break
                            out.write(buffer, 0, count)
                            copied += count
                            onProgress(copied, total)
                        }
                        out.fd.sync()
                    }
                }
                if (total >= 0 && copied != total) throw IOException("Ukuran file berubah atau salinan tidak lengkap")
                if (target.exists() && !target.delete()) throw IOException("Tidak bisa mengganti file ISO lama")
                if (!temp.renameTo(target)) throw IOException("Gagal menyelesaikan salinan ISO")
                target
            } catch (e: Exception) {
                temp.delete()
                throw e
            }
        }

    private fun formatBytes(value: Long): String {
        if (value < 0) return "?"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var n = value.toDouble()
        var i = 0
        while (n >= 1024 && i < units.lastIndex) { n /= 1024; i++ }
        return String.format(Locale.US, "%.1f %s", n, units[i])
    }
}
