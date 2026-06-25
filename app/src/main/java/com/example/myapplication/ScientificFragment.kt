package com.example.myapplication

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.databinding.FragmentScientificBinding
import com.example.myapplication.viewmodel.ScientificViewModel

/**
 * Modernized UI Layer in Kotlin.
 */
class ScientificFragment : Fragment() {

    private var _binding: FragmentScientificBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: ScientificViewModel
    private var degreeMode = false
    private var lastResult = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return try {
            _binding = FragmentScientificBinding.inflate(inflater, container, false)
            binding.root
        } catch (e: Exception) {
            TextView(context).apply {
                text = "加载失败: ${e.javaClass.simpleName}"
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(0xFF1C1C1E.toInt())
                gravity = 17
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[ScientificViewModel::class.java]

        setupObservers()
        setupInput()
        setupToolbar()
        setupKeyboard(view)
    }

    private fun setupObservers() {
        viewModel.result.observe(viewLifecycleOwner) { result ->
            if (result != null && result.isSuccess) {
                val s = viewModel.formatResult(result)
                binding.tvResult.text = "= $s"
                lastResult = s
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { msg ->
            msg?.let { binding.tvResult.text = "错误: $it" }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            if (loading) binding.tvResult.text = "计算中..."
        }

        viewModel.degreeMode.observe(viewLifecycleOwner) { isDeg ->
            degreeMode = isDeg
            updateDegDisplay()
        }
    }

    private fun setupInput() {
        binding.etExpression.apply {
            showSoftInputOnFocus = false
            setOnTouchListener { v, event -> 
                v.onTouchEvent(event)
                true 
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    binding.latexPreview.setExpression(s.toString())
                }
            })
        }
    }

    private fun setupToolbar() {
        updateDegDisplay()
        
        binding.btnDeg.setOnClickListener {
            viewModel.toggleDegreeMode()
        }
        
        binding.btnAns.setOnClickListener {
            if (lastResult.isNotEmpty()) binding.etExpression.append(lastResult)
        }
        
        binding.btnAns.setOnLongClickListener {
            showHistory()
            true
        }

        binding.btnCopy.setOnClickListener {
            val txt = binding.etExpression.text.toString()
            if (txt.isNotEmpty()) {
                val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("expr", txt))
                binding.tvResult.text = "已复制"
            }
        }

        binding.btnPaste.setOnClickListener {
            val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.primaryClip?.getItemAt(0)?.text?.let {
                binding.etExpression.append(it)
            }
        }
        
        // Simple symbols
        binding.btnComplexI.setOnClickListener { binding.etExpression.append("i") }
        binding.btnPi.setOnClickListener { binding.etExpression.append("pi") }
    }

    private fun setupKeyboard(view: View) {
        binding.kbView.apply {
            onInput = { text ->
                binding.etExpression.append(text)
            }
            onBackspace = {
                val t = binding.etExpression.text.toString()
                if (t.isNotEmpty()) binding.etExpression.setText(t.dropLast(1))
            }
            onClear = {
                binding.etExpression.setText("")
                binding.tvResult.text = ""
            }
            onEquals = {
                calculate()
            }
        }
    }

    private fun updateDegDisplay() {
        binding.btnDeg.text = if (degreeMode) "DEG" else "RAD"
        binding.tvModeIndicator.text = if (degreeMode) "DEG" else "RAD"
        binding.tvModeIndicator.setTextColor(
            if (degreeMode) 0xFFFF9F0A.toInt() else 0xFF888888.toInt()
        )
    }

    private fun calculate() {
        val expr = binding.etExpression.text.toString()
        if (expr.isEmpty()) return
        viewModel.calculate(expr, degreeMode)
    }

    private fun showHistory() {
        val items = viewModel.history.value ?: return
        if (items.isEmpty()) {
            binding.tvResult.text = "暂无历史记录"
            return
        }

        val lines = items.take(50).map { "${it.expression} = ${it.result}" }.toTypedArray()
        
        AlertDialog.Builder(context)
            .setTitle("历史记录 (长按清空)")
            .setItems(lines) { _, which ->
                binding.etExpression.setText(items[which].expression)
            }
            .setPositiveButton("清空") { _, _ -> viewModel.clearHistory() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
