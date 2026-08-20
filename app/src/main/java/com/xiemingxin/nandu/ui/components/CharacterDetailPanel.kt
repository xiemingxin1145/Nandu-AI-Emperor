package com.xiemingxin.nandu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiemingxin.nandu.game.AppointmentSystem
import com.xiemingxin.nandu.game.ArtResourceRegistry
import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.Officer
import com.xiemingxin.nandu.game.OfficerIntel
import com.xiemingxin.nandu.game.OfficerStatus
import com.xiemingxin.nandu.game.PropResourceRegistry
import com.xiemingxin.nandu.game.SkillEffects
import com.xiemingxin.nandu.game.VisualAssetV3
import com.xiemingxin.nandu.game.commandLimit
import com.xiemingxin.nandu.agent.CharacterAgentState
import com.xiemingxin.nandu.agent.CharacterAgentSystem
import com.xiemingxin.nandu.game.profile

private val PanelGold = Color(0xFFC9A227)
private val PanelCream = Color(0xFFE8DCC0)
private val PanelDark = Color(0xF21A1208)
private val PanelSub = Color(0xFF9A8862)

/** Stage 3 + Visual Integration V3 officer details. */
@Composable
fun CharacterDetailPanelWithState(
    officer: Officer,
    gameState: GameState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    agentState: CharacterAgentState? = gameState.characterAgentStates[officer.id]
) {
    val cityName = gameState.cities.find { it.id == officer.currentCityId }?.name ?: officer.currentCityId
    val isLeadPending = officer.id in gameState.talentLeads &&
        officer.status in setOf(OfficerStatus.HIDDEN, OfficerStatus.SOLDIER, OfficerStatus.WANDERING)
    val isFullyRevealed = officer.status in setOf(
        OfficerStatus.IN_COURT,
        OfficerStatus.DEPLOYED,
        OfficerStatus.DISMISSED,
        OfficerStatus.DECEASED
    ) || isLeadPending
    val currentRole = AppointmentSystem.currentRole(gameState, officer.id)

    if (!isFullyRevealed && officer.status == OfficerStatus.HIDDEN) {
        HiddenOfficerHintPanel(officer = officer, cityName = cityName, onDismiss = onDismiss, modifier = modifier)
    } else {
        CharacterDetailPanel(
            officer = officer,
            cityName = cityName,
            currentRole = currentRole,
            loyaltyRisk = AppointmentSystem.loyaltyRiskLabel(officer),
            onDismiss = onDismiss,
            modifier = modifier
        )
    }
}

@Composable
private fun HiddenOfficerHintPanel(
    officer: Officer,
    cityName: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PanelDark),
        border = BorderStroke(1.dp, PanelGold.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("？？？", color = PanelGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Text("X", color = PanelSub, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("此人尚未被发现", color = PanelCream, fontSize = 13.sp)
            Text("所在：$cityName 附近", color = PanelSub, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            Text("统率：未知 / 武力：未知 / 智略：未知", color = PanelSub, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            Text("← 在此城走访或寻访，可能获得更多线索", color = Color(0xFF8FB573), fontSize = 10.sp)
        }
    }
}

@Composable
fun CharacterDetailPanel(
    officer: Officer,
    cityName: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    currentRole: String = "",
    loyaltyRisk: String? = null
) {
    val p = officer.profile()
    val signatureProps = PropResourceRegistry.signaturePropsForOfficer(officer.id)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PanelDark),
        border = BorderStroke(1.dp, PanelGold.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                // The halfbody is now the visual anchor instead of a tiny 92dp portrait.
                AssetImage(
                    path = VisualAssetV3.halfbodyForOfficer(officer.id),
                    fallbackPath = ArtResourceRegistry.portraitForOfficer(officer.id),
                    contentDescription = officer.name,
                    contentScale = ContentScale.Fit,
                    placeholderText = officer.name.take(1),
                    modifier = Modifier
                        .width(140.dp)
                        .height(210.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF160F08))
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(officer.name, color = PanelGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Text("X", color = PanelSub, fontSize = 14.sp)
                        }
                    }
                    Text(p.rank + " · " + p.origin, color = PanelCream, fontSize = 13.sp)
                    Text("现驻 $cityName", color = PanelSub, fontSize = 11.sp)
                    if (currentRole.isNotBlank()) {
                        Text("职务：$currentRole", color = Color(0xFF8FB573), fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "忠：${OfficerIntel.loyaltyLabel(officer.loyalty)}",
                        color = PanelSub,
                        fontSize = 11.sp
                    )
                    Text(
                        "志：${OfficerIntel.ambitionLabel(p.ambition)}",
                        color = PanelSub,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    MiniStat("名望", OfficerIntel.fameLabel(p.fame))
                    Spacer(Modifier.height(5.dp))
                    MiniStat("资历", OfficerIntel.experienceLabel(p.experience))
                    Spacer(Modifier.height(5.dp))
                    MiniStat("可统兵", (officer.commandLimit() / 1000).toString() + "k")
                }
            }

            Spacer(Modifier.height(14.dp))
            StatBar("武", officer.force)
            StatBar("统", officer.command)
            StatBar("谋", officer.strategy)
            StatBar("政", officer.politics)
            StatBar("魅", p.charm)

            if (p.skills.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("专长", color = PanelGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                SkillTagRow(p.skills)
                Spacer(Modifier.height(4.dp))
                Text(SkillEffects.shortSummary(p.skills), color = PanelSub, fontSize = 10.sp)
            }

            if (signatureProps.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                PropShelf(
                    title = "象征物件",
                    props = signatureProps
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "人物页物件用于身份与叙事识别；兵权、资源和真实持有状态仍由游戏规则决定。",
                    color = PanelSub,
                    fontSize = 9.sp,
                    lineHeight = 14.sp
                )
            }

            Spacer(Modifier.height(12.dp))
            Text("评估", color = PanelGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                OfficerIntel.trustBrief(officer.loyalty, p.ambition),
                color = Color(0xFF8FB573),
                fontSize = 12.sp
            )
            if (loyaltyRisk != null) {
                Spacer(Modifier.height(6.dp))
                Text(loyaltyRisk, color = Color(0xFFE57373), fontSize = 11.sp)
            }
            if (officer.bio.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text("简介", color = PanelGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(officer.bio, color = PanelSub, fontSize = 10.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun StatBar(label: String, value: Int) {
    val frac = value.coerceIn(0, 100) / 100f
    val barColor = when {
        value >= 90 -> Color(0xFFD4AF37)
        value >= 75 -> Color(0xFF8FB573)
        value >= 55 -> Color(0xFF8A9BB5)
        else -> Color(0xFF8B7355)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = PanelSub, fontSize = 12.sp, modifier = Modifier.width(22.dp))
        Box(
            modifier = Modifier.weight(1f).height(14.dp)
                .clip(RoundedCornerShape(7.dp)).background(Color(0xFF2A2010))
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(frac).fillMaxHeight()
                    .clip(RoundedCornerShape(7.dp)).background(barColor)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            value.toString(),
            color = PanelCream,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(28.dp)
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(value, color = PanelGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(label, color = PanelSub, fontSize = 9.sp)
    }
}

@Composable
private fun SkillTagRow(skills: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        skills.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { skill ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF2E2210))
                            .border(BorderStroke(1.dp, PanelGold.copy(alpha = 0.4f)), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(skill, color = PanelGold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
