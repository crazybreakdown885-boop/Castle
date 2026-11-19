package com.castlecrashers.mobile

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
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

class MainActivity : Activity(), InputManager.InputDeviceListener {
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
        val scroll = ScrollView(this)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            setBackgroundColor(0xff111522.toInt())
        }
        page.addView(text("CASTLE", 14f, 0xffe5b96f.toInt()))
        page.addView(text("MOBILE 0.3.0", 32f, 0xffffe7b5.toInt()))
        page.addView(text("Launcher Android • executável original • múltiplos controles", 13f, 0xffaab2c5.toInt()))
        page.addView(gap(16))
        val banner = text("⚔  CASTLE CRASHERS • WINDOWS GAME\nImporte a pasta do jogo para verificar o executável.", 16f, 0xffffe8c0.toInt())
        banner.setPadding(dp(16), dp(20), dp(16), dp(20)); banner.setBackgroundColor(0xff252c40.toInt())
        page.addView(banner)
        page.addView(gap(12))
        page.addView(button("SELECIONAR PASTA DO JOGO", "Importar a pasta por seletor Android") { chooseFolder() })
        page.addView(button("VERIFICAR ARQUIVOS", "Procurar Castle.exe e arquivos do jogo") { inspectGame() })
        page.addView(button("VERIFICAR EXECUÇÃO DO EXE", "Checar Castle.exe e disponibilidade do runtime") { startGame() })
        page.addView(gap(16))
        page.addView(text("CONTROLES", 19f, 0xffffd58a.toInt()))
        devices = text("Detectando controles USB e Bluetooth…")
        page.addView(devices)
        page.addView(button("ATUALIZAR DISPOSITIVOS", "Redetectar controles conectados") { refreshControllers() })
        page.addView(button("CONFIGURAÇÕES", "Resolução, áudio e controles virtuais") { settings() })
        page.addView(button("TESTAR CONTROLE TOUCH", "Painel virtual estilo Xbox 360") { touchTest() })
        page.addView(gap(16))
        page.addView(text("MULTIPLAYER LOCAL", 19f, 0xffffd58a.toInt()))
        page.addView(text("Descoberta de salas na mesma rede Wi-Fi.", 13f, 0xffaab2c5.toInt()))
        page.addView(button("PROCURAR SALAS", "Encontrar amigos na rede local") { discoverRooms() })
        page.addView(button("ANUNCIAR MINHA SALA", "Disponibilizar serviço local") { advertiseRoom() })
        status = text("0.3.0 • Runtime Windows ainda não incorporado.", 13f, 0xffffbb78.toInt())
        page.addView(gap(12)); page.addView(status)
        page.addView(gap(8))
        page.addView(text("IMPORTANTE: esta versão 0.3.0 é uma base de launcher/input. Ainda não executa o EXE: faltam os binários Wine/Box64 ARM64 e a ponte XInput testada. Steam multiplayer também não está implementado.", 12f, 0xffaab2c5.toInt()))
        scroll.addView(page); setContentView(scroll)
    }

    private fun gap(n: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(n)) }
    private fun button(title: String, sub: String, action: () -> Unit): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(0xff20283a.toInt())
            isClickable = true; isFocusable = true; setOnClickListener { action() }
        }
        box.addView(text(title, 15f, 0xffffe2ae.toInt()))
        box.addView(text(sub, 12f, 0xffaab2c5.toInt()))
        box.layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }
        return box
    }

    private fun chooseFolder() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        startActivityForResult(i, 3001)
    }

    @Deprecated("SAF")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 3001 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            val flags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            contentResolver.takePersistableUriPermission(uri, flags)
            gameFolder = uri.toString()
            prefs.edit().putString("game_folder", gameFolder).apply()
            inspectGame()
        }
    }

    private fun inspectGame() {
        val uri = gameFolder ?: run { status.text = "Selecione a pasta do jogo primeiro."; return }
        val folder = DocumentFile.fromTreeUri(this, Uri.parse(uri))
        val files = folder?.listFiles()?.filter { it.isFile && it.name?.endsWith(".exe", true) == true } ?: emptyList()
        val castle = files.firstOrNull { it.name.equals("Castle.exe", true) }
        status.text = when {
            folder == null -> "Não foi possível acessar a pasta selecionada."
            castle != null -> "Castle.exe encontrado. O runtime ainda precisa ser integrado."
            files.isNotEmpty() -> "EXEs encontrados: " + files.joinToString { it.name ?: "arquivo" } + ". Não achei Castle.exe na raiz."
            else -> "Nenhum EXE encontrado na raiz. Selecione a pasta correta do jogo."
        }
    }

    private fun startGame() {
        val uri = gameFolder ?: run { status.text = "Importe a pasta do jogo."; return }
        val folder = DocumentFile.fromTreeUri(this, Uri.parse(uri))
        val exe = folder?.listFiles()?.firstOrNull { it.isFile && it.name.equals("Castle.exe", true) }
        if (exe == null) { status.text = "Castle.exe não encontrado."; return }
        val runtime = File(filesDir, "runtime")
        val box64 = File(runtime, "bin/box64")
        val wine = File(runtime, "wine/bin/wine")
        if (!box64.exists() || !wine.exists()) {
            AlertDialog.Builder(this).setTitle("Runtime ausente")
                .setMessage("A pasta do jogo foi encontrada, mas esta compilação ainda não inclui Wine + Box64 para Android. Não é possível executar o EXE sem esses componentes.")
                .setPositiveButton("OK", null).show()
            status.text = "Jogo localizado; execução aguardando runtime integrado."
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
        val values = arrayOf("Automática", "1280 × 720", "960 × 540", "854 × 480", "640 × 360")
        val resolution = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, values)
            setSelection(prefs.getInt("resolution", 0))
        }
        panel.addView(resolution)
        panel.addView(text("Volume (perfil)", 14f))
        val volume = SeekBar(this).apply { max = 100; progress = prefs.getInt("volume", 80) }
        panel.addView(volume)
        val touch = CheckBox(this).apply { text = "Mostrar controle touch"; isChecked = prefs.getBoolean("touch", true) }
        panel.addView(touch)
        val auto = CheckBox(this).apply { text = "Mapear controles conectados automaticamente"; isChecked = prefs.getBoolean("auto_map", true) }
        panel.addView(auto)
        AlertDialog.Builder(this).setTitle("Configurações").setView(panel)
            .setPositiveButton("Salvar") { _, _ ->
                prefs.edit().putInt("resolution", resolution.selectedItemPosition).putInt("volume", volume.progress)
                    .putBoolean("touch", touch.isChecked).putBoolean("auto_map", auto.isChecked).apply()
                status.text = "Preferências salvas. Aplicação real depende do runtime."
            }.setNegativeButton("Cancelar", null).show()
    }

    private fun touchTest() {
        val pad = VirtualPad(this) { status.text = "Touch: " + it }
        AlertDialog.Builder(this).setTitle("Controle virtual Xbox").setView(pad).setPositiveButton("Fechar", null).show()
    }

    private fun advertiseRoom() {
        val info = NsdServiceInfo().apply {
            serviceName = "Castle-" + android.os.Build.MODEL
            serviceType = "_castlecoop._tcp."
            port = 27888
        }
        nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(s: NsdServiceInfo) { runOnUiThread { status.text = "Sala anunciada: " + s.serviceName + ". Ainda falta sincronizar a partida." } }
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
                        runOnUiThread { status.text = "Salas encontradas:\n" + foundRooms.joinToString("\n") }
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

private class VirtualPad(context: Context, val changed: (String) -> Unit) : View(context) {
    private val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c: android.graphics.Canvas) {
        c.drawColor(0xff151b2a.toInt())
        val w = width.toFloat(); val h = height.toFloat()
        p.color = 0xff40516d.toInt()
        c.drawCircle(w*.23f, h*.53f, h*.27f, p)
        p.color = 0xff7385a0.toInt(); c.drawCircle(w*.23f, h*.53f, h*.12f, p)
        val pts = arrayOf(.72f to .34f, .88f to .52f, .72f to .70f, .56f to .52f)
        val names = arrayOf("Y", "B", "A", "X")
        pts.forEachIndexed { i, pt ->
            p.color = intArrayOf(0xff4c9c69.toInt(),0xffbb5554.toInt(),0xff478db5.toInt(),0xffbd9a4c.toInt())[i]
            c.drawCircle(w*pt.first, h*pt.second, h*.075f, p)
            p.color = -1; p.textSize = h*.07f
            c.drawText(names[i], w*pt.first-p.measureText(names[i])/2, h*pt.second+h*.025f, p)
        }
        p.color = 0xffffe0a0.toInt(); p.textSize = h*.05f
        c.drawText("D-PAD", w*.17f, h*.9f, p); c.drawText("A  B  X  Y", w*.65f, h*.9f, p)
    }
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN || e.action == MotionEvent.ACTION_MOVE) {
            val x=e.x/width; val y=e.y/height
            val key = if (x < .45f) when { y < .4f -> "DPAD_UP"; y > .68f -> "DPAD_DOWN"; x < .18f -> "DPAD_LEFT"; else -> "DPAD_RIGHT" }
                else when { y < .42f -> "Y"; y > .65f -> "A"; x > .8f -> "B"; else -> "X" }
            changed(key)
        }
        return true
    }
}
