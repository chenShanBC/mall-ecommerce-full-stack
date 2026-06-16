<template>
  <section class="grid stats-grid">
    <div class="glass stat-panel stat-panel--primary">
      <div class="panel-hd">
        <div>
          <b>{{ dashboard.headTitle }}</b>
          <p>{{ dashboard.headDesc }}</p>
        </div>
        <el-button link type="primary" @click="dashboard.action?.()">{{ dashboard.actionLabel }}</el-button>
      </div>
      <div class="stat-matrix">
        <button v-for="item in dashboard.metrics" :key="item.label" class="stat-box" type="button" @click="item.action?.()">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <em>{{ item.hint }}</em>
        </button>
      </div>
    </div>

    <div class="glass stat-panel stat-panel--side">
      <div class="panel-hd">
        <div>
          <b>{{ dashboard.sideTitle }}</b>
          <p>{{ dashboard.sideDesc }}</p>
        </div>
      </div>
      <div class="side-list">
        <div v-for="item in dashboard.sideItems" :key="item.label" class="side-item">
          <div>
            <span>{{ item.label }}</span>
            <p>{{ item.desc }}</p>
          </div>
          <strong :class="item.className">{{ item.value }}</strong>
        </div>
      </div>
    </div>
  </section>

  <section class="grid main-grid">
    <div class="glass panel">
      <div class="panel-hd">
        <div>
          <b>{{ dashboard.chartTitle }}</b>
          <p>{{ dashboard.chartDesc }}</p>
        </div>
      </div>
      <BaseEChart class="chart lg" :option="dashboard.chartOption" />
    </div>
    <div class="glass panel">
      <div class="panel-hd">
        <div>
          <b>{{ dashboard.auxChartTitle }}</b>
          <p>{{ dashboard.auxChartDesc }}</p>
        </div>
        <el-button v-if="dashboard.auxAction" link type="primary" @click="dashboard.auxAction()">{{ dashboard.auxActionLabel }}</el-button>
      </div>
      <BaseEChart class="chart" :option="dashboard.auxChartOption" />
    </div>
  </section>

  <section class="grid three-grid">
    <div class="glass panel">
      <div class="panel-hd">
        <div>
          <b>{{ dashboard.listTitle }}</b>
          <p>{{ dashboard.listDesc }}</p>
        </div>
        <el-button v-if="dashboard.listAction" link type="primary" @click="dashboard.listAction()">{{ dashboard.listActionLabel }}</el-button>
      </div>
      <div v-if="dashboard.listMode === 'todo'" class="todo-list">
        <div v-for="item in todos" :key="item.code" class="todo" @click="$emit('shortcut', item.path, item.queryKey, item.queryValue)">
          <div>
            <h4>{{ item.title }}</h4>
            <p>{{ item.description }}</p>
          </div>
          <span class="todo-badge">{{ item.count }}</span>
        </div>
      </div>
      <div v-else class="mini-list">
        <div v-for="item in dashboard.listItems" :key="item.label" class="mini-list__item">
          <div>
            <strong>{{ item.label }}</strong>
            <p>{{ item.desc }}</p>
          </div>
          <span>{{ item.value }}</span>
        </div>
      </div>
    </div>
    <div class="glass panel">
      <div class="panel-hd">
        <div>
          <b>{{ dashboard.quickTitle }}</b>
          <p>{{ dashboard.quickDesc }}</p>
        </div>
      </div>
      <div class="shortcut-grid">
        <button
          v-for="item in dashboard.shortcuts"
          :key="item.title"
          class="shortcut"
          type="button"
          @click="$emit('shortcut', item.path, item.queryKey, item.queryValue)"
        >
          <strong>{{ item.title }}</strong>
          <span>{{ item.description }}</span>
          <em>{{ item.count }}</em>
        </button>
      </div>
    </div>
    <div class="glass panel">
      <div class="panel-hd">
        <div>
          <b>{{ dashboard.tailTitle }}</b>
          <p>{{ dashboard.tailDesc }}</p>
        </div>
        <el-button v-if="dashboard.tailAction" link type="primary" @click="dashboard.tailAction()">{{ dashboard.tailActionLabel }}</el-button>
      </div>
      <BaseEChart class="chart" :option="dashboard.tailChartOption" />
    </div>
  </section>
