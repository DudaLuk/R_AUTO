package pl.huber.rauto

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.sin

/** Procedural scenery; no downloaded assets or continuous sound from the speaker. */
class RoadView(context: Context) : View(context) {
    var speed = 0f
    var distance = 0f
    var carColor = Color.rgb(246, 183, 69)
    var engineLevel = 0
    var turboLevel = 0
    var tiresLevel = 0
    var bodyLevel = 0
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val flame = Path()
    init { contentDescription = "Samochód na trasie. Prędkość jest podana nad planszą." }
    private fun box(c: Canvas, color: Int, l: Float, t: Float, r: Float, b: Float, radius: Float = 0f) {
        p.color = color; c.drawRoundRect(l,t,r,b,radius,radius,p)
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val c = canvas
        c.save(); c.scale(width / 400f, height / 260f)
        c.drawColor(Color.rgb(206,232,229))
        p.color = Color.rgb(255,230,166); c.drawCircle(339f,43f,23f,p)
        p.color = Color.rgb(164,199,177)
        c.drawOval(-80f,61f,230f,219f,p); c.drawOval(185f,68f,465f,219f,p)
        box(c, Color.rgb(109,165,137), 0f,136f,400f,260f)
        box(c, Color.rgb(216,221,209),0f,166f,400f,241f)
        box(c, Color.rgb(54,73,75),0f,172f,400f,235f)
        val offset = (distance * 12f) % 90f
        for (i in -1..5) box(c, Color.rgb(234,232,201),i*90f-offset,201f,i*90f+43f-offset,205f,2f)
        for (i in -1..4) {
            val x = i*130f - (distance*3f)%130f
            box(c,Color.rgb(91,103,76),x+8,117f,x+13,151f,2f)
            p.color=Color.rgb(51,117,95);c.drawCircle(x+10,112f,20f,p)
        }
        val x = 92f
        val y = 169f + if (speed > 25f) sin(distance*3f)*0.8f else 0f
        if (speed > 45f) {
            val flameLength = 25f + turboLevel * 5f
            p.color = Color.rgb(255,213,100)
            flame.reset()
            flame.apply { moveTo(x+5,y+35);lineTo(x-flameLength,y+27);lineTo(x-13,y+39);lineTo(x-flameLength-10f,y+43);lineTo(x+5,y+48);close() }
            c.drawPath(flame,p)
            if(turboLevel >= 3) {
                p.color=Color.rgb(255,145,75)
                c.drawCircle(x-15f-turboLevel*2f,y+40f,4f+turboLevel,p)
            }
        }
        p.color=Color.argb(50,0,0,0);c.drawOval(x-8,y+45,x+123,y+62,p)
        box(c,carColor,x+25,y-3,x+91,y+41,15f)
        box(c,Color.rgb(219,244,239),x+34,y+3,x+58,y+22,5f)
        box(c,Color.rgb(219,244,239),x+64,y+3,x+83,y+22,5f)
        box(c,carColor,x,y+20,x+119,y+49,12f)
        if(bodyLevel >= 1) {
            p.color=Color.argb(150,255,255,255)
            box(c,p.color,x+18,y+31,x+101,y+35,2f)
        }
        if(bodyLevel >= 2) {
            p.color=Color.rgb(45,65,68)
            box(c,p.color,x+8,y+13,x+34,y+17,3f)
            box(c,p.color,x+12,y+9,x+16,y+17,2f)
        }
        if(bodyLevel >= 3) {
            p.color=Color.rgb(255,245,185)
            c.drawCircle(x+64,y+29,6f,p)
        }
        if(bodyLevel >= 4) {
            p.color=Color.rgb(42,55,58)
            box(c,p.color,x+3,y+45,x+116,y+51,3f)
        }
        box(c,Color.rgb(255,250,210),x+105,y+24,x+120,y+32,3f)
        if(engineLevel >= 2) {
            p.color=Color.rgb(45,65,68)
            for(i in 0 until minOf(engineLevel,4)) box(c,p.color,x+39+i*9f,y-1,x+44+i*9f,y+2,2f)
        }
        for (index in 0..1) {
            val wheel = x + if(index==0) 25f else 94f
            val wheelRadius=14f + tiresLevel*0.8f
            p.color=Color.rgb(29,46,49);c.drawCircle(wheel,y+47,wheelRadius,p)
            p.color=if(tiresLevel >= 2) Color.rgb(190,204,198) else Color.rgb(218,227,221)
            c.drawCircle(wheel,y+47,7f+tiresLevel*0.5f,p)
            if(tiresLevel >= 3) {
                p.color=Color.rgb(90,110,108)
                c.drawCircle(wheel,y+47,2.5f,p)
            }
        }
        c.restore()
    }
}
