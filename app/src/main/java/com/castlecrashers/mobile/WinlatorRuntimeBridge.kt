package com.castlecrashers.mobile

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.winlator.XServerDisplayActivity
import com.winlator.container.Container
import com.winlator.container.ContainerManager
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
    private const val ROOTFS_TIMEOUT_MS = 10 * 60 * 1000L
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
            RootFSInstaller.installIfNeeded(activity)
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
                if (rootFS.isValid() && rootFS.version >= RootFSInstaller.LATEST_VERSION.toInt()) {
                    onStatus("Runtime base instalado. Preparando prefixo Wine…")
                    getOrCreateContainer(activity, gameFolder, gameExe, onStatus)
                } else if (SystemClock.elapsedRealtime() - startedAt >= ROOTFS_TIMEOUT_MS) {
                    onStatus("Tempo esgotado ao preparar o runtime. Verifique o espaço livre e tente novamente.")
                } else {
                    onStatus("Instalando arquivos do runtime Windows…")
                    mainHandler.postDelayed(this, 1000)
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
                onStatus("Não foi possível criar o prefixo Wine.")
            } else {
                prepareAndLaunch(activity, manager, container, gameFolder, gameExe, onStatus)
            }
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
            else -> "854x480"
        }
        container.setScreenSize(resolution)
        container.setAudioDriver(AudioDrivers.PULSEAUDIO)
        container.setAudioDriverConfig("volume=" + (prefs.getInt("volume", 80) / 100.0))
        container.setGraphicsDriver(GraphicsDrivers.getDefaultDriver(activity))
        container.setDXWrapper(if (prefs.getInt("renderer", 0) == 1) DXWrappers.WINED3D else DXWrappers.DXVK)
        container.saveData()
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