</template>

<script setup>
import BaseEChart from '../BaseEChart.vue';

defineProps({
  dashboard: { type: Object, required: true },
  todos: { type: Array, default: () => [] },
});

defineEmits(['shortcut']);
</script>

<style scoped>
.grid{display:grid;gap:18px}.glass{border-radius:30px;background:rgba(255,255,255,.78);backdrop-filter:blur(18px);border:1px solid rgba(255,255,255,.86);box-shadow:0 24px 60px rgba(108,123,225,.12)}.stats-grid{grid-template-columns:minmax(0,1.7fr) minmax(320px,1fr)}.main-grid{grid-template-columns:minmax(0,1.7fr) minmax(320px,1fr)}.three-grid{grid-template-columns:repeat(3,minmax(0,1fr))}.stat-panel,.panel{padding:22px}.panel-hd{display:flex;align-items:flex-start;justify-content:space-between;gap:12px;margin-bottom:18px}.panel-hd b{font-size:18px;color:#2d3a64}.panel-hd p{margin:6px 0 0;font-size:13px;color:#94a3b8}.stat-matrix{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}.stat-box{border:none;padding:18px;border-radius:22px;text-align:left;background:linear-gradient(135deg,rgba(248,250,255,.98),rgba(236,242,255,.92));cursor:pointer}.stat-box span{font-size:12px;color:#94a3b8}.stat-box strong{display:block;margin-top:10px;font-size:30px;color:#24304f}.stat-box em{display:block;margin-top:8px;font-style:normal;font-size:12px;color:#64748b}.side-list{display:flex;flex-direction:column;gap:12px}.side-item{display:flex;align-items:center;justify-content:space-between;gap:14px;padding:16px 18px;border-radius:22px;background:linear-gradient(135deg,rgba(248,250,255,.98),rgba(236,242,255,.92))}.side-item span{font-size:15px;font-weight:700;color:#334155}.side-item p{margin:6px 0 0;font-size:12px;color:#94a3b8}.side-item strong{font-size:24px;color:#64748b}.side-item strong.highlight{color:#5b6cff}.chart{height:260px}.lg{height:320px}.todo-list,.mini-list{display:flex;flex-direction:column;gap:12px}.todo{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:16px 18px;border-radius:22px;background:linear-gradient(135deg,rgba(248,250,255,.96),rgba(239,244,255,.92));cursor:pointer}.todo h4{margin:0;font-size:15px;color:#334155}.todo p{margin:6px 0 0;font-size:12px;color:#94a3b8}.todo-badge{min-width:56px;padding:12px 14px;border-radius:18px;text-align:center;font-weight:800;background:rgba(91,108,255,.12);color:#4f46e5}.mini-list__item{display:flex;align-items:flex-start;justify-content:space-between;gap:12px;padding:16px 18px;border-radius:22px;background:linear-gradient(135deg,rgba(248,250,255,.96),rgba(239,244,255,.92))}.mini-list__item strong{display:block;font-size:15px;color:#334155}.mini-list__item p{margin:6px 0 0;font-size:12px;color:#94a3b8}.mini-list__item span{padding:8px 12px;border-radius:999px;background:rgba(91,108,255,.12);color:#4f46e5;font-weight:700}.shortcut-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}.shortcut{border:none;display:flex;flex-direction:column;align-items:flex-start;padding:18px;border-radius:24px;background:linear-gradient(135deg,rgba(248,250,255,.98),rgba(236,242,255,.94));cursor:pointer}.shortcut strong{font-size:15px;color:#334155}.shortcut span{margin-top:8px;min-height:36px;font-size:12px;color:#94a3b8;text-align:left}.shortcut em{margin-top:16px;font-style:normal;font-size:28px;font-weight:800;color:#5b6cff}@media (max-width:1280px){.main-grid,.stats-grid,.three-grid{grid-template-columns:1fr}}@media (max-width:768px){.stat-matrix,.shortcut-grid{grid-template-columns:1fr}.stat-panel,.panel{padding:18px}}
</style>
