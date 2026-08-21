# V1.6.2 正式入口 Smoke Test 验收矩阵

> 用途：记录 STAB-007 正式集成后的自动证据、真实资源阻塞和手机待验收结果。
>
> STAB-006 已合流；自动检查通过不等于真机点击通过，因此没有手机验证的入口一律保留 `DEVICE_REQUIRED`。

集成基线：STAB-001～006 + COURT-001 PR #54 + ROSTER-001 审计 PR #57 + 最终入口修复。
预验收分支：`integration/v1.6.2-preacceptance`。
构建版本：`V1.6.1 / versionCode 28`，只用于集成验证，不作为新升级包交付。
状态来源：`GameState`、对应规则系统、已注册资源、实际 APK，禁止用静态演示数据替代。

## 结果定义

- `PASS`：该入口的全部正式验收条件已经真实完成；静态检查和 JVM 单测不能代替手机交互。
- `BLOCKED`：已存在客观资源缺口或代码缺陷，当前不能进入正式验收。
- `DEVICE_REQUIRED`：代码、资源或自动回归已有相应证据，但该入口仍未由真实 Android 手机点击、播放或执行系统返回。
- `AUTO` / `STATIC`：只描述自动证据，不是最终验收结果。
- `N/A`：该入口不应存在空白状态；若出现，视为失败。

**正式验收汇总：PASS 0 / BLOCKED 1 / DEVICE_REQUIRED 48，共 49 项。**

自动回归、APK 拆包和源码守卫单独留在“自动测试情况”列；任何未经过手机验收的交互入口都不得冒充 `PASS`。

## 启动、序章与身份

