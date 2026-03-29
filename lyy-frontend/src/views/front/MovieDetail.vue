<template>
    <div class="detail-container" v-if="movie">

        <div class="movie-header glass-panel">
            <img :src="movie.posterUrl" class="poster">
            <div class="meta">
                <h1 class="title">{{ movie.title }}</h1>
                <div class="tags">
                    <el-tag effect="plain" round>{{ movie.genres }}</el-tag>
                    <el-tag effect="plain" type="info" round>{{ movie.duration }} 分钟</el-tag>
                    <el-tag effect="dark" type="warning" round class="rating-tag"><el-icon>
                            <StarFilled />
                        </el-icon> {{ movie.globalRating }}</el-tag>
                </div>
                <div class="info-item"><span>导演：</span>{{ movie.director || '未知' }}</div>
                <div class="info-item"><span>主演：</span>{{ movie.actors || '未知' }}</div>
                <div class="intro"><span>剧情简介：</span>{{ movie.introduction }}</div>
            </div>
        </div>

        <div class="schedule-section">
            <h2 class="section-title">场次与购票</h2>
            <el-radio-group v-model="selectedDate" size="large" @change="fetchSchedules" class="date-selector">
                <el-radio-button :label="today">今天 {{ today.substring(5) }}</el-radio-button>
                <el-radio-button :label="tomorrow">明天 {{ tomorrow.substring(5) }}</el-radio-button>
            </el-radio-group>

            <div class="schedule-list" v-loading="loadingSchedules">
                <el-empty v-if="schedules.length === 0" description="今日暂无排片" />
                <div class="schedule-card glass-panel" v-for="sc in schedules" :key="sc.scheduleId">
                    <div class="time">
                        <span class="start">{{ sc.showTime.substring(0, 5) }}</span>
                        <span class="end">预计 {{ calculateEndTime(sc.showTime, movie.duration) }} 散场</span>
                    </div>
                    <div class="hall">{{ sc.hallName }}</div>
                    <div class="price">¥ {{ sc.price }}</div>
                    <el-button type="primary" round class="buy-btn" @click="openSeatModal(sc)">选座购票</el-button>
                </div>
            </div>
        </div>

        <el-dialog v-model="seatModalVisible" :title="'在线选座 - ' + currentSchedule?.hallName" width="800px"
            destroy-on-close class="glass-dialog">
            <div class="seat-map-container" v-loading="loadingSeats">
                <div class="screen-indicator">银幕中央</div>

                <div class="seats-matrix" :style="{ gridTemplateColumns: `repeat(${seatMap.colCount}, 1fr)` }">
                    <template v-for="r in seatMap.rowCount" :key="'r'+r">
                        <template v-for="c in seatMap.colCount" :key="'c' + c">
                            <div class="seat" :class="getSeatStatus(r, c)" @click="toggleSeat(r, c)">
                                <el-icon
                                    v-if="getSeatStatus(r, c) === 'available' || getSeatStatus(r, c) === 'selected'">
                                    <Check v-if="getSeatStatus(r, c) === 'selected'" />
                                </el-icon>
                                <el-icon v-else>
                                    <Close />
                                </el-icon>
                            </div>
                        </template>
                    </template>
                </div>

                <div class="seat-legend">
                    <div class="legend-item">
                        <div class="seat available"></div> 可选
                    </div>
                    <div class="legend-item">
                        <div class="seat selected"></div> 已选
                    </div>
                    <div class="legend-item">
                        <div class="seat sold"></div> 已售/锁定
                    </div>
                </div>
            </div>

            <template #footer>
                <div class="modal-footer">
                    <div class="selected-info">
                        已选座位：
                        <el-tag v-for="seat in selectedSeats" :key="seat" closable
                            @close="toggleSeat(seat.split('-')[0], seat.split('-')[1])" class="seat-tag">
                            {{ seat.split('-')[0] }}排{{ seat.split('-')[1] }}座
                        </el-tag>
                        <span v-if="selectedSeats.length === 0" class="empty-text">请在上方选座，最多4个</span>
                    </div>
                    <div class="actions">
                        <div class="total-price" v-if="selectedSeats.length > 0">总计: <span>¥ {{ (currentSchedule?.price
                            * selectedSeats.length).toFixed(2) }}</span></div>
                        <el-button type="primary" size="large" :disabled="selectedSeats.length === 0"
                            :loading="submitLoading" @click="submitOrder">
                            确认下单并支付
                        </el-button>
                    </div>
                </div>
            </template>
        </el-dialog>

    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { StarFilled, Check, Close } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import dayjs from 'dayjs' // Vue 项目常自带，或者可以用原生 Date 处理

