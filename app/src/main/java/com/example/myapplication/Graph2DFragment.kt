package com.example.myapplication

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.databinding.FragmentGraph2dBinding
import com.example.myapplication.viewmodel.GraphViewModel
import java.io.File
import java.io.FileOutputStream

/**
 * UI-only layer. 2D plotting delegated to GraphViewModel → PlotEngine (async).
 */
class Graph2DFragment : Fragment() {

    private var _binding: FragmentGraph2dBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: GraphViewModel
    private var plotMode = 0 // 0=y=f(x), 1=parametric, 2=polar
    private var focusedInput: EditText? = null

    private val COLORS = intArrayOf(
        -0xde690d, -0xbbbcc, -0xb34fb0, -0x67ff,
        -0x63d850, -0x86aa78, -0x9f8275, -0x14c5
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGraph2dBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[GraphViewModel::class.java]
        setupChart()

        // Observe computed curves
        viewModel.curves.observe(viewLifecycleOwner) { curves ->
            if (curves == null || curves.isEmpty()) return@observe
            binding.chart2d.clearCurves()
            for (c in curves) {
                binding.chart2d.addCurve(c.points, c.color, c.label)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            if (msg != null) Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            // Could show a progress indicator here
        }

        // Keyboard setup
        binding.kbView.apply {
            onInput = { text -> focusedInput?.append(text) }
            onBackspace = {
                val t = focusedInput?.text?.toString() ?: ""
                if (t.isNotEmpty()) focusedInput?.setText(t.substring(0, t.length - 1))
            }
            onClear = { focusedInput?.setText("") }
            onEquals = { plotAllGraphs() }
        }

        binding.btnAddFunction.setOnClickListener { addFunctionInput() }
        binding.btnPlot2d.setOnClickListener { plotAllGraphs() }
        binding.btnClearAll.setOnClickListener { clearAllFunctions() }
        binding.btnModeNormal.setOnClickListener { setMode(0) }
        binding.btnModeParam.setOnClickListener { setMode(1) }
        binding.btnModePolar.setOnClickListener { setMode(2) }
        binding.btnVarX.setOnClickListener { focusedInput?.append("x") }
        binding.btnVarY.setOnClickListener { focusedInput?.append("y") }
        binding.btnVarT.setOnClickListener { focusedInput?.append("t") }
        binding.btnVarTheta.setOnClickListener { focusedInput?.append("θ") }
        binding.btnVarComma.setOnClickListener { focusedInput?.append(",") }
        binding.btnResetView.setOnClickListener { binding.chart2d.resetBounds() }
        binding.btnPaste.setOnClickListener { pasteFromClipboard() }
        binding.btnSaveImg.setOnClickListener { saveChartImage() }

        updateModeButtons()
        addFunctionInput()
    }

    private fun setMode(mode: Int) {
        if (plotMode == mode) return
        plotMode = mode
        updateModeButtons()
        binding.layoutTRange.visibility = if (mode != 0) View.VISIBLE else View.GONE
        clearAllFunctions()
    }

    private fun updateModeButtons() {
        binding.btnModeNormal.alpha = if (plotMode == 0) 1f else 0.4f
        binding.btnModeParam.alpha = if (plotMode == 1) 1f else 0.4f
        binding.btnModePolar.alpha = if (plotMode == 2) 1f else 0.4f
    }

    private fun setupChart() {
        binding.chart2d.setBounds(-10f, 10f, -10f, 10f)
    }

    private fun addFunctionInput() {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(-1, -2)
            setPadding(0, 2, 0, 2)
        }

        when (plotMode) {
            1 -> {
                row.addView(makeEditText("x(t)"))
                row.addView(makeEditText("y(t)"))
            }
            2 -> {
                row.addView(makeEditText("r(θ)"))
            }
            else -> {
                row.addView(makeEditText("f(x)"))
            }
        }
        
        val del = Button(context).apply {
            layoutParams = LinearLayout.LayoutParams(-2, -2)
            text = "×"
            setTextColor(-0xba9c6)
            background = null
            setOnClickListener { (row.parent as ViewGroup).removeView(row) }
        }
        row.addView(del)
        binding.functionsContainer.addView(row)
    }

