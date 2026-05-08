import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/chat'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { guest: true }
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('../views/Chat.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/layout/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/admin/Dashboard.vue')
      },
      {
        path: 'documents',
        name: 'Documents',
        component: () => import('../views/admin/Documents.vue')
      },
      {
        path: 'audit',
        name: 'AuditLog',
        component: () => import('../views/admin/AuditLog.vue')
      },
      {
        path: 'config',
        name: 'Config',
        component: () => import('../views/admin/Config.vue')
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('../views/admin/Users.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if (to.meta.guest && token) {
    next('/chat')
  } else if (to.meta.requiresAdmin) {
    try {
      const raw = localStorage.getItem('user_info')
      const user = raw ? JSON.parse(raw) : null
      if (!user || user.role !== 'ADMIN') {
        next('/chat')
      } else {
        next()
      }
    } catch {
      next('/chat')
    }
  } else {
    next()
  }
})

export default router