| 编号 | 正式入口 | 入口位置 | 前置条件 | 期望页面与状态来源 | 是否允许空状态 | 返回路径 | 自动测试情况 | 真机待验项 | 正式验收结果 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| MENU-01 | 主菜单 | 冷启动 | 无 | `MainMenuScreen`；静态背景 + `VideoResourceRegistry.menu_loop` 动画 | N/A | 系统返回 / 退出按钮 | `AUTO`：51 条 V3 APK 拆包、统一 Media3 路径 | 冷启动无黑屏；背景视频可解码；主菜单布局适配 | `DEVICE_REQUIRED` |
| MENU-02 | 新游戏 | 主菜单“开辟新局” | 无 | 必须进入 `PrologueScreen`，不得直接跳过玩家穿越身份交代 | N/A | 序章按钮；退出重启 | `STATIC`：`showPrologue = true` | 点击后确实进入第一幕，连续点击不重入 | `DEVICE_REQUIRED` |
| MENU-03 | 继续游戏 | 主菜单“旧梦回溯” | 存档状态需分别测试有 / 无 | 进入皇宫主页，读取当前 `EmperorViewModel` 状态 | 无存档时需有明确说明或初始化行为 | 底部导航 / 重启 | `STATIC` | 首次安装与已存档状态分别验证；不得误覆盖存档 | `DEVICE_REQUIRED` |
| MENU-04 | 主菜单设置 | 主菜单“世事设置” | 无 | 应进入 `SettingsScreen`，显示模型、音频与存档配置 | N/A | 设置左上返回 -> 主菜单 | `AUTO`：主菜单/设置路由优先级与返回路径已有 JVM 回归 | 修复后点击进入并返回；见风险 `RISK-001` | `DEVICE_REQUIRED` |
| MENU-05 | 天命绘卷 | 主菜单“天命绘卷” | V3 资源打包成功 | `VideoGalleryOverlay`；51 条视频按注册表分组 | 51 条资源缺失时必须明确错误，不得黑屏 | 页面“返回” -> 主菜单 | `AUTO`：51 条 H.264/yuv420p/无音轨 APK 检查 | 随机播放序章、战场、角色视频，切换后不串音 | `DEVICE_REQUIRED` |
| PRO-01 | 序章第一幕 | 新游戏 | 六幕配置与对应旁白存在 | 山河将倾；`PrologueAct.ACT_1` 与 `prologue_act1_shanhejiangqing.m4a` | N/A | 下一幕 / 左滑 / 跳过 | `AUTO`：六条实际旁白资源入 APK | 文字停留足够阅读；第一幕旁白可听清 | `DEVICE_REQUIRED` |
| PRO-02 | 序章第二幕 | 第一幕前进 | ACT_2 视频存在 | 靖康剧情；`AssetVideoSurface` + `videos/intro/V03_intro_cinematic.mp4` | 视频失败时允许静态 CG 保底，不能黑屏 | 上一幕 / 下一幕 / 跳过 | `AUTO`：Media3 单链路、视频 H.264、音轨剥离 | 视频实际可解码，旁白不被视频音轨覆盖 | `DEVICE_REQUIRED` |
| PRO-03 | 序章第三幕 | 第二幕前进 | ACT_3 旁白存在 | 南渡剧情与 `prologue_act3_nandu.m4a` | N/A | 上一幕 / 下一幕 / 跳过 | `AUTO`：旁白文件打包 | 文案、音量、背景与历史时间是否一致 | `DEVICE_REQUIRED` |
| PRO-04 | 序章第四幕 | 第三幕前进 | ACT_4 旁白存在 | 历史偏转说明与 `prologue_act4_lishipianzhuan.m4a` | N/A | 上一幕 / 下一幕 / 跳过 | `AUTO`：旁白文件打包 | 玩家是否理解可以改变历史 | `DEVICE_REQUIRED` |
| PRO-05 | 序章第五幕 | 第四幕前进 | ACT_5 旁白存在 | 玩家内心独白，明确不是原生赵构视角 | N/A | 上一幕 / 下一幕 / 跳过 | `AUTO`：旁白文件打包 | 明确表达穿越身份，旁白实际播放 | `DEVICE_REQUIRED` |
| PRO-06 | 序章第六幕 | 第五幕前进 | ACT_6 旁白存在 | 内侍引入建炎元年，应天府开局 | N/A | 上一幕 / 进入游戏 / 跳过 | `AUTO`：旁白、应天府地图与城池路径 | 第六幕结束正确进入皇宫；不闪退 | `DEVICE_REQUIRED` |
| PRO-07 | 跳过序章 | 任意一幕“跳过序章” | 序章正在播放 | 立刻进入皇宫并释放当前演出状态 | N/A | 底部导航 | `STATIC` | 连续点击无重复进入；旁白及时停止 | `DEVICE_REQUIRED` |
| PRO-08 | 前进、后退与滑动 | 序章上下幕按钮 / 左右滑 | 已进入序章 | 幕序严格 1→6，可回看上一幕；视频、旁白随幕切换 | N/A | 第六幕进入皇宫 | `STATIC`：六幕枚举及前后映射已检查 | 快速切换不叠音、不串画面、不自动秒过 | `DEVICE_REQUIRED` |

## 朝堂、宫殿与待办

