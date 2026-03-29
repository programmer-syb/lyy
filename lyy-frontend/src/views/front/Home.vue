<template>
    <div class="home-container">

        <section class="hero-section" v-loading="loadingHot">
            <div class="section-header">
                <h2 class="section-title">热映榜单</h2>
                <span class="section-subtitle">Trending Now</span>
            </div>

            <el-carousel :interval="4000" type="card" height="400px" indicator-position="outside">
                <el-carousel-item v-for="movie in hotMovies" :key="movie.id">
                    <div class="carousel-card" :style="{ backgroundImage: `url(${movie.posterUrl || defaultPoster})` }">
                        <div class="movie-overlay">
                            <div class="movie-info">
                                <h3 class="movie-title">{{ movie.title }}</h3>
                                <div class="movie-meta">
                                    <el-tag effect="dark" round color="rgba(99, 102, 241, 0.8)" style="border: none">
                                        {{ movie.globalRating ? movie.globalRating + ' 分' : '暂无评分' }}
                                    </el-tag>
                                    <span class="genres">{{ movie.genres }}</span>
                                </div>
                            </div>
                            <el-button type="primary" class="glass-btn" @click="goToDetail(movie.id)">查看详情</el-button>
                        </div>
                    </div>
                </el-carousel-item>
            </el-carousel>
        </section>

        <section class="recommend-section" v-loading="loadingRec">
            <div class="section-header">
                <h2 class="section-title">猜你喜欢</h2>
                <span class="section-subtitle">For You</span>
            </div>

            <div class="bento-grid" v-if="recommendMovies.length > 0">
                <el-card v-for="(movie, index) in recommendMovies" :key="movie.id" class="movie-card"
                    :class="{ 'featured': index === 0 }" shadow="hover" @click="goToDetail(movie.id)">
                    <div class="poster-wrapper">
                        <img :src="movie.posterUrl || defaultPoster" alt="poster" class="poster-img" loading="lazy">
                        <div class="rating-badge" v-if="movie.globalRating">
                            {{ movie.globalRating }}
                        </div>
                    </div>
                    <div class="card-content">
                        <h4 class="card-title text-ellipsis">{{ movie.title }}</h4>
                        <p class="card-desc text-ellipsis">{{ movie.genres }}</p>
                    </div>
                </el-card>
            </div>

            <el-empty v-else description="暂无推荐数据，快去给喜欢的电影打分吧！" :image-size="200" />
        </section>

    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'

const router = useRouter()
const userStore = useUserStore()

const loadingHot = ref(true)
const loadingRec = ref(true)

const hotMovies = ref([])
const recommendMovies = ref([])

// 默认占位海报（万一数据库里没图片）
const defaultPoster = 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?ixlib=rb-4.0.3&auto=format&fit=crop&w=500&q=60'

// 1. 获取热门电影 (RC-01)
const fetchHotMovies = async () => {
    try {
        loadingHot.value = true
        const res = await request.get('/api/recommend/hot?limit=5')
        hotMovies.value = res || []
    } catch (error) {
        console.error('获取热门电影失败', error)
    } finally {
        loadingHot.value = false
    }
}

// 2. 获取个性化推荐 (智能降级策略)
const fetchRecommendMovies = async () => {
    loadingRec.value = true
    try {
        if (!userStore.token) {
            // 游客模式：直接用热门电影顶替“猜你喜欢”
            const res = await request.get('/api/recommend/hot?limit=8')
            recommendMovies.value = res || []
            return
        }

        // 已登录：先尝试调 SVD 推荐 (RC-02)
        let res = await request.get('/api/recommend/svd')

        if (!res || res.length === 0) {
            // 如果 SVD 没数据，降级调用 冷启动推荐 (RC-05)
            res = await request.get('/api/recommend/cold-start')
        }

        recommendMovies.value = res || []
    } catch (error) {
        console.error('获取推荐失败', error)
    } finally {
        loadingRec.value = false
    }
}

const goToDetail = (movieId) => {
    // 预留的电影详情页路由跳转
    console.log('Navigating to movie:', movieId)
    router.push(`/movie/${movieId}`)
}

