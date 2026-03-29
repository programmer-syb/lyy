<template>
    <div class="user-center-container">
        <div class="profile-header glass-panel">
            <el-upload class="avatar-uploader" action="http://localhost:8080/api/user/avatar" :headers="uploadHeaders"
                :show-file-list="false" :on-success="handleAvatarSuccess" :before-upload="beforeAvatarUpload">
                <img v-if="userInfo.avatar" :src="userInfo.avatar" class="avatar" />
                <el-icon v-else class="avatar-uploader-icon">
                    <Plus />
                </el-icon>
                <div class="avatar-hover-mask"><el-icon>
                        <Camera />
                    </el-icon></div>
            </el-upload>
            <div class="user-meta">
                <h2 class="nickname">{{ userInfo.nickname || userInfo.username }}</h2>
                <p class="join-time">加入智慧观影：{{ formatTime(userInfo.createTime, 'YYYY-MM-DD') }}</p>
            </div>
        </div>

        <el-tabs v-model="activeTab" class="custom-tabs" @tab-change="handleTabChange">

            <el-tab-pane label="我的订单" name="orders">
                <div class="bento-grid" v-loading="loadingOrders">
                    <el-empty v-if="orders.length === 0" description="暂无订单记录" />

                    <el-card v-for="order in orders" :key="order.id" class="order-card" shadow="hover">
                        <div class="order-header">
                            <span class="order-no">订单号：{{ order.orderNo }}</span>
                            <el-tag :type="getOrderStatusType(order.status)" effect="dark" round class="status-tag">
                                {{ getOrderStatusText(order.status) }}
                            </el-tag>
                        </div>

                        <div class="order-body">
                            <div class="ticket-info">
                                <div class="info-row">
                                    <span class="label">座位：</span>
                                    <span class="value seats">{{ order.seats ? order.seats.replace(/,/g, ' | ') : '未选座'
                                        }}</span>
                                </div>
                                <div class="info-row">
                                    <span class="label">总价：</span>
                                    <span class="value price">¥ {{ order.totalPrice }}</span>
                                </div>
                                <div class="info-row">
                                    <span class="label">下单时间：</span>
                                    <span class="value">{{ formatTime(order.createTime) }}</span>
                                </div>

                                <div class="info-row ticket-code-row" v-if="order.status === 1">
                                    <span class="label">取票码：</span>
                                    <span class="code-box">{{ order.ticketCode }}</span>
                                </div>
                            </div>
                        </div>

                        <div class="order-footer" v-if="order.status === 0">
                            <span class="expire-tips">请在 15 分钟内完成支付</span>
                            <div class="actions">
                                <el-button plain round @click="cancelOrder(order.id)">取消订单</el-button>
                                <el-button type="primary" round @click="payOrder(order.id)">立即支付</el-button>
                            </div>
                        </div>
                    </el-card>
                </div>
            </el-tab-pane>

            <el-tab-pane label="账号设置" name="profile">
                <div class="settings-container glass-panel">
                    <h3 class="section-title">基本信息</h3>
                    <el-form :model="userInfo" label-width="80px" class="profile-form">
                        <el-form-item label="昵称">
                            <el-input v-model="userInfo.nickname" placeholder="输入您的昵称" />
                        </el-form-item>
                        <el-form-item label="年龄">
                            <el-input-number v-model="userInfo.age" :min="1" :max="120" />
                        </el-form-item>
                        <el-form-item label="性别">
                            <el-radio-group v-model="userInfo.gender">
                                <el-radio :label="1">男</el-radio>
                                <el-radio :label="2">女</el-radio>
                                <el-radio :label="0">保密</el-radio>
                            </el-radio-group>
                        </el-form-item>
                        <el-form-item>
                            <el-button type="primary" round @click="updateProfile">保存基本信息</el-button>
                        </el-form-item>
                    </el-form>

                    <el-divider />

                    <h3 class="section-title">观影偏好 (决定推荐算法)</h3>
                    <div class="preferences-section">
                        <p class="pref-desc">选择您喜欢的电影类型：</p>
                        <div class="tag-group">
                            <el-tag v-for="genre in allGenres" :key="genre"
                                :effect="selectedPreferences.includes(genre) ? 'dark' : 'plain'" round class="pref-tag"
                                @click="togglePreference(genre)">
                                {{ genre }}
                            </el-tag>
                        </div>
                        <el-button type="success" round @click="savePreferences">更新偏好设置</el-button>
                    </div>
                </div>
            </el-tab-pane>

            <el-tab-pane label="观影足迹" name="history">
                <div class="history-timeline glass-panel" v-loading="loadingHistory">
                    <el-empty v-if="historyList.length === 0" description="还没有留下足迹，快去探索电影吧" />
                    <el-timeline v-else>
                        <el-timeline-item v-for="(item, index) in historyList" :key="index"
                            :timestamp="formatTime(item.actionTime)"
                            :type="item.actionType === '购票' ? 'primary' : 'warning'" placement="top">
                            <el-card class="history-card" shadow="hover"
                                @click="$router.push(`/movie/${item.movieId}`)">
                                <img :src="item.posterUrl" class="history-poster" />
                                <div class="history-info">
                                    <h4 class="m-title">{{ item.title }}</h4>
                                    <div class="m-action">
                                        <el-tag size="small" :type="item.actionType === '购票' ? 'success' : 'warning'"
                                            effect="plain">
                                            {{ item.actionType }}
                                        </el-tag>
                                        <span class="m-details">{{ item.details }}</span>

                                        <el-button v-if="item.actionType === '购票'" size="small" type="primary" plain
                                            style="margin-left: auto;"
                                            @click.stop="openRatingModal(item.movieId, item.title)">
                                            <el-icon>
                                                <Edit />
                                            </el-icon> 写影评
                                        </el-button>
                                    </div>
                                </div>
                            </el-card>
                        </el-timeline-item>
                    </el-timeline>
                </div>
            </el-tab-pane>
        </el-tabs>

        <el-dialog v-model="ratingVisible" :title="'评价电影：' + ratingMovieTitle" width="500px" destroy-on-close
            class="glass-dialog">
            <div style="text-align: center; margin-bottom: 20px;">
                <p style="color: #64748b; margin-bottom: 10px;">请为本片打分</p>
                <el-rate v-model="ratingForm.rating" :colors="['#99A9BF', '#F7BA2A', '#FF9900']" allow-half
                    size="large" />
            </div>
            <el-input v-model="ratingForm.comment" type="textarea" :rows="4" placeholder="写下您的观影感受吧..." maxlength="500"
                show-word-limit />
            <template #footer>
                <el-button @click="ratingVisible = false" round>取 消</el-button>
                <el-button type="primary" :loading="ratingLoading" @click="submitRating" round>提交评价</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus, Camera, Edit } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()
