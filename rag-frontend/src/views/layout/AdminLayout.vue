<template>
  <div class="admin-layout">
    <!-- Sidebar -->
    <div class="admin-sidebar">
      <div class="sidebar-logo">RAG 管理后台</div>
      <div class="sidebar-menu">
      <el-menu
        :default-active="activeMenu"
        router
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
    </div>

    <!-- Main Content -->
    <div class="admin-main">
      <header class="admin-topbar">
        <div class="topbar-left">
          <el-breadcrumb>
            <el-breadcrumb-item :to="{ path: '/admin/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="topbar-right">
          <span class="user-name">
            <el-icon><User /></el-icon>
            {{ nickname || username }}
          </span>
          <el-button class="back-btn" text @click="goToChat">
            <el-icon><ChatDotSquare /></el-icon>
            返回对话
          </el-button>
          <el-button class="logout-btn" text @click="handleLogout">
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
.admin-layout { display: flex; min-height: 100vh; }
.admin-sidebar {
  width: 220px; background: var(--color-sidebar-bg); border-right: 1px solid var(--color-border);
  display: flex; flex-direction: column; flex-shrink: 0;
}
.sidebar-logo {
  padding: 20px 20px 16px; font-size: 16px; font-weight: 700;
  color: var(--color-text); border-bottom: 1px solid var(--color-border);
}
.sidebar-menu { flex: 1; padding: 8px 0; }
.sidebar-menu .el-menu {
  border-right: none; background: transparent;
}
.sidebar-menu .el-menu-item {
  margin: 2px 8px; border-radius: var(--radius-sm); font-size: var(--font-size-base);
  color: var(--color-text-secondary); height: 40px; line-height: 40px;
}
.sidebar-menu .el-menu-item:hover { background: var(--color-sidebar-hover); color: var(--color-text); }
.sidebar-menu .el-menu-item.is-active {
  background: var(--color-sidebar-active-bg); color: var(--color-sidebar-active-text); font-weight: 600;
}
.admin-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.admin-topbar {
  height: 56px; display: flex; align-items: center; justify-content: space-between;
  padding: 0 var(--space-lg); background: var(--color-surface);
  border-bottom: 1px solid var(--color-border); flex-shrink: 0; box-shadow: var(--shadow-sm);
}
.topbar-left { display: flex; align-items: center; gap: var(--space-sm); }
.topbar-left .title { font-size: var(--font-size-lg); font-weight: 600; color: var(--color-text); }
.topbar-right { display: flex; align-items: center; gap: var(--space-md); }
.topbar-right .user-name { font-size: var(--font-size-sm); color: var(--color-text-secondary); }
.admin-content { flex: 1; overflow-y: auto; padding: var(--space-lg); }
.back-btn { font-size: var(--font-size-sm); color: var(--color-accent); cursor: pointer; text-decoration: none; }
.back-btn:hover { color: var(--color-accent-hover); }
.logout-btn { font-size: var(--font-size-sm); color: var(--color-text-secondary); cursor: pointer; }
.logout-btn:hover { color: var(--color-danger); }
</style>
