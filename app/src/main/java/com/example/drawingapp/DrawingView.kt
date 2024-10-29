package com.example.drawingapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class DrawingView(
    context: Context,
    attrs:AttributeSet
) : View(context,attrs)
{
    private var _drawPath : CustomPath ? =null //non public member like _varName in JC
    private val _paths = ArrayList<CustomPath>()
    private var _canvasBitmap : Bitmap?=null
    private var _drawPaint : Paint ?=null
    private var _canvasPaint : Paint ?=null


    //brush
    private var _brushStroke : Float = 0f
    private var _brushColor = Color.CYAN
    private var _canvas : Canvas? =null

    init {
        
        setUpDrawing()

    }

    //wtf is internal class now
    internal inner class CustomPath(
        var color :Int,
        var stroke : Float
    ) : Path(){



    }
    private fun setUpDrawing(){
        _drawPath = CustomPath(_brushColor,_brushStroke)
        _drawPaint = Paint()
        _drawPaint?.color = _brushColor
        _drawPaint?.style= Paint.Style.STROKE
        _drawPaint?.strokeJoin = Paint.Join.MITER
        _drawPaint?.strokeCap = Paint.Cap.ROUND
        _canvasPaint = Paint(Paint.DITHER_FLAG)
        //_brushStroke = 20F


    }

    override fun onSizeChanged(w: Int , h: Int , oldw: Int , oldh: Int) {
            super.onSizeChanged(w , h , oldw , oldh)
            _canvasBitmap = Bitmap.createBitmap(w , h , Bitmap.Config.ARGB_8888)
            _canvas = Canvas(_canvasBitmap!!)

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        _canvas!!.drawBitmap(_canvasBitmap!!,0f,0f,_canvasPaint)

        for(path in _paths){
            _drawPaint!!.strokeWidth = path.stroke
            _drawPaint!!.color = path.color
            canvas.drawPath(path,_drawPaint!!)
        }


        if(!_drawPath!!.isEmpty){
            //for the paint
            _drawPaint!!.strokeWidth = _drawPath!!.stroke
            _drawPaint!!.color = _drawPath!!.color

            canvas.drawPath(_drawPath!!,_drawPaint!!)

        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        val touchX = event?.x
        val touchY = event?.y
        when(event?.action){
            //first touch           ->LAMBDA
            MotionEvent.ACTION_DOWN ->{
                //how thicc the path is
                //draw path is the custom path thing
                _drawPath!!.color = _brushColor
                _drawPath!!.stroke = _brushStroke

                _drawPath!!.reset()
                if(touchX!=null && touchY!=null) {
                    _drawPath!!.moveTo(touchX , touchY)
                }

            }
            MotionEvent.ACTION_MOVE->{
                if(touchX!=null && touchY!=null) {
                    _drawPath!!.lineTo(touchX , touchY)
                }
            }
            MotionEvent.ACTION_UP->{
                _paths.add(_drawPath!!)
                _drawPath = CustomPath(_brushColor,_brushStroke)
            }
            else->{
                return false
            }

        }
        invalidate()
        return true
    }
    fun setStroke(size : Float){
        _brushStroke = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            size,resources.displayMetrics)
        _drawPaint!!.strokeWidth = _brushStroke
    }
    fun setColor(color: String){
        _brushColor = Color.parseColor(color)
        _drawPaint!!.color = _brushColor
    }
    fun undo(){
        if(_paths.isNotEmpty()){
            _paths.removeAt(_paths.size-1)
            invalidate()
        }
    }


}