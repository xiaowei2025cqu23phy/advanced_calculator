# Advanced Calculator (Pro Edition)

一款采用工业级架构重构的高性能 Android 计算器应用。本项目不仅支持基础和科学计算，还集成了符号计算引擎、高阶矩阵运算逻辑以及硬件加速的 2D/3D 图形渲染。

---

## 🚀 核心架构演进

本项目从传统的单模块架构演进为**现代化多模块架构**，遵循解耦、可测试和高性能的设计原则。

### 1. 模块化设计 (Multi-module)
- **`:app` (Android Module)**: 
  - 负责 UI 展示与交互逻辑。
  - 混合架构：核心界面（科学计算、单位换算）已迁移至 **Jetpack Compose**，复杂 3D 界面保留传统 View 结合 **OpenGL ES**。
  - 使用 **Navigation Component** 管理 Fragment 间导航。
- **`:math-engine` (Pure Kotlin/JVM Module)**:
  - 独立的数学逻辑中心，不依赖 Android SDK，可在桌面端或服务器端复用。
  - **Symja**: 符号计算核心，处理代数简化、求导等。
  - **mXparser**: 高性能数值计算解析。
  - **EJML**: 工业级线性代数库，处理矩阵运算。

### 2. 依赖注入与单例管理 (Hilt)
- 全面引入 **Dagger Hilt**。
- 自动管理 `AppDatabase`、`SettingsManager` 以及各类 `Repository` 的生命周期。
- 极大降低了 ViewModel 与数据层之间的耦合度，便于进行 Mock 测试。

### 3. 数据流与响应式 UI (Flow & Compose)
- **Kotlin Flow**: 异步任务（如复杂的图形网格采样、符号推导）完全基于协程处理。
- **StateFlow/LiveData**: UI 层实时监听底层数据变化，确保计算状态的一致性。

---

## 🌟 核心功能深度解析

### 🔢 科学计算与符号引擎
- **数值解析**: 支持超长表达式解析、大数运算。
- **符号计算**: 
  - **自动简化 (Simplify)**: 将 `(x+1)^2 - (x-1)^2` 自动简化为 `4x`。
  - **解析求导 (Differentiate)**: 一键生成函数的导数表达式。
- **LaTeX 预览**: 自定义 Canvas 渲染器，实时预览符合数学出版标准的公式。

### 📈 2D/3D 科学绘图
- **2D 绘图**: 
  - 支持显函数 ($y=f(x)$)、参数方程 ($x(t), y(t)$)、极坐标方程 ($r=f(\theta)$)。
  - **交互控制**: 具备 Pinch-to-zoom 缩放、双击重置、实时坐标轴捕捉。
  - **导出功能**: 支持将绘图结果保存为高清 PNG。
- **3D 绘图**: 
  - **OpenGL ES 2.0 引擎**: 后台线程异步采样，生成数万个网格点并在 GPU 上高效渲染。
  - **动态采样**: 根据函数复杂度自动调整网格密度（最高支持 128x64 采样）。
  - **全向观察**: 360 度手势旋转。

### 🧮 矩阵实验室 (Matrix Lab)
- **基础运算**: 矩阵加、减、乘、标量乘法。
- **高级分析**: 快速计算 **行列式 (Determinant)**、**逆矩阵 (Inverse)**、**转置 (Transpose)**、**迹 (Trace)**。
- **容错输入**: 智能识别空格、逗号、换行符作为分隔符，输入体验流畅。

### ⚖️ 单位换算系统 (Unit Converter)
- **多维度支持**: 长度、质量、温度、时间、能量等。
- **实时转换**: 采用 Compose 声明式 UI，输入数值即刻看到全维度转换结果。

---

## 🛠 开发环境
- **IDE**: Android Studio Ladybug+
- **Gradle**: 9.0 (使用 Version Catalog 管理依赖)
- **Language**: 100% Kotlin
- **Persistence**: Room Database (计算历史永久存储)
- **Architecture Component**: ViewModel, LiveData, Flow, Hilt

---

## 🚀 运行指南

### 环境准备
1. 确保已安装 JDK 11 或更高版本。
2. 开启 Android 设备的“硬件加速”以获得最佳 3D 体验。

### 编译指令
```bash
# 运行全部模块的单元测试（确保数学引擎准确性）
./gradlew test

# 构建 Debug APK
./gradlew :app:assembleDebug
```

---

*本项目已根据工业级规范进行重构，代码遵循严谨的空安全规范，适合作为高级 Android 开发及数学应用开发的参考范例。*