onMounted(() => {
    fetchHotMovies()
    fetchRecommendMovies()
})
</script>

<style scoped lang="scss">
.home-container {
    display: flex;
    flex-direction: column;
    gap: 48px;
}

.section-header {
    margin-bottom: 24px;
    display: flex;
    align-items: baseline;
    gap: 12px;

    .section-title {
        font-size: 28px;
        font-weight: 700;
        margin: 0;
        color: var(--text-main);
    }

    .section-subtitle {
        font-size: 16px;
        font-weight: 500;
        color: var(--color-primary);
        opacity: 0.8;
    }
}

/* 巨幕轮播卡片样式 */
.carousel-card {
    width: 100%;
    height: 100%;
    border-radius: var(--radius-large);
    background-size: cover;
    background-position: center;
    position: relative;
    overflow: hidden;

    /* 毛玻璃底部遮罩 */
    .movie-overlay {
        position: absolute;
        bottom: 0;
        left: 0;
        width: 100%;
        padding: 30px;
        box-sizing: border-box;
        background: linear-gradient(to top, rgba(0, 0, 0, 0.9) 0%, rgba(0, 0, 0, 0.5) 60%, transparent 100%);
        backdrop-filter: blur(4px);
        display: flex;
        justify-content: space-between;
        align-items: flex-end;

        .movie-info {
            color: #fff;

            .movie-title {
                font-size: 28px;
                margin: 0 0 12px 0;
                font-weight: 700;
                text-shadow: 0 2px 8px rgba(0, 0, 0, 0.5);
            }

            .movie-meta {
                display: flex;
                align-items: center;
                gap: 12px;

                .genres {
                    font-size: 14px;
                    opacity: 0.8;
                }
            }
        }

        .glass-btn {
            background: rgba(255, 255, 255, 0.15);
            border: 1px solid rgba(255, 255, 255, 0.3);
            color: #fff;
            backdrop-filter: blur(10px);

            &:hover {
                background: rgba(255, 255, 255, 0.25);
                transform: translateY(-2px);
            }
        }
    }
}

/* 深度重塑 Element Carousel，去掉原生难看的边框和角 */
:deep(.el-carousel__item) {
    border-radius: var(--radius-large);
    border: none;
}

:deep(.el-carousel__mask) {
    border-radius: var(--radius-large);
    background: rgba(255, 255, 255, 0.5);
}

/* 猜你喜欢 Bento Grid 排版 */
.bento-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 24px;
}

.movie-card {
    cursor: pointer;
    padding: 0 !important;
    /* 覆盖全局卡片的 padding */

    /* 第一张卡片放大，打破网格单调感 (Bento 核心要素) */
    &.featured {
        grid-column: span 2;
        grid-row: span 2;

        .poster-wrapper {
            height: 400px;
        }

        .card-title {
            font-size: 22px;
        }
    }

    .poster-wrapper {
        width: 100%;
        height: 300px;
        position: relative;
        overflow: hidden;

        .poster-img {
            width: 100%;
            height: 100%;
            object-fit: cover;
            transition: transform 0.5s cubic-bezier(0.25, 1, 0.5, 1);
        }

        .rating-badge {
            position: absolute;
            top: 16px;
            right: 16px;
            background: rgba(0, 0, 0, 0.6);
            backdrop-filter: blur(8px);
            color: #fbbf24;
            font-weight: 700;
            padding: 6px 12px;
            border-radius: 12px;
            font-size: 14px;
            border: 1px solid rgba(255, 255, 255, 0.1);
        }
    }

    &:hover .poster-img {
        transform: scale(1.05);
        /* 图片微放大互动 */
    }

    .card-content {
        padding: 16px 20px;

        .card-title {
            margin: 0 0 6px 0;
            font-size: 16px;
            color: var(--text-main);
        }

        .card-desc {
            margin: 0;
            font-size: 13px;
            color: var(--text-muted);
        }
    }
}

.text-ellipsis {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}
</style>