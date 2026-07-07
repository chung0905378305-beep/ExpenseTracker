// 数据库定时备份调度器
// 由 PM2 管理，每天凌晨 3 点自动备份
import { createBackup, cleanOldBackups } from './utils/backup';

const BACKUP_INTERVAL_MS = 24 * 60 * 60 * 1000; // 24 小时
const KEEP_COUNT = 30; // 保留最近 30 个备份

async function runBackup() {
  const ts = new Date().toISOString();
  try {
    const result = await createBackup();
    console.log(`[${ts}] ✅ 备份成功: ${result.name} (${(result.size / 1024).toFixed(1)} KB)`);
    const cleaned = cleanOldBackups(KEEP_COUNT);
    if (cleaned > 0) console.log(`[${ts}] 🧹 清理了 ${cleaned} 个旧备份`);
  } catch (err: any) {
    console.error(`[${ts}] ❌ 备份失败: ${err.message}`);
  }
}

// 启动后等 30 秒执行首次备份，然后每天一次
setTimeout(() => {
  runBackup();
  setInterval(runBackup, BACKUP_INTERVAL_MS);
}, 30000);

console.log('📦 备份调度器已启动，每天自动备份');
