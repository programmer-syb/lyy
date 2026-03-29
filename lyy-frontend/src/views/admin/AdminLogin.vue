<template>
    <div class="admin-login-wrapper">
        <div class="login-box glass-panel">
            <div class="brand">
                <el-icon :size="40" color="#6366f1">
                    <DataAnalysis />
                </el-icon>
                <h2>影院管理后台系统</h2>
                <p>System Administration</p>
            </div>

            <el-form :model="form" @keyup.enter="handleLogin" size="large">
                <el-form-item>
                    <el-input v-model="form.username" placeholder="管理员账号" :prefix-icon="User" />
                </el-form-item>
                <el-form-item>
                    <el-input v-model="form.password" type="password" placeholder="管理密码" show-password
                        :prefix-icon="Lock" />
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">安全登录</el-button>
                </el-form-item>

                <div class="back-entrance">
                    <a href="javascript:void(0)" @click="router.push('/login')">
                        <el-icon>
                            <Back />
                        </el-icon> 返回观众端
                    </a>
                </div>
            </el-form>
        </div>
    </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { User, Lock, DataAnalysis, Back } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const handleLogin = async () => {
    if (!form.username || !form.password) return ElMessage.warning('请输入账号和密码')
    loading.value = true
    try {
        const res = await request.post('/api/admin/sys/login', form)
        // 存储管理员 Token 和角色信息
        localStorage.setItem('adminToken', res.token)
        localStorage.setItem('adminRole', res.roleType)
        localStorage.setItem('adminName', res.username)
        ElMessage.success('欢迎回来，' + res.username)
        router.push('/admin/dashboard')
    } catch (e) { } finally { loading.value = false }
}
</script>

<style scoped lang="scss">
.admin-login-wrapper {
    height: 100vh;
    display: flex;
    justify-content: center;
    align-items: center;
    background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
    /* 后台用深邃背景区分前台 */
}

.login-box {
    width: 400px;
    padding: 48px;
    background: rgba(255, 255, 255, 0.95);
    border-radius: 24px;
    box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
    text-align: center;
}

.brand {
    margin-bottom: 40px;

    h2 {
        margin: 16px 0 8px 0;
        color: #0f172a;
        font-size: 24px;
    }

    p {
        margin: 0;
        color: #64748b;
        font-size: 14px;
        letter-spacing: 1px;
    }
}

.login-btn {
    width: 100%;
    border-radius: 12px;
    height: 48px;
    font-size: 16px;
    margin-top: 10px;
}

.back-entrance {
    margin-top: 24px;
    text-align: center;

    a {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        color: #64748b;
        font-size: 14px;
        text-decoration: none;
        transition: color 0.2s;

        &:hover {
            color: #6366f1;
        }
    }
}
</style>