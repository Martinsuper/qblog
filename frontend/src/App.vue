<template>
  <el-config-provider :locale="zhCn">
    <router-view />
    <AgentDrawer />
  </el-config-provider>
</template>

<script setup>
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import AgentDrawer from '@/components/agent/AgentDrawer.vue'
</script>

<style>
/* 全局根样式：页面组件自带布局（知识库工作区 / 设置 / 管理后台均为独立布局） */
html, body, #app {
  min-height: 100vh;
  margin: 0;
  background: var(--bg-primary);
}
</style>
