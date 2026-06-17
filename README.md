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
```

`npm run dev` 时会自动加载，将 `/tiles`、`/images` 等请求代理到远程插件。无需在本机跑 MC 服。

修改代理目标后需重启 dev 进程。

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

## 目录结构

```
web/                  # 前端（Leaflet 地图 UI）
common/               # 插件核心（渲染、HTTP 服务、配置）
paper/                # Paper 平台插件入口
deploy/magicflower/   # Magic Flower 服务端配置示例
RAW_README.md         # 原 squaremap 项目 README
```

## 许可证

继承原项目 [MIT License](./LICENSE)。Leaflet 使用 BSD-2-Clause。
