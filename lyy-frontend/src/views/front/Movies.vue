<template>
    <div class="movies-container">
        <div class="page-header">
            <h1 class="page-title">探索电影</h1>
            <el-input v-model="queryParams.keyword" placeholder="搜索电影名称..." clearable class="search-input"
                @keyup.enter="handleSearch">
                <template #prefix><el-icon>
                        <Search />
                    </el-icon></template>
            </el-input>
        </div>

        <div class="filter-panel glass-panel">
            <div class="filter-row">
                <span class="filter-label">类型：</span>
                <div class="filter-options">
                    <span v-for="genre in genresList" :key="genre" class="filter-pill"
                        :class="{ active: queryParams.genre === (genre === '全部' ? '' : genre) }"
                        @click="selectGenre(genre)">
                        {{ genre }}
                    </span>
                </div>
            </div>
            <div class="filter-row">
                <span class="filter-label">排序：</span>
                <div class="filter-options">
                    <span class="filter-pill" :class="{ active: queryParams.sortBy === '' }"
                        @click="selectSort('')">最新上映</span>
                    <span class="filter-pill" :class="{ active: queryParams.sortBy === 'rating' }"
                        @click="selectSort('rating')">口碑最高</span>
                    <span class="filter-pill" :class="{ active: queryParams.sortBy === 'heat' }"
                        @click="selectSort('heat')">热度最高</span>
                </div>
            </div>
        </div>

        <div class="movies-grid" v-loading="loading">
            <el-card v-for="movie in movieList" :key="movie.id" class="movie-card" shadow="hover"
                @click="goToDetail(movie.id)">
                <div class="poster-wrapper">
                    <img :src="movie.posterUrl || 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=500&q=60'"
                        class="poster">
                    <div class="rating-badge" v-if="movie.globalRating">{{ movie.globalRating }}</div>
                </div>
                <div class="info">
                    <h3 class="title text-ellipsis">{{ movie.title }}</h3>
                    <p class="desc text-ellipsis">{{ movie.genres }} | {{ movie.duration || 120 }}分钟</p>
                </div>
            </el-card>
        </div>

        <el-empty v-if="!loading && movieList.length === 0" description="没有找到符合条件的电影" />

        <div class="pagination-wrapper" v-if="total > 0">
            <el-pagination background layout="prev, pager, next" :total="total" :page-size="queryParams.size"
                v-model:current-page="queryParams.current" @current-change="fetchMovies" />
        </div>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const loading = ref(false)
const movieList = ref([])
const total = ref(0)

const genresList = ['全部', '科幻', '动作', '悬疑', '喜剧', '爱情', '动画', '灾难', '历史']

const queryParams = reactive({
    current: 1,
    size: 12,
    keyword: '',
    genre: '',
    sortBy: ''
})

const fetchMovies = async () => {
    loading.value = true
    try {
        const res = await request.post('/api/movie/page', queryParams)
        movieList.value = res.records || []
        total.value = res.total || 0
    } catch (e) {
        console.error(e)
    } finally {
        loading.value = false
    }
}

const handleSearch = () => { queryParams.current = 1; fetchMovies() }
const selectGenre = (genre) => { queryParams.genre = genre === '全部' ? '' : genre; handleSearch() }
const selectSort = (sort) => { queryParams.sortBy = sort; handleSearch() }
const goToDetail = (id) => { router.push(`/movie/${id}`) }

onMounted(() => fetchMovies())
</script>

<style scoped lang="scss">
.movies-container {
    display: flex;
    flex-direction: column;
    gap: 32px;
}

.page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.page-title {
    font-size: 32px;
    margin: 0;
}

.search-input {
    width: 300px;
}

.filter-panel {
    padding: 24px 32px;
    background: rgba(255, 255, 255, 0.6);
    backdrop-filter: blur(20px);
    border-radius: var(--radius-large);
    border: 1px solid rgba(255, 255, 255, 0.8);
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.filter-row {
    display: flex;
    align-items: flex-start;
    gap: 16px;
}

.filter-label {
    font-weight: 600;
    color: var(--text-muted);
    width: 60px;
    line-height: 32px;
}

.filter-options {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    flex: 1;
}

.filter-pill {
    padding: 6px 16px;
    border-radius: 20px;
    font-size: 14px;
    color: var(--text-main);
    background: rgba(0, 0, 0, 0.03);
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
        background: rgba(0, 0, 0, 0.08);
    }

    &.active {
        background: var(--color-primary);
        color: #fff;
        font-weight: 600;
        box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
    }
}

.movies-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 24px;
}

.movie-card {
    padding: 0 !important;
    cursor: pointer;

    .poster-wrapper {
        height: 340px;
        position: relative;
        overflow: hidden;

        .poster {
            width: 100%;
            height: 100%;
            object-fit: cover;
            transition: transform 0.4s ease;
        }

        .rating-badge {
            position: absolute;
            top: 12px;
            right: 12px;
            background: rgba(0, 0, 0, 0.7);
            color: #fbbf24;
            padding: 4px 10px;
            border-radius: 12px;
            font-weight: bold;
            backdrop-filter: blur(4px);
        }
    }

    &:hover .poster {
        transform: scale(1.05);
    }

    .info {
        padding: 16px 20px;

        .title {
            margin: 0 0 6px 0;
            font-size: 18px;
        }

        .desc {
            margin: 0;
            color: var(--text-muted);
            font-size: 13px;
        }
    }
}

.pagination-wrapper {
    display: flex;
    justify-content: center;
    margin-top: 20px;
}
</style>