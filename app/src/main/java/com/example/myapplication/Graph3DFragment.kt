package com.example.myapplication

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.FragmentGraph3dBinding

/**
 * Modernized 3D Graph Fragment in Kotlin.
 */
class Graph3DFragment : Fragment() {

    private var _binding: FragmentGraph3dBinding? = null
    private val binding get() = _binding!!
    
    private var renderer: Graph3DRenderer? = null
    private var curveMode = false
    private var prevX = 0f
    private var prevY = 0f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return try {
            _binding = FragmentGraph3dBinding.inflate(inflater, container, false)
            binding.root
        } catch (e: Exception) {
            TextView(context).apply {
                text = "3D加载失败: ${e.message}"
                gravity = Gravity.CENTER
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(0xFF1C1C1E.toInt())
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupGL()
        setupInput()
        setupControls()
        setupKeyboard(view)
        
        setCurveMode(false)
    }

    private fun setupGL() {
        renderer = Graph3DRenderer()
        binding.glSurface.apply {
            setEGLContextClientVersion(2)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            
            setOnTouchListener { _, event ->
                val x = event.x
                val y = event.y
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        prevX = x
                        prevY = y
                    }
                    MotionEvent.ACTION_MOVE -> {
                        renderer?.let { r ->
                            r.setRotation(
                                (r.rotX + (y - prevY) * 0.5f).coerceIn(-90f, 90f),
                                r.rotY + (x - prevX) * 0.5f
                            )
                        }
                        prevX = x
                        prevY = y
                    }
                }
                true
            }
        }
    }

    private fun setupInput() {
        listOf(binding.etFunction3d, binding.etPx, binding.etPy, binding.etPz).forEach {
            preventSystemKeyboard(it)
        }
    }

    private fun setupControls() {
        binding.btnModeSurface.setOnClickListener { setCurveMode(false) }
        binding.btnModeCurve.setOnClickListener { setCurveMode(true) }
        
        binding.btnVarX.setOnClickListener { appendToInput("x") }
        binding.btnVarY.setOnClickListener { appendToInput("y") }
        binding.btnVarT.setOnClickListener { appendToInput("t") }
        binding.btnReset3d.setOnClickListener { renderer?.setRotation(-25f, 0f) }
        
        binding.btnPlot3d.setOnClickListener { plotFunction() }
    }

    private fun setupKeyboard(view: View) {
        val kbView = layoutInflater.inflate(R.layout.layout_math_keyboard, binding.kbContainer, true)
        MathKeyboardHelper(kbView) { appendToInput(it) }.apply {
            backspaceButton?.setOnClickListener {
                val t = getActiveInputText()
                if (t.isNotEmpty()) setActiveInputText(t.dropLast(1))
            }
            acButton?.setOnClickListener { setActiveInputText("") }
            equalsButton?.setOnClickListener { plotFunction() }
        }
    }

    private fun appendToInput(text: String) {
        val et = when {
            !curveMode -> binding.etFunction3d
            binding.etPy.hasFocus() -> binding.etPy
            binding.etPz.hasFocus() -> binding.etPz
            else -> binding.etPx
        }
        et.append(text)
    }

    private fun getActiveInputText(): String {
        val et = when {
            !curveMode -> binding.etFunction3d
            binding.etPy.hasFocus() -> binding.etPy
            binding.etPz.hasFocus() -> binding.etPz
            else -> binding.etPx
        }
        return et.text.toString()
    }

    private fun setActiveInputText(s: String) {
        val et = when {
            !curveMode -> binding.etFunction3d
            binding.etPy.hasFocus() -> binding.etPy
            binding.etPz.hasFocus() -> binding.etPz
            else -> binding.etPx
        }
        et.setText(s)
    }

    private fun setCurveMode(mode: Boolean) {
        curveMode = mode
        binding.apply {
            layoutSurface.visibility = if (mode) View.GONE else View.VISIBLE
            layoutParametric.visibility = if (mode) View.VISIBLE else View.GONE
            layoutTRange.visibility = if (mode) View.VISIBLE else View.GONE
            btnModeSurface.alpha = if (mode) 0.4f else 1f
            btnModeCurve.alpha = if (mode) 1f else 0.4f
        }
    }

    private fun plotFunction() {
        if (curveMode) {
            val xs = binding.etPx.text.toString().trim()
            val ys = binding.etPy.text.toString().trim()
            val zs = binding.etPz.text.toString().trim()
            if (xs.isEmpty() || ys.isEmpty() || zs.isEmpty()) {
                Toast.makeText(context, "请填写 x(t), y(t), z(t)", Toast.LENGTH_SHORT).show()
                return
            }
            try {
                val tMin = binding.etTMin.text.toString().toFloat()
                val tMax = binding.etTMax.text.toString().toFloat()
                val tStep = binding.etTStep.text.toString().toFloat()
                binding.glSurface.queueEvent {
                    renderer?.setParametricFunction(xs, ys, zs, tMin, tMax, tStep)
                }
            } catch (e: NumberFormatException) {
                Toast.makeText(context, "t范围无效", Toast.LENGTH_SHORT).show()
            }
        } else {
            val expr = binding.etFunction3d.text.toString().trim()
            if (expr.isEmpty()) {
                Toast.makeText(context, "请输入函数", Toast.LENGTH_SHORT).show()
                return
            }
            binding.glSurface.queueEvent { renderer?.setFunction(expr) }
        }
    }

    private fun preventSystemKeyboard(et: EditText) {
        et.showSoftInputOnFocus = false
        et.setOnTouchListener { v, event ->
            v.onTouchEvent(event)
            true
        }
    }

    override fun onPause() {
        super.onPause()
        binding.glSurface.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.glSurface.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        renderer?.shutdown()
    }
}
