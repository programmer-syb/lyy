<template>
    <div class="front-layout">
        <header class="navbar" :class="{ 'navbar-scrolled': isScrolled }">
            <div class="nav-container">
                <div class="logo" @click="router.push('/')">
                    <el-icon :size="28" color="#6366f1">
                        <VideoCamera />
                    </el-icon>
                    <span class="logo-text">智慧观影</span>
                </div>

                <nav class="nav-links">
                    <router-link to="/home" class="nav-item" active-class="active">首页发现</router-link>
                    <router-link to="/movies" class="nav-item" active-class="active">电影资料库</router-link>
                    <router-link to="/schedules" class="nav-item" active-class="active">场次与购票</router-link>
                </nav>

                <div class="user-action">
                    <template v-if="userStore.token">
                        <el-dropdown trigger="click" @command="handleCommand">
                            <div class="avatar-wrapper">
                                <el-avatar :size="36"
                                    :src="userStore.userInfo.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
                                <span class="nickname">{{ userStore.userInfo.nickname || userStore.userInfo.username
                                    }}</span>
                            </div>
                            <template #dropdown>
                                <el-dropdown-menu>
                                    <el-dropdown-item command="profile"><el-icon>
                                            <User />
                                        </el-icon>个人中心</el-dropdown-item>
                                    <el-dropdown-item command="orders"><el-icon>
                                            <Ticket />
                                        </el-icon>我的影票</el-dropdown-item>
                                    <el-dropdown-item divided command="logout"><el-icon>
                                            <SwitchButton />
                                        </el-icon>退出登录</el-dropdown-item>
                                </el-dropdown-menu>
                            </template>
                        </el-dropdown>
                    </template>
                    <template v-else>
                        <el-button type="primary" round plain @click="router.push('/login')">登录 / 注册</el-button>
                    </template>
                </div>
            </div>
        </header>

        <main class="main-content">
            <router-view v-slot="{ Component }">
                <transition name="fade-slide" mode="out-in">
                    <component :is="Component" />
                </transition>
            </router-view>
        </main>
    </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { VideoCamera, User, Ticket, SwitchButton } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const isScrolled = ref(false)

// 监听页面滚动，为导航栏增加阴影效果
const handleScroll = () => {
    isScrolled.value = window.scrollY > 20
}

onMounted(() => {
    window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
    window.removeEventListener('scroll', handleScroll)
})

// 处理下拉菜单点击
const handleCommand = async (command) => {
    if (command === 'logout') {
        try {
            await request.post('/api/user/logout')
        } catch (e) { } // 忽略网络错误，强行清空前端
        userStore.logout()
        ElMessage.success('已退出登录')
        router.push('/login')
    } else if (command === 'profile') {
        router.push('/profile')
    } else if (command === 'orders') {
        router.push('/orders')
    }
}
</script>

<style scoped lang="scss">
.front-layout {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
}

/* 导航栏苹果风设计 */
.navbar {
    position: fixed;
    top: 0;
    width: 100%;
    height: 64px;
    z-index: 100;
    background: rgba(248, 250, 252, 0.6);
    backdrop-filter: blur(16px);
    -webkit-backdrop-filter: blur(16px);
    border-bottom: 1px solid transparent;
    transition: all 0.3s ease;

    &.navbar-scrolled {
        background: rgba(255, 255, 255, 0.85);
        border-bottom: 1px solid rgba(0, 0, 0, 0.05);
        box-shadow: 0 4px 20px -5px rgba(0, 0, 0, 0.05);
    }

    .nav-container {
        max-width: 1200px;
        margin: 0 auto;
        height: 100%;
        padding: 0 24px;
        display: flex;
        justify-content: space-between;
        align-items: center;
    }
}

.logo {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;

    .logo-text {
        font-size: 20px;
        font-weight: 700;
        letter-spacing: -0.5px;
        color: #0f172a;
    }
}

.nav-links {
    display: flex;
    gap: 32px;

    .nav-item {
        text-decoration: none;
        font-size: 15px;
        font-weight: 500;
        color: #64748b;
        padding: 6px 12px;
        border-radius: 8px;
        transition: all 0.2s ease;

        &:hover {
            color: #0f172a;
            background: rgba(0, 0, 0, 0.03);
        }

        &.active {
            color: #6366f1;
            font-weight: 600;
        }
    }
}

.user-action {
    .avatar-wrapper {
        display: flex;
        align-items: center;
        gap: 10px;
        cursor: pointer;
        padding: 4px 12px 4px 4px;
        border-radius: 24px;
        transition: background 0.2s ease;

        &:hover {
            background: rgba(0, 0, 0, 0.04);
        }

        .nickname {
            font-size: 14px;
            font-weight: 500;
            color: #334155;
        }
    }
}

/* 主内容区域，留出 navbar 的高度 */
.main-content {
    margin-top: 64px;
    flex: 1;
    max-width: 1200px;
    width: 100%;
    align-self: center;
    padding: 32px 24px;
}

/* 页面切换动画 */
.fade-slide-enter-active,
.fade-slide-leave-active {
    transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-slide-enter-from {
    opacity: 0;
    transform: translateY(10px);
}

.fade-slide-leave-to {
    opacity: 0;
    transform: translateY(-10px);
}
</style>