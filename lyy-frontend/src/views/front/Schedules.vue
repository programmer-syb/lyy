<template>
    <div class="schedules-dashboard">
        <div class="page-header">
            <h1 class="page-title">极速购票</h1>
            <p class="subtitle">Quick Ticketing</p>
        </div>

        <div class="dashboard-layout glass-panel">
            <div class="movie-sidebar" v-loading="loadingMovies">
                <div class="sidebar-header">选择影片</div>
                <div class="movie-list">
                    <div v-for="movie in movieList" :key="movie.id" class="movie-item"
                        :class="{ active: selectedMovie?.id === movie.id }" @click="selectMovie(movie)">
                        <img :src="movie.posterUrl" class="mini-poster" />
                        <div class="m-info">
                            <h4 class="m-title text-ellipsis">{{ movie.title }}</h4>
                            <span class="m-rating" v-if="movie.globalRating">{{ movie.globalRating }} 分</span>
                        </div>
                        <el-icon class="arrow-icon">
                            <ArrowRight />
                        </el-icon>
                    </div>
                </div>
            </div>

            <div class="schedule-main">
                <template v-if="selectedMovie">
                    <div class="main-header">
                        <h2 class="selected-title">{{ selectedMovie.title }}</h2>
                        <el-radio-group v-model="selectedDate" size="large" @change="fetchSchedules"
                            class="custom-radio">
                            <el-radio-button :label="today">今天 {{ today.substring(5) }}</el-radio-button>
                            <el-radio-button :label="tomorrow">明天 {{ tomorrow.substring(5) }}</el-radio-button>
                        </el-radio-group>
                    </div>

                    <div class="schedules-grid" v-loading="loadingSchedules">
                        <el-empty v-if="schedules.length === 0" description="该影片当日暂无排片" />

                        <el-card v-for="sc in schedules" :key="sc.scheduleId" class="sc-card" shadow="hover">
                            <div class="sc-time">{{ sc.showTime.substring(0, 5) }}</div>
                            <div class="sc-hall">{{ sc.hallName }}</div>
                            <div class="sc-price">¥ {{ sc.price }}</div>
                            <el-button type="primary" round class="buy-btn"
                                @click="$router.push(`/movie/${selectedMovie.id}`)">
                                去选座
                            </el-button>
                        </el-card>
                    </div>
                </template>

                <div v-else class="empty-state">
                    <el-icon :size="60" color="#cbd5e1">
                        <Ticket />
                    </el-icon>
                    <p>请在左侧选择一部影片</p>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ArrowRight, Ticket } from '@element-plus/icons-vue'
import request from '@/utils/request'
import dayjs from 'dayjs'

const loadingMovies = ref(false)
const loadingSchedules = ref(false)

const movieList = ref([])
const selectedMovie = ref(null)

const today = dayjs().format('YYYY-MM-DD')
const tomorrow = dayjs().add(1, 'day').format('YYYY-MM-DD')
const selectedDate = ref(today)
const schedules = ref([])

// 1. 获取近期热映电影列表 (直接复用分页接口，取前20部)
const fetchMovies = async () => {
    loadingMovies.value = true
    try {
        const res = await request.post('/api/movie/page', { current: 1, size: 20 })
        movieList.value = res.records || []
        if (movieList.value.length > 0) {
            selectMovie(movieList.value[0]) // 默认选中第一部
        }
    } catch (e) { } finally { loadingMovies.value = false }
}

// 2. 选中电影
const selectMovie = (movie) => {
    selectedMovie.value = movie
    selectedDate.value = today
    fetchSchedules()
}

// 3. 获取选中电影的排片
const fetchSchedules = async () => {
    if (!selectedMovie.value) return
    loadingSchedules.value = true
    try {
        schedules.value = await request.get(`/api/schedule/list?movieId=${selectedMovie.value.id}&showDate=${selectedDate.value}`)
    } catch (e) { } finally { loadingSchedules.value = false }
}

onMounted(() => {
    fetchMovies()
})
</script>

<style scoped lang="scss">
.schedules-dashboard {
    display: flex;
    flex-direction: column;
    gap: 24px;
    height: calc(100vh - 120px);
}

.page-header {
    .page-title {
        font-size: 32px;
        margin: 0;
    }

    .subtitle {
        color: var(--color-primary);
        font-weight: 600;
        margin: 8px 0 0 0;
    }
}

.glass-panel {
    background: rgba(255, 255, 255, 0.7);
    backdrop-filter: blur(20px);
    border: 1px solid rgba(255, 255, 255, 0.8);
    border-radius: var(--radius-large);
    box-shadow: var(--shadow-soft);
}

.dashboard-layout {
    display: flex;
    flex: 1;
    overflow: hidden;
}

/* 左侧电影列表 */
.movie-sidebar {
    width: 320px;
    border-right: 1px solid rgba(0, 0, 0, 0.05);
    display: flex;
    flex-direction: column;
    background: rgba(255, 255, 255, 0.4);

    .sidebar-header {
        padding: 20px 24px;
        font-weight: 700;
        font-size: 18px;
        border-bottom: 1px solid rgba(0, 0, 0, 0.05);
    }

    .movie-list {
        flex: 1;
        overflow-y: auto;
        padding: 12px;

        &::-webkit-scrollbar {
            width: 6px;
        }

        &::-webkit-scrollbar-thumb {
            background: #cbd5e1;
            border-radius: 4px;
        }
    }
}

.movie-item {
    display: flex;
    align-items: center;
    padding: 12px;
    gap: 16px;
    border-radius: 16px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
        background: rgba(255, 255, 255, 0.8);
    }

    &.active {
        background: #fff;
        box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
        border: 1px solid var(--color-primary-light);

        .m-title {
            color: var(--color-primary);
        }

        .arrow-icon {
            opacity: 1;
            color: var(--color-primary);
            transform: translateX(4px);
        }
    }

    .mini-poster {
        width: 48px;
        height: 64px;
        border-radius: 8px;
        object-fit: cover;
    }

    .m-info {
        flex: 1;
        overflow: hidden;

        .m-title {
            margin: 0 0 6px 0;
            font-size: 15px;
        }

        .m-rating {
            font-size: 12px;
            color: #fbbf24;
            font-weight: 700;
            background: rgba(251, 191, 36, 0.1);
            padding: 2px 8px;
            border-radius: 10px;
        }
    }

    .arrow-icon {
        opacity: 0;
        transition: all 0.2s;
    }
}

/* 右侧排片展示区 */
.schedule-main {
    flex: 1;
    display: flex;
    flex-direction: column;
    padding: 32px;
    background: #fff;

    .main-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 32px;

        .selected-title {
            margin: 0;
            font-size: 24px;
        }
    }
}

.schedules-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 20px;
}

.sc-card {
    text-align: center;
    border: 1px solid #f1f5f9;

    .sc-time {
        font-size: 28px;
        font-weight: 700;
        color: var(--text-main);
        margin-bottom: 8px;
    }

    .sc-hall {
        font-size: 14px;
        color: var(--text-muted);
        margin-bottom: 16px;
    }

    .sc-price {
        font-size: 18px;
        color: #ef4444;
        font-weight: 700;
        margin-bottom: 20px;
    }

    .buy-btn {
        width: 100%;
    }
}

.empty-state {
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: var(--text-muted);
    font-size: 16px;
    gap: 16px;
}

.text-ellipsis {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}
</style>