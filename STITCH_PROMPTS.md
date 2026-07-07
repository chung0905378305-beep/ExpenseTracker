# 记账本 iOS App — Stitch Prompt 包

> 配合 `DESIGN.md` v3.4（排版冻结）+ `REQUIREMENTS.md` v3.4（功能需求）使用。
> 目标：把已定稿的设计系统转成 **Google Stitch** 能直接消费的 prompt，逐个生成组件与页面，保证风格一致。

---

## 0. 使用方法（先读）

1. **准备参考图**：把 `preview-ios.html` 在浏览器打开截图，上传到 Stitch 作为 **风格参考图**（style reference）。它能让 Stitch 直接学走我们的四主题配色和精致扁平质感。
2. **生成顺序（关键，避免风格漂移）**：
   - 第一步：跑 **§1 全局风格设定**（只跑一次，定基调）
   - 第二步：跑 **§2 组件 prompts**（先锁基础组件视觉语言）
   - 第三步：跑 **§3 页面 prompts**（每屏末尾都加 "Use the same visual language as the components/pages already generated above."）
   - 第四步（可选）：跑 **§4 主题变体**，把其他三套主题各生成一版
3. **一致性技巧**：Stitch 支持"在同一画布继续生成"。每新出一屏，prompt 末尾都引用上屏，例如 "same clean iOS style, same rounded cards radius 16, same accent #007AFF as the screens above"。
4. **导出**：Stitch → Figma 拿标注稿；或直接导出 HTML/CSS/React 静态骨架。⚠️ 复杂交互（底部 Sheet 拖拽、仪表盘重排、自定义数字键盘）Stitch 只出静态外观，动态逻辑需手写。

> 基准主题默认用 **A 原生 iOS 简洁 · Light**。下面 token 为该基准的精确值，prompt 里直接引用。

### 基准 Token（A 原生 iOS 简洁 · Light）
```
Background (canvas) : #F2F2F7
Card / Surface       : #FFFFFF
Primary text         : #1C1C1E
Secondary text       : #8E8E93
Separator / hairline : #E5E5EA
Accent (primary)     : #007AFF
Accent soft (tint)   : rgba(0,122,255,0.10)
Expense red          : #FF3B30   | soft rgba(255,59,48,0.10)
Income green         : #34C759   | soft rgba(52,199,89,0.10)
Shadow card          : 0 1px 2px rgba(0,0,0,0.05)
Radius  card 16 / row 12 / icon 14 / button 12 / sheet-top 20
Font                 : SF Pro / PingFang SC
Side padding         : 16pt   |  Spacing unit 4pt
```
### Dark 变体（同主题暗色）
```
Background : #000000   Card : #1C1C1E   Primary text : #FFFFFF
Secondary  : #98989F   Separator : #38383A   Accent : #0A84FF
Expense red: #FF453A   Income green: #30D158
```

---

## 1. 全局风格设定（首条，只跑一次）

> 用途：建立整站基调，让后续组件/页面都继承这套语言。

```
Design a personal finance iOS app UI in a "refined flat" native iOS style.
Visual language:
- No large navigation titles; content sits right below the status bar.
- Generous whitespace, content side padding 16pt, inner cards with 16pt corner radius and a very subtle shadow (0 1px 2px rgba(0,0,0,0.05)).
- System font SF Pro / PingFang SC. Amounts use tabular-nums, right-aligned.
- Color: canvas #F2F2F7, cards #FFFFFF, primary text #1C1C1E, secondary text #8E8E93.
  Accent #007AFF. Expense = red #FF3B30, Income = green #34C759 (consistent across all screens).
- Touch targets >= 44pt. Light theme. Clean, calm, premium but minimal.
- Reference the uploaded screenshot for exact look & feel.
```

---

## 2. 组件 Prompts（先锁视觉语言）

### 2.1 按钮 Button
```
A set of iOS buttons on a white card (radius 16):
1. Primary button: full-width, height 50pt, background accent #007AFF, white text "保存", radius 12, subtle shadow.
2. Secondary button: white background, 1pt border #E5E5EA, primary text, radius 12.
3. Tertiary / text button: no background, accent-colored text only.
4. A gradient "Save" bar (green gradient #34C759 -> #30D158) used as the bottom row of the numeric keypad.
Same refined flat iOS style, light theme as defined.
```

