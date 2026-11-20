package com.castlecrashers.mobile

import androidx.appcompat.app.AppCompatActivity
import android.app.ActivityManager
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.winlator.XServerDisplayActivity
import com.winlator.container.Container
import com.winlator.container.ContainerManager
import com.winlator.box64.Box64Preset
import com.winlator.container.AudioDrivers
import com.winlator.container.DXWrappers
import com.winlator.container.GraphicsDrivers
import com.winlator.xenvironment.RootFS
import com.winlator.xenvironment.RootFSInstaller
import org.json.JSONObject
import java.io.File

/**
 * Single-app bridge into the embedded Winlator engine.
 * The game is copied into the container C: drive and launched inside Winlator's X server.
 */
object WinlatorRuntimeBridge {
    private const val CONTAINER_NAME = "Castle Crashers"
    private const val ROOTFS_TIMEOUT_MS = 15 * 60 * 1000L
    private val mainHandler = Handler(Looper.getMainLooper())

    fun launch(
        activity: AppCompatActivity,
        gameFolder: File,
        gameExe: File,
        onStatus: (String) -> Unit
    ) {
        if (!gameFolder.isDirectory || !gameExe.isFile) {
            onStatus("A pasta importada ou Castle.exe não está disponível.")
            return
        }
        if (!android.os.Build.SUPPORTED_ABIS.contains("arm64-v8a")) {
            onStatus("Este runtime integrado requer Android ARM64 (arm64-v8a).")
            return
        }

        val rootFS = RootFS.find(activity)
        onStatus("Preparando o runtime Windows integrado…")
        try {
            if (!rootFS.isValid() || rootFS.version < RootFSInstaller.LATEST_VERSION.toInt()) {
                RootFSInstaller.install(activity)
            }
        } catch (error: Exception) {
            onStatus("Falha ao iniciar a instalação do runtime: ${error.message}")
            return
        }
        waitForRootFS(activity, rootFS, gameFolder, gameExe, onStatus)
    }

    private fun waitForRootFS(
        activity: AppCompatActivity,
        rootFS: RootFS,
        gameFolder: File,
        gameExe: File,
        onStatus: (String) -> Unit
    ) {
        val startedAt = SystemClock.elapsedRealtime()
        val check = object : Runnable {
            override fun run() {
                if (activity.isFinishing || activity.isDestroyed) return
                // The installer creates the version marker before writing its contents.
                // getVersion() can briefly throw while that marker is empty; don't crash the launcher.
                val rootReady = runCatching {
                    rootFS.isValid() && rootFS.version >= RootFSInstaller.LATEST_VERSION.toInt()
                }.getOrDefault(false)
                val stateFile = File(activity.filesDir, ".castle_rootfs_install_state")
                val installState = runCatching { stateFile.readText().trim() }.getOrDefault("")
                val elapsed = SystemClock.elapsedRealtime() - startedAt
                if (rootReady) {
                    onStatus("Runtime base instalado. Preparando prefixo Wine…")
                    getOrCreateContainer(activity, gameFolder, gameExe, onStatus)
                } else if (installState == "failed") {
                    onStatus("Falha ao extrair o runtime Windows. Libere espaço no armazenamento e tente novamente.")
                } else if (elapsed >= ROOTFS_TIMEOUT_MS) {
                    onStatus("Tempo esgotado após ${elapsed / 60000} min instalando o runtime. A extração não terminou; reinicie o app e tente novamente.")
                } else {
                    val minutes = elapsed / 60000
                    onStatus(if (minutes > 0) "Instalando runtime Windows… ${minutes} min" else "Instalando runtime Windows… aguarde a primeira extração.")
                    mainHandler.postDelayed(this, 5000)
                }
            }
        }
        mainHandler.postDelayed(check, 500)
    }

    private fun getOrCreateContainer(
        activity: AppCompatActivity,
        gameFolder: File,
        gameExe: File,
        onStatus: (String) -> Unit
    ) {
        try {
            val manager = ContainerManager(activity)
            val existing = manager.containers.firstOrNull { it.name == CONTAINER_NAME }
            if (existing != null) {
                prepareAndLaunch(activity, manager, existing, gameFolder, gameExe, onStatus)
                return
            }
            onStatus("Criando prefixo Wine para Castle Crashers…")
            val data = JSONObject().put("name", CONTAINER_NAME)
            manager.createContainerAsync(data) { container ->
                if (container == null) {
                    onStatus("Não foi possível criar o prefixo Wine. Verifique o espaço livre e tente novamente.")
                } else {
                    prepareAndLaunch(activity, manager, container, gameFolder, gameExe, onStatus)
                }
            }
        } catch (error: Exception) {
            onStatus("Falha ao preparar o contêiner Wine: ${error.message ?: "erro desconhecido"}")
        }
    }

