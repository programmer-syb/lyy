<template>
    <div class="admin-schedules">
        <div class="page-header">
            <h2>排片调度</h2>
            <el-button type="primary" @click="openAddDialog">
                <el-icon>
                    <Calendar />
                </el-icon> 新增场次
            </el-button>
        </div>

        <el-card class="filter-card glass-panel" shadow="never">
            <el-form :inline="true" :model="queryParams" size="default">
                <el-form-item label="放映日期">
                    <el-date-picker v-model="queryParams.showDate" type="date" value-format="YYYY-MM-DD"
                        placeholder="选择日期" @change="fetchData" />
                </el-form-item>
                <el-form-item label="影厅">
                    <el-select v-model="queryParams.hallId" placeholder="全部影厅" clearable @change="fetchData">
                        <el-option v-for="h in hallList" :key="h.id" :label="h.name" :value="h.id" />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button @click="resetQuery">重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>

        <el-card class="table-card" shadow="never">
            <el-table :data="tableData" v-loading="loading" stripe>
                <el-table-column prop="showDate" label="放映日期" width="120" />
                <el-table-column prop="showTime" label="时间" width="100">
                    <template #default="{ row }"><strong>{{ row.showTime.substring(0, 5) }}</strong></template>
                </el-table-column>
                <el-table-column label="影片信息" min-width="200">
                    <template #default="{ row }">
                        <div class="movie-cell">
                            <img :src="getMoviePoster(row.movieId)" class="mini-poster" />
                            <span>{{ getMovieTitle(row.movieId) }}</span>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column prop="hallId" label="放映影厅" width="180">
                    <template #default="{ row }">{{ getHallName(row.hallId) }}</template>
                </el-table-column>
                <el-table-column prop="price" label="票价" width="120">
                    <template #default="{ row }"><span class="price-text">¥ {{ row.price }}</span></template>
                </el-table-column>
                <el-table-column label="操作" width="150" fixed="right">
                    <template #default="{ row }">
                        <el-button size="small" link type="primary" @click="openEditDialog(row)">编辑</el-button>
                        <el-popconfirm title="确定取消此场次吗？" @confirm="handleDelete(row.id)">
                            <template #reference><el-button size="small" link type="danger">取消</el-button></template>
                        </el-popconfirm>
                    </template>
                </el-table-column>
            </el-table>
            <div class="pagination-wrapper">
                <el-pagination background layout="total, prev, pager, next" :total="total"
                    v-model:current-page="queryParams.current" @current-change="fetchData" />
            </div>
        </el-card>

        <el-dialog :title="dialogType === 'add' ? '新增场次' : '修改场次'" v-model="dialogVisible" width="500px">
            <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
                <el-form-item label="选择影片" prop="movieId">
                    <el-select v-model="form.movieId" placeholder="请选择电影" style="width:100%" filterable>
                        <el-option v-for="m in movieList" :key="m.id" :label="m.title" :value="m.id" />
                    </el-select>
                </el-form-item>
                <el-form-item label="选择影厅" prop="hallId">
                    <el-select v-model="form.hallId" placeholder="请选择影厅" style="width:100%">
                        <el-option v-for="h in hallList" :key="h.id" :label="h.name" :value="h.id" />
                    </el-select>
                </el-form-item>
                <el-row :gutter="20">
                    <el-col :span="14">
                        <el-form-item label="日期" prop="showDate">
                            <el-date-picker v-model="form.showDate" type="date" value-format="YYYY-MM-DD"
                                style="width:100%" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="10">
                        <el-form-item label="时间" prop="showTime" label-width="50px">
                            <el-time-picker v-model="form.showTime" format="HH:mm" value-format="HH:mm:ss"
                                style="width:100%" />
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-form-item label="票价" prop="price">
                    <el-input-number v-model="form.price" :min="0" :precision="2" :step="5" style="width:100%" />
                </el-form-item>
            </el-form>
            <div class="conflict-notice">
                <el-icon>
                    <InfoFilled />
                </el-icon> 系统将自动校验影厅时间冲突，包含15分钟打扫时间
            </div>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存排片</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Calendar, InfoFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const movieList = ref([])
