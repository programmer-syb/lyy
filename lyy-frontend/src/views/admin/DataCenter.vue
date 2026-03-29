<template>
    <div class="data-center-container">
        <div class="page-header">
            <div class="title-info">
                <h2>数据中枢</h2>
                <p class="subtitle">管理系统底层数据集，同步全球影视元数据</p>
            </div>
            <el-tag type="danger" effect="dark" round>核心机密区域</el-tag>
        </div>

        <div class="bento-grid">

            <el-card class="bento-card import-card">
                <div class="card-icon movies"><el-icon>
                        <Film />
                    </el-icon></div>
                <h3 class="card-title">MovieLens 电影导入</h3>
                <p class="card-desc">支持导入标准 CSV 格式电影数据，包含 ID、片名及类型标签。</p>

                <el-upload drag action="http://localhost:8080/api/admin/sys/data/import-movies" :headers="uploadHeaders"
                    :on-success="() => handleSuccess('电影数据导入成功')" :on-error="handleError" accept=".csv"
                    class="custom-uploader">
                    <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                    <div class="el-upload__text">将 movies.csv 拖到此处，或<em>点击上传</em></div>
                </el-upload>
            </el-card>

            <el-card class="bento-card import-card">
                <div class="card-icon ratings"><el-icon>
                        <Star />
                    </el-icon></div>
                <h3 class="card-title">用户评分导入</h3>
                <p class="card-desc">导入大规模用户评分样本，为协同过滤推荐算法提供训练基石。</p>

                <el-upload drag action="http://localhost:8080/api/admin/sys/data/import-ratings"
                    :headers="uploadHeaders" :on-success="() => handleSuccess('评分数据导入成功')" :on-error="handleError"
                    accept=".csv" class="custom-uploader">
                    <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                    <div class="el-upload__text">将 ratings.csv 拖到此处，或<em>点击上传</em></div>
                </el-upload>
            </el-card>

            <el-card class="bento-card sync-card span-2">
                <div class="sync-content">
                    <div class="text-zone">
                        <h3 class="card-title">TMDB 自动补全计划</h3>
                        <p class="card-desc">
                            MovieLens 数据仅包含文本。点击启动同步后，系统将自动连接 TMDB API，
                            为数据库中缺失信息的电影抓取：<strong>高清海报、剧情简介、导演信息</strong>。
                        </p>
                        <div class="sync-status" v-if="isSyncing">
                            <el-icon class="is-loading">
                                <Loading />
                            </el-icon>
                            正在与 tmdb.org 建立连接并同步数据...
                        </div>
                    </div>
                    <el-button type="primary" size="large" class="sync-btn" :loading="isSyncing" @click="triggerSync">
                        <el-icon>
                            <Refresh />
                        </el-icon> 启动元数据同步
                    </el-button>
                </div>
            </el-card>

            <el-card class="bento-card stats-card">
                <h3 class="card-title">系统数据规模</h3>
                <div class="stats-list">
                    <div class="stats-item">
                        <span class="label">存量电影</span>
                        <span class="value">1,248</span>
                    </div>
                    <div class="stats-item">
                        <span class="label">累积评分</span>
                        <span class="value">10W+</span>
                    </div>
                    <div class="stats-item">
                        <span class="label">覆盖类型</span>
                        <span class="value">24 种</span>
                    </div>
                </div>
            </el-card>

        </div>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { Film, Star, UploadFilled, Refresh, Loading } from '@element-plus/icons-vue'
import { ElMessage, ElNotification } from 'element-plus'
import request from '@/utils/request'

const uploadHeaders = { Authorization: localStorage.getItem('adminToken') }
const isSyncing = ref(false)

const handleSuccess = (msg) => {
    ElNotification({ title: '处理成功', message: msg, type: 'success', position: 'bottom-right' })
}

const handleError = () => {
    ElMessage.error('上传解析失败，请检查 CSV 格式')
}

const triggerSync = async () => {
    isSyncing.value = true
    try {
        const res = await request.post('/api/admin/sys/data/sync-tmdb')
        ElNotification({
            title: '同步完成',
            message: res || '电影元数据已补全',
            type: 'success',
            duration: 5000
        })
    } catch (e) {
    } finally {
        isSyncing.value = false
    }
}
</script>

<style scoped lang="scss">
.data-center-container {
    display: flex;
    flex-direction: column;
    gap: 32px;
}

.page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .subtitle {
        color: #64748b;
        font-size: 14px;
        margin-top: 8px;
    }
}

/* Bento Grid */
.bento-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    grid-auto-rows: minmax(300px, auto);
    gap: 32px;
}

.span-2 {
    grid-column: span 2;
}

.bento-card {
    border-radius: 24px;
    position: relative;
    overflow: hidden;

    .card-icon {
        width: 56px;
        height: 56px;
        border-radius: 16px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 28px;
        margin-bottom: 24px;

        &.movies {
            background: #e0e7ff;
            color: #6366f1;
        }

        &.ratings {
            background: #fef3c7;
            color: #f59e0b;
        }
    }

    .card-title {
        font-size: 20px;
        font-weight: 700;
        margin: 0 0 12px 0;
        color: #0f172a;
    }

    .card-desc {
        font-size: 14px;
        color: #64748b;
        line-height: 1.6;
        margin-bottom: 24px;
    }
}

/* Uploader 样式重塑 */
.custom-uploader {
    :deep(.el-upload-dragger) {
        border-radius: 16px;
        border: 2px dashed #e2e8f0;
        background: #f8fafc;
        transition: all 0.3s ease;

        &:hover {
            border-color: #6366f1;
            background: #fff;
        }
    }
}

/* Sync Card 深度定制 */
.sync-card {
    background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
    color: #fff;

    .sync-content {
        display: flex;
        justify-content: space-between;
        align-items: center;
        height: 100%;

        .text-zone {
            flex: 1;
            padding-right: 40px;

            .card-title {
                color: #fff;
            }

            .card-desc {
                color: #94a3b8;
            }

            .sync-status {
                font-size: 14px;
                color: #38bdf8;
                display: flex;
                align-items: center;
                gap: 10px;
                margin-top: 20px;
            }
        }
    }

    .sync-btn {
        padding: 28px 40px;
        border-radius: 20px;
        font-size: 18px;
        font-weight: 700;
        box-shadow: 0 10px 30px rgba(99, 102, 241, 0.4);
    }
}

.stats-card {
    .stats-list {
        display: flex;
        flex-direction: column;
        gap: 20px;
        margin-top: 10px;

        .stats-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding-bottom: 12px;
            border-bottom: 1px solid #f1f5f9;

            .label {
                color: #64748b;
                font-weight: 500;
            }

            .value {
                color: #0f172a;
                font-weight: 700;
                font-size: 18px;
            }
        }
    }
}
</style>