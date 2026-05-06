<template>
  <div class="admin-layout">
    <!-- Sidebar -->
    <div class="admin-sidebar">
      <div class="sidebar-logo">
        <h2>RAG 管理后台</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        style="border-right: none"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/admin/documents">
          <el-icon><Folder /></el-icon>
          <span>文档管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/audit">
          <el-icon><Tickets /></el-icon>
          <span>审计日志</span>
        </el-menu-item>
        <el-menu-item index="/admin/config">
          <el-icon><Setting /></el-icon>
          <span>系统配置</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
      </el-menu>
    </div>

    <!-- Main Content -->
    <div class="admin-main">
      <header class="admin-header">
        <div class="header-left">
          <el-breadcrumb>
            <el-breadcrumb-item :to="{ path: '/admin/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <span class="user-info">
            <el-icon><User /></el-icon>
            {{ nickname || username }}
          </span>
          <el-button text @click="goToChat">
            <el-icon><ChatDotSquare /></el-icon>
            返回对话
          </el-button>
          <el-button text @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            退出
          </el-button>
        </div>
      </header>
      <main class="admin-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  DataAnalysis, Folder, Tickets, Setting, User,
  ChatDotSquare, SwitchButton
} from '@element-plus/icons-vue'
import { useAuth } from '../../store/auth'

const router = useRouter()
const route = useRoute()
const { user, logout } = useAuth()
const { nickname, username } = user

const pageTitles = {
  '/admin/dashboard': '仪表盘',
  '/admin/documents': '文档管理',
  '/admin/audit': '审计日志',
  '/admin/config': '系统配置',
  '/admin/users': '用户管理'
}

const activeMenu = computed(() => route.path)
const currentPageTitle = computed(() => pageTitles[route.path] || '')

function goToChat() {
  router.push('/chat')
}

async function handleLogout() {
  logout()
  await router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.admin-sidebar {
  width: 220px;
  background: #304156;
  color: #bfcbd9;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.sidebar-logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.sidebar-logo h2 {
  font-size: 16px;
  color: #fff;
  margin: 0;
}

.admin-sidebar .el-menu {
  background: transparent;
  border-right: none;
}

.admin-sidebar .el-menu-item {
  color: #bfcbd9;
}

.admin-sidebar .el-menu-item:hover {
  background: rgba(255, 255, 255, 0.05);
}

.admin-sidebar .el-menu-item.is-active {
  color: #409eff;
  background: rgba(64, 158, 255, 0.1);
}

.admin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.admin-header {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-info {
  font-size: 14px;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-right: 12px;
}

.admin-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background: #f0f2f5;
}
</style>