const route = useRoute()
const router = useRouter()
const movieId = route.params.id

const movie = ref(null)
const today = dayjs().format('YYYY-MM-DD')
const tomorrow = dayjs().add(1, 'day').format('YYYY-MM-DD')
const selectedDate = ref(today)

const schedules = ref([])
const loadingSchedules = ref(false)

// 选座相关
const seatModalVisible = ref(false)
const currentSchedule = ref(null)
const loadingSeats = ref(false)
const seatMap = ref({ rowCount: 0, colCount: 0, soldSeats: [], lockedSeats: [] })
const selectedSeats = ref([])
const submitLoading = ref(false)

// 获取电影详情
const fetchMovieDetail = async () => {
    try {
        movie.value = await request.get(`/api/movie/${movieId}`)
    } catch (e) { ElMessage.error('电影加载失败') }
}

// 获取排片
const fetchSchedules = async () => {
    loadingSchedules.value = true
    try {
        schedules.value = await request.get(`/api/schedule/list?movieId=${movieId}&showDate=${selectedDate.value}`)
    } catch (e) { } finally { loadingSchedules.value = false }
}

const calculateEndTime = (startTime, duration = 120) => {
    if (!startTime) return ''
    return dayjs(`2000-01-01 ${startTime}`).add(duration, 'minute').format('HH:mm')
}

// 打开选座弹窗
const openSeatModal = async (schedule) => {
    const token = localStorage.getItem('userToken')
    if (!token) {
        ElMessage.warning('请先登录再进行选座')
        router.push(`/login?redirect=/movie/${movieId}`)
        return
    }

    currentSchedule.value = schedule
    selectedSeats.value = []
    seatModalVisible.value = true
    loadingSeats.value = true
    try {
        const res = await request.get(`/api/schedule/${schedule.scheduleId}/seats`)
        seatMap.value = res || { rowCount: 10, colCount: 12, soldSeats: [], lockedSeats: [] }
    } catch (e) { } finally { loadingSeats.value = false }
}

// 座位状态计算逻辑
const getSeatStatus = (r, c) => {
    const seatId = `${r}-${c}`
    if (seatMap.value.soldSeats.includes(seatId) || seatMap.value.lockedSeats.includes(seatId)) return 'sold'
    if (selectedSeats.value.includes(seatId)) return 'selected'
    return 'available'
}

// 选座/取消选座逻辑
const toggleSeat = (r, c) => {
    const seatId = `${r}-${c}`
    const status = getSeatStatus(r, c)
    if (status === 'sold') return // 别人买的不能点

    if (status === 'selected') {
        selectedSeats.value = selectedSeats.value.filter(id => id !== seatId)
    } else {
        if (selectedSeats.value.length >= 4) {
            ElMessage.warning('每笔订单最多选择4个座位')
            return
        }
        selectedSeats.value.push(seatId)
    }
}

// 提交订单并模拟支付
const submitOrder = async () => {
    submitLoading.value = true
    try {
        // 1. 创建订单 (锁定座位)
        const orderRes = await request.post('/api/order/create', {
            scheduleId: currentSchedule.value.scheduleId,
            seats: selectedSeats.value
        })

        // 2. 弹出模拟支付确认框
        await ElMessageBox.confirm(`订单创建成功，总计 ${orderRes.totalPrice} 元。是否立即支付？`, '模拟收银台', {
            confirmButtonText: '微信/支付宝支付',
            cancelButtonText: '稍后支付',
            type: 'success',
            center: true
        })

        // 3. 点击支付，调用支付接口
        await request.post(`/api/order/pay/${orderRes.id}`)
        ElMessage.success('支付成功！您的取票码已生成。')
        seatModalVisible.value = false

        // 选做：支付成功后跳到订单页
        // router.push('/orders')

    } catch (e) {
        if (e !== 'cancel') {
            ElMessage.error(e.message || '下单失败')
            // 如果报错(如超卖)，刷新座位图
            openSeatModal(currentSchedule.value)
        } else {
            ElMessage.info('订单已锁定，请在15分钟内到个人中心完成支付')
            seatModalVisible.value = false
        }
    } finally {
        submitLoading.value = false
    }
}