### 2.2 卡片 Card
```
A generic content card for an iOS app: white background, 16pt corner radius, 1pt hairline border #E5E5EA or subtle shadow (0 1px 2px rgba(0,0,0,0.05)), inner padding 16pt. Show one card with a title row (17pt semibold) + a secondary line (13pt gray) + a value on the right. Clean spacing.
```

### 2.3 账单列表行 List Row
```
One transaction list row, 44pt+ tall, left-to-right:
- Left: 40pt rounded-square (radius 14) tinted background (rgba(0,122,255,0.10)) containing a SF Symbol icon (e.g. fork.knife for dining).
- Middle: category name (17pt, #1C1C1E) on top, note/subtitle (13pt, #8E8E93) below; under the note a tiny secondary line showing date + account (12pt, #8E8E93).
- Right: amount in tabular-nums, right-aligned; expense shown in red #FF3B30 with "-" prefix, income in green #34C759 with "+"; large transactions 17pt.
Hairline separator #E5E5EA between rows. Same iOS style.
```

### 2.4 金额数字键盘 Numeric Keypad
```
A custom iOS numeric keypad pinned to the bottom of the screen:
- 4 rows: [1][2][3] / [4][5][6] / [7][8][9] / [.][0][⌫].
- Each key height >= 48pt, key background white or #F2F2F7, number 28pt #1C1C1E, backspace as a SF Symbol.
- Last row is a full-width green gradient bar (#34C759 -> #30D158) with white text "保存 / Save".
- Keys separated by thin 1px lines, no heavy borders. Clean, premium.
```

### 2.5 底部 Sheet Bottom Sheet
```
A bottom sheet modal (used for transfer / add subscription / add holding / edit transaction):
- Slides up from bottom, top corners 20pt radius, a small drag handle (rounded pill) centered at top.
- Background behind is blurred/dimmed.
- Inside: a title (17pt semibold) + vertically stacked fields, each with a left label (15pt gray) and a right value/input (17pt), separated by hairlines.
- A primary "保存" button pinned at the bottom.
Refined flat iOS, light theme.
```

### 2.6 分段控件 Segmented Control
```
An iOS segmented control (pill style): a rounded container (radius 12) with 3 segments, e.g. 支出 / 收入 / 转账. Selected segment has white background + subtle shadow + accent text #007AFF; unselected segments transparent with secondary gray text. Width fits content, centered. Also show a 4-segment time-range control: 周 / 本月 / 本年 / 自定义, same style.
```

### 2.7 标签 / Badge / 盈亏 Pill
```
Three small pill components:
1. A status badge (e.g. "待确认" red #FF3B30 text on soft red tint background, radius 8, 12pt).
2. A category tag pill (gray text on #F2F2F7, radius 8).
3. A profit/loss pill: green tint background with "+12.4%" green text, or red tint with "-3.1%" red text, radius 10, 13pt, used for holding P&L.
Consistent soft-tint backgrounds via color-mix.
```

### 2.8 空状态 Empty State
```
An empty state centered in the screen: a large 56pt SF Symbol icon (e.g. tray) in secondary gray, a title (17pt semibold #1C1C1E) like "还没有记账", a subtitle (13pt #8E8E93) "点击右上角 + 记录第一笔", and a primary button below "记第一笔". Generous vertical spacing, calm.
```

### 2.9 汇总 Hero 卡 Summary Hero Card
```
A monthly summary card (white, radius 16):
- Top: a big balance hero number (34pt, tabular-nums, #1C1C1E) labeled "本月结余 / Balance" (13pt gray).
- Below: two mini blocks side by side — left "收入 +12,800.00" on green soft-tint (#34C759 tint), right "支出 -8,642.30" on red soft-tint (#FF3B30 tint). Each mini block radius 12, padding 12pt, label 12pt gray + value 15pt semibold in the semantic color.
Clear hierarchy: balance dominates, income/expense are secondary.
```

### 2.10 可拖拽仪表盘卡 Dashboard Card (draggable)
```
A dashboard card used in the stats screen, white radius 16, with:
- A small icon square (radius 10, accent tint) at top-left + a card title (15pt semibold).
- A drag handle (⠿) at top-right in secondary gray.
- Body content varies: for the "订阅 / Subscription" card show "最近续费 7-15 · 共 4 笔"; for trend card show a line chart with gradient area fill; for category card show horizontal bars.
Show 3 such cards stacked to imply a draggable reorderable list. Same iOS style.
```

