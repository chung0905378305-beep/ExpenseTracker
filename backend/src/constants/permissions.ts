// 所有可用权限名及其说明
// Plan.features 使用的是中文名，权限管理用的是英文 key，后台会做映射
export const ALL_PERMISSIONS = [
  { key: 'ai_analysis',        label: 'AI分析',          desc: '月度洞察报告 + 对话式问答' },
  { key: 'asset_management',   label: '资产管理',        desc: '股票/加密/基金/黄金/房产/现金外币' },
  { key: 'subscription_tracking', label: '订阅管理',     desc: '周期/一次性/手动提醒订阅跟踪' },
  { key: 'budget',             label: '预算管理',        desc: '月度总预算 + 分类预算' },
  { key: 'auto_entry',         label: '自动记账',        desc: '规则导入/CSV/OCR/邮箱拉取' },
  { key: 'data_export',        label: '数据导出',        desc: 'CSV 导出 + JSON 完整备份' },
  { key: 'multi_currency',     label: '多币种',          desc: '切换基础币种 + 汇率折算' },
  { key: 'global_search',      label: '全局搜索',        desc: '跨字段全文搜索' },
  { key: 'custom_categories',  label: '自定义分类',      desc: '大类子类增删改 + 关键词归类' },
  { key: 'recurring_rules',    label: '循环记账',        desc: '日/周/月/年自动生成' },
] as const;

export type PermissionKey = typeof ALL_PERMISSIONS[number]['key'];

// 中文字段 ↔ 英文 key 的映射
export const PERMISSION_LABEL_MAP: Record<string, string> = {};
for (const p of ALL_PERMISSIONS) {
  PERMISSION_LABEL_MAP[p.label] = p.key;
}

export const PERMISSION_KEYS = ALL_PERMISSIONS.map(p => p.key) as PermissionKey[];
