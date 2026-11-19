package com.castlecrashers.mobile

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.view.View

/** Cartoon medieval scene drawn locally so the launcher needs no network assets. */
class CastleMenuArtView(context: Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private fun col(v: Long) = v.toInt()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        val sx = width / 900f
        val sy = height / 520f
        canvas.save()
        canvas.scale(sx, sy)
        p.shader = LinearGradient(0f, 0f, 0f, 520f, col(0xff192a48), col(0xffcf8055), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, 900f, 520f, p)
        p.shader = null

        p.color = col(0xffffdf9a)
        canvas.drawCircle(720f, 90f, 43f, p)
        p.color = col(0x55fff0c8)
        canvas.drawCircle(720f, 90f, 57f, p)
        p.color = col(0x55f4d5b0)
        canvas.drawOval(520f, 95f, 690f, 125f, p)
        canvas.drawOval(610f, 118f, 790f, 145f, p)

        p.color = col(0xff49516a)
        poly(canvas, floatArrayOf(0f,330f, 130f,190f, 260f,330f, 390f,205f, 560f,340f, 700f,220f, 900f,350f, 900f,520f, 0f,520f))
        p.color = col(0xff252b40)
        canvas.drawRect(575f, 190f, 860f, 390f, p)
        canvas.drawRect(610f, 145f, 680f, 390f, p)
        canvas.drawRect(745f, 125f, 815f, 390f, p)
        roof(canvas, 610f,145f,680f,95f)
        roof(canvas,745f,125f,815f,75f)
        roof(canvas,575f,190f,630f,150f)
        p.color = col(0xffffc77b)
        for (x in floatArrayOf(630f,655f,770f,795f,600f,835f)) canvas.drawRoundRect(x,220f,x+9f,255f,4f,4f,p)
        p.color = col(0xff171e31)
        canvas.drawRect(690f,285f,730f,390f,p)
        canvas.drawOval(690f,265f,730f,305f,p)

        p.color = col(0xff526443)
        canvas.drawOval(-130f,325f,590f,610f,p)
        p.color = col(0xff394d3d)
        canvas.drawOval(260f,335f,1040f,620f,p)
        p.color = col(0xff263b35)
        canvas.drawRect(0f,440f,900f,520f,p)

        knight(canvas, 390f, 300f, 1.02f, col(0xffc33f3b), col(0xffe6c04c))
        knight(canvas, 555f, 326f, 0.82f, col(0xff3e78b9), col(0xffe6e0d0))
        p.color = col(0xffd8b46d)
        canvas.drawRect(820f,305f,824f,425f,p)
        poly(canvas, floatArrayOf(824f,307f,870f,321f,824f,338f))
        canvas.restore()
    }

    private fun roof(c: Canvas, l: Float, b: Float, r: Float, peak: Float) {
        val path = Path().apply { moveTo(l-8f,b); lineTo((l+r)/2f,peak); lineTo(r+8f,b); close() }
        c.drawPath(path,p)
    }

    private fun poly(c: Canvas, pts: FloatArray) {
        val path = Path().apply {
            moveTo(pts[0],pts[1])
            var i=2
            while (i<pts.size) { lineTo(pts[i],pts[i+1]); i+=2 }
            close()
        }
        c.drawPath(path,p)
    }

    private fun knight(c: Canvas, x: Float, y: Float, scale: Float, armor: Int, plume: Int) {
        c.save()
        c.translate(x,y)
        c.scale(scale,scale)
        p.style=Paint.Style.STROKE
        p.strokeWidth=8f
        p.strokeJoin=Paint.Join.ROUND
        p.color=col(0xff171521)
        c.drawLine(55f,12f,105f,-85f,p)
        c.drawLine(92f,-64f,119f,-91f,p)
        p.style=Paint.Style.FILL
        p.color=col(0xffd8d0bd)
        poly(c,floatArrayOf(99f,-80f,118f,-112f,124f,-106f,108f,-72f))
        p.color=col(0xff805b3d)
        c.drawRoundRect(87f,-71f,106f,-64f,3f,3f,p)

        p.color=armor
        c.drawRoundRect(-42f,8f,45f,122f,24f,24f,p)
        p.color=col(0xff171521)
        p.style=Paint.Style.STROKE
        c.drawRoundRect(-42f,8f,45f,122f,24f,24f,p)
        p.style=Paint.Style.FILL
        p.color=col(0xffe8d9b9)
        c.drawRoundRect(-27f,112f,-4f,174f,8f,8f,p)
        c.drawRoundRect(8f,112f,31f,174f,8f,8f,p)
        p.color=col(0xff282533)
        c.drawRoundRect(-31f,163f,0f,181f,5f,5f,p)
        c.drawRoundRect(5f,163f,36f,181f,5f,5f,p)

        p.color=col(0xffd5a94f)
        val shield=Path().apply {
            moveTo(-48f,28f); lineTo(-94f,42f); lineTo(-88f,100f)
            lineTo(-67f,119f); lineTo(-47f,98f); close()
        }
        c.drawPath(shield,p)
        p.color=col(0xff4a2630)
        c.drawCircle(-70f,67f,12f,p)

        p.color=col(0xffe9d9bc)
        c.drawCircle(0f,-16f,34f,p)
        p.color=armor
        val helmet=Path().apply {
            moveTo(-36f,-14f); lineTo(-31f,-48f); lineTo(0f,-66f)
            lineTo(31f,-48f); lineTo(36f,-14f); close()
        }
        c.drawPath(helmet,p)
        p.color=col(0xff171521)
        p.style=Paint.Style.STROKE
        c.drawPath(helmet,p)
        p.style=Paint.Style.FILL
        p.color=plume
        poly(c,floatArrayOf(-8f,-61f,1f,-105f,18f,-66f))
        p.color=col(0xff171521)
        c.drawRect(-18f,-23f,18f,-16f,p)
        p.style=Paint.Style.STROKE
        p.strokeWidth=5f
        c.drawCircle(0f,-16f,34f,p)
        p.style=Paint.Style.FILL
        c.restore()
    }
}
