# weiran-squaremap

基于 [squaremap](https://github.com/jpenilla/squaremap) 的二开版本，为 **Magic Flower** 服务器（squaremap 插件：`magicflower.org:25566`）定制地图前端与配置。

原项目 README 见 [RAW_README.md](./RAW_README.md)。

## 这是什么

squaremap 是一个 Minecraft 实时世界地图插件。它运行在**游戏服务端内部**，读取世界区块并生成 PNG 瓦片，同时启动一个内嵌 HTTP 服务，供浏览器查看地图。

和传统 Web 项目不同：**没有独立的 Node/Java 后端进程**。地图的「后端」就是 Minecraft 插件本身。

```
浏览器 ──HTTP──► squaremap 插件内嵌 Web 服务
                    │
                    └── 读取 MC 世界、渲染瓦片、写入 JSON
```

**Magic Flower 服端口说明：**

| 地址 | 用途 |
|------|------|
| `http://magicflower.org:25566` | squaremap 插件 HTTP 服务（线上地图、瓦片与 JSON 数据源） |
| `http://localhost:5173` | 本机 `npm run dev` 前端（二开 UI 时浏览器打开这个） |

squaremap 上游默认 HTTP 端口是 `8080`；Magic Flower 服已改为 **25566**（见 `deploy/magicflower/config.yml`）。

## 技术栈

| 部分 | 语言 | 框架 / 工具 |
|------|------|-------------|
| 前端 | JavaScript (ES Modules) | Vite 8、Leaflet |
| 服务端插件 | Java 21+ | Gradle、Guice、Undertow |
| MC 平台适配 | Java | Paper / Fabric / NeoForge / Sponge |
| 包管理（前端） | — | Bun（推荐；也可用 npm） |

前端代码在 `web/`，插件核心在 `common/`，各 MC 平台入口在 `paper/`、`fabric/` 等目录。

## 服务端配置（Magic Flower）

已为本服写好示例配置，位于 `deploy/magicflower/`：

| 文件 | 复制到服务端路径 |
|------|------------------|
| `config.yml` | `plugins/squaremap/config.yml` |
| `lang-zh-cn.yml`（可选） | `plugins/squaremap/locale/lang-zh-cn.yml` |

### 部署步骤

1. 将本仓库 `./gradlew build` 生成的插件 JAR 放入服务端 `plugins/`（Paper 示例：`paper/build/libs/squaremap-paper-mc*.jar`）。
2. 首次启动后，用 `deploy/magicflower/config.yml` 覆盖 `plugins/squaremap/config.yml`。
3. （可选）复制 `deploy/magicflower/lang-zh-cn.yml` 到 `plugins/squaremap/locale/`。
4. 在服务端控制台或游戏内执行 `/squaremap reload`，或重启服务器。
5. 确认防火墙/安全组已放行 **25566**（squaremap 插件 HTTP 端口）。
6. 浏览器访问：**http://magicflower.org:25566**

若使用 Nginx 反代到 HTTPS（例如 `https://map.magicflower.org`），需同步修改 `config.yml` 中的 `settings.web-address`。

### 关键配置项

```yaml
settings:
  web-address: http://magicflower.org:25566     # 对外展示的地图 URL
  internal-webserver:
    enabled: true
    bind: 0.0.0.0
    port: 25566                                 # squaremap 插件监听的 HTTP 端口
  web-directory:
    auto-update: false                          # 二开后建议关闭，避免覆盖自定义前端
```

## 前端开发（给 Web 背景的同学）

如果你熟悉 `npm install` / `npm run dev`，这里是对照表：

| 你熟悉的 | 本项目 |
|----------|--------|
| `npm install` | `bun install`（在 `web/` 目录） |
| `npm run dev` | `bun run dev` |
| `npm run build` | `bun run build` |
| 独立后端 API | 无；数据来自 squaremap 插件的 HTTP 服务 |

### 推荐：本机前端 + 远程 Magic Flower 数据

**UI 在本机热更新，瓦片和玩家数据来自线上 squaremap 插件。**

| 地址 | 用途 |
|------|------|
| `http://localhost:5173` | 本机 `npm run dev`，浏览器打开这个看前端 |
| `http://magicflower.org:25566` | 远程 squaremap 插件 HTTP 服务（Vite 自动代理到这里取数据） |

```bash
cd web
npm install
npm run dev
```

浏览器访问：**http://localhost:5173**

仓库已包含 `web/.env.development`：

```env
MAP_SERVER_URL=http://magicflower.org:25566
PROXY_MAX_SOCKETS=3
PROXY_TIMEOUT_MS=60000
VITE_MAP_TILE_CONCURRENCY=3
VITE_MAP_TICK_MS=3000
```

`npm run dev` 时会自动加载，将 `/tiles`、`/images` 等请求代理到远程插件。为避免远程服并发连接过多导致 `ETIMEDOUT`，默认做了两层限流：

| 变量 | 作用 | 默认（开发） |
|------|------|----------------|
| `PROXY_MAX_SOCKETS` | Vite 代理到远程的最大并发 TCP 连接 | 3 |
| `PROXY_TIMEOUT_MS` | 代理超时（毫秒） | 60000 |
| `VITE_MAP_TILE_CONCURRENCY` | 浏览器同时请求的 PNG 瓦片数 | 3 |
| `VITE_MAP_TICK_MS` | 玩家/标记/瓦片刷新主循环间隔（毫秒） | 3000（生产构建默认 1000） |

修改后需重启 `npm run dev`。

### 可选：本机前端 + 本机 MC 测试服

若用 `./gradlew :squaremap-paper:runServer` 在本机起测试服（squaremap 默认 HTTP 端口 **8080**），把 `web/.env.development` 改为：

```env
MAP_SERVER_URL=http://localhost:8080
```

### 前置条件

- Node.js + npm（或 Bun）
- 远程 Magic Flower 服 squaremap 插件已启动，且 **25566** 可从你的电脑访问

### 初始化

```bash
cd web
npm install             # 或 bun install
```

### 构建并部署到服务器

前端不会单独 `npm start` 跑在生产环境，而是打包进插件 JAR：

```bash
# 在项目根目录
./gradlew build
```

Gradle 会自动执行 `bun run build`，将 `web/` 构建产物打入插件。把新的 JAR 上传到服务端 `plugins/` 并重启（或 reload）即可。

### 预览构建结果

```bash
cd web
bun run build
bun run preview
```

注意：`preview` 仍需要代理或插件 HTTP 服务提供 `/tiles` 数据，否则地图是空的。日常开发请用 `npm run dev`。

## 完整本地环境（可选）

若要在本机同时跑 MC 服务端 + 热更新前端，需要：

- JDK 21+
- Bun
- `./gradlew :squaremap-paper:runServer`（会启动测试用 Paper 服，并自动代理 Vite 开发服务器；此时插件 HTTP 默认为 **8080**）

这对纯前端二开通常不是必须的；`npm run dev` + 代理到 `magicflower.org:25566` 即可。

## 图层双数据源

地图左上角可勾选的 overlay 图层来自两个来源，前端自动合并：

| 来源 | 数据文件 | 维护方式 |
|------|----------|----------|
| **squaremap 原生** | `tiles/{世界名}/markers.json` | 游戏服插件生成（出生点、世界边界、第三方 API 等） |
| **蔚然 GIS** | `web/data/weiran-gis/` | 本仓库本地 JSON（图层定义 + 实例），随前端打包部署 |

图层控件分为 **Square Map**（服务端，已过滤误传的 Weiran GIS 图层）与 **Weiran GIS**（本地配置）两组。逻辑见 `web/src/js/util/markerLayers.js`。

### 编辑蔚然 GIS 图层

蔚然 GIS 拆成两个 JSON：**图层类型定义**（样式）与 **实例**（具体点位）。运行时由 `web/src/js/util/weiranGis.js` 合并。

#### 1. 图层定义 `web/data/weiran-gis/layers.json`

每种图层类型只定义一次，包含显示样式（字体、颜色透明度、背景、圆角、阴影、icon 等）：

```json
{
  "version": 1,
  "layers": {
    "admin-names": {
      "name": "行政区名",
      "control": true,
      "hide": false,
      "order": 0,
      "z_index": 10,
      "version": 1,
      "markerType": "label",
      "style": {
        "fontSize": 13,
        "fontWeight": 600,
        "color": "#ffffff",
        "colorOpacity": 1,
        "backgroundColor": "#000000",
        "backgroundOpacity": 0.55,
        "borderRadius": 6,
        "paddingX": 10,
        "paddingY": 4,
        "boxShadow": "0 1px 4px rgba(0,0,0,0.35)",
        "icon": null,
        "iconSize": 16,
        "gap": 6
      }
    }
  }
}
```

修改样式后递增对应图层的 `version`，前端会自动重绘该层。

#### 2. 实例 `web/data/weiran-gis/instances.json`

同一类型可有多个实例（例如多个行政区名）。`layer` 字段引用 `layers.json` 中的 key；`dimension` 表示 POI 所在维度，与 squaremap 世界 `type` 对齐：

| `dimension` | 含义 |
|-------------|------|
| `normal` | 主世界（默认） |
| `nether` | 下界 |
| `the_end` | 末地 |

```json
{
  "version": 1,
  "instances": [
    {
      "layer": "admin-names",
      "id": "yinggu",
      "dimension": "normal",
      "point": { "x": 3094, "z": 3324 },
      "text": "樱谷"
    }
  ]
}
```

单个实例可通过 `icon` 覆盖图层默认 icon。增删实例或修改 `dimension` 后，递增 `instances.json` 的 `version` 即可。

前端左上角图层控件分为 **Square Map**（服务端）与 **Weiran GIS**（本地 JSON）两个分组展示。

修改 marker 内容后，递增 `timestamp` 可强制前端刷新该层。`npm run dev` 下保存 JSON 后刷新页面即可；上线需重新 `./gradlew build` 并部署插件。

### 前端版本号

设置面板「关于」中显示的版本为 **大版本.小版本.编辑版本**（例如 `1.2.22`），由两处配置组成：

| 段 | 配置文件 | 何时递增 |
|----|----------|----------|
| **大版本** | `web/data/weiran-gis/release.json` → `major` | 架构级或不兼容变更 |
| **小版本** | `web/data/weiran-gis/release.json` → `minor` | 每次向服主交付新的前端构建（`npm run build` 并发包） |
| **编辑版本** | `web/data/weiran-gis/instances.json` → 顶层 `version` | 增删改 GIS 实例或实例字段（如 `dimension`、坐标、名称） |

`release.json` 示例：

```json
{
  "major": 1,
  "minor": 2
}
```

当前示例：`instances.json` 的 `version` 为 `22`，`release.json` 为 `1.2`，故界面显示 **1.2.22**。

交付记录（蔚然 GIS 前端）：

| 小版本 | 说明 |
|--------|------|
| `1.0` | 首次前端构建交付 |
| `1.1` | 第二次前端构建交付 |
| `1.2` | 当前：搜索、组织树、皮肤、维度等 |

仅改 GIS 数据、未发新包时：只递增 `instances.json` 的 `version`（编辑版本 +1），小版本不变，例如仍为 `1.2.23`。

## 目录结构

```
web/                  # 前端（Leaflet 地图 UI）
web/data/weiran-gis/   # 蔚然 GIS：layers.json、instances.json、release.json（大/小版本）
common/               # 插件核心（渲染、HTTP 服务、配置）
paper/                # Paper 平台插件入口
deploy/magicflower/   # Magic Flower 服务端配置示例
RAW_README.md         # 原 squaremap 项目 README
```

## 许可证

继承原项目 [MIT License](./LICENSE)。Leaflet 使用 BSD-2-Clause。
