package com.castlecrashers.mobile

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.MotionEvent
import android.view.KeyEvent
import android.content.Context
import android.graphics.*
import kotlin.math.*
import kotlin.random.Random

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(16, 19, 29)
        window.navigationBarColor = Color.rgb(16, 19, 29)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        setContentView(BrawlerGame(this))
    }
}

private data class Foe(var x: Float, var y: Float, var hp: Int = 3, var hitFlash: Float = 0f, var attackWait: Float = 0f)
private data class Spark(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, val color: Int)

private class BrawlerGame(context: Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val worldW = 960f
    private val worldH = 540f
    private var last = 0L
    private var elapsed = 0f
    private var heroX = 250f
    private var heroY = 390f
    private var heroHp = 100f
    private var facing = 1
    private var attack = 0f
    private var attackCooldown = 0f
    private var jump = 0f
    private var jumpV = 0f
    private var hurt = 0f
    private var score = 0
    private var wave = 1
    private var spawnClock = 1f
    private var gameOver = false
    private var moveX = 0f
    private var moveY = 0f
    private var attackHeld = false
    private var jumpHeld = false
    private var lastAttackTouch = 0L
    private val foes = mutableListOf<Foe>()
    private val sparks = mutableListOf<Spark>()
    private val keys = mutableSetOf<Int>()
    private val sky = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ui = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create("sans-serif-black", Typeface.BOLD) }

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        repeat(3) { spawnFoe(700f + it * 90f) }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val sx = width / worldW
        val sy = height / worldH
        canvas.save()
        canvas.scale(sx, sy)
        drawWorld(canvas)
        drawFoes(canvas)
        drawHero(canvas)
        drawSparks(canvas)
        drawHud(canvas)
        drawControls(canvas)
        if (gameOver) drawGameOver(canvas)
        canvas.restore()
        val now = System.nanoTime()
        if (last == 0L) last = now
        val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.04f)
        last = now
        update(dt)
        postInvalidateDelayed(16)
    }

    private fun color(c: Int) { p.color = c; p.style = Paint.Style.FILL; p.strokeWidth = 1f }
    private fun rect(c: Canvas, x: Float, y: Float, w: Float, h: Float, col: Int, r: Float = 0f) {
        color(col); if (r > 0) c.drawRoundRect(x, y, x+w, y+h, r, r, p) else c.drawRect(x,y,x+w,y+h,p)
    }
    private fun circle(c: Canvas, x: Float, y: Float, r: Float, col: Int) { color(col); c.drawCircle(x,y,r,p) }
    private fun line(c: Canvas, x: Float, y: Float, x2: Float, y2: Float, col: Int, sw: Float) {
        color(col); p.strokeWidth=sw; p.strokeCap=Paint.Cap.ROUND; c.drawLine(x,y,x2,y2,p)
    }
    private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, col: Int, align: Paint.Align = Paint.Align.LEFT) {
        ui.textSize=size; ui.color=col; ui.textAlign=align; c.drawText(s,x,y,ui)
    }

    private fun drawWorld(c: Canvas) {
        sky.shader = LinearGradient(0f,0f,0f,390f, intArrayOf(Color.rgb(28,37,63),Color.rgb(93,73,83),Color.rgb(225,143,91)), null, Shader.TileMode.CLAMP)
        c.drawRect(0f,0f,worldW,worldH,sky); sky.shader=null
        // distant mountains
        val path=Path().apply { moveTo(0f,330f); lineTo(100f,210f); lineTo(190f,310f); lineTo(310f,180f); lineTo(450f,315f); lineTo(590f,205f); lineTo(760f,320f); lineTo(875f,190f); lineTo(960f,300f); lineTo(960f,410f); lineTo(0f,410f); close() }
        color(Color.rgb(55,59,78)); c.drawPath(path,p)
        // castle silhouette
        rect(c,610f,160f,260f,190f,Color.rgb(37,42,59))
        rect(c,640f,105f,55f,245f,Color.rgb(43,47,64)); rect(c,780f,90f,62f,260f,Color.rgb(43,47,64))
        for (x in listOf(640f,660f,680f,780f,802f,824f)) rect(c,x,82f,13f,28f,Color.rgb(43,47,64))
        rect(c,700f,225f,80f,125f,Color.rgb(28,32,48))
        for (x in 0..8) rect(c,625f+x*30f,190f,9f,17f,Color.rgb(218,159,98),2f)
        // battlements / trees
        for (x in listOf(70f,145f,520f,920f)) {
            rect(c,x+14,300f,9f,65f,Color.rgb(65,48,42))
            circle(c,x+18,284f,31f,Color.rgb(34,75,65)); circle(c,x,300f,22f,Color.rgb(39,86,68))
        }
        rect(c,0f,365f,worldW,175f,Color.rgb(62,76,68))
        rect(c,0f,365f,worldW,8f,Color.rgb(119,119,83))
        // stone path
        for (i in 0..11) {
            val x=i*90f
            rect(c,x,410f,78f,3f,Color.rgb(81,91,78))
            rect(c,x+35f,450f,42f,2f,Color.rgb(81,91,78))
        }
        for (i in 0..32) {
            val x=(i*137f)%worldW; val y=380f+(i*29%145)
            line(c,x,y,x+5,y-2,Color.rgb(130,133,93),2f)
        }
    }

    private fun drawKnight(c: Canvas, x: Float, y: Float, scale: Float, facing: Int, enemy: Boolean, flash: Float, swinging: Boolean) {
        c.save(); c.translate(x,y); c.scale(if(facing<0) -scale else scale,scale)
        val armor=if(enemy) Color.rgb(112,65,63) else Color.rgb(54,123,183)
        val trim=if(enemy) Color.rgb(218,124,76) else Color.rgb(238,194,92)
        if(flash>0) { circle(c,0f,-44f,34f,Color.argb(100,255,255,255)) }
        // shadow
        color(Color.argb(65,0,0,0)); c.drawOval(-26f,-3f,27f,7f,p)
        // legs
        rect(c,-18f,-37f,13f,34f,Color.rgb(39,43,57),5f); rect(c,5f,-37f,13f,34f,Color.rgb(39,43,57),5f)
        rect(c,-20f,-8f,20f,9f,Color.rgb(105,62,48),4f); rect(c,3f,-8f,20f,9f,Color.rgb(105,62,48),4f)
        // body and belt
        rect(c,-23f,-75f,46f,43f,armor,10f); rect(c,-22f,-45f,44f,7f,trim,2f)
        rect(c,-7f,-74f,14f,34f,Color.argb(80,255,255,255),3f)
        // arms
        rect(c,-34f,-72f,13f,34f,armor,6f); rect(c,22f,-72f,13f,34f,armor,6f)
        circle(c,-27f,-39f,8f,Color.rgb(207,157,119))
        // helmet
        rect(c,-24f,-111f,48f,38f,armor,12f); rect(c,-27f,-93f,54f,9f,trim,3f)
        rect(c,-16f,-104f,32f,5f,Color.rgb(27,31,42),2f)
        rect(c,-13f,-103f,8f,3f,Color.WHITE,1f)
        // plume
        val plume=Path().apply { moveTo(-5f,-111f); quadTo(-22f,-143f,7f,-137f); quadTo(17f,-130f,11f,-111f); close() }
        color(if(enemy) Color.rgb(225,111,63) else Color.rgb(205,62,68)); c.drawPath(plume,p)
        // sword
        if(swinging) {
            line(c,27f,-57f,72f,-92f,Color.rgb(225,225,222),9f)
            line(c,24f,-55f,41f,-38f,trim,6f)
            line(c,72f,-92f,81f,-101f,Color.WHITE,3f)
        } else {
            line(c,28f,-48f,47f,-91f,Color.rgb(213,220,223),7f)
            line(c,22f,-50f,36f,-43f,trim,5f)
        }
        c.restore()
    }

    private fun drawHero(c: Canvas) {
        val bob=if(jump>0) -jump*22f else sin(elapsed*8f)*1.5f
        drawKnight(c,heroX,heroY+bob,1f,facing,false,hurt,attack>0f)
        if(attack>0f) {
            color(Color.argb((110*attack.coerceAtMost(1f)).toInt(),255,239,172))
            p.style=Paint.Style.STROKE; p.strokeWidth=5f
            c.drawArc(if(facing>0) heroX+15 else heroX-90,heroY-120,if(facing>0) heroX+115 else heroX+10,heroY-20,if(facing>0) 210f: -30f,110f,false,p); p.style=Paint.Style.FILL
        }
    }

    private fun drawFoes(c: Canvas) {
        foes.forEach { drawKnight(c,it.x,it.y,0.9f,if(heroX<it.x) -1 else 1,true,it.hitFlash,false)
            if(it.hp<3) { rect(c,it.x-20,it.y-126,40f,5f,Color.rgb(48,36,39),2f); rect(c,it.x-20,it.y-126,40f*it.hp/3f,5f,Color.rgb(219,74,65),2f) }
        }
    }

    private fun drawSparks(c: Canvas) {
        sparks.forEach { circle(c,it.x,it.y,3f+it.life*2,it.color) }
    }

    private fun drawHud(c: Canvas) {
        rect(c,20f,18f,260f,72f,Color.argb(185,19,24,39),14f)
        text(c,"CAVALEIRO DO CREPÚSCULO",36f,42f,15f,Color.rgb(247,215,157))
        rect(c,36f,54f,220f,13f,Color.rgb(69,48,55),6f)
        rect(c,36f,54f,220f*heroHp/100f,13f,Color.rgb(202,68,65),6f)
        text(c,"HP  ${heroHp.toInt()}%",36f,83f,12f,Color.WHITE)
        rect(c,790f,18f,150f,62f,Color.argb(185,19,24,39),12f)
        text(c,"FASE  $wave",865f,43f,16f,Color.rgb(247,215,157),Paint.Align.CENTER)
        text(c,"PONTOS  $score",865f,65f,13f,Color.WHITE,Paint.Align.CENTER)
    }

    private fun drawControls(c: Canvas) {
        // left analog-style pad
        circle(c,104f,444f,69f,Color.argb(70,12,17,29)); circle(c,104f,444f,45f,Color.argb(100,200,210,220))
        circle(c,104f,444f,24f,Color.argb(170,36,48,65))
        line(c,104f,411f,104f,477f,Color.argb(130,255,255,255),3f)
        line(c,71f,444f,137f,444f,Color.argb(130,255,255,255),3f)
        // action buttons
        circle(c,848f,454f,39f,Color.argb(190,176,55,57)); circle(c,848f,454f,31f,Color.rgb(205,75,70))
        text(c,"A",848f,464f,27f,Color.WHITE,Paint.Align.CENTER)
        circle(c,757f,411f,31f,Color.argb(190,45,113,161)); circle(c,757f,411f,25f,Color.rgb(58,139,185))
        text(c,"B",757f,420f,22f,Color.WHITE,Paint.Align.CENTER)
        text(c,"ATAQUE",848f,510f,11f,Color.WHITE,Paint.Align.CENTER)
        text(c,"PULO",757f,458f,10f,Color.WHITE,Paint.Align.CENTER)
    }

    private fun drawGameOver(c: Canvas) {
        rect(c,0f,0f,worldW,worldH,Color.argb(185,9,12,22))
        text(c,"VOCÊ CAIU NA BATALHA",480f,220f,35f,Color.rgb(255,218,170),Paint.Align.CENTER)
        text(c,"Toque para tentar novamente",480f,270f,20f,Color.WHITE,Paint.Align.CENTER)
        text(c,"PONTUAÇÃO: $score",480f,315f,17f,Color.rgb(230,190,120),Paint.Align.CENTER)
    }

    private fun spawnFoe(x: Float = Random.nextInt(620,900).toFloat()) {
        foes.add(Foe(x.coerceIn(540f,930f),390f))
    }

    private fun update(dt: Float) {
        elapsed+=dt
        if(gameOver) return
        attack=(attack-dt).coerceAtLeast(0f); attackCooldown=(attackCooldown-dt).coerceAtLeast(0f)
        hurt=(hurt-dt).coerceAtLeast(0f)
        val left=moveX<-.2f || keys.contains(KeyEvent.KEYCODE_A) || keys.contains(KeyEvent.KEYCODE_DPAD_LEFT)
        val right=moveX>.2f || keys.contains(KeyEvent.KEYCODE_D) || keys.contains(KeyEvent.KEYCODE_DPAD_RIGHT)
        val up=moveY<-.35f || keys.contains(KeyEvent.KEYCODE_W) || keys.contains(KeyEvent.KEYCODE_DPAD_UP)
        val down=moveY>.35f || keys.contains(KeyEvent.KEYCODE_S) || keys.contains(KeyEvent.KEYCODE_DPAD_DOWN)
        val speed=if(jump>0) 190f else 230f
        if(left) { heroX-=speed*dt; facing=-1 }; if(right) { heroX+=speed*dt; facing=1 }
        if(up) heroY-=speed*.65f*dt; if(down) heroY+=speed*.65f*dt
        heroX=heroX.coerceIn(55f,920f); heroY=heroY.coerceIn(365f,475f)
        if(jump>0 || jumpV>0) { jump=(jump+jumpV*dt).coerceAtLeast(0f); jumpV-=3.6f*dt; if(jump<=0) { jump=0f; jumpV=0f } }
        if((jumpHeld || keys.contains(KeyEvent.KEYCODE_Z) || keys.contains(KeyEvent.KEYCODE_SPACE)) && jump==0f) { jumpV=1.9f }
        if((attackHeld || keys.contains(KeyEvent.KEYCODE_X) || keys.contains(KeyEvent.KEYCODE_J)) && attackCooldown==0f) {
            attack=.24f; attackCooldown=.34f
            foes.filter { abs(it.x-heroX)<105f && abs(it.y-heroY)<65f && (it.x-heroX)*facing>-25f }.forEach {
                it.hp--; it.hitFlash=.18f; it.x+=facing*25f
                repeat(7) { _ -> sparks.add(Spark(it.x,it.y-65,Random.nextFloat()*6-3f,Random.nextFloat()*-6-1f,.55f,Color.rgb(255,208,112))) }
                if(it.hp<=0) score+=100
            }
            foes.removeAll { it.hp<=0 }
        }
        foes.forEach { f ->
            f.hitFlash=(f.hitFlash-dt).coerceAtLeast(0f); f.attackWait=(f.attackWait-dt).coerceAtLeast(0f)
            if(f.hitFlash==0f) {
                val dx=heroX-f.x; val dy=heroY-f.y
                if(abs(dx)>45) f.x+=dx.sign*92f*dt
                if(abs(dy)>18) f.y+=dy.sign*55f*dt
                if(abs(dx)<54 && abs(dy)<48 && f.attackWait==0f && hurt==0f && jump==0f) {
                    heroHp=(heroHp-7f).coerceAtLeast(0f); hurt=.45f; f.attackWait=.9f
                    heroX+=(if(dx>=0) -1 else 1)*20f
                    repeat(5) { sparks.add(Spark(heroX,heroY-55,Random.nextFloat()*5-2.5f,-Random.nextFloat()*5,.4f,Color.rgb(255,105,83))) }
                }
            }
        }
        sparks.forEach { it.x+=it.vx; it.y+=it.vy; it.vy+=12f*dt; it.life-=dt }
        sparks.removeAll { it.life<=0 }
        spawnClock-=dt
        if(spawnClock<=0 && foes.size<5) { spawnFoe(); spawnClock=2.8f; if(score>0 && score%500==0) wave++ }
        if(heroHp<=0) gameOver=true
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if(gameOver && e.action==MotionEvent.ACTION_DOWN) { reset(); return true }
        val sx=worldW/width; val sy=worldH/height
        when(e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_MOVE -> {
                moveX=0f; moveY=0f; attackHeld=false; jumpHeld=false
                for(i in 0 until e.pointerCount) {
                    val x=e.getX(i)*sx; val y=e.getY(i)*sy
                    if(x<205f && y>365f) { moveX=((x-104f)/48f).coerceIn(-1f,1f); moveY=((y-444f)/48f).coerceIn(-1f,1f) }
                    if(x>800f && y>390f) attackHeld=true
                    if(x>700f && x<810f && y>370f && y<465f) jumpHeld=true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                if(e.actionMasked==MotionEvent.ACTION_UP) { moveX=0f; moveY=0f; attackHeld=false; jumpHeld=false }
            }
        }
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        keys.add(keyCode)
        if(keyCode==KeyEvent.KEYCODE_ESCAPE || keyCode==KeyEvent.KEYCODE_BACK) { reset(); return true }
        return true
    }
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean { keys.remove(keyCode); return true }

    private fun reset() {
        heroX=250f; heroY=390f; heroHp=100f; score=0; wave=1; gameOver=false
        foes.clear(); sparks.clear(); repeat(3) { spawnFoe(690f+it*90f) }
        spawnClock=2f; invalidate()
    }
}