| 编号 | 正式入口 | 入口位置 | 前置条件 | 期望页面与状态来源 | 是否允许空状态 | 返回路径 | 自动测试情况 | 真机待验项 | 正式验收结果 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| COURT-01 | 皇宫主页 | 底部“皇宫” | 已进入游戏 | `PalaceHallScreen`；八宫殿入口、真实任务数量、应天开局 | 无任务允许明确空状态 | 底部五标签 | `AUTO`：八宫殿数量与列表一致；首都状态一致 | 首屏不空白；宫殿卡片不被装饰层挤掉 | `DEVICE_REQUIRED` |
| COURT-02 | 朝会议政 | 底部“朝议” | 已进入游戏 | `EmperorMainScreen`；真实 `GameState`、前景正式官员、群像装饰 | 没有可见名臣时可显示合规普通朝官，不得造 Officer | 底部五标签 | `AUTO`：54 张素材校验、官员装饰不污染正式人物 | 群像层次正常；岳飞、秦桧、吴玠不错误实体出席 | `DEVICE_REQUIRED` |
| COURT-03 | 垂拱殿 | 皇宫宫殿卡片 | 任意局势 | 朝议待办来自 `PalaceTaskSystem`，前景人物经过出场门控 | 允许“本旬暂无待办 / 不是加载失败” | 左上返回 -> 皇宫 | `AUTO`：badge 与列表、真实 choice consequence | 待办不空白；群像头像与文案不穿帮 | `DEVICE_REQUIRED` |
| COURT-04 | 政事堂 | 皇宫宫殿卡片 | 任意局势 | 财政、贸易待办来自真实国库与贸易状态 | 允许明确空状态 | 左上返回 -> 皇宫 | `AUTO`：财政审计、贸易 choice 修改世界 | 点击后财政/贸易确实变化 | `DEVICE_REQUIRED` |
| COURT-05 | 枢密院 | 皇宫宫殿卡片 | 任意局势 | 军务、军报与战区态势来自真实军团 | 允许明确空状态 | 左上返回 -> 皇宫 | `AUTO`：军务后果；顺昌条件门控 | 异地将领不得瞬移到枢密院 | `DEVICE_REQUIRED` |
| COURT-06 | 文德殿 | 皇宫宫殿卡片 | 任意局势 | 人才举荐与试用来自实际 Officer / 线索 | 允许明确空状态 | 左上返回 -> 皇宫 | `AUTO`：人才 `field` choice 真实后果 | 隐藏 / 未登场人物不能被直接任命 | `DEVICE_REQUIRED` |
| COURT-07 | 御书房 | 皇宫宫殿卡片 | 任意局势 | 外交、奏疏待办来自真实事件与势力 | 允许明确空状态 | 左上返回 -> 皇宫 | `AUTO`：专属 choice 不掉进占位后果 | 不出现空白卡片与错位点击 | `DEVICE_REQUIRED` |
| COURT-08 | 皇城司 | 皇宫宫殿卡片 | 任意局势 | 谣言、密报来自真实 `rumors` 等状态 | 允许明确空状态 | 左上返回 -> 皇宫 | `AUTO`：专属 choice 修改世界 | 没有情报时显示原因；有情报时能处理 | `DEVICE_REQUIRED` |
| COURT-09 | 后苑 | 皇宫宫殿卡片 | 任意局势 | 仅在真实金军压力、财政压力、朝局变化下出现待办 | 允许明确空状态 | 左上返回 -> 皇宫 | `AUTO`：无机械 `turn % 4` 假待办；压力驱动测试 | 安静局势不假造事件；危局文案解释原因 | `DEVICE_REQUIRED` |
| COURT-10 | 太庙 | 皇宫宫殿卡片 | 任意局势 | 礼制、誓师来自实际国家状态与选择 | 允许明确空状态 | 左上返回 -> 皇宫 | `AUTO`：礼制 choice 修改世界 | 页面有内容或明确空状态 | `DEVICE_REQUIRED` |
| COURT-11 | 待办详情与选择 | 任一宫殿任务卡 | 实际任务存在 | `CourtCouncilSystem.sceneForTask`；所有 choice 产生 `GameState` 差异 | 无任务时只显示明确空状态 | 详情返回 -> 任务列表 -> 皇宫 | `AUTO`：8 宫殿 badge/list 一致、choice 分支与世界状态回写 | 连续点击不重复刷奖励；返回路径完整 | `DEVICE_REQUIRED` |

## 地图、人物、军务与历史

