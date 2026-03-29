<template>
    <div class="dashboard-layout">
        <header class="glass-header">
            <div class="header-content">
                <h1 class="page-title">运营数据大盘</h1>
                <div class="user-profile">
                    <el-avatar :size="40" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
                    <span class="greeting">Hi, {{ adminName }}</span>
                </div>
            </div>
        </header>

        <main class="bento-container" v-loading="loadingData">

            <el-card class="bento-card span-2 highlight-card">
                <div class="stat-header">
                    <span class="stat-title">{{ periodLabel }}总计票房收益</span>
                    <el-tag effect="dark" round color="rgba(255,255,255,0.2)" style="border: none">Real-time</el-tag>
                </div>
                <div class="stat-value">¥ {{ totalBoxOffice.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}
                </div>
                <div class="stat-trend">基于已支付订单实时统计</div>
            </el-card>

            <el-card class="bento-card action-card">
                <h3 class="card-title">快捷操作</h3>
                <div class="action-grid">
                    <el-button class="custom-btn" type="primary" plain @click="$router.push('/admin/schedules')">+
                        新增排片</el-button>
                    <el-button class="custom-btn" type="success" plain
                        @click="$router.push('/admin/movies')">电影管理</el-button>
                </div>
            </el-card>

            <el-card class="bento-card span-3 chart-card">
                <div class="card-header">
                    <h3 class="card-title">票房收益趋势 (算法模拟)</h3>
                    <el-radio-group v-model="trendTimeRange" size="small" class="custom-radio"
                        @change="updateTrendChart">
                        <el-radio-button label="7days">7天</el-radio-button>
                        <el-radio-button label="30days">30天</el-radio-button>
                    </el-radio-group>
                </div>
                <div ref="trendChartRef" class="chart-container"></div>
            </el-card>

            <el-card class="bento-card span-2 chart-card">
                <div class="card-header">
                    <h3 class="card-title">热映票房 Top 5</h3>
                    <el-radio-group v-model="rankingType" size="small" class="custom-radio"
                        @change="fetchRealBoxOfficeData">
                        <el-radio-button label="daily">日榜</el-radio-button>
                        <el-radio-button label="weekly">周榜</el-radio-button>
                        <el-radio-button label="monthly">月榜</el-radio-button>
                        <el-radio-button label="all">总榜</el-radio-button>
                    </el-radio-group>
                </div>
                <div ref="rankChartRef" class="chart-container"></div>
            </el-card>

            <el-card class="bento-card chart-card">
                <h3 class="card-title">人群画像</h3>
                <div ref="pieChartRef" class="chart-container"></div>
            </el-card>

        </main>
    </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import request from '@/utils/request'

const adminName = ref(localStorage.getItem('adminName') || '运营经理')

// 趋势图开关与排行图开关分离
const trendTimeRange = ref('7days')
const rankingType = ref('all')

const trendChartRef = ref(null)
const rankChartRef = ref(null)
const pieChartRef = ref(null)

let trendChart, rankChart, pieChart

const loadingData = ref(false)
const totalBoxOffice = ref(0)

// 动态显示当前计算的是什么时间的票房
const periodLabel = computed(() => {
    const map = { 'daily': '今日', 'weekly': '本周', 'monthly': '本月', 'all': '系统历史' }
    return map[rankingType.value]
})

const colors = { primary: '#6366f1', secondary: '#8b5cf6', tertiary: '#ec4899', background: '#f8fafc', text: '#1e293b', textLight: '#64748b' }

// =================== 接入后端真实数据 (Top 5 排行榜) ===================
const fetchRealBoxOfficeData = async () => {
    loadingData.value = true
    try {
        // 动态传递 type 参数给后端 (daily/weekly/monthly/all)
        const res = await request.get(`/api/admin/sys/analysis/box-office?type=${rankingType.value}`)
        const list = res || []

        // 1. 动态计算当前所选区间的总票房
        let sum = 0
        list.forEach(item => { sum += item.boxOffice })
        totalBoxOffice.value = sum

        // 2. 截取前 5 名更新图表
        const top5 = list.slice(0, 5)
        const titles = top5.map(item => item.title).reverse()
        const values = top5.map(item => item.boxOffice).reverse()

        if (rankChart) {
            rankChart.setOption({
                yAxis: { data: titles.length > 0 ? titles : ['暂无数据'] },
                series: [{ data: values.length > 0 ? values : [0] }]
            })
        }
    } catch (e) {
        console.error("获取数据失败", e)
    } finally {
        loadingData.value = false
    }
}

// =================== 图表初始化与更新 ===================
const initTrendChart = () => {
    trendChart = echarts.init(trendChartRef.value)
    const option = {
        tooltip: { trigger: 'axis', backgroundColor: 'rgba(255, 255, 255, 0.9)', borderRadius: 12, borderWidth: 0, textStyle: { color: colors.text } },
        grid: { top: 20, right: 10, bottom: 20, left: 40, containLabel: true },
        xAxis: { type: 'category', data: [], axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: colors.textLight, margin: 16 } },
        yAxis: { type: 'value', splitLine: { show: false }, axisLabel: { color: colors.textLight } },
        series: [{
            data: [], type: 'line', smooth: true, symbol: 'none',
            lineStyle: { width: 4, color: colors.primary },
            areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(99, 102, 241, 0.4)' }, { offset: 1, color: 'rgba(99, 102, 241, 0.0)' }]) }
        }]
    }
    trendChart.setOption(option)
    updateTrendChart() // 初始化时加载一次数据
}