    private fun prepareAndLaunch(
        activity: AppCompatActivity,
        manager: ContainerManager,
        container: Container,
        gameFolder: File,
        gameExe: File,
        onStatus: (String) -> Unit
    ) {
        onStatus("Copiando os arquivos do jogo para o disco C: do Wine…")
        Thread {
            try {
                val driveC = File(container.rootDir, ".wine/drive_c")
                if (!driveC.isDirectory && !driveC.mkdirs()) {
                    throw IllegalStateException("Não foi possível preparar o disco C: do Wine.")
                }
                val requiredBytes = gameFolder.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                if (driveC.usableSpace < requiredBytes + 256L * 1024L * 1024L) {
                    throw IllegalStateException("Espaço insuficiente para copiar o jogo e preparar o Wine. Libere armazenamento e tente novamente.")
                }
                val targetDir = File(driveC, "CastleCrashers")
                val relativeExe = gameExe.relativeTo(gameFolder)
                val targetExe = File(targetDir, relativeExe.path)
                val stamp = File(targetDir, ".castle_import_stamp")
                val sourceStamp = "${gameFolder.lastModified()}:${gameExe.lastModified()}:${gameExe.length()}"

                if (!targetExe.isFile || !stamp.isFile || stamp.readText() != sourceStamp) {
                    if (targetDir.exists() && !targetDir.deleteRecursively()) {
                        throw IllegalStateException("Não foi possível substituir a cópia do jogo no Wine.")
                    }
                    copyTree(gameFolder, targetDir)
                    if (!targetExe.isFile) throw IllegalStateException("Castle.exe não foi copiado para o prefixo Wine.")
                    stamp.writeText(sourceStamp)
                }

                activity.runOnUiThread {
                    try {
                        applyContainerSettings(activity, container)
                        manager.activateContainer(container)
                        onStatus("Abrindo Castle Crashers no runtime integrado…")
                        val intent = Intent(activity, XServerDisplayActivity::class.java).apply {
                            putExtra("container_id", container.id)
                            putExtra("exec_path", targetExe.absolutePath)
                            putExtra("castle_touch_controls", activity.getSharedPreferences("castle_v3", android.content.Context.MODE_PRIVATE).getBoolean("touch", true))
                        }
                        activity.startActivity(intent)
                    } catch (error: Exception) {
                        onStatus("Falha ao abrir a sessão Wine: ${error.message}")
                    }
                }
            } catch (error: Exception) {
                activity.runOnUiThread {
                    onStatus("Falha ao preparar os arquivos no Wine: ${error.message}")
                }
            }
        }.start()
    }

    private fun applyContainerSettings(activity: AppCompatActivity, container: Container) {
        val prefs = activity.getSharedPreferences("castle_v3", android.content.Context.MODE_PRIVATE)
        val resolution = when (prefs.getInt("resolution", 0)) {
            1 -> "1280x720"
            2 -> "960x540"
            3 -> "854x480"
            4 -> "640x360"
            else -> if (memoryForProfile(activity) <= 3L * 1024L * 1024L * 1024L) "640x360" else "854x480"
        }
        container.setScreenSize(resolution)
        container.setAudioDriver(if (prefs.getInt("audio_driver", 0) == 1) AudioDrivers.ALSA else AudioDrivers.PULSEAUDIO)
        container.setAudioDriverConfig("volume=" + (prefs.getInt("volume", 80) / 100.0))
        val graphicsDriver = when (prefs.getInt("graphics_driver", 0)) {
            1 -> GraphicsDrivers.TURNIP + "," + GraphicsDrivers.GLADIO
            2 -> GraphicsDrivers.VORTEK + "," + GraphicsDrivers.GLADIO
            3 -> GraphicsDrivers.TURNIP + "," + GraphicsDrivers.ZINK
            4 -> GraphicsDrivers.VORTEK + "," + GraphicsDrivers.VIRGL
            5 -> GraphicsDrivers.VORTEK + "," + GraphicsDrivers.ZINK
            else -> if (memoryForProfile(activity) <= 3L * 1024L * 1024L * 1024L)
                GraphicsDrivers.VORTEK + "," + GraphicsDrivers.VIRGL
            else GraphicsDrivers.getDefaultDriver(activity)
        }
        container.setGraphicsDriver(graphicsDriver)
        val dxWrapper = when (prefs.getInt("renderer", 0)) {
            1 -> DXWrappers.DXVK
            2 -> DXWrappers.WINED3D
            else -> {
                val memory = ActivityManager.MemoryInfo()
                (activity.getSystemService(android.content.Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memory)
                if (memory.totalMem <= 3L * 1024L * 1024L * 1024L) DXWrappers.WINED3D else DXWrappers.DXVK
            }
        }
        container.setDXWrapper(dxWrapper)
        val box64Preset = when (prefs.getInt("box64_preset", 0)) {
            1 -> Box64Preset.PERFORMANCE
            2 -> Box64Preset.INTERMEDIATE
            3 -> Box64Preset.STABILITY
            else -> if (memoryForProfile(activity) <= 3L * 1024L * 1024L * 1024L)
                Box64Preset.STABILITY else Box64Preset.PERFORMANCE
        }
        container.setBox64Preset(box64Preset)
        container.setHUDMode(
            if (prefs.getBoolean("performance_hud", false))
                com.winlator.widget.FrameRating.Mode.FULL.ordinal.toByte()
            else com.winlator.widget.FrameRating.Mode.DISABLED.ordinal.toByte()
        )
        container.saveData()
    }

    private fun memoryForProfile(activity: AppCompatActivity): Long {
        val memory = ActivityManager.MemoryInfo()
        (activity.getSystemService(android.content.Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memory)
        return memory.totalMem
    }

    private fun copyTree(source: File, destination: File) {
        if (source.isDirectory) {
            if (!destination.exists() && !destination.mkdirs()) {
                throw IllegalStateException("Não foi possível criar ${destination.absolutePath}")
            }
            val children = source.listFiles()
                ?: throw IllegalStateException("Não foi possível listar ${source.absolutePath}")
            children.forEach { child -> copyTree(child, File(destination, child.name)) }
        } else {
            destination.parentFile?.let {
                if (!it.exists() && !it.mkdirs()) throw IllegalStateException("Não foi possível criar ${it.absolutePath}")
            }
            source.copyTo(destination, overwrite = true)
        }
    }
}
