# Advanced Calculator (Pro Edition)

一款采用工业级架构重构的高性能 Android 计算器应用。本项目不仅支持基础和科学计算，还集成了符号计算引擎、高阶矩阵运算逻辑以及硬件加速的 2D/3D 图形渲染。

## 🚀 核心架构演进

### 1. 模块化架构 (Multi-module Project)
- **`:app`**: 现代化的 UI 模块。采用了 **Jetpack Compose** 与传统 View 系统的混合架构，确保了 UI 的灵活性与响应速度。
- **`:math-engine`**: 独立的纯 Kotlin 数学核心模块。封装了 **mXparser** (数值计算)、**Symja** (符号计算) 以及 **EJML** (高性能矩阵运算)，实现了计算逻辑与 UI 层的彻底解耦。

### 2. 现代化技术栈
- **依赖注入 (Hilt)**: 全面引入 **Dagger Hilt** 负责全局单例（如数据库、偏好设置）及 Repository 的生命周期管理。
- **Jetpack Compose**: 科学计算与单位换算模块已全面迁移至 Compose 声明式 UI，带来更流畅的交互体验。
- **Kotlin Flow & Coroutines**: 异步任务（如复杂的图形采样、符号推导）完全基于协程处理，UI 刷新采用 Flow/StateFlow。
- **Room Persistence**: 用户的计算历史记录持久化存储，支持自动清理过期数据。

## 🌟 核心功能

- **符号计算 (Symbolic Computation)**: 依托 Symja 引擎，支持表达式的 **自动简化**、**解析求导** 及因式分解。
- **2D/3D 科学绘图**: 
  - **2D**: 支持显函数、参数方程、极坐标，具备 Pinch-to-zoom 缩放效果。
  - **3D**: 基于 OpenGL ES 渲染引擎，支持 $z=f(x,y)$ 曲面及空间参数曲线。
- **矩阵实验室**: 支持行列式、逆矩阵、转置、迹及矩阵四则运算。
- **单位换算系统**: 预置长度、质量、温度等多维度转换逻辑，由 Compose 驱动。

## 🛠 开发环境
- **IDE**: Android Studio Ladybug+
- **Gradle**: 9.0 (Version Catalog)
- **Language**: 100% Kotlin
- **JDK**: Java 11

## 🚀 运行指南
```bash
# 运行全部模块的单元测试
./gradlew test

# 构建 APK
./gradlew :app:assembleDebug
```

---
*本项目已根据工业级规范进行重构，代码遵循严谨的空安全规范，适合作为高级 Android 开发的参考范例。*