// 模拟趋势图切换逻辑
const updateTrendChart = () => {
    if (!trendChart) return

    let xAxisData = []
    let seriesData = []

    if (trendTimeRange.value === '7days') {
        xAxisData = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
        seriesData = [1200, 1500, 800, 1340, 2900, 4300, 3900]
    } else {
        // 生成 30 天的模拟数据
        xAxisData = Array.from({ length: 30 }, (_, i) => `${i + 1}号`)
        seriesData = Array.from({ length: 30 }, () => Math.floor(Math.random() * 5000) + 1000)
    }

    trendChart.setOption({
        xAxis: { data: xAxisData },
        series: [{ data: seriesData }]
    })
}

const initRankChart = () => {
    rankChart = echarts.init(rankChartRef.value)
    const option = {
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, backgroundColor: 'rgba(255, 255, 255, 0.9)', borderRadius: 12, borderWidth: 0 },
        grid: { top: 20, right: 80, bottom: 20, left: 10, containLabel: true },
        xAxis: { type: 'value', splitLine: { show: false }, axisLabel: { show: false } },
        yAxis: {
            type: 'category', data: [],
            axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: colors.text, fontWeight: 500 }
        },
        series: [{
            type: 'bar', data: [], barWidth: 16,
            itemStyle: {
                borderRadius: [0, 8, 8, 0],
                color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [{ offset: 0, color: colors.secondary }, { offset: 1, color: colors.primary }])
            },
            label: { show: true, position: 'right', formatter: '¥ {c}', color: colors.primary, fontWeight: 700 }
        }]
    }
    rankChart.setOption(option)
}

const initPieChart = () => {
    pieChart = echarts.init(pieChartRef.value)
    const option = {
        tooltip: { trigger: 'item', backgroundColor: 'rgba(255, 255, 255, 0.9)', borderRadius: 12, borderWidth: 0 },
        legend: { bottom: '0', icon: 'circle', itemGap: 20, textStyle: { color: colors.textLight } },
        series: [{
            type: 'pie', radius: ['50%', '75%'], center: ['50%', '40%'], avoidLabelOverlap: false,
            itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 }, label: { show: false },
            data: [
                { value: 1048, name: '18-24岁 (主)', itemStyle: { color: colors.primary } },
                { value: 735, name: '25-34岁', itemStyle: { color: colors.secondary } },
                { value: 480, name: '35岁+', itemStyle: { color: colors.tertiary } }
            ]
        }]
    }
    pieChart.setOption(option)
}