---

## 3. 页面 Prompts（每屏末尾加一致性句）

### 3.1 记一笔（首屏）Add Transaction — first screen
```
Design the "Add Transaction" first screen of a personal finance iOS app. NO navigation bar; content starts below the status bar.
Vertical stack:
1. A segmented control (pill) with 支出 / 收入 / 转账; selected = 支出, accent blue.
2. Amount hero: small "¥" + a 48pt right-aligned tabular number in expense red, e.g. "-38.50".
3. A 5-column grid of category icons: each 40pt rounded-square (radius 14) tinted background + SF Symbol; first item selected with accent border + slight upward shift. 13pt label below. (Rows: 餐饮/交通/购物/娱乐/居住 then 医疗/工资/其他/+/...).
4. Three fields: 账户 "招商银行 ›" / 日期 "今天 ›" / 备注·标签 "添加 ›"; hairline separators, label left 15pt gray, value right 17pt.
5. Custom numeric keypad pinned to bottom (see keypad component), last row green gradient "保存".
Top-right: a small text link "明细 ›" to enter the list.
Style: native iOS clean, light theme, canvas #F2F2F7, cards white radius 16, accent #007AFF, expense red #FF3B30, income green #34C759, SF Pro font, subtle shadow. Same visual language as the components above.
```

### 3.2 明细 Transaction List
```
Design the "明细 / Transactions" screen:
- Top-left: a back link "‹ 记一笔".
- A search bar pinned at top (rounded, #F2F2F7 background, placeholder "搜索交易、商户…", search SF Symbol).
- A summary hero card (see Summary Hero Card component): balance big number + income/expense mini blocks.
- Below: grouped list. A group header row shows month "7月" on left and "支出 8,642 · 收入 12,800" on right (13pt gray).
- Under it, transaction rows (see List Row component), hairline separated, expense red / income green amounts.
- A floating "+" button bottom-right (accent circle, 50pt, shadow).
Same iOS clean style as above.
```

### 3.3 交易详情 Transaction Detail
```
Design a "交易详情 / Transaction Detail" screen (pushed from a list row):
- Top-left back "‹ 明细".
- A white card (radius 16) showing all fields: large category icon + category name (17pt) + type tag pill; amount hero (34pt, red for expense); then rows: 账户 / 日期 / 备注·标签 / 关联退款 (if any) each label-left value-right with hairlines.
- A "编辑" button (secondary) and a "删除" button (red text) at the bottom.
Refined flat iOS, light theme, consistent with the list screen above.
```

### 3.4 统计仪表盘 Stats Dashboard
```
Design the "统计 / Statistics" dashboard screen:
- Top: a time-range segmented control with 4 segments: 周 / 本月 / 本年 / 自定义; default = 周 highlighted.
- Below: a vertically scrollable stack of draggable dashboard cards (see Dashboard Card component):
  1. "我的订阅" card first (icon square + "最近续费 7-15 · 共 4 笔", tappable to subscription page).
  2. Monthly summary card (balance hero + income/expense mini).
  3. Category proportion card (horizontal bars, tap to drill down).
  4. Trend card (line chart with gradient area fill, last 6 months).
  5. Yearly card (bar chart, 12 months).
- Each card has a drag handle ⠿ top-right; cards reorderable.
Refined flat iOS, light theme, accent #007AFF, charts use theme colors. Consistent with components above.
```

### 3.5 资产 Assets
```
Design the "资产 / Assets" screen, single scrolling page:
- Top: a net-worth hero card with accent gradient background (blue gradient) + 1pt border: big "净资产 / Net Worth" number (34pt white tabular) + a small secondary line "较上月 +2.3%".
- Section title "账户" then grouped rows: asset-type accounts (现金/银行卡/支付宝, each showing balance right) and a "负债" subsection (信用卡/贷款 showing amounts, red-tinted).
- Section title "持仓" then holding rows: each shows name/code, market value right, and a P&L pill (green/red soft tint, e.g. "+12.4%").
- A refresh button top-right.
- Tap a holding row to sell.
Refined flat iOS, light theme, consistent above. Net-worth card uses accent gradient; P&L uses semantic soft tints.
```

