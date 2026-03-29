// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录 / 注册' }
  },
  {
    path: '/',
    name: 'FrontLayout',
    component: () => import('@/layout/FrontLayout.vue'),
    redirect: '/home', // 访问根目录直接重定向到首页
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/front/Home.vue'),
        meta: { title: '首页发现' }
      },
      {
        path: 'movies',
        name: 'Movies',
        component: () => import('@/views/front/Movies.vue'),
        meta: { title: '电影资料库' }
      },
      {
        // 动态路由参数 id
        path: 'movie/:id',
        name: 'MovieDetail',
        component: () => import('@/views/front/MovieDetail.vue'),
        meta: { title: '影片详情与购票' }
      },
      {
        path: 'profile', // 我们在 FrontLayout.vue 中的个人中心跳转路径
        name: 'Profile',
        component: () => import('@/views/front/UserCenter.vue'),
        meta: { title: '个人中心' }
      },
      {
        path: 'orders', // 点击“我的影票”也跳转到这里（UserCenter内部默认就是订单Tab）
        name: 'Orders',
        component: () => import('@/views/front/UserCenter.vue'),
        meta: { title: '我的影票' }
      },
      {
        path: 'schedules',
        name: 'Schedules',
        component: () => import('@/views/front/Schedules.vue'),
        meta: { title: '场次与购票' }
      },
    ]
  }
]

const adminRoutes = [
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/admin/AdminLogin.vue'),
    meta: { title: '后台登录' }
  },
  {
    path: '/admin',
    name: 'AdminLayout',
    component: () => import('@/layout/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '运营数据大盘' }
      },
      {
        path: 'movies',
        name: 'AdminMovies',
        component: () => import('@/views/admin/Movies.vue'),
        meta: { title: '电影管理' }
      },
      {
        path: 'halls',
        name: 'AdminHalls',
        component: () => import('@/views/admin/Halls.vue'),
        meta: { title: '影厅管理' }
      },
      {
        path: 'schedules',
        name: 'AdminSchedules',
        component: () => import('@/views/admin/Schedules.vue'),
        meta: { title: '排片调度' }
      },
      {
        path: 'data-import',
        name: 'AdminDataCenter',
        component: () => import('@/views/admin/DataCenter.vue'),
        meta: { title: '数据中枢' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [...routes, ...adminRoutes]
})

// 路由白名单：不需要登录就能访问的路径
const whiteList = ['/login', '/admin/login', '/home', '/movies', '/schedules']

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 智慧观影` : '智慧观影系统'

  // 1. 如果是去后台登录页，直接放行 (最高优先级)
  if (to.path === '/admin/login') {
    return next()
  }

  // 2. 后台其他路由的拦截逻辑
  if (to.path.startsWith('/admin')) {
    const adminToken = localStorage.getItem('adminToken')
    if (!adminToken) {
      ElMessage.warning('请先登录管理后台')
      return next('/admin/login')
    }
    return next() // 后台验证通过，放行
  }

  // 3. 观众端前台路由拦截逻辑
  const userStore = useUserStore()

  if (userStore.token) {
    // 已登录用户想去登录页，直接踢回首页
    if (to.path === '/login') {
      return next({ path: '/' })
    } else {
      return next() // 已登录，去其他页面全部放行
    }
  } else {
    // 未登录用户
    // 判断当前路径是否在白名单中，或者是动态路由如 /movie/1
    const isWhiteListed = whiteList.includes(to.path) || to.path.startsWith('/movie/')

    if (isWhiteListed) {
      return next() // 在白名单，游客模式放行
    } else {
      ElMessage.warning('请先登录后再进行操作')
      return next(`/login?redirect=${to.path}`) // 不在白名单，拦截去登录页
    }
  }
})

export default router