    private fun makeEditText(hint: String): EditText {
        return EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            this.hint = hint
            textSize = 13f
            setTextColor(-0x1)
            setHintTextColor(0x88FFFFFF.toInt())
            background = null
            isSingleLine = true
            id = View.generateViewId()
            showSoftInputOnFocus = false
            setOnTouchListener { v, event ->
                v.onTouchEvent(event)
                true
            }
            setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) focusedInput = v as EditText
            }
        }
    }

    private fun plotAllGraphs() {
        when (plotMode) {
            1 -> plotParametric()
            2 -> plotPolar()
            else -> plotNormal()
        }
    }

    private fun plotNormal() {
        val xMin: Double
        val xMax: Double
        val step: Double
        try {
            xMin = binding.etXMin.text.toString().toDouble()
            xMax = binding.etXMax.text.toString().toDouble()
            step = binding.etStep.text.toString().toDouble()
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "范围无效", Toast.LENGTH_SHORT).show()
            return
        }
        if (xMin >= xMax || step <= 0) {
            Toast.makeText(context, "范围不正确", Toast.LENGTH_SHORT).show()
            return
        }

        val inputs = mutableListOf<GraphViewModel.CartesianInput>()
        for (i in 0 until binding.functionsContainer.childCount) {
            val c = binding.functionsContainer.getChildAt(i)
            if (c is LinearLayout) {
                val txt = (c.getChildAt(0) as EditText).text.toString().trim()
                if (txt.isNotEmpty()) {
                    inputs.add(
                        GraphViewModel.CartesianInput(
                            txt,
                            COLORS[inputs.size % COLORS.size],
                            "f${inputs.size + 1}"
                        )
                    )
                }
            }
        }
        if (inputs.isEmpty()) {
            Toast.makeText(context, "请添加函数", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.generateCartesian(inputs, xMin, xMax, step)
    }

    private fun plotParametric() {
        val tMin: Double
        val tMax: Double
        val tStep: Double
        try {
            tMin = binding.etTMin.text.toString().toDouble()
            tMax = binding.etTMax.text.toString().toDouble()
            tStep = binding.etTStep.text.toString().toDouble()
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "t范围无效", Toast.LENGTH_SHORT).show()
            return
        }
        if (tMin >= tMax || tStep <= 0) {
            Toast.makeText(context, "范围不正确", Toast.LENGTH_SHORT).show()
            return
        }

        val inputs = mutableListOf<GraphViewModel.ParametricInput>()
        for (i in 0 until binding.functionsContainer.childCount) {
            val c = binding.functionsContainer.getChildAt(i)
            if (c is LinearLayout && c.childCount >= 3) {
                val xs = (c.getChildAt(0) as EditText).text.toString().trim()
                val ys = (c.getChildAt(1) as EditText).text.toString().trim()
                if (xs.isNotEmpty() && ys.isNotEmpty()) {
                    inputs.add(
                        GraphViewModel.ParametricInput(
                            xs, ys,
                            COLORS[inputs.size % COLORS.size],
                            "C${inputs.size + 1}"
                        )
                    )
                }
            }
        }
        if (inputs.isEmpty()) {
            Toast.makeText(context, "请添加参数方程", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.generateParametric(inputs, tMin, tMax, tStep)
    }

    private fun plotPolar() {
        val tMin: Double
        val tMax: Double
        val tStep: Double
        try {
            tMin = binding.etTMin.text.toString().toDouble()
            tMax = binding.etTMax.text.toString().toDouble()
            tStep = binding.etTStep.text.toString().toDouble()
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "θ范围无效", Toast.LENGTH_SHORT).show()
            return
        }
        if (tMin >= tMax || tStep <= 0) {
            Toast.makeText(context, "范围不正确", Toast.LENGTH_SHORT).show()
            return
        }

        val inputs = mutableListOf<GraphViewModel.PolarInput>()
        for (i in 0 until binding.functionsContainer.childCount) {
            val c = binding.functionsContainer.getChildAt(i)
            if (c is LinearLayout) {
                val txt = (c.getChildAt(0) as EditText).text.toString().trim()
                if (txt.isNotEmpty()) {
                    inputs.add(
                        GraphViewModel.PolarInput(
                            txt,
                            COLORS[inputs.size % COLORS.size],
                            "r${inputs.size + 1}"
                        )
                    )
                }
            }
        }
        if (inputs.isEmpty()) {
            Toast.makeText(context, "请添加函数", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.generatePolar(inputs, tMin, tMax, tStep)
    }

    private fun pasteFromClipboard() {
        val cm = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        if (cm.hasPrimaryClip() && focusedInput != null) {
            val s = cm.primaryClip?.getItemAt(0)?.text
            if (s != null) focusedInput?.append(s)
        }
    }

    private fun saveChartImage() {
        try {
            val bmp = Bitmap.createBitmap(
                binding.chart2d.width, binding.chart2d.height,
                Bitmap.Config.ARGB_8888
            )
            val c = Canvas(bmp)
            binding.chart2d.draw(c)

            val dir = File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                ), "Calculator"
            )
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "graph_${System.currentTimeMillis()}.png")
            val out = FileOutputStream(file)
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
            bmp.recycle()

            Toast.makeText(context, "已保存: ${file.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearAllFunctions() {
        binding.functionsContainer.removeAllViews()
        binding.chart2d.clearCurves()
        viewModel.clear()
        addFunctionInput()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