### 3.6 我的 Profile / Settings
```
Design the "我的 / Mine" screen:
- Top: an account card with accent gradient background + border: if logged out show "登录 / 注册 ›"; if logged in show an avatar (circle, accent ring) + nickname + "会员 · 到期 2026-08-01".
- A "会员订阅" group card: rows 会员状态 / 管理订阅 / 恢复购买, plus an "升级到会员版" CTA button (accent gradient).
- Group "外观": 主题 (原生简洁 ›) / 隐藏金额 (toggle on).
- Group "账户与安全": App 锁 (面容 ID, toggle) / 基础币种 (CNY ›).
- Group "数据与同步": iCloud 同步 (toggle on) / 备份与导入 › / 订阅卡片 (显示, toggle).
- Group "AI 与扩展": AI 接口 (未设置 ›) / 邮箱记账 (未绑定 ›) / 行情源 (Yahoo ›).
- Group "关于": 隐私政策 › / 版本 1.0.0.
Each group is a white card radius 16, rows separated by hairlines, label left / value or toggle right. Refined flat iOS, light theme, consistent above.
```

### 3.7 订阅页 Subscriptions
```
Design the "订阅 / Subscriptions" screen (entered from the stats subscription card):
- Top: a segmented control [即将续费 | 全部]; default 即将续费.
- A "即将续费" highlight card: list upcoming renewals sorted by days left, each showing service name + price + "还有 N 天" + a small badge.
- Below: a list of subscriptions, each row with name + price + a mode tag pill (周期自动 / 一次性 / 手动).
- Floating "+" bottom-right to add.
Refined flat iOS, light theme, consistent above. Mode tags use the pill component.
```

---

## 4. 主题变体 Prompts（可选，生成其他三套）

> 在 §1 风格基础上，换配色/氛围各生成一版。每套都保持「支出红/收入绿语义一致」「圆角/间距一致」。

### B 温暖亲和 Warm & Friendly
```
Re-skin the finance app to a warm, friendly theme:
- Canvas warm off-white #FBF7F2, cards #FFFFFF.
- Accent = warm coral #FF6B6B (or amber #FF9F0A). Accent soft tint warm.
- Larger radii: card 20, icon 16, button 14 (softer, rounder).
- Add subtle soft gradients on hero cards; rounded, approachable, illustration-friendly.
- Keep expense red #FF3B30 / income green #34C759 semantics. Same layout & spacing as the screens above.
```

### C 专业金融 Professional Finance
```
Re-skin to a professional finance tool look:
- Canvas pure white #FFFFFF, cards #FFFFFF with 1pt #E5E5EA border (sharper, higher density).
- Accent = deep indigo #1A56DB (or refined gold #C9A227 for premium).
- Tighter row height, more data per screen, restrained color use, no gradients.
- Keep expense red / income green. Same layout, but visually denser and more "serious". Consistent with screens above.
```

### D 极简黑白 Minimal Mono
```
Re-skin to a minimal black/white/gray theme:
- Canvas #FFFFFF (light) or #000000 (dark), cards same with hairline.
- NO chromatic accent: accent = near-black #111111; all interactive elements monochrome.
- Expense still distinguishable by red #FF3B30 / income green #34C759 ONLY on amounts (rest strictly grayscale) for high contrast, premium minimal feel.
- Same layout & radii as screens above.
```

---

## 5. 限制与提示（交给开发前须知）

- **Stitch 产出 = UI 设计稿/静态骨架**：底部 Sheet 拖拽、仪表盘卡片重排、自定义数字键盘、iCloud 同步、行情自动刷新等**交互逻辑 Stitch 不生成**，需手写（衔接我们计划中由我产出的 iOS 源码）。
- **一致性靠顺序**：务必先跑 §1 定调 → §2 组件 → §3 页面，且每屏引用上屏，否则 Stitch 容易逐屏漂移。
- **参考图优先**：上传 `preview-ios.html` 截图比纯文字更稳，能让四主题配色一次到位。
- **导出后核对 WCAG AA**：Stitch 生成的对比度不一定达标，导出后用我们 §9.3 的标准（正文≥4.5:1）复核红/绿在浅暗下的可读性。
- **四主题建议分 4 次生成**：一次只做一个主题变体最可控，别让 Stitch 一次混出四套。

---
*UI Designer · 2026-07-07 · 配合 DESIGN.md v3.4 + REQUIREMENTS.md v3.4*
