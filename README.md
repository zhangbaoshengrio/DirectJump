# DirectJump

**中文** | [English](#english)

---

## 简介

DirectJump 是一个 **LSPosed 模块**，拦截指定 App 内的链接跳转，直接打开对应的电商 App，跳过浏览器中转步骤。

### 目前支持

| 来源 App | 链接类型 | 直跳目标 |
|---------|---------|---------|
| 水龙头 (com.exinone.exinearn) | 京东链接 | 京东 App |

### 使用方法

1. 下载并安装 APK（见 [Releases](../../releases)）
2. 打开 **LSPosed 管理器** → 模块列表 → 启用 **DirectJump**
3. 在**作用域**中勾选对应的来源 App
4. 重启来源 App

### 效果

**之前：** 水龙头 → 点「京东购买」→ Edge 浏览器 → 再次点击 → 京东 App

**之后：** 水龙头 → 点「京东购买」→ 京东 App ✓

### 扩展 / 添加新规则

编辑 [`RedirectConfig.kt`](app/src/main/java/com/wizpizz/directjump/config/RedirectConfig.kt)：

```kotlin
// 1. 添加新的跳转规则（URL → 目标 App）
val RULE_TAOBAO = RedirectRule(
    hosts = setOf("item.taobao.com", "taobao.com", "tb.cn"),
    targetPkg = "com.taobao.taobao",
    name = "Taobao"
)

// 2. 在 apps 列表里添加来源 App
AppConfig(
    packageName = "com.example.otherapp",
    rules = listOf(RULE_JD, RULE_TAOBAO)
)
```

---

## 要求

- Android 8.0+
- LSPosed 框架已安装

---

<a name="english"></a>

# DirectJump

[中文](#directjump) | **English**

---

## Overview

DirectJump is an **LSPosed module** that intercepts URL-based app launches and redirects them directly to the target e-commerce app, bypassing the browser detour.

### Currently Supported

| Source App | Link Type | Redirects To |
|-----------|-----------|-------------|
| 水龙头 (com.exinone.exinearn) | JD.com links | JD App |

### How to Use

1. Download and install the APK from [Releases](../../releases)
2. Open **LSPosed Manager** → Modules → Enable **DirectJump**
3. Set the **scope** to include the source app(s)
4. Restart the source app

### Before / After

**Before:** 水龙头 → tap "Buy on JD" → Edge browser → tap again → JD App

**After:** 水龙头 → tap "Buy on JD" → JD App ✓

### Adding New Rules

Edit [`RedirectConfig.kt`](app/src/main/java/com/wizpizz/directjump/config/RedirectConfig.kt):

```kotlin
// 1. Define a new redirect rule (URL pattern → target app)
val RULE_TAOBAO = RedirectRule(
    hosts = setOf("item.taobao.com", "taobao.com", "tb.cn"),
    targetPkg = "com.taobao.taobao",
    name = "Taobao"
)

// 2. Add a new source app entry
AppConfig(
    packageName = "com.example.otherapp",
    rules = listOf(RULE_JD, RULE_TAOBAO)
)
```

---

## Requirements

- Android 8.0+
- LSPosed framework installed
