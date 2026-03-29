<template>
    <div class="admin-halls">
        <div class="page-header">
            <div class="title-info">
                <h2>影厅管理</h2>
                <p class="subtitle">配置影院物理空间与座位布局</p>
            </div>
            <el-button type="primary" @click="openAddDialog">
                <el-icon>
                    <Plus />
                </el-icon> 新增影厅
            </el-button>
        </div>

        <div class="halls-grid" v-loading="loading">
            <el-card v-for="hall in tableData" :key="hall.id" class="hall-card" shadow="hover">
                <div class="hall-info">
                    <div class="hall-icon"><el-icon>
                            <OfficeBuilding />
                        </el-icon></div>
                    <div class="meta">
                        <h3 class="name">{{ hall.name }}</h3>
                        <p class="capacity">总容量：{{ hall.rowCount * hall.colCount }} 个座位</p>
                    </div>
                </div>
                <div class="layout-details">
                    <el-tag size="small" effect="plain" round>行数：{{ hall.rowCount }}</el-tag>
                    <el-tag size="small" effect="plain" round>列数：{{ hall.colCount }}</el-tag>
                </div>
                <div class="actions">
                    <el-button size="small" link type="primary" @click="openEditDialog(hall)">编辑布局</el-button>
                    <el-popconfirm title="删除影厅将影响关联排片，确定吗？" @confirm="handleDelete(hall.id)">
                        <template #reference>
                            <el-button size="small" link type="danger">删除</el-button>
                        </template>
                    </el-popconfirm>
                </div>
            </el-card>
        </div>

        <el-dialog :title="dialogType === 'add' ? '新增影厅' : '编辑影厅布局'" v-model="dialogVisible" width="700px"
            destroy-on-close class="custom-dialog">
            <div class="dialog-layout">
                <el-form :model="form" :rules="rules" ref="formRef" label-position="top" class="hall-form">
                    <el-form-item label="影厅名称" prop="name">
                        <el-input v-model="form.name" placeholder="例如：1号IMAX巨幕厅" />
                    </el-form-item>
                    <el-row :gutter="20">
                        <el-col :span="12">
                            <el-form-item label="行数 (Rows)" prop="rowCount">
                                <el-input-number v-model="form.rowCount" :min="1" :max="20" style="width: 100%" />
                            </el-form-item>
                        </el-col>
                        <el-col :span="12">
                            <el-form-item label="列数 (Cols)" prop="colCount">
                                <el-input-number v-model="form.colCount" :min="1" :max="25" style="width: 100%" />
                            </el-form-item>
                        </el-col>
                    </el-row>
                    <div class="form-tips">调整行列数将即时更新右侧预览</div>
                </el-form>

                <div class="preview-section">
                    <div class="preview-header">座位图预览</div>
                    <div class="preview-container">
                        <div class="screen">SCREEN</div>
                        <div class="seat-matrix" :style="{ gridTemplateColumns: `repeat(${form.colCount}, 1fr)` }">
                            <div v-for="n in (form.rowCount * form.colCount)" :key="n" class="mini-seat"></div>
                        </div>
                    </div>
                </div>
            </div>
            <template #footer>
                <el-button @click="dialogVisible = false" round>取 消</el-button>
                <el-button type="primary" :loading="submitLoading" @click="handleSubmit" round>确 定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus, OfficeBuilding } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const dialogType = ref('add')
const submitLoading = ref(false)
const formRef = ref(null)

const form = reactive({ id: null, name: '', rowCount: 8, colCount: 10 })
const rules = { name: [{ required: true, message: '请输入影厅名称', trigger: 'blur' }] }

const fetchData = async () => {
    loading.value = true
    try {
        const res = await request.get('/api/admin/sys/hall/list')
        tableData.value = res || []
    } catch (e) { } finally { loading.value = false }
}

const openAddDialog = () => {
    dialogType.value = 'add'
    Object.assign(form, { id: null, name: '', rowCount: 8, colCount: 10 })
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
            if (dialogType.value === 'add') {
                await request.post('/api/admin/sys/hall/add', form)
                ElMessage.success('影厅添加成功')
            } else {
                await request.put('/api/admin/sys/hall/update', form)
                ElMessage.success('影厅布局更新成功')
            }
            dialogVisible.value = false
            fetchData()
        } catch (e) { } finally { submitLoading.value = false }
    })
}

const handleDelete = async (id) => {
    try {
        await request.delete(`/api/admin/sys/hall/delete/${id}`)
        ElMessage.success('删除成功')
        fetchData()
    } catch (e) { }
}

onMounted(() => fetchData())
</script>

<style scoped lang="scss">
.admin-halls {
    display: flex;
    flex-direction: column;
    gap: 32px;
}

.page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .subtitle {
        margin-top: 8px;
        color: #64748b;
        font-size: 14px;
    }
}

.halls-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 24px;
}

.hall-card {
    .hall-info {
        display: flex;
        align-items: center;
        gap: 16px;
        margin-bottom: 20px;

        .hall-icon {
            width: 48px;
            height: 48px;
            border-radius: 12px;
            background: var(--color-primary-light);
            color: var(--color-primary);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 24px;
        }

        .name {
            margin: 0;
            font-size: 18px;
            color: #0f172a;
        }

        .capacity {
            margin: 4px 0 0 0;
            font-size: 12px;
            color: #94a3b8;
        }
    }

    .layout-details {
        display: flex;
        gap: 8px;
        margin-bottom: 20px;
    }

    .actions {
        display: flex;
        justify-content: flex-end;
        gap: 12px;
        padding-top: 12px;
        border-top: 1px solid #f1f5f9;
    }
}

.dialog-layout {
    display: flex;
    gap: 40px;

    .hall-form {
        flex: 1;
    }

    .preview-section {
        width: 320px;
        background: #f8fafc;
        border-radius: 16px;
        padding: 20px;
        display: flex;
        flex-direction: column;
        gap: 16px;

        .preview-header {
            font-weight: 600;
            color: #64748b;
            font-size: 13px;
            text-align: center;
        }

        .preview-container {
            flex: 1;
            border: 1px solid #e2e8f0;
            border-radius: 8px;
            background: #fff;
            padding: 16px;

            .screen {
                height: 6px;
                background: #cbd5e1;
                border-radius: 4px;
                text-align: center;
                line-height: 0;
                font-size: 10px;
                color: #94a3b8;
                margin-bottom: 24px;
                font-weight: bold;
            }

            .seat-matrix {
                display: grid;
                gap: 4px;

                .mini-seat {
                    height: 10px;
                    background: #e2e8f0;
                    border-radius: 2px;
                }
            }
        }
    }
}

.form-tips {
    font-size: 12px;
    color: #94a3b8;
    margin-top: 8px;
}
</style>