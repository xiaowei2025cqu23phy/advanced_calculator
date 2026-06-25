# Advanced Calculator (高级计算器)

一款功能强大的 Android 计算器应用，采用现代 Android 开发技术栈，旨在为学生、工程师和科研人员提供全面的数学计算与可视化工具。

## 🌟 核心功能

### 1. 科学计算 (Scientific)
- **实时渲染**：采用 LaTeX 技术实时预览复杂的数学公式。
- **现代化自定义键盘**：内置完整数学符号键盘，支持高阶函数输入，具备震动反馈和布局折叠功能。
- **模式管理**：支持角度 (DEG) 与弧度 (RAD) 模式切换，并自动保存用户偏好。
- **持久化历史回溯**：基于 Room 数据库的计算记录存储，支持长按管理和快速复用，即使应用重启也不会丢失。

### 2. 2D 绘图 (2D Graphing)
- **多模式绘图**：支持显函数 $y=f(x)$、参数方程 $(x(t), y(t))$ 和极坐标方程 $r=f(\theta)$。
- **动态交互**：支持手势缩放 (Pinch-to-zoom) 和平移 (Pan)，带惯性滑动效果。
- **多图层对比**：支持在同一坐标系下绘制多个函数。
- **图像导出**：一键将生成的函数图像保存至系统相册。

### 3. 3D 绘图 (3D Graphing)
- **硬件加速**：基于 OpenGL ES 2.0 的 3D 渲染引擎，计算逻辑在后台线程执行。
- **曲面与曲线**：支持三维曲面 $z=f(x,y)$ 和空间参数曲线。
- **全向观察**：通过触摸手势实现 360 度视角旋转。

### 4. 矩阵运算 (Matrix)
- **全面算法**：支持矩阵加减乘、求逆、行列式 (Det)、转置 (Trans) 和迹 (Trace)。
- **高性能解析**：基于 EJML 库实现高效矩阵运算。
- **灵活输入**：智能识别空格、逗号和换行作为分隔符。

### 5. 复数处理 (Complex)
- **基础运算**：支持复数的四则运算。
- **坐标转换**：代数形式 (a+bi) 与极坐标形式 ($r\angle\theta$) 的快速转换。
- **特征计算**：支持求共轭 (Conj) 和辐角 (Arg)。

## 🛠 技术实现

- **开发语言**: 100% Kotlin
- **架构**: MVVM (ViewModel + LiveData + Repository)
- **数据存储**: Room Database (历史记录), SharedPreferences (用户设置)
- **异步处理**: Kotlin Coroutines
- **UI 组件**: View Binding, Navigation Component, Custom Components (MathKeyboardView)
- **数学解析**: mXparser, EJML
- **图形渲染**: OpenGL ES, Custom Views, Canvas Drawing

## 🚀 开发与运行
1. **环境**: Android Studio Ladybug (或更高版本)。
2. **构建**: 使用 Gradle 8.0+ 和 Kotlin 1.9+。
3. **测试**: 项目包含完整的 JUnit 单元测试，覆盖了所有核心数学逻辑和绘图引擎。

```bash
# 运行单元测试
./gradlew test
```
