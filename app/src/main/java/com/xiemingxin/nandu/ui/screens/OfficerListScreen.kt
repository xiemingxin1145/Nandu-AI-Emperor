package com.xiemingxin.nandu.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiemingxin.nandu.game.*
import com.xiemingxin.nandu.ui.components.AssetImage
import com.xiemingxin.nandu.ui.components.AssetVideoSurface
import com.xiemingxin.nandu.ui.components.CharacterDetailPanel

private val BgDark    = Color(0xFF0E0A05)
private val CardDark  = Color(0xFF1A1208)
private val Gold      = Color(0xFFC9A227)
private val Cream     = Color(0xFFE8DCC0)
private val Sub       = Color(0xFF9A8862)
private val GreenSoft = Color(0xFF8FB573)
private val RedSoft   = Color(0xFFE57373)

/**
 * Stage 3 人才总览页面。
 * Visual Integration V3：可识别人物显示头像；有 V3 char_live 视频的人物在展开详情时自动播放动态立绘。
 */
@Composable
fun OfficerListScreen(
    gameState: GameState,
    onBack: () -> Unit
) {
    val tabs = listOf("朝廷", "已任职", "待征辟", "在野", "隐藏线索")
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedOfficer by remember { mutableStateOf<Officer?>(null) }

    val leadIds = gameState.talentLeads

    val courtOfficers = gameState.officers.filter { it.status == OfficerStatus.IN_COURT }
    val deployedOfficers = gameState.officers.filter { it.status == OfficerStatus.DEPLOYED }
    val leadOfficers = gameState.officers.filter {
        it.id in leadIds &&
            it.status in setOf(OfficerStatus.HIDDEN, OfficerStatus.SOLDIER, OfficerStatus.WANDERING)
    }
    val wanderingOfficers = gameState.officers.filter {
        it.status == OfficerStatus.WANDERING && it.id !in leadIds
    }
    val hiddenHintOfficers = gameState.officers.filter {
        it.status == OfficerStatus.HIDDEN && it.id !in leadIds
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "←",
                color = Gold,
                fontSize = 20.sp,
                modifier = Modifier.clickable { onBack() }.padding(end = 12.dp)
            )
            Text("人才总览", color = Gold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                "共 ${gameState.officers.count { it.status in setOf(OfficerStatus.IN_COURT, OfficerStatus.DEPLOYED) }} 人在朝/任职",
                color = Sub,
                fontSize = 12.sp
            )
        }

        HorizontalDivider(color = Gold.copy(alpha = 0.3f), thickness = 1.dp)

        LazyRow(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabs.indices.toList()) { idx ->
                val count = when (idx) {
                    0 -> courtOfficers.size
                    1 -> deployedOfficers.size
                    2 -> leadOfficers.size
                    3 -> wanderingOfficers.size
                    4 -> hiddenHintOfficers.size
                    else -> 0
                }
                FilterChip(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx; selectedOfficer = null },
                    label = { Text("${tabs[idx]}($count)", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Gold.copy(alpha = 0.2f),
                        selectedLabelColor = Gold,
                        containerColor = CardDark,
                        labelColor = Sub
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedTab == idx,
                        selectedBorderColor = Gold.copy(alpha = 0.5f),
                        borderColor = Sub.copy(alpha = 0.3f)
                    )
                )
            }
        }

        HorizontalDivider(color = Gold.copy(alpha = 0.2f), thickness = 0.5.dp)

        val listToShow: List<Officer> = when (selectedTab) {
            0 -> courtOfficers
            1 -> deployedOfficers
            2 -> leadOfficers
            3 -> wanderingOfficers
            4 -> hiddenHintOfficers
            else -> emptyList()
        }

        if (listToShow.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                Text(
                    when (selectedTab) {
                        0 -> "朝中尚无登册人才"
                        1 -> "当前无人任职各地"
                        2 -> "尚无待征辟人才线索"
                        3 -> "暂无在野已知人才"
                        4 -> "暂无隐藏线索记录"
                        else -> "无"
                    },
                    color = Sub,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(listToShow, key = { it.id }) { officer ->
                    OfficerRowCard(
                        officer = officer,
                        gameState = gameState,
                        isHidden = selectedTab == 4,
                        isLead = selectedTab == 2,
                        onClick = { selectedOfficer = if (selectedOfficer?.id == officer.id) null else officer }
                    )
                    if (selectedOfficer?.id == officer.id) {
                        val cityName = gameState.cities.find { it.id == officer.currentCityId }?.name
                            ?: officer.currentCityId
                        val role = AppointmentSystem.currentRole(gameState, officer.id)

                        if (selectedTab != 4) {
                            OfficerLivePortrait(officer = officer, cityName = cityName, role = role)
                        }

                        CharacterDetailPanel(
                            officer = officer,
                            cityName = cityName,
                            onDismiss = { selectedOfficer = null },
                            currentRole = role,
                            loyaltyRisk = AppointmentSystem.loyaltyRiskLabel(officer)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OfficerLivePortrait(officer: Officer, cityName: String, role: String) {
    val liveClip = VideoResourceRegistry.characterLiveFor(officer.id) ?: return

    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF120D08)),
        border = BorderStroke(1.dp, Gold.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(116.dp)
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
            ) {
                // 静态立绘垫底，HEVC 不兼容时视频自动消失并露出静态图。
                AssetImage(
                    path = VisualAssetV3.halfbodyForOfficer(officer.id),
                    fallbackPath = ArtResourceRegistry.portraitForOfficer(officer.id),
                    contentDescription = officer.name,
                    contentScale = ContentScale.Crop,
                    placeholderText = officer.name.take(1),
                    modifier = Modifier.fillMaxSize()
                )
                AssetVideoSurface(
                    path = liveClip.path,
                    loop = true,
                    muted = true,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("动态觐见 · ${officer.name}", color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("现驻：$cityName", color = Cream, fontSize = 11.sp)
                Text("职务：${role.ifBlank { "御前待命" }}", color = GreenSoft, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "人物动态素材已进入实际人物页；后续这里还会接固定音色、点击问候与朝议奏对。",
                    color = Sub,
                    fontSize = 9.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun OfficerRowCard(
    officer: Officer,
    gameState: GameState,
    isHidden: Boolean,
    isLead: Boolean,
    onClick: () -> Unit
) {
    val cityName = gameState.cities.find { it.id == officer.currentCityId }?.name ?: officer.currentCityId
    val role = AppointmentSystem.currentRole(gameState, officer.id)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(0.5.dp, if (isLead) Gold.copy(alpha = 0.6f) else Sub.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssetImage(
                path = if (isHidden) ArtResourceRegistry.Fallback.portrait
                    else VisualAssetV3.portraitForOfficer(officer.id),
                fallbackPath = ArtResourceRegistry.Fallback.portrait,
                contentDescription = if (isHidden) "未发现人才" else officer.name,
                contentScale = ContentScale.Crop,
                placeholderText = if (isHidden) "?" else officer.name.take(1),
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(9.dp))
            )
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (isHidden) {
                    Text("？？？", color = Sub, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("$cityName 附近 · 未发现", color = Sub.copy(alpha = 0.6f), fontSize = 11.sp)
                } else {
                    Text(
                        officer.name,
                        color = if (isLead) Gold else Cream,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(cityName, color = Sub, fontSize = 11.sp)
                        if (role.isNotBlank() && role != "无职" && role != "御前待命") {
                            Text("· $role", color = GreenSoft, fontSize = 11.sp)
                        }
                    }
                }
            }

            if (isHidden) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("统?  武?  谋?", color = Sub.copy(alpha = 0.5f), fontSize = 11.sp)
                }
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    StatMini("统", officer.command)
                    StatMini("武", officer.force)
                    StatMini("谋", officer.strategy)
                }
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    StatMini("政", officer.politics)
                    val loyaltyColor = when {
                        officer.loyalty >= 80 -> GreenSoft
                        officer.loyalty >= 50 -> Cream
                        else -> RedSoft
                    }
                    Text(
                        "忠 ${OfficerIntel.loyaltyLabel(officer.loyalty)}",
                        color = loyaltyColor,
                        fontSize = 10.sp
                    )
                    if (isLead) {
                        Text("待征辟", color = Gold, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMini(label: String, value: Int) {
    val color = when {
        value >= 90 -> Gold
        value >= 75 -> GreenSoft
        value >= 55 -> Cream
        else -> Sub
    }
    Text("$label $value", color = color, fontSize = 10.sp)
}