onMounted(() => {
    fetchMovieDetail()
    fetchSchedules()
})
</script>

<style scoped lang="scss">
.detail-container {
    display: flex;
    flex-direction: column;
    gap: 40px;
    padding-bottom: 60px;
}

.glass-panel {
    background: rgba(255, 255, 255, 0.7);
    backdrop-filter: blur(20px);
    border-radius: var(--radius-large);
    border: 1px solid rgba(255, 255, 255, 0.8);
    box-shadow: var(--shadow-soft);
}

.movie-header {
    display: flex;
    gap: 40px;
    padding: 40px;

    .poster {
        width: 240px;
        border-radius: 16px;
        box-shadow: 0 15px 35px rgba(0, 0, 0, 0.2);
    }

    .meta {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 16px;

        .title {
            font-size: 36px;
            margin: 0;
        }

        .tags {
            display: flex;
            gap: 12px;
        }

        .rating-tag {
            background: #fbbf24;
            color: #fff;
            border: none;
            font-weight: bold;
        }

        .info-item {
            font-size: 15px;

            span {
                color: var(--text-muted);
                font-weight: 600;
            }
        }

        .intro {
            margin-top: 10px;
            line-height: 1.8;
            color: #334155;

            span {
                color: var(--text-muted);
                font-weight: 600;
                display: block;
                margin-bottom: 8px;
            }
        }
    }
}

.section-title {
    font-size: 24px;
    margin-bottom: 24px;
}

.date-selector {
    margin-bottom: 24px;
}

.schedule-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.schedule-card {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 24px 32px;
    transition: transform 0.2s ease;

    &:hover {
        transform: scale(1.01);
    }

    .time {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .start {
            font-size: 24px;
            font-weight: 700;
            color: var(--color-primary);
        }

        .end {
            font-size: 13px;
            color: var(--text-muted);
        }
    }

    .hall {
        font-size: 16px;
        font-weight: 500;
        width: 200px;
        text-align: center;
    }

    .price {
        font-size: 20px;
        font-weight: 700;
        color: #ef4444;
        width: 100px;
        text-align: center;
    }

    .buy-btn {
        width: 120px;
    }
}

/* 选座矩阵核心样式 */
.seat-map-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 20px;
    background: #f8fafc;
    border-radius: 16px;
}

.screen-indicator {
    width: 60%;
    height: 30px;
    border-top: 4px solid #cbd5e1;
    border-radius: 50% 50% 0 0 / 100% 100% 0 0;
    text-align: center;
    line-height: 40px;
    color: var(--text-muted);
    font-weight: 600;
    margin-bottom: 40px;
}

.seats-matrix {
    display: grid;
    gap: 10px;
    margin-bottom: 30px;
    justify-content: center;
}

.seat {
    width: 32px;
    height: 32px;
    border-radius: 8px 8px 4px 4px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s;

    &.available {
        background: #e2e8f0;
        border: 1px solid #cbd5e1;
        color: transparent;

        &:hover {
            background: #cbd5e1;
            transform: translateY(-2px);
        }
    }

    &.selected {
        background: var(--color-primary);
        color: #fff;
        box-shadow: 0 4px 10px rgba(99, 102, 241, 0.4);
        transform: translateY(-2px);
    }

    &.sold {
        background: #fecaca;
        color: #ef4444;
        cursor: not-allowed;
        opacity: 0.6;
    }
}

.seat-legend {
    display: flex;
    gap: 24px;
    margin-top: 20px;

    .legend-item {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 14px;
        color: var(--text-muted);

        .seat {
            width: 20px;
            height: 20px;
            cursor: default;
        }
    }
}

.modal-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;

    .selected-info {
        flex: 1;
        text-align: left;

        .seat-tag {
            margin-right: 8px;
            margin-bottom: 8px;
        }

        .empty-text {
            color: var(--text-muted);
            font-size: 14px;
        }
    }

    .actions {
        display: flex;
        align-items: center;
        gap: 20px;

        .total-price span {
            color: #ef4444;
            font-size: 24px;
            font-weight: 700;
        }
    }
}

/* 深度修改 Dialog 圆角以契合苹果风 */
:deep(.el-dialog) {
    border-radius: 24px;
    overflow: hidden;
}
</style>