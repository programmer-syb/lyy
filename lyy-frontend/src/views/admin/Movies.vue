<template>
    <div class="admin-movies">
        <div class="page-header">
            <h2>电影管理</h2>
            <div class="actions">
                <el-input v-model="queryParams.title" placeholder="搜索电影名称" clearable @keyup.enter="fetchData"
                    style="width: 250px;">
                    <template #prefix><el-icon>
                            <Search />
                        </el-icon></template>
                </el-input>
                <el-button type="primary" @click="openAddDialog"><el-icon>
                        <Plus />
                    </el-icon> 新增电影</el-button>
            </div>
        </div>

        <el-card class="table-card" shadow="never">
            <el-table :data="tableData" v-loading="loading" style="width: 100%" stripe>
                <el-table-column label="海报" width="90">
                    <template #default="{ row }">
                        <el-image :src="row.posterUrl" class="table-poster" fit="cover"
                            :preview-src-list="[row.posterUrl]" preview-teleported />
                    </template>
                </el-table-column>
                <el-table-column prop="title" label="片名" min-width="160" show-overflow-tooltip>
                    <template #default="{ row }">
                        <span style="font-weight: 600; color: #0f172a;">{{ row.title }}</span>
                    </template>
                </el-table-column>
                <el-table-column prop="genres" label="类型" width="160" show-overflow-tooltip>
                    <template #default="{ row }">
                        <el-tag size="small" type="info" style="margin-right: 4px;"
                            v-for="g in (row.genres || '').split(',')" :key="g">{{ g }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="director" label="导演" width="140" show-overflow-tooltip />
                <el-table-column prop="duration" label="时长" width="100">
                    <template #default="{ row }">{{ row.duration }} 分钟</template>
                </el-table-column>
                <el-table-column prop="globalRating" label="评分" width="80">
                    <template #default="{ row }">
                        <span style="color: #fbbf24; font-weight: bold;"><el-icon>
                                <StarFilled />
                            </el-icon> {{ row.globalRating }}</span>
                    </template>
                </el-table-column>

                <el-table-column label="操作" width="180" fixed="right">
                    <template #default="{ row }">
                        <el-button size="small" type="primary" plain @click="openEditDialog(row)">编辑</el-button>
                        <el-popconfirm title="确定要下架并删除这部电影吗？" @confirm="handleDelete(row.id)">
                            <template #reference>
                                <el-button size="small" type="danger" plain>删除</el-button>
                            </template>
                        </el-popconfirm>
                    </template>
                </el-table-column>
            </el-table>

            <div class="pagination-wrapper">
                <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="queryParams.size"
                    v-model:current-page="queryParams.current" @current-change="fetchData" />
            </div>
        </el-card>

        <el-dialog :title="dialogType === 'add' ? '新增电影' : '编辑电影'" v-model="dialogVisible" width="650px"
            destroy-on-close class="custom-dialog">
            <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
                <el-form-item label="电影片名" prop="title">
                    <el-input v-model="form.title" placeholder="请输入片名" />
                </el-form-item>
                <el-form-item label="海报上传">
                    <el-upload class="poster-uploader" action="http://localhost:8080/api/admin/sys/movie/upload"
                        :headers="uploadHeaders" :show-file-list="false" :on-success="handleUploadSuccess">
                        <img v-if="form.posterUrl" :src="form.posterUrl" class="uploaded-poster" />
                        <el-icon v-else class="poster-uploader-icon">
                            <Plus />
                        </el-icon>
                    </el-upload>
                    <div style="font-size: 12px; color: #94a3b8; margin-left: 16px; line-height: 1.5;">
                        建议尺寸：300x400<br>仅支持
                        JPG/PNG</div>
                </el-form-item>
                <el-row :gutter="20">
                    <el-col :span="12">
                        <el-form-item label="电影类型" prop="genres">
                            <el-input v-model="form.genres" placeholder="例如: 科幻,动作" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="时长(分)" prop="duration">
                            <el-input-number v-model="form.duration" :min="1" :max="300" style="width: 100%;" />
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-row :gutter="20">
                    <el-col :span="12">
                        <el-form-item label="导演" prop="director">
                            <el-input v-model="form.director" placeholder="请输入导演" />
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="主演">
                            <el-input v-model="form.actors" placeholder="请输入主演" />
                        </el-form-item>
                    </el-col>
                </el-row>
                <el-form-item label="电影简介">
                    <el-input v-model="form.introduction" type="textarea" :rows="4" placeholder="请输入简介" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false" round>取 消</el-button>
                <el-button type="primary" :loading="submitLoading" @click="handleSubmit" round>确 定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Plus, StarFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

// 后台请求必须带 Admin Token
const uploadHeaders = { Authorization: localStorage.getItem('adminToken') }

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const queryParams = reactive({ current: 1, size: 10, title: '' })

// 弹窗表单相关
const dialogVisible = ref(false)
const dialogType = ref('add') // 'add' or 'edit'
const submitLoading = ref(false)
const formRef = ref(null)
const form = reactive({
    id: null, title: '', posterUrl: '', genres: '', director: '', actors: '', duration: 120, introduction: ''
})

const rules = {
    title: [{ required: true, message: '片名不能为空', trigger: 'blur' }],
    genres: [{ required: true, message: '类型不能为空', trigger: 'blur' }],
    director: [{ required: true, message: '导演不能为空', trigger: 'blur' }],
    duration: [{ required: true, message: '时长不能为空', trigger: 'blur' }]
}

// 1. 获取表格数据
const fetchData = async () => {
    loading.value = true
    try {
        const res = await request.get(`/api/admin/sys/movie/page?current=${queryParams.current}&size=${queryParams.size}&title=${queryParams.title}`)
        tableData.value = res.records || []
        total.value = res.total || 0
    } catch (e) { } finally { loading.value = false }
}

// 2. 弹窗操作
const openAddDialog = () => {
    dialogType.value = 'add'
    resetForm()
    dialogVisible.value = true
}

const openEditDialog = (row) => {
    dialogType.value = 'edit'
    Object.assign(form, row)
    dialogVisible.value = true
}

const resetForm = () => {
    Object.keys(form).forEach(key => form[key] = key === 'duration' ? 120 : (key === 'id' ? null : ''))
    if (formRef.value) formRef.value.clearValidate()
}

// 3. 上传海报成功回调
const handleUploadSuccess = (res) => {
    if (res.code === 200) {
        form.posterUrl = res.data
        ElMessage.success('海报上传成功')
    } else {
        ElMessage.error(res.msg || '上传失败')
    }
}

// 4. 提交表单 (新增或修改)
const handleSubmit = () => {
    formRef.value?.validate(async (valid) => {
        if (!valid) return
        submitLoading.value = true
        try {
            if (dialogType.value === 'add') {
                await request.post('/api/admin/sys/movie/add', form)
                ElMessage.success('新增电影成功')
            } else {
                await request.put('/api/admin/sys/movie/update', form)
                ElMessage.success('修改电影成功')
            }
            dialogVisible.value = false
            fetchData()
        } catch (e) { } finally { submitLoading.value = false }
    })
}

// 5. 删除电影
const handleDelete = async (id) => {
    try {
        await request.delete(`/api/admin/sys/movie/delete/${id}`)
        ElMessage.success('删除成功')
        fetchData()
    } catch (e) { }
}

onMounted(() => fetchData())
</script>

<style scoped lang="scss">
.admin-movies {
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    h2 {
        margin: 0;
        color: #0f172a;
        font-size: 24px;
    }

    .actions {
        display: flex;
        gap: 16px;
    }
}

.table-card {
    border-radius: 16px;
    border: 1px solid #e2e8f0;
}

.table-poster {
    width: 54px;
    height: 76px;
    border-radius: 6px;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    transition: transform 0.2s;

    &:hover {
        transform: scale(1.1);
        z-index: 10;
    }
}

.pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    padding-top: 24px;
}

/* 海报上传组件样式重写 */
.poster-uploader {
    border: 1px dashed #cbd5e1;
    border-radius: 8px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    transition: border-color 0.2s;
    display: inline-block;

    &:hover {
        border-color: #6366f1;
    }

    .uploaded-poster {
        width: 100px;
        height: 140px;
        display: block;
        object-fit: cover;
    }

    .poster-uploader-icon {
        font-size: 28px;
        color: #8c939d;
        width: 100px;
        height: 140px;
        line-height: 140px;
        text-align: center;
    }
}

:deep(.el-form-item__label) {
    font-weight: 600;
    color: #475569;
}
</style>