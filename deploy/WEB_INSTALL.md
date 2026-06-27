# 地图前端安装说明（服主）

本文说明如何安装 **weiran-squaremap** 二开版的前端构建产物。该更新**只替换地图网页 UI**，**不需要**更换 squaremap 插件 JAR，**不需要**在服务器上安装 Node.js 或运行 `npm`。

开发者会在本地执行 `npm run build`，将 `common/build/web/` 目录打成压缩包（如 `web.zip`）通过 IM 等方式发送给你。

---

## 你需要提前具备什么

- 服务器已安装并运行过 **squaremap** 插件（`plugins/` 目录下有 squaremap 的 `.jar` 文件）。
- 插件至少成功启动过一次，已生成 `plugins/squaremap/` 数据目录。
- 地图 HTTP 服务正常（Magic Flower 服默认：**http://magicflower.org:25566**）。

若尚未配置 squaremap，请先完成插件安装，并将 `deploy/magicflower/config.yml` 复制到 `plugins/squaremap/config.yml`（详见项目根目录 `README.md`）。

---

## 安装后的目录结构

以下 `<服务器根目录>` 指 Minecraft 服务端根目录（含 `plugins/`、`world/` 等文件夹的那一层）：

```
<服务器根目录>/
└── plugins/
    ├── squaremap-paper-*.jar          ← 已有，本次不用动
    └── squaremap/
        ├── config.yml                 ← 已有，一般不用动
        ├── locale/                    ← 已有，不用动
        ├── data/                      ← 插件生成的数据，不要删
        └── web/                       ← ★ 本次更新目标目录
            ├── index.html
            ├── favicon.ico
            ├── assets/
            │   ├── index-*.css
            │   └── index-*.js
            ├── images/
            │   └── …
            └── tiles/                 ← ★ 插件生成的地图瓦片，务必保留
                └── <世界名>/
                    ├── *.png
                    └── markers.json
```

**重要**：`web/tiles/` 是插件运行时生成的地图图片与 JSON，**不在**开发者发来的压缩包内。更新时请**覆盖 UI 文件**，**不要删除整个 `web/` 目录**。

---

## 压缩包内应该有什么

解压 `web.zip` 后，应看到类似结构（文件名中的 hash 每次构建可能不同）：

```
index.html
favicon.ico
assets/
├── index-xxxxxxxx.css
└── index-xxxxxxxx.js
images/
├── grass.png
├── health/
├── armor/
└── …
```

若压缩包内多包了一层 `web/` 文件夹，请使用 **`web/` 里面的内容**，而不是外层空目录。

`.js.map` 文件为调试用途，有则一并复制，没有也不影响运行。

---

## 安装步骤

### 1. 备份（推荐）

在更新前备份当前前端，便于回滚：

**Linux / macOS：**

```bash
cd <服务器根目录>
cp -a plugins/squaremap/web plugins/squaremap/web.bak
```

**Windows：** 复制整个 `plugins\squaremap\web` 文件夹，粘贴为 `web.bak`。

### 2. 解压压缩包

将收到的 `web.zip` 解压到临时目录，确认根目录下有 `index.html` 和 `assets/` 文件夹。

### 3. 复制文件到目标位置

把以下内容**覆盖**到 `plugins/squaremap/web/`：

- `index.html`
- `favicon.ico`
- `assets/`（整个文件夹）
- `images/`（整个文件夹）

**Linux / macOS 示例**（假设 zip 解压在 `/tmp/web-build`）：

```bash
cd <服务器根目录>
cp -r /tmp/web-build/index.html /tmp/web-build/favicon.ico \
      /tmp/web-build/assets /tmp/web-build/images \
      plugins/squaremap/web/
```

**Windows：** 在资源管理器中进入 `plugins\squaremap\web\`，将上述文件和文件夹拖入，选择**替换/覆盖**。

### 4. 确认 `tiles/` 仍在

更新后检查：

```bash
ls plugins/squaremap/web/tiles
```

应能看到世界名子目录（如 `world/`）。若 `tiles/` 不存在或为空，说明可能被误删，需要从备份恢复或等待插件重新渲染地图。

### 5. 重载插件

在游戏内或服务器控制台执行：

```
/squaremap reload
```

或重启 Minecraft 服务器。

### 6. 验证

1. 浏览器打开地图地址：**http://magicflower.org:25566**
2. 使用 **Ctrl+F5**（Mac：**Cmd+Shift+R**）强制刷新，避免浏览器缓存旧页面。
3. 确认：
   - 地图瓦片正常显示；
   - 左上角图层控件中有 **Weiran GIS** 分组及自定义图层；
   - 自定义标注（行政区、单位等）位置与样式正确。

---

## 配置说明（可选）

若 `plugins/squaremap/config.yml` 中尚未设置，建议确认以下项（与 `deploy/magicflower/config.yml` 一致）：

```yaml
settings:
  web-directory:
    path: web
    auto-update: false    # 设为 false，避免插件启动时用 JAR 内旧前端覆盖你安装的二开 UI
  internal-webserver:
    enabled: true
    port: 25566
```

修改配置后同样需要 `/squaremap reload` 或重启服务器。

---

## 常见错误

| 错误做法 | 后果 |
|----------|------|
| 解压到 `plugins/web/` | 路径错误，插件无法加载前端 |
| 解压到 `plugins/squaremap/`（与 `web/` 同级） | 路径错误 |
| 删除整个 `plugins/squaremap/web/` 后再解压 | **`tiles/` 丢失，地图需重新渲染** |
| 只上传 `assets/` 或只上传 `index.html` | 页面白屏、样式或脚本缺失 |
| 未执行 reload 或未强制刷新浏览器 | 看起来仍是旧版 |

---

## 回滚

若更新后出现问题，恢复备份即可：

**Linux / macOS：**

```bash
cd <服务器根目录>
rm -rf plugins/squaremap/web
mv plugins/squaremap/web.bak plugins/squaremap/web
```

然后在控制台执行 `/squaremap reload`。

---

## 一句话总结

将压缩包里的 `index.html`、`favicon.ico`、`assets/`、`images/` **覆盖到** `plugins/squaremap/web/`，**保留原有 `tiles/` 文件夹**，执行 `/squaremap reload`，浏览器强制刷新后访问地图。

如有问题，请联系提供压缩包的开发者。