| 编号 | 正式入口 | 入口位置 | 前置条件 | 期望页面与状态来源 | 是否允许空状态 | 返回路径 | 自动测试情况 | 真机待验项 | 正式验收结果 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| MAP-01 | 天下地图 | 底部“山河” | 已进入游戏 | `MapData.nodes` + `GameState.cities`；应天是开局都城，图标与前线/商路/军旗来自真实状态 | N/A | 底部导航 | `AUTO`：16/16 地图图标、24 个注册别名、15 个重点城市、动态回退、4 张正式图层底图和 5 个安全装饰均校验真实文件及 APK 入包；杭州不是首都 | 缩放、拖动、图标点击；真实前线预警、商路节点和宋金军旗随状态显示 | `DEVICE_REQUIRED` |
| MAP-02 | 城池内景 | 地图城池 -> 进入 | 存在对应真实 City | `CityInteriorScreen`；人口、粮草、城防、Rumor 来自 City/GameState；已有专属图统一走 `images/city/` | 无传闻时允许明确说明 | 页面返回 -> 地图 | `AUTO`：31/31 已注册正式城市背景真实存在；应天、鄂州、扬州专属图片路径与 APK 校验；禁止误用 `images/cities/` | 应天、鄂州、扬州显示各自专属图；尚无专属图的 18 座开局城市需人工观察通用回退 | `DEVICE_REQUIRED` |
| MAP-03 | 建设与招募 | 城池内景按钮 | 资源、行动力、城池控制条件满足 | `EmperorViewModel.buildInCity/recruitInCity` 回写真实城市状态 | 条件不足应解释原因 | 页面返回 -> 地图 | `STATIC` | 钱粮、行动点、兵力变化；禁止假成功 | `DEVICE_REQUIRED` |
| PEOPLE-01 | 人物列表 | 军务 -> 人物列表 | 已进入游戏 | 当前 12 名真实 Officer；ROSTER 审计 35 人不应直接入库 | 无可见人物需明确空状态 | 页面返回 -> 军务 | `AUTO`：正式人物仍为 12，人像装饰非 Officer | 列表头像正确；俘虏、在野、未登场身份准确 | `DEVICE_REQUIRED` |
| PEOPLE-02 | 人物详情 | 人物列表点击 | 人物真实可见 | 官职、位置、生死、状态来自实时 Officer | 隐藏人物不得绕过入口 | 详情返回 -> 人物列表 | `AUTO`：STAB-006 人物生死/前置事件/城市归属门控回归 | 岳飞在襄阳、秦桧被俘、吴玠未进入核心视野 | `DEVICE_REQUIRED` |
| PEOPLE-03 | 任命与人才 | 文德殿 / 人物页 | 人物可发现、可任命且位置合法 | 任命写回 Officer / cityGovernors / cityGarrisons，不能只改文案 | 没有候选人允许明确空状态 | 返回原人物页 / 宫殿 | `AUTO`：STAB-006 人物状态与前置条件门控回归 | 在野 / 俘虏 / 死亡人物不可直接上任 | `DEVICE_REQUIRED` |
| GOV-01 | 国政总览 | 底部“国政” | 已进入游戏 | `StateScreen` 展示财政、城市、朝局、威胁等 `GameState` 数据 | N/A | 底部导航 | `STATIC` | 国库与地图、事件选择结果一致 | `DEVICE_REQUIRED` |
| GOV-02 | 财政 | 国政 / 政事堂 | 已进入游戏 | 金钱、粮草、财政待办和贸易后果来自 GameState | 无特殊议题允许明确空状态 | 底部导航 / 左上返回 | `AUTO`：财政与贸易 choice 回写 | 审计、贸易后数值可见且持久化 | `DEVICE_REQUIRED` |
| MIL-01 | 军务总览 | 底部“军务” | 已进入游戏 | `MilitaryScreenV4`；真实 army、city、粮草、士气 | 无军团时需明确提示 | 军务左上返回 -> 皇宫；Android 系统返回 -> 皇宫 | `AUTO`：军务返回已接真实路由；系统返回策略已有 JVM 回归 | 页面返回与 Android 系统返回；不得卡住 | `DEVICE_REQUIRED` |
| MIL-02 | 军团 | 军务列表 | 对应真实 Army 存在 | 阵营、位置、状态、路线、兵力来自 `GameState.armies` | 无军团时允许解释 | 返回军务 | `AUTO`：军团与行军现有 JVM 测试 | 不凭空造兵；金军不能出现不存在的将领 | `DEVICE_REQUIRED` |
| MIL-03 | 行军 | 军团 -> 调动 / 战役驰援 | 路线可达且补给足够 | `ArmyMovementSystem` 生成真实路线与 `MARCHING` | 不可行必须给出原因 | 返回军务 / 战区 | `AUTO`：STAB-003 驰援、粮草消耗、存档往返 | 多旬移动而非瞬移；路径与地图一致 | `DEVICE_REQUIRED` |
| MIL-04 | 战役与军报 | 军务 / 世界事件 | 真正敌军、战区、城市、时间条件满足 | 战区简报由 GameState 派生；决策进入 BattleDirectiveSystem | 条件不满足时不显示正式战役入口 | 战役返回 -> 来源页面 | `AUTO`：固守 / 驰援 / 再议与真实状态 | 兵力、将领、官职、日期不得写死 | `DEVICE_REQUIRED` |
| MIL-05 | 顺昌候选入口 | 皇宫条件性卡片 | `HistoricalBattleAvailability.forShunchang(state).available` | 历史窗口 + 金军威胁 + 城市归属 + 人物状态全部满足 | 不满足时直接隐藏，不是空白测试按钮 | 返回 -> 皇宫 | `AUTO`：1127 开局不出现；战役门控已有回归 | 金国提前衰弱、刘锜死亡 / 异地时不得出现 | `DEVICE_REQUIRED` |
| HIST-01 | 剧情事件与选项 | 朝议事件弹窗 | 前置、人物存活、地点、flags、城市归属满足 | `EventDirector` + Loader + EffectApplier；选择真实回写世界 | 无符合条件事件时无需伪造 | 选择结算后返回朝议 | `AUTO`：STAB-006 已合流；人物生死、前置事件、flags、城市归属、概率和效果回写均有 JVM 回归 | 金国战败、秦桧被俘、扬州易手等分支逐个验证 | `DEVICE_REQUIRED` |

