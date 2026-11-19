package com.castlecrashers.mobile

import androidx.activity.ComponentActivity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Gravity
import android.view.ViewGroup
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile

class MainActivity : ComponentActivity() {
    private lateinit var folderLabel: TextView
    private lateinit var inputLabel: TextView
    private var gameFolder: Uri? = null

    private val folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            gameFolder = uri
            val root = DocumentFile.fromTreeUri(this, uri)
            val exe = root?.listFiles()?.firstOrNull {
                it.name?.contains("castle", ignoreCase = true) == true &&
                    it.name?.endsWith(".exe", ignoreCase = true) == true
            }
            folderLabel.text = if (exe != null) "Executável encontrado: ${exe.name}"
                else "Pasta selecionada. O executável não foi localizado na raiz."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(18, 15, 24)
        window.navigationBarColor = Color.rgb(18, 15, 24)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = 5894 or 1024 or 512
        buildUi()
    }

    private fun buildUi() {
        val bg = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 18, 28, 18)
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(20, 17, 30), Color.rgb(49, 25, 37), Color.rgb(19, 25, 35))
            )
        }
        val title = TextView(this).apply {
            text = "CASTLE CRASHERS\\nMOBILE"
            textSize = 30f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        val sub = TextView(this).apply {
            text = "LAUNCHER ANDROID • MODO LOCAL"
            textSize = 11f
            setTextColor(Color.rgb(235, 190, 112))
            gravity = Gravity.CENTER
        }
        val play = button("JOGAR", Color.rgb(196, 68, 48)).apply {
            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "O runtime Windows (Wine/Box64) ainda não está integrado. Este app é um launcher protótipo.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        val choose = button("LOCALIZAR PASTA DO JOGO", Color.rgb(63, 83, 112)).apply {
            setOnClickListener { folderPicker.launch(null) }
        }
        folderLabel = TextView(this).apply {
            text = "Nenhuma pasta selecionada"
            textSize = 13f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }
        inputLabel = TextView(this).apply {
            text = "CONTROLES: aguardando teclado ou gamepad"
            textSize = 12f
            setTextColor(Color.rgb(180, 210, 190))
            gravity = Gravity.CENTER
        }
        val note = TextView(this).apply {
            text = "Steam online e multiplayer ainda não estão disponíveis nesta versão."
            textSize = 11f
            setTextColor(Color.rgb(190, 180, 190))
            gravity = Gravity.CENTER
        }
        bg.addView(title, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 12))
        bg.addView(sub, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 6))
        bg.addView(play, lp(280, 58, 18))
        bg.addView(choose, lp(280, 48, 8))
        bg.addView(folderLabel, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 8))
        bg.addView(inputLabel, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 12))
        bg.addView(note, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 8))
        setContentView(bg)
    }

    private fun button(label: String, color: Int) = Button(this).apply {
        text = label
        textSize = 14f
        setTextColor(Color.WHITE)
        isAllCaps = false
        background = GradientDrawable().apply {
            setColor(color)
            cornerRadius = 18f
        }
    }

    private fun lp(width: Int, height: Int, top: Int) =
        LinearLayout.LayoutParams(width, height).apply { topMargin = top }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.device?.isVirtual != true) {
            inputLabel.text = "ENTRADA: ${KeyEvent.keyCodeToString(event.keyCode)}"
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.isFromSource(InputDevice.SOURCE_JOYSTICK) ||
            event.isFromSource(InputDevice.SOURCE_GAMEPAD)) {
            inputLabel.text = "GAMEPAD DETECTADO: ${event.device?.name ?: "controle"}"
        }
        return super.onGenericMotionEvent(event)
    }
}
