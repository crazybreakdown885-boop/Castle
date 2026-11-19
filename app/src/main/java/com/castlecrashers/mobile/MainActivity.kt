package com.castlecrashers.mobile

import androidx.appcompat.app.AppCompatActivity
import android.app.AlertDialog
import android.widget.ImageView
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

    private fun home() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(0xff0b101a.toInt())
            isFillViewport = true
        }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(28))
            setBackgroundColor(0xff0b101a.toInt())
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val brand = text("♜", 28f, 0xffffc45f.toInt())
        top.addView(brand, LinearLayout.LayoutParams(dp(42), dp(44)))
        val brandText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brandText.addView(text("CASTLE CRASHERS", 16f, 0xffffe8bd.toInt()))
        brandText.addView(text("MOBILE  •  VERSÃO 1.0.0", 10f, 0xff9ca9bd.toInt()))
        top.addView(brandText)
        page.addView(top)
        page.addView(gap(18))

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(22))
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xff34435f.toInt(), 0xff1b263a.toInt(), 0xff171c2b.toInt())
            ).apply {
                cornerRadius = dp(22).toFloat()
                setStroke(dp(1), 0xff65718a.toInt())
            }
        }
        val artwork = ImageView(this).apply {
            // Banner art will be supplied as a licensed fan/official image; do not generate replacement artwork.
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xff52617b.toInt(), 0xff25334b.toInt(), 0xff111827.toInt())
            ).apply { cornerRadius = dp(14).toFloat() }
            contentDescription = "Banner Castle Crashers"
        }
        hero.addView(artwork, LinearLayout.LayoutParams(-1, dp(188)))
        hero.addView(gap(12))
        hero.addView(text("A AVENTURA COMEÇA AQUI", 11f, 0xffffc96e.toInt()))
        hero.addView(gap(5))
        hero.addView(text("CASTLE\nCRASHERS", 34f, 0xfffff1d6.toInt()))
        hero.addView(gap(7))
        hero.addView(text("Seu jogo. Seu castelo. Sua batalha.", 13f, 0xffc1c9d8.toInt()))
        hero.addView(gap(14))
        val heroHint = text("Importe sua pasta original do jogo e jogue pelo runtime Windows integrado.", 12f, 0xffd6dbea.toInt())
        heroHint.setLineSpacing(dp(3).toFloat(), 1f)
        hero.addView(heroHint)
        page.addView(hero)
        page.addView(gap(16))

        page.addView(button("⚔   JOGAR", "Iniciar Castle Crashers no runtime integrado", primary = true) { startGame() })
        page.addView(button("＋   IMPORTAR JOGO", "Selecionar uma pasta extraída ou um arquivo ZIP do jogo") { chooseFolder() })
        page.addView(button("▣   VERIFICAR ARQUIVOS", "Conferir se a cópia do jogo está completa") { inspectGame() })
        page.addView(button("⌕   DIAGNÓSTICO", "Verificar arquitetura do executável e estado do runtime") { diagnoseGame() })

        page.addView(gap(14))
        page.addView(text("CONTROLES", 18f, 0xffffd28a.toInt()))
        page.addView(gap(7))
        val controllerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = rounded(0xff171f30.toInt(), 0xff303c52.toInt())
        }
        devices = text("Detectando controles USB e Bluetooth…", 13f, 0xffd4dbea.toInt())
        controllerCard.addView(devices)
        controllerCard.addView(gap(8))
        controllerCard.addView(button("ATUALIZAR DISPOSITIVOS", "Redetectar controles conectados") { refreshControllers() })
        page.addView(controllerCard)
        page.addView(gap(8))
        page.addView(button("CONFIGURAÇÕES", "Resolução, áudio, renderizador e controles virtuais") { settings() })
        page.addView(button("CONTROLES DURANTE O JOGO", "Ver como funciona o perfil touch Xbox integrado") { touchInfo() })

        page.addView(gap(14))
        page.addView(text("MULTIPLAYER LOCAL", 18f, 0xffffd28a.toInt()))
        page.addView(gap(5))
        page.addView(text("Descoberta de dispositivos na rede Wi-Fi. A sincronização de partidas ainda não está disponível.", 12f, 0xff9ca9bd.toInt()))
        page.addView(gap(7))
        page.addView(button("PROCURAR SALAS LAN", "Buscar anúncios locais na rede") { discoverRooms() })
        page.addView(button("ANUNCIAR SALA DE TESTE", "Anunciar este aparelho para descoberta") { advertiseRoom() })

        status = text("Runtime Wine/Box64 integrado. Importe o jogo para começar.", 12f, 0xffa8e6b0.toInt())
        status.setPadding(dp(14), dp(12), dp(14), dp(12))
        status.background = rounded(0xff14251f.toInt(), 0xff315b48.toInt())
        page.addView(gap(12))
        page.addView(status)
        page.addView(gap(10))
        page.addView(text("Na primeira execução, o app prepara o runtime Windows e cria o prefixo Wine. Reserve espaço livre. O APK não inclui os arquivos comerciais do jogo: importe uma cópia que você tenha direito de usar. LAN gameplay e Steam online ainda não estão implementados.", 11f, 0xff8e9bb0.toInt()))

        scroll.addView(page)
        setContentView(scroll)
    }

    private fun rounded(fill: Int, stroke: Int) =
        android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = dp(16).toFloat()
            setColor(fill)
            setStroke(dp(1), stroke)
        }

    private fun gap(n: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(n))
    }

    private fun button(title: String, sub: String, primary: Boolean = false, action: () -> Unit): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(13), dp(16), dp(13))
            background = if (primary) rounded(0xff8e343d.toInt(), 0xffffc46f.toInt())
                else rounded(0xff192337.toInt(), 0xff35445e.toInt())
            isClickable = true
            isFocusable = true
            elevation = dp(if (primary) 4 else 1).toFloat()
            setOnClickListener { action() }
        }
        box.addView(text(title, if (primary) 17f else 14f,
            if (primary) 0xfffff0d7.toInt() else 0xffffd995.toInt()))
        box.addView(text(sub, 11f, 0xffaebbd0.toInt()))
        box.layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(8)
        }
        return box
    }

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
            type = "application/zip"
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
        val installed = prefs.getString("game_install_path", null)?.let { File(it) }
        val exe = installed?.walkTopDown()?.firstOrNull { it.isFile && it.name.equals("castle.exe", true) }
        if (installed == null || exe == null) {
            status.text = "Importe a pasta completa do jogo antes de iniciar."
            return
        }
        val pe = try { PeExecutableInspector.inspect(exe) } catch (e: Exception) {
            status.text = "Executável inválido: ${e.message}"
            return
        }
        if (pe.machine != 0x014C || pe.bits != 32) {
            status.text = "O runtime está configurado para o Castle Crashers Windows x86/32-bit."
            return
        }
        status.text = "Iniciando o runtime Windows integrado…"
        WinlatorRuntimeBridge.launch(this, installed, exe) { message ->
            runOnUiThread { status.text = message }
        }
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
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(8)) }
        panel.addView(text("Resolução do perfil do jogo", 14f))
        val values = arrayOf("Automática (854 × 480)", "1280 × 720", "960 × 540", "854 × 480", "640 × 360")
        val resolution = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, values)
            setSelection(prefs.getInt("resolution", 0))
        }
        panel.addView(resolution)
        panel.addView(text("Renderizador Direct3D 9", 14f))
        val renderers = arrayOf("Automático (ajustado ao aparelho)", "DXVK (Vulkan • desempenho)", "WineD3D (OpenGL • compatibilidade)")
        val renderer = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, renderers)
            setSelection(prefs.getInt("renderer", 0))
        }
        panel.addView(renderer)
        panel.addView(text("Driver gráfico", 14f))
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
        panel.addView(text("Volume do jogo", 14f))
        val volume = SeekBar(this).apply { max = 100; progress = prefs.getInt("volume", 80) }
        panel.addView(volume)
        val touch = CheckBox(this).apply { text = "Mostrar controle touch Xbox"; isChecked = prefs.getBoolean("touch", true) }
        panel.addView(touch)
        val performanceHud = CheckBox(this).apply {
            text = "Exibir FPS, CPU e RAM durante o jogo"
            isChecked = prefs.getBoolean("performance_hud", true)
        }
        panel.addView(performanceHud)
        panel.addView(text("USB/Bluetooth: detecção e atribuição automática de até 4 controles pelo runtime.", 12f, 0xffaab2c5.toInt()))
        val scroll = ScrollView(this).apply { addView(panel) }
        AlertDialog.Builder(this).setTitle("Configurações").setView(scroll)
            .setPositiveButton("Salvar") { _, _ ->
                prefs.edit().putInt("resolution", resolution.selectedItemPosition).putInt("renderer", renderer.selectedItemPosition)
                    .putInt("graphics_driver", graphicsDriver.selectedItemPosition).putInt("volume", volume.progress)
                    .putBoolean("touch", touch.isChecked).putBoolean("performance_hud", performanceHud.isChecked).apply()
                status.text = "Configurações salvas e aplicadas na próxima inicialização do jogo."
            }.setNegativeButton("Cancelar", null).show()
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