## 音视频、模型、存档与系统返回

| 编号 | 正式入口 | 入口位置 | 前置条件 | 期望页面与状态来源 | 是否允许空状态 | 返回路径 | 自动测试情况 | 真机待验项 | 正式验收结果 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| MEDIA-01 | 剧情 CG | 剧情事件“播放过场 CG” | `CgResourceRegistry.videoFor` 返回真实资源 | `CgVideoDialog` -> `AssetVideoSurface`；单次播放、视频静音、静态图保底 | 解码失败显示真实错误与静态图 | Dialog“返回” -> 剧情事件 | `AUTO`：源码禁止 VideoView，CG 复用唯一 Media3 实现 | 连续开关不黑屏、不串音、不泄漏播放器 | `DEVICE_REQUIRED` |
| MEDIA-02 | V3 动态视频 | 序章 / 主菜单 / 天命绘卷 | V3 MP4 实际打包 | 51 条视频全部 H.264 + yuv420p，0 内嵌音轨 | 播放失败时静态画面保底 | 关闭 / 返回来源页 | `AUTO`：CI 转码、ffprobe、APK 内逐个 ffprobe | 至少抽测主菜单、ACT_2、角色、战场四类 | `DEVICE_REQUIRED` |
| MEDIA-03 | BGM | 主菜单、皇宫、地图、军务 | 已审核 8 首 OGG 实际进入 APK | 场景切换播放对应正式纯器乐，旧污染音频不可回流 | 禁止用旧音乐冒充；缺失视为发布阻塞 | 页面切换时平滑路由 | `BLOCKED`：8/8 已登记正式 BGM 文件未入仓库 / CI APK | 必须恢复验收通过的 8 首原始 OGG 后再真机试听 | `BLOCKED` |
| MEDIA-04 | 环境声与音效 | 朝堂、城池、地图、操作 | 对应 ambience / ui / sfx 资源存在 | `GameAudioPlayer` 分层路由，音量设置即时生效 | 缺少专用槽位可明确静默，不得循环提示词人声 | 场景切换自动停止 / 替换 | `STATIC`：环境、UI、SFX 资源已存在 | 古风环境柔和；不重复播读提示词 | `DEVICE_REQUIRED` |
| MEDIA-05 | 六幕旁白 | 序章 ACT_1～ACT_6 | 六条 m4a 真实打包 | 旁白独立于 BGM 和视频内嵌音轨 | N/A | 跳过 / 前后幕 | `AUTO`：6/6 实际文件和 APK 条目 | 每幕听见正确旁白，快进后旧旁白停止 | `DEVICE_REQUIRED` |
| AI-01 | AI 设置 | 皇宫设置 / 朝议设置 | 游戏中设置入口可用 | `SettingsScreen`：MOCK、OpenAI、OpenRouter、CUSTOM、音量与存档 | 未配置模型必须清楚显示 Mock / 错误 | 左上返回 -> 来源页面 | `AUTO`：主菜单和游戏中设置统一使用可测试路由，并支持返回 | 手机键盘、滚动、保存与返回 | `DEVICE_REQUIRED` |
| AI-02 | 自定义模型中转地址 | AI 设置 -> CUSTOM | 自定义 baseUrl / model 字段可编辑 | 支持 `/v1` 与完整 `/chat/completions`；设置以 `baseUrl\|model` 持久化 | 免鉴权服务 Key 允许为空 | 保存 -> 返回 | `STATIC`：Custom provider 与 URL 规范化路径已检查 | 用真实中转站测试连接；失败后自动回退本地规则 | `DEVICE_REQUIRED` |
| SAVE-01 | 存档导出 | AI 设置 -> 存档码 | 至少存在当前 GameState | 导出 `NANDU_SAVE_V1`，包括城市、军团、路线与军令记录 | 导出失败必须显示原因 | 设置返回 -> 来源页 | `AUTO`：STAB-003 军令 export/import 持久化 | 实际复制完整存档码到手机剪贴板 | `DEVICE_REQUIRED` |
| SAVE-02 | 存档导入 | AI 设置 -> 存档码 | 有合法导出内容 | `GameSaveCodec` 恢复真实世界状态；坏码不破坏旧状态 | 无存档码时显示明确提示 | 设置返回 -> 来源页 | `AUTO`：战役军令、行军、粮草往返测试 | 同手机 / 重装场景恢复并比对人物、城市、军团 | `DEVICE_REQUIRED` |
| NAV-01 | 页面可见返回按钮 | 设置、宫殿、人物、内景、CG、战役 | 页面已进入 | 点击后返回直接上一级，不丢失真实 GameState | N/A | 各页面左上 / Dialog 返回 | `AUTO`：军务返回已真实接线；设置/宫殿/人物/内景/战役分层返回策略已有 JVM 回归 | 全入口点击返回；不能回到空白页面 | `DEVICE_REQUIRED` |
| NAV-02 | Android 系统返回键 / 手势 | 所有二级页面与 Dialog | 真机启用返回手势 | 先关闭当前层，再回上一级；不会直接退出整个游戏 | N/A | 系统返回手势 | `AUTO`：统一 Compose `BackHandler` 与绘卷独立返回已接线；仍须真机手势验收 | 设置、宫殿、城池、CG、序章、绘卷逐个测试 | `DEVICE_REQUIRED` |

