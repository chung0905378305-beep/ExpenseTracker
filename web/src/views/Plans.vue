<template>
  <div>
    <div class="toolbar">
      <button class="btn btn-primary" @click="openCreate">+ 新建套餐</button>
    </div>
    <div class="card">
      <table class="tbl">
        <thead>
          <tr>
            <th>名称</th>
            <th>价格</th>
            <th>周期</th>
            <th>权益</th>
            <th>状态</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in plans" :key="p.id">
            <td>{{ p.name }}</td>
            <td>¥{{ p.price }}</td>
            <td>{{ p.interval === 'month' ? '月' : '年' }}</td>
            <td class="feat">
              <span v-for="(k, i) in p.features" :key="k" class="tag-pill">{{ tagLabel(k) }}</span>
            </td>
            <td>
              <span class="toggle" :class="{ on: p.isActive }" @click="toggle(p)">
                <span class="toggle-knob"></span>
              </span>
            </td>
            <td>
              <button class="btn btn-sm" @click="openEdit(p)">编辑</button>
            </td>
          </tr>
          <tr v-if="!plans.length">
            <td colspan="6" class="muted">暂无套餐</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 新建/编辑弹窗 -->
    <div class="mask" v-if="form" @click.self="closeForm">
      <div class="dialog card">
        <div class="card-title">{{ form.id ? '编辑套餐' : '新建套餐' }}</div>
        <label>名称</label>
        <input class="input" v-model="form.name" />
        <label>描述</label>
        <input class="input" v-model="form.description" />
        <div class="grid2">
          <div>
            <label>价格</label>
            <input class="input" type="number" v-model.number="form.price" />
          </div>
          <div>
            <label>周期</label>
            <select class="select" v-model="form.interval">
              <option value="month">月</option>
              <option value="year">年</option>
            </select>
          </div>
        </div>

        <label>权益（勾选功能标签）</label>
        <div class="tag-grid">
          <template v-for="cat in tagCategories" :key="cat">
            <div class="tag-cat-label">{{ cat }}</div>
            <div class="tag-cat-group">
              <label
                v-for="t in tagsByCategory[cat]"
                :key="t.key"
                class="tag-option"
                :class="{ checked: selectedTags.has(t.key) }"
              >
                <input
                  type="checkbox"
                  :value="t.key"
                  :checked="selectedTags.has(t.key)"
                  @change="onTagToggle(t.key)"
                />
                <span>{{ t.label }}</span>
              </label>
            </div>
          </template>
        </div>

        <label>排序</label>
        <input class="input" type="number" v-model.number="form.sortOrder" />
        <div class="dialog-actions">
          <button class="btn" @click="closeForm">取消</button>
          <button class="btn btn-primary" @click="save">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import api from '@/api';

interface FeatureTag {
  key: string;
  label: string;
  category: string;
}

const plans = ref<any[]>([]);
const featureTags = ref<FeatureTag[]>([]);
const form = ref<any>(null);
const selectedTags = ref<Set<string>>(new Set());

const tagsByCategory = computed(() => {
  const map: Record<string, FeatureTag[]> = {};
  for (const t of featureTags.value) {
    (map[t.category] ||= []).push(t);
  }
  return map;
});

const tagCategories = computed(() => Object.keys(tagsByCategory.value));

function tagLabel(key: string) {
  return featureTags.value.find((t) => t.key === key)?.label || key;
}

function onTagToggle(key: string) {
  if (selectedTags.value.has(key)) {
    selectedTags.value.delete(key);
  } else {
    selectedTags.value.add(key);
  }
  // 触发响应式
  selectedTags.value = new Set(selectedTags.value);
}

async function load() {
  const [res1, res2] = await Promise.all([
    api.get('/plans'),
    api.get('/plans/feature-tags'),
  ]);
  plans.value = res1.data.data;
  featureTags.value = res2.data.data;
}

function openCreate() {
  form.value = { name: '', description: '', price: 0, interval: 'month', sortOrder: 0 };
  selectedTags.value = new Set();
}
function openEdit(p: any) {
  form.value = { ...p };
  selectedTags.value = new Set(p.features || []);
}
function closeForm() {
  form.value = null;
  selectedTags.value = new Set();
}

async function save() {
  const payload = {
    ...form.value,
    features: [...selectedTags.value],
  };
  if (form.value.id) {
    await api.patch('/plans/' + form.value.id, payload);
  } else {
    await api.post('/plans', payload);
  }
  closeForm();
  load();
}

async function toggle(p: any) {
  await api.patch('/plans/' + p.id + '/toggle');
  load();
}

onMounted(load);
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
.tbl {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.tbl th,
.tbl td {
  text-align: left;
  padding: 10px 8px;
  border-bottom: 1px solid var(--border);
}
.tbl th {
  color: var(--text-3);
  font-weight: 500;
}
.feat {
  color: var(--text-2);
  max-width: 360px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.tag-pill {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 11px;
  background: var(--bg);
  border: 1px solid var(--border);
  color: var(--text-2);
  white-space: nowrap;
}
.muted {
  color: var(--text-3);
  text-align: center;
  padding: 16px;
}

/* Toggle Switch */
.toggle {
  position: relative;
  display: inline-block;
  width: 40px;
  height: 22px;
  border-radius: 11px;
  background: #ccc;
  cursor: pointer;
  transition: background 0.2s;
}
.toggle.on {
  background: var(--primary, #007AFF);
}
.toggle-knob {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  transition: left 0.2s;
}
.toggle.on .toggle-knob {
  left: 20px;
}

/* Mask / Dialog */
.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
}
.dialog {
  width: 520px;
  max-width: 94vw;
  max-height: 85vh;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 6px;
}
label {
  font-size: 12px;
  color: var(--text-2);
  margin-top: 8px;
}
.grid2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
}

/* Tag Grid */
.tag-grid {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: 2px 8px;
  align-items: start;
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 8px;
  max-height: 260px;
  overflow-y: auto;
}
.tag-cat-label {
  grid-column: 1;
  font-size: 11px;
  color: var(--text-3);
  padding: 4px 0;
  font-weight: 500;
}
.tag-cat-group {
  grid-column: 2;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  padding: 2px 0;
}
.tag-option {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  border: 1px solid var(--border);
  cursor: pointer;
  transition: all 0.15s;
  user-select: none;
}
.tag-option input {
  display: none;
}
.tag-option.checked {
  background: var(--primary, #007AFF);
  color: #fff;
  border-color: var(--primary, #007AFF);
}
.tag-option:hover:not(.checked) {
  border-color: var(--primary, #007AFF);
}
</style>
