package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.viewmodel.UnitViewModel
import com.example.myapplication.repository.UnitRepository

@Composable
fun UnitScreen(viewModel: UnitViewModel) {
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val units by viewModel.units.collectAsState()
    val result by viewModel.result.collectAsState()

    var inputValue by remember { mutableStateOf("1") }
    var fromUnit by remember { mutableStateOf<UnitRepository.Unit?>(null) }
    var toUnit by remember { mutableStateOf<UnitRepository.Unit?>(null) }

    LaunchedEffect(units) {
        if (units.isNotEmpty()) {
            fromUnit = units[0]
            toUnit = units[0]
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("单位换算", color = Color.White, fontSize = 24.sp)
        
        Spacer(modifier = Modifier.height(16.dp))

        // Category Selector
        ScrollableTabRow(
            selectedTabIndex = categories.indexOf(selectedCategory),
            containerColor = Color.Transparent,
            contentColor = Color(0xFFFF9F0A),
            edgePadding = 0.dp
        ) {
            categories.forEach { category ->
                Tab(
                    selected = selectedCategory == category,
                    onClick = { viewModel.selectCategory(category) },
                    text = { Text(category.name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input
        OutlinedTextField(
            value = inputValue,
            onValueChange = { inputValue = it },
            label = { Text("数值") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFF9F0A)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // From Unit
        UnitDropdown(label = "从", selectedUnit = fromUnit, units = units) { fromUnit = it }

        Spacer(modifier = Modifier.height(8.dp))

        // To Unit
        UnitDropdown(label = "到", selectedUnit = toUnit, units = units) { toUnit = it }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val value = inputValue.toDoubleOrNull() ?: 0.0
                if (fromUnit != null && toUnit != null) {
                    viewModel.convert(value, fromUnit!!, toUnit!!)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9F0A))
        ) {
            Text("换算")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "结果: $result",
            color = Color(0xFFFF9F0A),
            fontSize = 20.sp
        )
    }
}

@Composable
fun UnitDropdown(
    label: String,
    selectedUnit: UnitRepository.Unit?,
    units: List<UnitRepository.Unit>,
    onUnitSelected: (UnitRepository.Unit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("$label: ${selectedUnit?.name ?: "选择单位"}", color = Color.White)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            units.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.name) },
                    onClick = {
                        onUnitSelected(unit)
                        expanded = false
                    }
                )
            }
        }
    }
}