## 自动化命令与构建门禁

```bash
python3 scripts/check_v162_preacceptance.py
./gradlew :app:testDebugUnitTest --stacktrace
./scripts/prepare_android_video_assets.sh
./scripts/check_video_assets.sh
./gradlew :app:assembleDebug --stacktrace
python3 scripts/check_v162_preacceptance.py --apk app/build/outputs/apk/debug/app-debug.apk
```

两套 GitHub Actions 还必须核对固定开发签名：

`EC:1B:19:D8:FF:E5:19:83:A1:B3:3B:BE:DC:77:24:D9:D4:41:21:8B:7E:DD:6C:6E:20:62:CF:97:D3:84:38:75`

## 实际执行结果与剩余正式阻塞

1. STAB-006 已从正确 STAB-005 基线合流，事件门控、世界效果、年号切换及全部新增 JVM 测试均在同一集成线。
2. 主菜单设置、真实行在亡国判定、军务返回、Android 系统返回、天命绘卷返回和主菜单 Demo 文案已修复并加入自动守卫。
3. COURT-001 全部 54 张正式素材、应天府专属图、51 条 H.264/yuv420p/无内嵌音轨视频、6 条序章旁白继续由双 CI 和 APK 拆包逐项校验。
   `ART-HOTFIX-001` 额外校验 16 张地图图标、24 个注册引用、10 张现有装饰、5 个安全接线装饰、6 张地图底图和 31 张正式城市背景。
4. 文件库和仓库均未找到此前人工验收通过的原始 OGG/MP3；正式 BGM 真实状态是 **0/8**，`MEDIA-03` 保持 `BLOCKED`。
5. 其余 48 个入口没有可代替手机的真实点击/滑动/硬件解码/系统返回证据，因此全部保持 `DEVICE_REQUIRED`，不会因自动检查通过而伪造 `PASS`。
6. 在 8 首正式 BGM 回到仓库、真实 APK 入包并完成 49 项手机验收前，STAB-007 保持 `IN_PROGRESS`，里程碑保持 **6/8**，不得启动 STAB-008。
