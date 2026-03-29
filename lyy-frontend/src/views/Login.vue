<template>
    <div class="login-container">
        <div class="blob blob-1"></div>
        <div class="blob blob-2"></div>

        <div class="glass-card">
            <div class="card-header">
                <h2 class="title">{{ isLogin ? '欢迎回来' : '创建账号' }}</h2>
                <p class="subtitle">{{ isLogin ? '登录您的智慧观影账号' : '开启您的个性化观影之旅' }}</p>
            </div>

            <el-form ref="formRef" :model="formData" :rules="rules" class="auth-form" size="large">
                <el-form-item prop="username">
                    <el-input v-model="formData.username" placeholder="请输入账号" :prefix-icon="User" />
                </el-form-item>

                <el-form-item prop="password">
                    <el-input v-model="formData.password" type="password" placeholder="请输入密码" show-password
                        :prefix-icon="Lock" @keyup.enter="handleSubmit" />
                </el-form-item>

                <el-form-item>
                    <el-button type="primary" class="submit-btn" :loading="loading" @click="handleSubmit">
                        {{ isLogin ? '登 录' : '注 册' }}
                    </el-button>
                </el-form-item>
            </el-form>

            <div class="toggle-action">
                <span>{{ isLogin ? '还没有账号？' : '已有账号？' }}</span>
                <a href="javascript:void(0)" @click="toggleMode">{{ isLogin ? '立即注册' : '直接登录' }}</a>
            </div>

            <div class="admin-entrance">
                <el-divider border-style="dashed" />
                <a href="javascript:void(0)" @click="router.push('/admin/login')">
                    <el-icon>
                        <Setting />
                    </el-icon> 切换至影院后台管理
                </a>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { User, Lock, Setting } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import request from '@/utils/request' // 我们上一轮封装的 axios

const router = useRouter()
const userStore = useUserStore()

const isLogin = ref(true)
const loading = ref(false)
const formRef = ref(null)

const formData = reactive({
    username: '',
    password: ''
})

const rules = {
    username: [{ required: true, message: '账号不能为空', trigger: 'blur' }, { min: 3, message: '账号长度不能小于3位', trigger: 'blur' }],
    password: [{ required: true, message: '密码不能为空', trigger: 'blur' }, { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }]
}

const toggleMode = () => {
    isLogin.value = !isLogin.value
    formRef.value?.resetFields()
}

const handleSubmit = () => {
    formRef.value?.validate(async (valid) => {
        if (!valid) return
        loading.value = true
        try {
            const url = isLogin.value ? '/api/user/login' : '/api/user/register'
            const res = await request.post(url, formData)

            if (isLogin.value) {
                ElMessage.success('登录成功')
                // 将后端返回的 token 和信息存入 Pinia
                userStore.setLoginInfo(res.token, { id: res.id, username: res.username, nickname: res.nickname, avatar: res.avatar })
                router.push('/')
            } else {
                ElMessage.success('注册成功，请登录')
                toggleMode() // 注册成功后切回登录模式
            }
        } catch (error) {
            // 错误由 request.js 中的拦截器处理，此处无需额外操作
        } finally {
            loading.value = false
        }
    })
}
</script>

<style scoped lang="scss">
.login-container {
    min-height: 100vh;
    display: flex;
    justify-content: center;
    align-items: center;
    background-color: #f1f5f9;
    position: relative;
    overflow: hidden;

    /* 背景装饰球营造弥散感 */
    .blob {
        position: absolute;
        filter: blur(80px);
        z-index: 0;
        opacity: 0.6;
    }

    .blob-1 {
        width: 400px;
        height: 400px;
        background: #6366f1;
        top: -100px;
        left: -100px;
        border-radius: 50%;
    }

    .blob-2 {
        width: 500px;
        height: 500px;
        background: #ec4899;
        bottom: -150px;
        right: -150px;
        border-radius: 50%;
    }
}

.glass-card {
    position: relative;
    z-index: 1;
    width: 420px;
    padding: 48px 40px;
    background: rgba(255, 255, 255, 0.6);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border: 1px solid rgba(255, 255, 255, 0.4);
    border-radius: 24px;
    box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.1);

    .card-header {
        text-align: center;
        margin-bottom: 32px;

        .title {
            font-size: 28px;
            margin: 0 0 8px 0;
            color: #0f172a;
        }

        .subtitle {
            font-size: 14px;
            color: #64748b;
            margin: 0;
        }
    }

    .submit-btn {
        width: 100%;
        border-radius: 12px;
        height: 44px;
        font-size: 16px;
        margin-top: 10px;
    }

    .toggle-action {
        margin-top: 24px;
        text-align: center;
        font-size: 14px;
        color: #64748b;

        a {
            color: #6366f1;
            font-weight: 600;
            text-decoration: none;
            margin-left: 8px;
            transition: color 0.2s ease;

            &:hover {
                color: #4f46e5;
            }
        }
    }
}

/* 深度重塑 Element UI 的 Input 样式，使其符合玻璃态 */
:deep(.el-input__wrapper) {
    background: rgba(255, 255, 255, 0.5) !important;
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.8) !important;
    box-shadow: none !important;
    border-radius: 12px !important;
    padding: 8px 15px;

    &.is-focus {
        background: rgba(255, 255, 255, 0.9) !important;
        border-color: #6366f1 !important;
        box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1) !important;
    }
}

.admin-entrance {
    margin-top: 10px;
    text-align: center;

    :deep(.el-divider) {
        margin: 16px 0;
        opacity: 0.5;
    }

    a {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 6px;
        color: #94a3b8;
        font-size: 13px;
        text-decoration: none;
        transition: all 0.2s;

        &:hover {
            color: #6366f1;
        }
    }
}
</style>