const hallList = ref([])

const queryParams = reactive({ current: 1, size: 10, showDate: '', hallId: null })
const dialogVisible = ref(false)
const dialogType = ref('add')
const submitLoading = ref(false)
const formRef = ref(null)

const form = reactive({ id: null, movieId: null, hallId: null, showDate: '', showTime: '', price: 40 })
const rules = {
    movieId: [{ required: true, message: '请选择电影' }],
    hallId: [{ required: true, message: '请选择影厅' }],
    showDate: [{ required: true, message: '请选择日期' }],
    showTime: [{ required: true, message: '请选择时间' }]
}

const fetchData = async () => {
    loading.value = true;
    try {
        // 动态构建参数，只发送有值的字段
        const params = {
            current: queryParams.current,
            size: queryParams.size
        };

        if (queryParams.showDate) params.showDate = queryParams.showDate;
        if (queryParams.hallId) params.hallId = queryParams.hallId;
        if (queryParams.movieId) params.movieId = queryParams.movieId;

        // 使用 axios 自动处理对象转 URL 参数
        const res = await request.get('/api/admin/sys/schedule/page', { params });

        tableData.value = res.records || [];
        total.value = res.total || 0;
    } catch (e) {
        console.error("加载排片失败", e);
    } finally {
        loading.value = false;
    }
};

const fetchBasics = async () => {
    const [movies, halls] = await Promise.all([
        request.post('/api/movie/page', { current: 1, size: 100 }),
        request.get('/api/admin/sys/hall/list')
    ])
    movieList.value = movies.records || []
    hallList.value = halls || []
}

const getMovieTitle = (id) => movieList.value.find(m => m.id === id)?.title || '未知电影'
const getMoviePoster = (id) => movieList.value.find(m => m.id === id)?.posterUrl || ''
const getHallName = (id) => hallList.value.find(h => h.id === id)?.name || '未知影厅'

const openAddDialog = () => {
    dialogType.value = 'add'
    Object.assign(form, { id: null, movieId: null, hallId: null, showDate: '', showTime: '', price: 40 })
    dialogVisible.value = true
}

const openEditDialog = (row) => {
    dialogType.value = 'edit'
    Object.assign(form, row)
    dialogVisible.value = true
}

const handleSubmit = () => {
    formRef.value?.validate(async (valid) => {
        if (!valid) return
        submitLoading.value = true
        try {
            const url = dialogType.value === 'add' ? '/api/admin/sys/schedule/add' : '/api/admin/sys/schedule/update'
            await request[dialogType.value === 'add' ? 'post' : 'put'](url, form)
            ElMessage.success('操作成功')
            dialogVisible.value = false
            fetchData()
        } catch (e) {
            // 冲突校验报错由 request.js 自动弹出
        } finally { submitLoading.value = false }
    })
}

const handleDelete = async (id) => {
    try {
        await request.delete(`/api/admin/sys/schedule/delete/${id}`)
        ElMessage.success('场次已成功取消')
        fetchData()
    } catch (e) { }
}

const resetQuery = () => { Object.assign(queryParams, { showDate: '', hallId: null }); fetchData(); }

onMounted(() => { fetchData(); fetchBasics(); })
</script>

<style scoped lang="scss">
.admin-schedules {
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.filter-card {
    border-radius: 16px;
    margin-bottom: 8px;
}

.movie-cell {
    display: flex;
    align-items: center;
    gap: 12px;

    .mini-poster {
        width: 36px;
        height: 50px;
        border-radius: 4px;
        object-fit: cover;
    }
}

.price-text {
    color: #ef4444;
    font-weight: 700;
}

.pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 24px;
}

.conflict-notice {
    margin-top: 16px;
    padding: 12px;
    background: #fff7ed;
    color: #c2410b;
    border-radius: 8px;
    font-size: 13px;
    display: flex;
    align-items: center;
    gap: 8px;
}
</style>