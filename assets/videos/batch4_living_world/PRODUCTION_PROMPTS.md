# Batch4 Living World — Production Prompts

Project: 南渡无悔 / Nandu-AI-Emperor  
Branch: `art/video-batch4-living-world`  
Rule: **video-only MP4 (no audio track)**, H.264 preferred, 720p or 1080p landscape, Southern Song aesthetic.

Global negative prompt (append to every generation):

```
no subtitles, no text, no logo, no watermark, no Qing dynasty costume, no queue braid,
no Ming flying-fish robe, no Tang exaggerated dress, no Japanese architecture,
no Western palace, no modern buildings, no floating xianxia palace, no Hollywood magic VFX,
no neon, no cold blue cinematic grade, no modern film crew, no microphone, no camera
```

Post-process every clip:

```bash
ffmpeg -i input.mp4 -c:v libx264 -pix_fmt yuv420p -an -movflags +faststart output.mp4
```

---

## 1. B4_01_chuigong_empty_loop — 垂拱殿动态背景

**Duration:** 10–15s seamless loop  
**Camera:** slow push or subtle drift inside hall

Prompt:

```
Southern Song dynasty imperial audience hall (Chuigongdian), empty or nearly empty throne hall,
wooden pillars, lattice windows, palace lanterns gently swaying, silk curtains soft motion,
distant eunuch silhouette optional, soft daylight and dust motes, restrained Chinese historical realism,
inspired by Southern Song court painting, muted lacquer and ink tones, seamless loop friendly motion
```

---

## 2. B4_02_emperor_enter_hall — 皇帝入殿

**Duration:** 6–10s oneshot  
**Characters:** Zhao Gou (use repo portrait lock if available)

Prompt:

```
Southern Song emperor entering the Chuigong audience hall, officials standing in solemn rows and bowing,
no shouting, no theatrical long live the emperor, restrained ceremonial atmosphere,
robe movement, soft hall light, historical realism, no close lip-sync dialogue shots
```

---

## 3. B4_03_imperial_garden_loop — 御花园

**Duration:** 10–15s loop

Prompt:

```
Southern Song imperial garden, bamboo shadows, still pond with light ripples, covered corridor,
trees, distant palace maids or eunuchs walking slowly, quiet realistic atmosphere,
Jiangnan palace garden, soft natural light, no fantasy architecture
```

---

## 4. B4_04_yushu_study_loop — 御书房

**Duration:** 10–15s loop

Prompt:

```
Southern Song imperial study room, candle flame flicker, memorial scrolls on desk,
window lattice light shifting, paper pages slight movement, brush and inkstone on imperial desk,
quiet scholarly atmosphere, warm low light, historical realism
```

---

## 5. B4_05_linan_city_loop — 临安城

**Duration:** 10–15s loop

Prompt:

```
Southern Song Lin'an city riverside, canals, wooden boats, stone bridges, shop fronts,
pedestrians, carts, banners, busy but historical, not modern Hengdian tourism look,
mist optional, earth and ink color palette
```

---

## 6. B4_06_world_map_loop — 天下地图

**Duration:** 10–15s loop

Prompt:

```
Ancient Chinese painted strategic map of rivers and mountains, soft cloud shadows moving,
faint river shimmer, distant war smoke columns, subtle faction border glow, parchment and ink texture,
top-down or oblique map presentation for mobile strategy game, no modern UI text
```

---

## 7. B4_07_song_camp_loop — 宋军营寨

**Duration:** 10–15s loop

Prompt:

```
Southern Song military camp, banners, soldiers in Song-style armor, horses, campfires,
patrols walking, tents and wooden stockade, dusk or day, realistic historical military life,
no modern gear
```

---

## 8. B4_08_prewar_council — 战前军议

**Duration:** 10–15s oneshot  
**No fixed lip-sync faces**

Prompt:

```
Southern Song pre-battle war council, candlelit tent or hall, map table and sand table,
officer silhouettes reviewing reports, hands pointing at map, restrained tension,
no close-up talking mouths, no modern furniture
```

---

## 9. B4_09_become_zhaogou — 穿越成为赵构

**Duration:** 8–12s oneshot  
**Style:** first person / extreme close

Prompt:

```
First-person awakening as Southern Song emperor Zhao Gou, hand and dragon-robe sleeve close-up,
bronze mirror reflection of a face starting blurred then gradually clarifying into Zhao Gou likeness,
no modern light beam, no xianxia teleport VFX, no sci-fi portal, historical room interior only
```

Character lock: use existing `assets/characters` Zhao Gou design if image-to-video.

---

## 10. Prologue supplements (B4_10a–e)

Each 6–12s, video-only, connectable after existing intro pack.

### B4_10a 靖康陷落
```
Fall of Bianjing in Jingkang era, burning city silhouette, broken walls, smoke, distant Jin cavalry implication, tragic Southern Song historical tone, no gore close-ups
```

### B4_10b 二帝北狩
```
Two Song emperors taken north in winter, long captive procession on cold dusty road, subdued historical tragedy
```

### B4_10c 宋室南渡
```
Song court crossing the Yangtze southward, boats in river mist, refugees and imperial carriages, residual mountains and rivers
```

### B4_10d 康王即位
```
Kangwang Zhao Gou enthronement at Yingtian, solemn restrained ceremony, Southern Song court, no theatrical long-live-the-emperor chorus staging
```

### B4_10e 江南残山剩水
```
Jiangnan after the fall of the north, misty broken mountains and leftover waters, melancholic landscape painting motion, Southern Song ink atmosphere
```

---

## Delivery checklist per file

- [ ] H.264 (or dual-export H.264 primary)
- [ ] Audio stream removed (`-an`)
- [ ] No burned-in text
- [ ] Duration in target range
- [ ] Loop clips visually seamless
- [ ] Filename matches `manifest.json`
- [ ] Update clip `status` from `pending_generation` → `ready` and fill real duration/resolution after probe