onMounted(() => {
    initTrendChart()
    initRankChart()
    initPieChart()
    fetchRealBoxOfficeData() // 初始拉取总榜真实数据

    window.addEventListener('resize', () => {
        trendChart?.resize(); rankChart?.resize(); pieChart?.resize()
    })
})

onUnmounted(() => {
    window.removeEventListener('resize', () => { })
    trendChart?.dispose(); rankChart?.dispose(); pieChart?.dispose()
})
</script>

<style scoped lang="scss">
/* ====== 样式保持完全一致，无需改动 ====== */
$bg-color: #f8fafc;
$card-radius: 24px;
$shadow-soft: 0 10px 25px -5px rgba(0, 0, 0, 0.04);
$shadow-hover: 0 20px 35px -5px rgba(0, 0, 0, 0.08);
$text-main: #0f172a;
$text-muted: #64748b;
$primary-gradient: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);

.dashboard-layout {
    min-height: 100vh;
    background-color: $bg-color;
    padding-bottom: 40px;
}

.glass-header {
    position: sticky;
    top: 0;
    z-index: 100;
    background: rgba(248, 250, 252, 0.7);
    backdrop-filter: blur(16px);
    padding: 16px 40px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.3);
}

.header-content {
    max-width: 1440px;
    margin: 0 auto;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .page-title {
        margin: 0;
        font-size: 24px;
        color: $text-main;
    }

    .user-profile {
        display: flex;
        align-items: center;
        gap: 12px;

        .greeting {
            font-weight: 600;
            color: $text-muted;
        }
    }
}

.bento-container {
    max-width: 1440px;
    margin: 32px auto 0;
    padding: 0 40px;
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    grid-auto-rows: minmax(180px, auto);
    gap: 32px;
}

.span-2 {
    grid-column: span 2;
}

.span-3 {
    grid-column: span 3;
}

:deep(.el-card) {
    border: none !important;
    border-radius: $card-radius !important;
    background: #ffffff;
    box-shadow: $shadow-soft !important;
    transition: transform 0.4s ease, box-shadow 0.4s ease !important;
    overflow: visible;
}

:deep(.el-card:hover) {
    transform: translateY(-6px) !important;
    box-shadow: $shadow-hover !important;
}

:deep(.el-card__body) {
    padding: 28px !important;
    height: 100%;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
}

.card-title {
    margin: 0 0 20px 0;
    font-size: 18px;
    font-weight: 600;
    color: $text-main;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .card-title {
        margin: 0;
    }
}

.highlight-card {
    background: $primary-gradient !important;
    color: #fff;

    .stat-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        opacity: 0.9;
        font-weight: 500;
    }

    .stat-value {
        font-size: 48px;
        font-weight: 700;
        margin: 16px 0;
        letter-spacing: -1px;
    }

    .stat-trend {
        font-size: 14px;
        opacity: 0.9;
    }
}

.action-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
    height: 100%;
}

:deep(.custom-btn) {
    height: 100%;
    min-height: 80px;
    border-radius: 16px;
    font-weight: 700;
    font-size: 16px;
    border: none;
    transition: all 0.3s ease;

    &.el-button--primary.is-plain {
        color: #4f46e5;
        background: #e0e7ff;

        &:hover {
            background: #6366f1;
            color: #fff;
            transform: scale(1.03);
        }
    }

    &.el-button--success.is-plain {
        color: #059669;
        background: #d1fae5;

        &:hover {
            background: #10b981;
            color: #fff;
            transform: scale(1.03);
        }
    }
}

:deep(.custom-radio) {
    background: $bg-color;
    padding: 4px;
    border-radius: 12px;

    .el-radio-button__inner {
        border: none !important;
        background: transparent;
        border-radius: 8px !important;
        color: $text-muted;
        font-weight: 600;
        box-shadow: none !important;
    }

    .el-radio-button__original-radio:checked+.el-radio-button__inner {
        background: #fff;
        color: #0f172a;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05) !important;
    }
}

.chart-container {
    flex: 1;
    width: 100%;
    min-height: 260px;
}
</style>