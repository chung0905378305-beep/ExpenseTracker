package com.expensetracker.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.ui.component.LoadingSkeletonCard
import com.expensetracker.app.ui.theme.Accent
import com.expensetracker.app.ui.theme.Border
import com.expensetracker.app.ui.theme.TextSecondary
import com.expensetracker.app.ui.theme.TextOnDark
import com.expensetracker.app.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onAIClick: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Column(modifier = Modifier.padding(padding)) {
                repeat(5) { LoadingSkeletonCard() }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Account card
                AccountCard(
                    memberInfo = uiState.memberInfo,
                    modifier = Modifier.padding(16.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 会员订阅
                SectionGroup(title = "会员订阅") {
                    ProfileRow(
                        icon = Icons.Default.Star,
                        label = "会员状态",
                        trailing = {
                            Text(
                                text = if (uiState.memberInfo?.isMember == true) "已订阅" else "免费版",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (uiState.memberInfo?.isMember == true) Color(0xFFF59E0B) else TextSecondary
                            )
                        }
                    )
                    ProfileRow(icon = Icons.Default.Settings, label = "管理订阅", onClick = {})
                    ProfileRow(icon = Icons.Default.CloudSync, label = "恢复购买", onClick = {})
                }

                // 外观
                SectionGroup(title = "外观") {
                    ProfileRow(icon = Icons.Default.Brush, label = "主题选择", trailing = {
                        Text("跟随系统", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    })
                    ProfileRow(
                        icon = Icons.Default.VisibilityOff,
                        label = "隐藏金额",
                        trailing = {
                            Switch(
                                checked = settings.hideAmount,
                                onCheckedChange = { viewModel.updateSetting { it.copy(hideAmount = !it.hideAmount) } },
                                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.surface, checkedTrackColor = MaterialTheme.colorScheme.primary)
                            )
                        }
                    )
                }

                // 账户与安全
                SectionGroup(title = "账户与安全") {
                    ProfileRow(
                        icon = Icons.Default.Lock,
                        label = "App锁",
                        trailing = {
                            Switch(
                                checked = settings.appLockEnabled,
                                onCheckedChange = { viewModel.updateSetting { it.copy(appLockEnabled = !it.appLockEnabled) } },
                                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.surface, checkedTrackColor = MaterialTheme.colorScheme.primary)
                            )
                        }
                    )
                    ProfileRow(
                        icon = Icons.Default.CurrencyExchange,
                        label = "币种",
                        trailing = {
                            Text("CNY (¥)", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    )
                }

                // 数据与同步
                SectionGroup(title = "数据与同步") {
                    ProfileRow(icon = Icons.Default.Backup, label = "备份数据", onClick = {})
                    ProfileRow(icon = Icons.Default.FileUpload, label = "导入CSV", onClick = {})
                    ProfileRow(icon = Icons.Default.FileDownload, label = "导出CSV", onClick = {})
                    ProfileRow(icon = Icons.Default.DataObject, label = "完整JSON备份", onClick = {})
                }

                // AI与扩展
                SectionGroup(title = "AI与扩展") {
                    ProfileRow(
                        icon = Icons.Default.SmartToy,
                        label = "AI助手",
                        onClick = onAIClick
                    )
                    ProfileRow(icon = Icons.Default.Settings, label = "API配置", onClick = {})
                    ProfileRow(icon = Icons.Default.Mail, label = "邮箱绑定", onClick = {})
                    ProfileRow(icon = Icons.Default.Security, label = "行情设置", onClick = {})
                }

                // 关于
                SectionGroup(title = "关于") {
                    ProfileRow(icon = Icons.Default.Policy, label = "隐私政策", onClick = {})
                    ProfileRow(
                        icon = Icons.Default.Settings,
                        label = "版本号",
                        trailing = { Text("1.0.0", style = MaterialTheme.typography.bodyMedium, color = TextSecondary) }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun AccountCard(
    memberInfo: com.expensetracker.app.ui.viewmodel.MemberInfo?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (memberInfo?.isMember == true) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .then(
                            if (memberInfo.avatar.isNotEmpty()) Modifier else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (memberInfo.avatar.isNotEmpty()) {
                        // Avatar image would go here
                    }
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(memberInfo.nickname, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("会员到期: ${memberInfo.expiryDate}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            } else {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp), tint = TextSecondary)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("登录 / 注册", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Text("同步数据到云端", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SectionGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = TextSecondary,
            modifier = Modifier.padding(vertical = 6.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun ProfileRow(
    icon: ImageVector,
    label: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
