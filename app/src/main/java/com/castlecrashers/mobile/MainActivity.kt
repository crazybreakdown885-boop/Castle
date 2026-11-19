package com.castlecrashers.mobile


import androidx.appcompat.app.AppCompatActivity
import com.winlator.R
import android.app.AlertDialog
import android.content.Intent
import android.hardware.input.InputManager
import android.net.Uri
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.documentfile.provider.DocumentFile
import java.io.File

class MainActivity : AppCompatActivity(), InputManager.InputDeviceListener {
    // Castle Crashers PC-style landscape launcher.
    private lateinit var status: TextView
    private lateinit var devices: TextView
    private lateinit var input: InputManager
    private lateinit var nsd: NsdManager
    private lateinit var prefs: android.content.SharedPreferences
    private var gameFolder: String? = null
    private val foundRooms = linkedSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xff10131d.toInt()
        window.navigationBarColor = 0xff10131d.toInt()
        prefs = getSharedPreferences("castle_v3", MODE_PRIVATE)
        input = getSystemService(INPUT_SERVICE) as InputManager
        nsd = getSystemService(NSD_SERVICE) as NsdManager
        gameFolder = prefs.getString("game_folder", null)
        input.registerInputDeviceListener(this, null)
        home()
        refreshControllers()
    }

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun text(s: String, size: Float = 14f, color: Int = 0xffeee9df.toInt()) =
        TextView(this).apply { text = s; textSize = size; setTextColor(color) }

    private var launchInProgress = false

    private fun home() {
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )
        val page = FrameLayout(this).apply { setBackgroundColor(0xff17121b.toInt()) }
        val artwork = ImageView(this).apply {
            setImageResource(R.drawable.castle_header)
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "Arte de Castle Crashers"
        }
        page.addView(artwork, FrameLayout.LayoutParams(-1, -1))
        val shade = View(this).apply {
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0xf20c0b12.toInt(), 0xe6101018.toInt(), 0x18101018)
            )
        }
        page.addView(shade, FrameLayout.LayoutParams(-1, -1))

        val panelWidth = (resources.displayMetrics.widthPixels * 0.42f).toInt().coerceAtLeast(dp(300))
        val menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(24), dp(16), dp(24), dp(16))
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0xe91a1720.toInt(), 0xc91a1720.toInt(), 0x001a1720)
            )
        }
        page.addView(menu, FrameLayout.LayoutParams(panelWidth, -1, android.view.Gravity.START))

        menu.addView(text("CASTLE CRASHERS", 25f, 0xffffe6a6.toInt()).apply {
            typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD_ITALIC)
            letterSpacing = -0.02f
            setShadowLayer(dp(3).toFloat(), dp(2).toFloat(), dp(3).toFloat(), 0xff120e16.toInt())
        })
        menu.addView(text("THE BEHEMOTH  •  ANDROID EDITION", 9f, 0xffd9c39b.toInt()).apply {
            letterSpacing = 0.12f
        })
        menu.addView(gap(20))
        menu.addView(menuButton("LOCAL GAME", 0xffd2a65e.toInt()) { startGame() })
        menu.addView(menuButton("XBOX LIVE", 0xff8c4540.toInt()) {
            AlertDialog.Builder(this).setTitle("XBOX LIVE")
                .setMessage("O multiplayer online do jogo original depende dos serviços da plataforma. Nesta versão, a conexão online ainda não está implementada.")
                .setPositiveButton("OK", null).show()
        })
        menu.addView(menuButton("HELP & OPTIONS", 0xffa18b69.toInt()) { settings() })
        menu.addView(menuButton("LEADERBOARDS", 0xff756b59.toInt()) {
            AlertDialog.Builder(this).setTitle("LEADERBOARDS")
                .setMessage("Os placares online do jogo original ainda não estão disponíveis nesta versão.")
                .setPositiveButton("OK", null).show()
        })
        menu.addView(menuButton("CREDITS", 0xff75604b.toInt()) {
            AlertDialog.Builder(this).setTitle("CASTLE CRASHERS")
                .setMessage("Castle Crashers é uma criação da The Behemoth.\n\nEste launcher Android integra o runtime Winlator para executar uma cópia legítima do jogo para Windows.")
                .setPositiveButton("OK", null).show()
        })
        menu.addView(gap(12))
        menu.addView(text("A  SELECT                         B  BACK", 9f, 0xffdfc99f.toInt()).apply {
            gravity = android.view.Gravity.CENTER
            letterSpacing = 0.08f
        })
        devices = text("CONTROLES: procurando dispositivos…", 9f, 0xffe6d7bd.toInt()).apply {
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, 0)
            maxLines = 2
        }
        status = text("SELECIONE LOCAL GAME", 10f, 0xffffd98b.toInt()).apply {
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, 0)
            maxLines = 2
        }
        menu.addView(status)
        menu.addView(devices)
        setContentView(page)
    }

    private fun menuButton(label: String, accent: Int, action: () -> Unit): View {
        val button = TextView(this).apply {
            text = label; textSize = 14f; gravity = android.view.Gravity.CENTER_VERTICAL
            typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD)
            setTextColor(0xffffefd0.toInt()); setPadding(dp(17), 0, dp(12), 0)
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xff51402f.toInt(), 0xff342b29.toInt(), 0xff211c22.toInt())
            ).apply { cornerRadius = dp(2).toFloat(); setStroke(dp(2), accent) }
            isClickable = true; isFocusable = true; setOnClickListener { action() }
        }
        button.layoutParams = LinearLayout.LayoutParams(-1, dp(40)).apply { bottomMargin = dp(5) }
        return button
    }

    private fun menuFooterButton(label: String, color: Int, action: () -> Unit): View =
        TextView(this).apply {
            text = label; textSize = 10f; gravity = android.view.Gravity.CENTER
            typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD)
            setTextColor(0xffe6d4b4.toInt())
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(color); cornerRadius = dp(3).toFloat(); setStroke(dp(1), 0xff8c704c.toInt())
            }
            isClickable = true; isFocusable = true; setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply { marginStart = dp(4); marginEnd = dp(4) }
        }

    private fun gap(n: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(n)) }

    private fun chooseFolder() {
        AlertDialog.Builder(this)
            .setTitle("Importar Castle Crashers")
            .setItems(arrayOf("Selecionar pasta extraída", "Selecionar arquivo ZIP")) { _, which ->
                if (which == 0) chooseGameFolder() else chooseGameZip()
            }
            .show()
    }

    private fun chooseGameFolder() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        startActivityForResult(i, 3001)
    }

    private fun chooseGameZip() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(i, 3002)
    }

    @Deprecated("SAF")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == 3001 || requestCode == 3002) && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            val flags = data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
            runCatching { contentResolver.takePersistableUriPermission(uri, flags) }
            if (requestCode == 3001) {
                gameFolder = uri.toString()
                prefs.edit().putString("game_folder", gameFolder).apply()
                importGame(uri)
            } else {
                importGameZip(uri)
            }
        }
    }

    private fun importGameZip(uri: Uri) {
        status.text = "Extraindo e validando o ZIP do jogo…"
        Thread {
            try {
                val destination = File(filesDir, "games/castlecrashers")
                val result = GameFolderInstaller.installZip(this, uri, destination) { message ->
                    runOnUiThread { status.text = message }
                }
                prefs.edit().putString("game_install_path", result.root.absolutePath).apply()
                runOnUiThread {
                    val pe = PeExecutableInspector.inspect(result.exe)
                    status.text = "ZIP importado: ${result.filesCopied} arquivos • PE ${pe.architecture}/${pe.bits}-bit."
                    AlertDialog.Builder(this).setTitle("Importação concluída")
                        .setMessage("Castle.exe foi encontrado em:\\n" + result.exe.absolutePath +
                            "\\n\\nArquitetura: " + pe.architecture + " / " + pe.bits + "-bit" +
                            "\\nArquivos: " + result.filesCopied +
                            "\\nTamanho: " + (result.bytesCopied / (1024 * 1024)) + " MB")
                        .setPositiveButton("OK", null).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Falha ao importar ZIP: " + (e.message ?: "erro desconhecido")
                    AlertDialog.Builder(this).setTitle("Falha na importação do ZIP")
                        .setMessage(e.message ?: "Não foi possível extrair o arquivo.")
                        .setPositiveButton("OK", null).show()
                }
            }
        }.start()
    }

    private fun importGame(uri: Uri) {
        status.text = "Preparando importação completa do jogo…"
        Thread {
            try {
                val destination = File(filesDir, "games/castlecrashers")
                val result = GameFolderInstaller.install(this, uri, destination) { message ->
                    runOnUiThread { status.text = message }
                }
                prefs.edit().putString("game_install_path", result.root.absolutePath).apply()
                runOnUiThread {
                    val pe = PeExecutableInspector.inspect(result.exe)
                    status.text = "Jogo importado: " + result.filesCopied + " arquivos • PE " + pe.architecture + "/" + pe.bits + "-bit."
                    AlertDialog.Builder(this).setTitle("Importação concluída")
                        .setMessage("Castle.exe foi encontrado em:\n" + result.exe.absolutePath +
                            "\n\nArquitetura: " + pe.architecture + " / " + pe.bits + "-bit" +
                            "\nArquivos: " + result.filesCopied +
                            "\nTamanho: " + (result.bytesCopied / (1024 * 1024)) + " MB")
                        .setPositiveButton("OK", null).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Falha ao importar: " + (e.message ?: "erro desconhecido")
                    AlertDialog.Builder(this).setTitle("Falha na importação")
                        .setMessage(e.message ?: "Não foi possível copiar a pasta.")
                        .setPositiveButton("OK", null).show()
                }
            }
        }.start()
    }

    private fun inspectGame() {
        val installed = prefs.getString("game_install_path", null)?.let { File(it) }
        val installedExe = installed?.walkTopDown()?.firstOrNull { it.isFile && it.name.equals("castle.exe", true) }
        if (installedExe != null) {
            val pe = PeExecutableInspector.inspect(installedExe)
            status.text = "Cópia validada • PE " + pe.architecture + "/" + pe.bits + "-bit • runtime integrado pronto para iniciar."
            return
        }
        val uri = gameFolder ?: run { status.text = "Selecione a pasta do jogo primeiro."; return }
        val folder = DocumentFile.fromTreeUri(this, Uri.parse(uri))
        val exe = folder?.let { findExeInTree(it) }
        status.text = when {
            folder == null -> "Não foi possível acessar a pasta selecionada."
            exe != null -> "Castle.exe encontrado na origem. Importe a pasta completa para o armazenamento do app."
            else -> "Castle.exe não encontrado nesta pasta ou subpastas. Selecione a pasta correta do jogo."
        }
    }

    private fun findExeInTree(folder: DocumentFile): DocumentFile? {
        for (child in folder.listFiles()) {
            if (child.isFile && child.name.equals("castle.exe", true)) return child
            if (child.isDirectory) {
                val nested = findExeInTree(child)
                if (nested != null) return nested
            }
        }
        return null
    }

    private fun diagnoseGame() {
        val installed = prefs.getString("game_install_path", null)?.let { File(it) }
        val exe = installed?.walkTopDown()?.firstOrNull { it.isFile && it.name.equals("castle.exe", true) }
        if (exe == null) {
            status.text = "Importe a pasta completa do jogo antes do diagnóstico."
            return
        }
        try {
            val pe = PeExecutableInspector.inspect(exe)
            val abi = android.os.Build.SUPPORTED_ABIS.joinToString(", ")
            val rootFS = com.winlator.xenvironment.RootFS.find(this)
            val rootfsStatus = if (rootFS.isValid() && rootFS.version >= com.winlator.xenvironment.RootFSInstaller.LATEST_VERSION.toInt())
                "runtime extraído e instalado"
            else "runtime embutido no APK; será extraído no primeiro início"
            val report = "Executável: ${exe.name}\nFormato: PE\nArquitetura: ${pe.architecture} / ${pe.bits}-bit\n" +
                "Máquina PE: 0x${pe.machine.toString(16)}\nAndroid ABI: $abi\n" +
                "Runtime: $rootfsStatus\n\n" +
                if (pe.machine == 0x014C && pe.bits == 32)
                    "Alvo confirmado: Windows x86 de 32 bits. O APK inclui Wine WoW64, Box64 e renderizadores D3D9."
                else "Este executável não corresponde ao Castle Crashers x86 esperado."
            AlertDialog.Builder(this).setTitle("Diagnóstico técnico").setMessage(report)
                .setPositiveButton("OK", null).show()
            status.text = "Diagnóstico concluído • PE ${pe.architecture}/${pe.bits}-bit."
        } catch (e: Exception) {
            status.text = "Falha no diagnóstico: ${e.message ?: "arquivo inválido"}"
        }
    }

    private fun startGame() {
        if (launchInProgress) return
        val path = prefs.getString("game_install_path", null)
        if (path.isNullOrBlank()) {
            status.text = "Importe a pasta completa do jogo primeiro."
            chooseFolder()
            return
        }
        launchInProgress = true
        status.text = "Verificando arquivos do jogo em segundo plano…"
        Thread {
            try {
                val installed = File(path)
                val exe = installed.walkTopDown().firstOrNull { it.isFile && it.name.equals("castle.exe", true) }
                if (exe == null) throw IllegalStateException("Castle.exe não foi encontrado na pasta importada.")
                val pe = PeExecutableInspector.inspect(exe)
                if (pe.machine != 0x014C || pe.bits != 32) {
                    throw IllegalStateException("O Castle.exe precisa ser Windows x86 de 32 bits.")
                }
                runOnUiThread {
                    status.text = "Preparando Wine/Box64… mantenha o app aberto."
                    WinlatorRuntimeBridge.launch(this, installed, exe) { message ->
                        runOnUiThread {
                            status.text = message
                            if (message.startsWith("Falha") || message.startsWith("Tempo esgotado") ||
                                message.startsWith("Não foi possível")) launchInProgress = false
                        }
                    }
                }
            } catch (error: Exception) {
                runOnUiThread {
                    launchInProgress = false
                    status.text = "Não foi possível iniciar: ${error.message ?: "erro desconhecido"}"
                    AlertDialog.Builder(this).setTitle("Falha ao iniciar o jogo")
                        .setMessage(error.message ?: "Verifique a pasta importada e tente novamente.")
                        .setPositiveButton("OK", null).show()
                }
            }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        launchInProgress = false
    }

    private fun refreshControllers() {
        val deviceIds: IntArray = InputDevice.getDeviceIds()
        val allDevices: List<InputDevice> = deviceIds.map { deviceId: Int ->
            InputDevice.getDevice(deviceId)
        }.filterNotNull()
        val pads: List<InputDevice> = allDevices.filter { device: InputDevice ->
            (device.sources and (InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK)) != 0
        }
        devices.text = if (pads.isEmpty()) "Nenhum controle detectado. Conecte USB/Bluetooth."
        else pads.mapIndexed { index: Int, device: InputDevice ->
            "Jogador " + (index + 1) + ": " + device.name + " (ID " + device.id + ")"
        }.joinToString("\n")
    }
    override fun onInputDeviceAdded(deviceId: Int) = refreshControllers()
    override fun onInputDeviceRemoved(deviceId: Int) = refreshControllers()
    override fun onInputDeviceChanged(deviceId: Int) = refreshControllers()

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val device = InputDevice.getDevice(event.deviceId)
        if (device != null && device.sources and (InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK) != 0) {
            if (event.action == KeyEvent.ACTION_DOWN) status.text = device.name + " • " + KeyEvent.keyCodeToString(event.keyCode)
        } else if (event.source and InputDevice.SOURCE_KEYBOARD != 0 && event.action == KeyEvent.ACTION_DOWN) {
            status.text = "Teclado: " + KeyEvent.keyCodeToString(event.keyCode)
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.isFromSource(InputDevice.SOURCE_JOYSTICK) || event.isFromSource(InputDevice.SOURCE_GAMEPAD)) {
            if (event.action == MotionEvent.ACTION_MOVE) {
                val d = InputDevice.getDevice(event.deviceId)
                status.text = (d?.name ?: "Controle") + " • X " + event.getAxisValue(MotionEvent.AXIS_X) + " • Y " + event.getAxisValue(MotionEvent.AXIS_Y)
                return true
            }
        }
        if (event.isFromSource(InputDevice.SOURCE_MOUSE)) status.text = "Mouse externo detectado."
        return super.dispatchGenericMotionEvent(event)
    }

    private fun settings() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            setBackgroundColor(0xff211d27.toInt())
        }
        panel.addView(text("CASTLE CRASHERS  •  OPTIONS", 17f, 0xffffd98b.toInt()).apply {
            typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD)
            letterSpacing = 0.04f
        })
        panel.addView(text("Ajuste o equipamento e o desempenho do seu cavaleiro.", 11f, 0xffd6c7ae.toInt()))
        panel.addView(gap(8))
        panel.addView(text("Resolução do perfil do jogo", 14f, 0xffffd98b.toInt()))
        val values = arrayOf("Automática (854 × 480)", "1280 × 720", "960 × 540", "854 × 480", "640 × 360")
        val resolution = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, values)
            setSelection(prefs.getInt("resolution", 0))
        }
        panel.addView(resolution)
        panel.addView(text("Renderizador Direct3D 9", 14f, 0xffffd98b.toInt()))
        val renderers = arrayOf("Automático (ajustado ao aparelho)", "DXVK (Vulkan • desempenho)", "WineD3D (OpenGL • compatibilidade)")
        val renderer = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, renderers)
            setSelection(prefs.getInt("renderer", 0))
        }
        panel.addView(renderer)
        panel.addView(text("Perfil Box64", 14f, 0xffffd98b.toInt()))
        val box64Presets = arrayOf("Automático (por aparelho)", "Performance", "Intermediate", "Stability")
        val box64Preset = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, box64Presets)
            setSelection(prefs.getInt("box64_preset", 0))
        }
        panel.addView(box64Preset)
        panel.addView(text("Driver gráfico", 14f, 0xffffd98b.toInt()))
        val graphicsDrivers = arrayOf(
            "Automático (recomendado)",
            "Turnip + Gladio",
            "Vortek + Gladio",
            "Turnip + Zink",
            "Vortek + VirGL",
            "Vortek + Zink (compatibilidade)"
        )
        val graphicsDriver = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, graphicsDrivers)
            setSelection(prefs.getInt("graphics_driver", 0))
        }
        panel.addView(graphicsDriver)
        panel.addView(text("Motor de áudio", 14f, 0xffffd98b.toInt()))
        val audioDrivers = arrayOf("PulseAudio (recomendado)", "ALSA (alternativo)")
        val audioDriver = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, audioDrivers)
            setSelection(prefs.getInt("audio_driver", 0))
        }
        panel.addView(audioDriver)
        panel.addView(text("Volume do jogo", 14f, 0xffffd98b.toInt()))
        val volume = SeekBar(this).apply {
            max = 100
            progress = prefs.getInt("volume", 80)
            progressTintList = android.content.res.ColorStateList.valueOf(0xffc79b56.toInt())
            thumbTintList = android.content.res.ColorStateList.valueOf(0xffffd98b.toInt())
        }
        panel.addView(volume)
        val touch = CheckBox(this).apply {
            text = "Mostrar controle touch Xbox"
            isChecked = prefs.getBoolean("touch", true)
            setTextColor(0xffeee2cc.toInt())
            buttonTintList = android.content.res.ColorStateList.valueOf(0xffc79b56.toInt())
        }
        panel.addView(touch)
        val performanceHud = CheckBox(this).apply {
            text = "Exibir FPS, CPU e RAM durante o jogo"
            isChecked = prefs.getBoolean("performance_hud", true)
            setTextColor(0xffeee2cc.toInt())
            buttonTintList = android.content.res.ColorStateList.valueOf(0xffc79b56.toInt())
        }
        panel.addView(performanceHud)
        panel.addView(text("USB/Bluetooth: detecção e atribuição automática de até 4 controles pelo runtime.", 12f, 0xffc8bba5.toInt()))
        val scroll = ScrollView(this).apply {
            setBackgroundColor(0xff211d27.toInt())
            addView(panel)
        }
        val dialog = AlertDialog.Builder(this).setTitle("HELP & OPTIONS").setView(scroll)
            .setPositiveButton("SALVAR") { _, _ ->
                prefs.edit().putInt("resolution", resolution.selectedItemPosition).putInt("renderer", renderer.selectedItemPosition)
                    .putInt("graphics_driver", graphicsDriver.selectedItemPosition).putInt("volume", volume.progress)
                    .putInt("box64_preset", box64Preset.selectedItemPosition)
                    .putInt("audio_driver", audioDriver.selectedItemPosition)
                    .putBoolean("touch", touch.isChecked).putBoolean("performance_hud", performanceHud.isChecked).apply()
                status.text = "Configurações salvas e aplicadas na próxima inicialização do jogo."
            }.setNegativeButton("VOLTAR", null).create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(android.graphics.drawable.GradientDrawable().apply {
                setColor(0xff211d27.toInt())
                cornerRadius = dp(8).toFloat()
                setStroke(dp(2), 0xffa9814b.toInt())
            })
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(0xffffd98b.toInt())
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(0xffd5c4a4.toInt())
        }
        dialog.show()
    }

    private fun touchInfo() {
        AlertDialog.Builder(this)
            .setTitle("Controles touch Xbox")
            .setMessage(
                "O perfil virtual é carregado dentro da sessão Winlator ao iniciar o jogo. " +
                "Ele usa o perfil de controles 3 incluído no runtime, em vez de um desenho de demonstração que não envia comandos ao jogo.\n\n" +
                "Controles externos USB/OTG e Bluetooth, teclado e mouse são encaminhados pelo Winlator. " +
                "Você também pode abrir o editor de controles do runtime durante a sessão."
            )
            .setPositiveButton("Entendi", null)
            .show()
    }

    private fun advertiseRoom() {
        val info = NsdServiceInfo().apply {
            serviceName = "Castle-" + android.os.Build.MODEL
            serviceType = "_castlecoop._tcp."
            port = 27888
        }
        nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(s: NsdServiceInfo) { runOnUiThread { status.text = "Anúncio local ativo: " + s.serviceName + ". É apenas descoberta, sem sincronização do jogo." } }
            override fun onRegistrationFailed(s: NsdServiceInfo, e: Int) { runOnUiThread { status.text = "Falha ao anunciar sala: " + e } }
            override fun onServiceUnregistered(s: NsdServiceInfo) {}
            override fun onUnregistrationFailed(s: NsdServiceInfo, e: Int) {}
        })
    }

    private fun discoverRooms() {
        foundRooms.clear()
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(type: String) { runOnUiThread { status.text = "Procurando salas na rede…" } }
            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType == "_castlecoop._tcp.") nsd.resolveService(service, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(s: NsdServiceInfo, e: Int) {}
                    override fun onServiceResolved(s: NsdServiceInfo) {
                        foundRooms.add(s.serviceName + " • " + s.host.hostAddress + ":" + s.port)
                        runOnUiThread { status.text = "Anúncios encontrados (não são partidas conectadas):\n" + foundRooms.joinToString("\n") }
                    }
                })
            }
            override fun onServiceLost(s: NsdServiceInfo) {}
            override fun onDiscoveryStopped(type: String) {}
            override fun onStartDiscoveryFailed(type: String, e: Int) { nsd.stopServiceDiscovery(this) }
            override fun onStopDiscoveryFailed(type: String, e: Int) { nsd.stopServiceDiscovery(this) }
        }
        nsd.discoverServices("_castlecoop._tcp.", NsdManager.PROTOCOL_DNS_SD, listener)
    }

    override fun onDestroy() {
        input.unregisterInputDeviceListener(this)
        super.onDestroy()
    }
}
