<template>
    <div class="admin-layout">
        <aside class="sidebar" :class="{ 'is-collapse': isCollapse }">
            <div class="logo-zone">
                <el-icon :size="24" color="#6366f1">
                    <DataAnalysis />
                </el-icon>
                <span class="logo-text" v-show="!isCollapse">影院管理中枢</span>
            </div>

            <el-menu :default-active="route.path" class="admin-menu" :collapse="isCollapse" router
                background-color="transparent">
                <el-menu-item index="/admin/dashboard">
                    <el-icon>
                        <Odometer />
                    </el-icon>
                    <template #title>运营大盘</template>
                </el-menu-item>

                <el-menu-item index="/admin/movies">
                    <el-icon>
                        <Film />
                    </el-icon>
                    <template #title>电影管理</template>
                </el-menu-item>

                <el-menu-item index="/admin/halls">
                    <el-icon>
                        <OfficeBuilding />
                    </el-icon>
                    <template #title>影厅管理</template>
                </el-menu-item>

                <el-menu-item index="/admin/schedules">
                    <el-icon>
                        <Calendar />
                    </el-icon>
                    <template #title>排片调度</template>
                </el-menu-item>

                <el-menu-item index="/admin/data-import" v-if="adminRole === 'SUPER_ADMIN'">
                    <el-icon>
                        <UploadFilled />
                    </el-icon>
                    <template #title>数据中心</template>
                </el-menu-item>
            </el-menu>
        </aside>

        <div class="main-wrapper">
            <header class="admin-header glass-panel">
                <div class="header-left">
                    <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
                        <Fold v-if="!isCollapse" />
                        <Expand v-else />
                    </el-icon>
                    <el-breadcrumb separator="/">
                        <el-breadcrumb-item :to="{ path: '/admin/dashboard' }">管理后台</el-breadcrumb-item>
                        <el-breadcrumb-item>{{ route.meta.title }}</el-breadcrumb-item>
                    </el-breadcrumb>
                </div>

                <div class="header-right">
                    <el-tag :type="adminRole === 'SUPER_ADMIN' ? 'danger' : 'success'" effect="dark" round
                        class="role-tag">
                        {{ adminRole === 'SUPER_ADMIN' ? '超级管理员' : '运营经理' }}
                    </el-tag>
                    <el-dropdown trigger="click" @command="handleCommand">
                        <div class="admin-avatar">
                            <el-avatar :size="36"
                                src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
                            <span class="admin-name">{{ adminName }}</span>
                        </div>
                        <template #dropdown>
                            <el-dropdown-menu>
                                <el-dropdown-item command="logout"><el-icon>
                                        <SwitchButton />
                                    </el-icon>退出系统</el-dropdown-item>
                            </el-dropdown-menu>
                        </template>
                    </el-dropdown>
                </div>
            </header>

            <main class="admin-content">
                <router-view v-slot="{ Component }">
                    <transition name="fade-slide" mode="out-in">
                        <component :is="Component" />
                    </transition>
                </router-view>
            </main>
        </div>
    </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DataAnalysis, Odometer, Film, OfficeBuilding, Calendar, UploadFilled, Fold, Expand, SwitchButton } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const isCollapse = ref(false)

// 这里我们简单从 localStorage 读取管理员信息 (后续会在 Login 中存入)
const adminRole = computed(() => localStorage.getItem('adminRole') || 'MANAGER')
const adminName = computed(() => localStorage.getItem('adminName') || 'Admin')

const handleCommand = (command) => {
    if (command === 'logout') {
        localStorage.removeItem('adminToken')
        localStorage.removeItem('adminRole')
        localStorage.removeItem('adminName')
        ElMessage.success('已退出管理后台')
        router.push('/admin/login')
    }
}
</script>

<style scoped lang="scss">
.admin-layout {
    display: flex;
    height: 100vh;
    background-color: #f1f5f9;
    overflow: hidden;
}

/* 侧边栏 */
.sidebar {
    width: 260px;
    background: #fff;
    border-right: 1px solid rgba(0, 0, 0, 0.05);
    display: flex;
    flex-direction: column;
    transition: width 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
    box-shadow: 4px 0 24px rgba(0, 0, 0, 0.02);
    z-index: 20;

    &.is-collapse {
        width: 80px;
    }

    .logo-zone {
        height: 72px;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 12px;
        border-bottom: 1px solid rgba(0, 0, 0, 0.05);

        .logo-text {
            font-size: 18px;
            font-weight: 700;
            color: #0f172a;
            white-space: nowrap;
        }
    }

    .admin-menu {
        border-right: none;
        padding: 12px 0;
    }

    :deep(.el-menu-item) {
        margin: 4px 12px;
        border-radius: 12px;
        height: 48px;
        line-height: 48px;
        color: #64748b;
        font-weight: 500;

        &:hover {
            background: #f8fafc;
            color: #0f172a;
        }

        &.is-active {
            background: var(--color-primary-light);
            color: var(--color-primary);
            font-weight: 600;
        }
    }
}

/* 主内容区 */
.main-wrapper {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

/* 顶部毛玻璃 Header */
.admin-header {
    height: 72px;
    padding: 0 32px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: rgba(255, 255, 255, 0.7);
    backdrop-filter: blur(20px);
    z-index: 10;
    border-bottom: 1px solid rgba(255, 255, 255, 0.8);

    .header-left {
        display: flex;
        align-items: center;
        gap: 24px;

        .collapse-btn {
            font-size: 20px;
            cursor: pointer;
            color: #64748b;
            transition: color 0.2s;

            &:hover {
                color: var(--color-primary);
            }
        }
    }

    .header-right {
        display: flex;
        align-items: center;
        gap: 24px;

        .role-tag {
            border: none;
        }

        .admin-avatar {
            display: flex;
            align-items: center;
            gap: 10px;
            cursor: pointer;
            padding: 4px 12px 4px 4px;
            border-radius: 24px;
            transition: background 0.2s;

            &:hover {
                background: rgba(0, 0, 0, 0.04);
            }

            .admin-name {
                font-weight: 600;
                color: #334155;
            }
        }
    }
}

.admin-content {
    flex: 1;
    padding: 32px;
    overflow-y: auto;

    &::-webkit-scrollbar {
        width: 8px;
    }

    &::-webkit-scrollbar-thumb {
        background: #cbd5e1;
        border-radius: 4px;
    }
}

.fade-slide-enter-active,
.fade-slide-leave-active {
    transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-slide-enter-from {
    opacity: 0;
    transform: translateY(15px);
}

.fade-slide-leave-to {
    opacity: 0;
    transform: translateY(-15px);
}
</style>