<template>
  <div class="app-container home">
    <el-card shadow="never" class="welcome-card">
      <h2>VQMS 电压质量管理系统</h2>
      <p class="intro">
        VQMS 面向变电站母线电压质量的采集、统计与可视化。系统从外部 AVC 数据源读取母线电压曲线，按日 / 月 / 年三个粒度统计电压合格率、超上限率、超下限率与超限时间，提供 Web 看板与 Excel 导出，支撑电压质量监测与考核。
      </p>
    </el-card>

    <el-row :gutter="16" class="func-row">
      <el-col :sm="24" :md="12">
        <el-card shadow="hover">
          <template #header>曲线监测</template>
          <ul>
            <li>母线 0 / 1 切换查看电压曲线</li>
            <li>叠加上下限（high_SV / low_SV）区间带</li>
            <li>任意时间范围回查</li>
          </ul>
        </el-card>
      </el-col>
      <el-col :sm="24" :md="12">
        <el-card shadow="hover">
          <template #header>合格率统计</template>
          <ul>
            <li>日 / 月 / 年 三粒度合格率</li>
            <li>超上限率、超下限率、超限时间</li>
            <li>ECharts 图表 + Excel 导出</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
// VQMS 首页：项目简介占位，后续接入电压质量仪表盘
</script>

<style scoped lang="scss">
.home {
  padding: 16px;
}
.welcome-card {
  margin-bottom: 16px;
}
.welcome-card h2 {
  margin: 0 0 12px 0;
}
.intro {
  line-height: 1.8;
  color: #666;
  margin: 0;
}
.func-row ul {
  line-height: 1.9;
  padding-left: 20px;
  color: #555;
  margin: 0;
}
</style>