const activeTab = ref('orders')

const uploadHeaders = { Authorization: userStore.token }
const userInfo = ref({})
const selectedPreferences = ref([])
const allGenres = ['科幻', '动作', '喜剧', '爱情', '动画', '悬疑', '犯罪', '奇幻', '灾难', '历史']

const orders = ref([])
const historyList = ref([])
const loadingOrders = ref(false)
const loadingHistory = ref(false)

// 评价表单
const ratingVisible = ref(false)
const ratingLoading = ref(false)
const ratingMovieTitle = ref('')
const ratingForm = reactive({ movieId: null, rating: 5.0, comment: '' })

const formatTime = (timeStr, format = 'YYYY-MM-DD HH:mm') => {
    if (!timeStr) return '无时间记录'
    const date = new Date(timeStr)
    const pad = (n) => n < 10 ? '0' + n : n
    if (format === 'YYYY-MM-DD') return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

const fetchProfile = async () => {
    try {
        const res = await request.get('/api/user/profile')
        userInfo.value = res
        if (res.preferences) selectedPreferences.value = res.preferences.split(',')
    } catch (e) { }
}

const togglePreference = (genre) => {
    const index = selectedPreferences.value.indexOf(genre)
    if (index > -1) selectedPreferences.value.splice(index, 1)
    else selectedPreferences.value.push(genre)
}

const savePreferences = async () => {
    try {
        await request.put(`/api/user/preferences?preferences=${selectedPreferences.value.join(',')}`)
        ElMessage.success('偏好设置已更新')
    } catch (e) { }
}

const updateProfile = async () => {
    try {
        await request.put('/api/user/profile', userInfo.value)
        ElMessage.success('基本信息已保存')
        userStore.userInfo.nickname = userInfo.value.nickname
        userStore.userInfo.avatar = userInfo.value.avatar
    } catch (e) { }
}

const handleAvatarSuccess = (res) => {
    if (res.code === 200) {
        userInfo.value.avatar = res.data
        userStore.userInfo.avatar = res.data
        ElMessage.success('头像更新成功')
    }
}

const beforeAvatarUpload = (file) => {
    const isLt2M = file.size / 1024 / 1024 < 2
    if (!isLt2M) ElMessage.error('头像大小不能超过 2MB!')
    return isLt2M
}

const fetchOrders = async () => {
    loadingOrders.value = true
    try {
        const res = await request.get('/api/order/history?size=50')
        orders.value = res.records || []
    } catch (e) { } finally { loadingOrders.value = false }
}

const getOrderStatusType = (s) => (s === 0 ? 'warning' : s === 1 ? 'success' : 'info')
const getOrderStatusText = (s) => {
    const map = { 0: '待支付', 1: '购票成功', 2: '已取消', 3: '已退票' }
    return map[s] || '未知状态'
}

const payOrder = async (id) => {
    try {
        await request.post(`/api/order/pay/${id}`)
        ElMessage.success('支付成功！')
        fetchOrders()
    } catch (e) { }
}

const cancelOrder = async (id) => {
    try {
        await ElMessageBox.confirm('确定取消订单吗？', '提示', { type: 'warning' })
        await request.post(`/api/order/cancel/${id}`)
        ElMessage.success('订单已取消')
        fetchOrders()
    } catch (e) { }
}

const fetchHistory = async () => {
    loadingHistory.value = true
    try {
        const res = await request.get('/api/user/history')
        historyList.value = res || []
    } catch (e) { } finally { loadingHistory.value = false }
}

const handleTabChange = (name) => {
    if (name === 'orders') fetchOrders()
    if (name === 'history') fetchHistory()
}

// 评价逻辑
const openRatingModal = (movieId, title) => {
    ratingForm.movieId = movieId
    ratingForm.rating = 5.0
    ratingForm.comment = ''
    ratingMovieTitle.value = title
    ratingVisible.value = true
}

const submitRating = async () => {
    ratingLoading.value = true
    try {
        await request.post('/api/movie/rate', ratingForm)
        ElMessage.success('评价成功')
        ratingVisible.value = false
        fetchHistory()
    } catch (e) { } finally { ratingLoading.value = false }
}

onMounted(() => {
    fetchProfile()
    // 根据路由路径自动切换 Tab
    if (route.path.includes('orders')) activeTab.value = 'orders'
    else if (route.path.includes('profile')) activeTab.value = 'profile'

    handleTabChange(activeTab.value)
})
</script>

<style scoped lang="scss">
.user-center-container {
    max-width: 1000px;
    margin: 0 auto;
    display: flex;
    flex-direction: column;
    gap: 32px;
}

.glass-panel {
    background: rgba(255, 255, 255, 0.6);
    backdrop-filter: blur(20px);
    border-radius: var(--radius-large);
    border: 1px solid rgba(255, 255, 255, 0.8);
    box-shadow: var(--shadow-soft);
    padding: 32px;
}

.profile-header {
    display: flex;
    align-items: center;
    gap: 32px;
    padding: 40px;

    .avatar-uploader {
        width: 100px;
        height: 100px;
        border-radius: 50%;
        overflow: hidden;
        cursor: pointer;
        position: relative;

        .avatar {
            width: 100%;
            height: 100%;
            object-fit: cover;
        }

        .avatar-uploader-icon {
            font-size: 28px;
            width: 100%;
            height: 100%;
            display: flex;
            align-items: center;
            justify-content: center;
            background: #f1f5f9;
        }

        .avatar-hover-mask {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0, 0, 0, 0.4);
            color: #fff;
            display: flex;
            align-items: center;
            justify-content: center;
            opacity: 0;
            transition: 0.3s;
        }

        &:hover .avatar-hover-mask {
            opacity: 1;
        }
    }

    .nickname {
        font-size: 28px;
        margin: 0 0 8px 0;
    }

    .join-time {
        color: var(--text-muted);
        font-size: 14px;
        margin: 0;
    }
}

:deep(.el-tabs__item) {
    padding: 0 24px !important;
    height: 48px;
    line-height: 48px;
    font-weight: 500;
    transition: 0.3s;

    &.is-active {
        color: var(--color-primary);
        font-weight: 700;
        background: var(--color-primary-light);
        border-radius: 24px;
    }
}

:deep(.el-tabs__nav-wrap::after),
:deep(.el-tabs__active-bar) {
    display: none;
}

.bento-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 24px;
    margin-top: 20px;
}

.order-card {
    .order-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 16px 20px;
        background: #f8fafc;
        border-bottom: 1px solid #f1f5f9;

        .order-no {
            font-size: 12px;
            color: var(--text-muted);
            font-family: monospace;
        }
    }

    .order-body {
        padding: 20px;

        .ticket-info {
            display: flex;
            flex-direction: column;
            gap: 12px;

            .info-row {
                display: flex;
                font-size: 14px;

                .label {
                    width: 70px;
                    color: var(--text-muted);
                }

                .price {
                    color: #ef4444;
                    font-weight: 700;
                    font-size: 18px;
                }

                .seats {
                    color: var(--color-primary);
                }
            }

            .ticket-code-row {
                margin-top: 10px;
                padding-top: 10px;
                border-top: 1px dashed #e2e8f0;

                .code-box {
                    background: #f0fdf4;
                    color: #16a34a;
                    padding: 4px 12px;
                    border-radius: 8px;
                    font-size: 20px;
                    font-weight: 700;
                    letter-spacing: 2px;
                }
            }
        }
    }

    .order-footer {
        padding: 16px 20px;
        border-top: 1px solid #f1f5f9;
        display: flex;
        justify-content: space-between;
        align-items: center;

        .expire-tips {
            color: #ef4444;
            font-size: 12px;
        }
    }
}

.history-timeline {
    padding: 40px;
    margin-top: 20px;

    .history-card {
        display: flex;
        gap: 16px;
        padding: 12px !important;
        cursor: pointer;
        border: 1px solid #f1f5f9;
        background: transparent;
        box-shadow: none !important;

        &:hover {
            background: #fff;
            box-shadow: var(--shadow-soft) !important;
        }

        .history-poster {
            width: 60px;
            height: 85px;
            object-fit: cover;
            border-radius: 8px;
        }

        .history-info {
            flex: 1;
            display: flex;
            flex-direction: column;
            justify-content: center;

            .m-title {
                margin: 0 0 8px 0;
            }

            .m-action {
                display: flex;
                align-items: center;
                gap: 12px;
                font-size: 13px;
                color: var(--text-muted);
            }
        }
    }
}

.pref-tag {
    margin-right: 12px;
    margin-bottom: 12px;
    cursor: pointer;
    border: none;
    padding: 8px 16px;
}
</style>