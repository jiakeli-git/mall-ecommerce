<template>
  <div class="product-page">
    <!-- ==================== 多条件搜索区 ==================== -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="商品名称">
          <el-input
            v-model="query.keyword"
            placeholder="按商品名称模糊查询"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="货号">
          <el-input
            v-model="query.productSn"
            placeholder="货号（精确匹配）"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="上架状态">
          <el-select v-model="query.publishStatus" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="i in PUBLISH_STATUS_OPTIONS" :key="i.value" :label="i.label" :value="i.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="query.verifyStatus" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="i in VERIFY_STATUS_OPTIONS" :key="i.value" :label="i.label" :value="i.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          <el-button type="success" :icon="Plus" @click="handleCreate">新增商品</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ==================== 商品列表 ==================== -->
    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column label="主图" width="76" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.pic"
              :src="row.pic"
              :preview-src-list="[row.pic]"
              preview-teleported
              fit="cover"
              class="thumb"
            />
            <span v-else class="muted">无图</span>
          </template>
        </el-table-column>

        <el-table-column prop="id" label="ID" width="70" align="center" />

        <el-table-column label="货号" width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.productSn || '—' }}</template>
        </el-table-column>

        <el-table-column label="商品名称" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="name-cell">
              <span class="name">{{ row.name }}</span>
              <span v-if="row.subTitle" class="sub-title">{{ row.subTitle }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="价格" width="140" align="right">
          <template #default="{ row }">
            <div class="price">{{ formatMoney(row.price) }}</div>
            <div v-if="row.originalPrice" class="origin-price">原价 {{ formatMoney(row.originalPrice) }}</div>
          </template>
        </el-table-column>

        <el-table-column label="库存" width="96" align="center">
          <template #default="{ row }">
            <el-tag :type="stockTagType(row.stock)" effect="plain">{{ row.stock ?? 0 }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="销量" width="80" align="center">
          <template #default="{ row }">{{ row.sale ?? 0 }}</template>
        </el-table-column>

        <el-table-column label="上架状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="publishStatusTagType(row.publishStatus)">
              {{ publishStatusLabel(row.publishStatus) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="审核状态" width="110" align="center">
          <template #default="{ row }">
            <el-tooltip
              v-if="row.verifyStatus === 2 && row.rejectReason"
              :content="`驳回原因：${row.rejectReason}`"
              placement="top"
            >
              <el-tag :type="verifyStatusTagType(row.verifyStatus)">{{ verifyStatusLabel(row.verifyStatus) }}</el-tag>
            </el-tooltip>
            <el-tag v-else :type="verifyStatusTagType(row.verifyStatus)">
              {{ verifyStatusLabel(row.verifyStatus) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="170" align="center">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="170" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" link :icon="View" @click="handleDetail(row)">详情</el-button>
            <el-button size="small" type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>

            <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, row)">
              <el-button size="small" type="info" link class="more-btn">
                更多<el-icon><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <!-- 上架：后端要求 审核通过 + 当前下架 + 未删除；不可用时悬浮显示原因 -->
                  <el-dropdown-item
                    command="publish"
                    :disabled="!canPublish(row).ok"
                    :icon="Top"
                    :title="canPublish(row).reason"
                  >
                    上架
                  </el-dropdown-item>
                  <!-- 下架：后端要求 当前为上架 -->
                  <el-dropdown-item
                    command="unpublish"
                    :disabled="!canUnpublish(row).ok"
                    :icon="Bottom"
                    :title="canUnpublish(row).reason"
                  >
                    下架
                  </el-dropdown-item>
                  <el-dropdown-item command="verifyPass" :disabled="row.verifyStatus !== 0" :icon="CircleCheck" divided>
                    审核通过
                  </el-dropdown-item>
                  <el-dropdown-item command="verifyReject" :disabled="row.verifyStatus !== 0" :icon="CircleClose">
                    审核驳回
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" divided :icon="Delete">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <!-- ==================== 分页 ==================== -->
      <el-pagination
        class="pagination"
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :page-sizes="[5, 10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="fetchList"
      />
    </el-card>

    <!-- ==================== 新增 / 编辑弹窗 ==================== -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? `编辑商品（ID: ${form.id}）` : '新增商品'"
      width="760px"
      top="6vh"
      destroy-on-close
    >
      <el-alert
        v-if="!isEdit"
        type="info"
        :closable="false"
        show-icon
        title="新增后商品默认为「下架 / 待审核」状态，需先审核通过才能上架，货号由后端自动生成。"
        class="form-tip"
      />
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="商品名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入商品名称" maxlength="120" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="副标题">
              <el-input v-model="form.subTitle" placeholder="卖点 / 副标题" maxlength="160" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="销售价" prop="price">
              <el-input-number
                v-model="form.price"
                :min="0"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="原价">
              <el-input-number
                v-model="form.originalPrice"
                :min="0"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="库存" prop="stock">
              <el-input-number
                v-model="form.stock"
                :min="0"
                :precision="0"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="分类ID">
              <el-input-number
                v-model="form.categoryId"
                :min="0"
                :precision="0"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="品牌ID">
              <el-input-number
                v-model="form.brandId"
                :min="0"
                :precision="0"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="计量单位">
              <el-input v-model="form.unit" placeholder="件 / 个 / 台" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="销量">
              <el-input-number
                v-model="form.sale"
                :min="0"
                :precision="0"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序值">
              <el-input-number
                v-model="form.sort"
                :min="0"
                :precision="0"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="主图URL">
              <el-input v-model="form.pic" placeholder="图片地址" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="轮播图">
          <el-input v-model="form.albumPics" placeholder="多张图片地址，用英文逗号分隔" />
        </el-form-item>

        <el-form-item label="商品简介">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="商品简介描述" />
        </el-form-item>

        <el-form-item label="图文详情">
          <el-input v-model="form.detailHtml" type="textarea" :rows="5" placeholder="支持 HTML 富文本（图片 + 文字）" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存修改' : '确认新增' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ==================== 商品详情抽屉 ==================== -->
    <el-drawer v-model="detailVisible" title="商品详情" size="620px" destroy-on-close>
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="商品ID">{{ detail.id }}</el-descriptions-item>
            <el-descriptions-item label="货号">{{ detail.productSn || '—' }}</el-descriptions-item>
            <el-descriptions-item label="商品名称" :span="2">{{ detail.name }}</el-descriptions-item>
            <el-descriptions-item label="副标题" :span="2">{{ detail.subTitle || '—' }}</el-descriptions-item>
            <el-descriptions-item label="销售价">{{ formatMoney(detail.price) }}</el-descriptions-item>
            <el-descriptions-item label="原价">{{ formatMoney(detail.originalPrice) }}</el-descriptions-item>
            <el-descriptions-item label="库存">
              <el-tag :type="stockTagType(detail.stock)" effect="plain">{{ detail.stock ?? 0 }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="销量">{{ detail.sale ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="分类ID">{{ detail.categoryId ?? '—' }}</el-descriptions-item>
            <el-descriptions-item label="品牌ID">{{ detail.brandId ?? '—' }}</el-descriptions-item>
            <el-descriptions-item label="计量单位">{{ detail.unit || '—' }}</el-descriptions-item>
            <el-descriptions-item label="排序值">{{ detail.sort ?? '—' }}</el-descriptions-item>
            <el-descriptions-item label="上架状态">
              <el-tag :type="publishStatusTagType(detail.publishStatus)">
                {{ publishStatusLabel(detail.publishStatus) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="审核状态">
              <el-tag :type="verifyStatusTagType(detail.verifyStatus)">
                {{ verifyStatusLabel(detail.verifyStatus) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="驳回原因" :span="2">{{ detail.rejectReason || '—' }}</el-descriptions-item>
            <el-descriptions-item label="创建人">{{ detail.createBy || '—' }}</el-descriptions-item>
            <el-descriptions-item label="乐观锁版本">{{ detail.version ?? '—' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ formatDateTime(detail.updateTime) }}</el-descriptions-item>
            <el-descriptions-item label="商品简介" :span="2">{{ detail.description || '—' }}</el-descriptions-item>
          </el-descriptions>

          <div v-if="albumList.length" class="detail-block">
            <div class="block-title">商品图片</div>
            <div class="album">
              <el-image
                v-for="(url, idx) in albumList"
                :key="idx"
                :src="url"
                :preview-src-list="albumList"
                :initial-index="idx"
                preview-teleported
                fit="cover"
                class="album-item"
              />
            </div>
          </div>

          <div v-if="detail.detailHtml" class="detail-block">
            <div class="block-title">图文详情</div>
            <!-- 管理端自己录入的富文本内容 -->
            <div class="rich-text" v-html="detail.detailHtml"></div>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowDown,
  Bottom,
  CircleCheck,
  CircleClose,
  Delete,
  Edit,
  Plus,
  Refresh,
  Search,
  Top,
  View
} from '@element-plus/icons-vue'
import {
  createProduct,
  deleteProduct,
  getProductDetail,
  getProductList,
  publishProduct,
  unpublishProduct,
  updateProduct,
  verifyPassProduct,
  verifyRejectProduct
} from '@/api/product'
import {
  PUBLISH_STATUS_OPTIONS,
  VERIFY_STATUS_OPTIONS,
  formatDateTime,
  formatMoney,
  normalizePageResult,
  publishStatusLabel,
  publishStatusTagType,
  verifyStatusLabel,
  verifyStatusTagType
} from '@/utils/constants'

/* ==================== 列表 & 查询 ==================== */
const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({
  keyword: '',
  productSn: '',
  publishStatus: undefined,
  verifyStatus: undefined,
  pageNum: 1,
  pageSize: 10
})

/** 组装查询参数：保留 0，剔除空字符串 / undefined */
function buildParams() {
  const params = { pageNum: query.pageNum, pageSize: query.pageSize }
  Object.entries(query).forEach(([key, value]) => {
    if (key === 'pageNum' || key === 'pageSize') return
    if (value === '' || value === null || value === undefined) return
    params[key] = value
  })
  return params
}

async function fetchList() {
  loading.value = true
  try {
    const data = await getProductList(buildParams())
    const page = normalizePageResult(data, { pageNum: query.pageNum, pageSize: query.pageSize })
    list.value = page.list
    total.value = page.total
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  fetchList()
}

function handleReset() {
  query.keyword = ''
  query.productSn = ''
  query.publishStatus = undefined
  query.verifyStatus = undefined
  query.pageNum = 1
  query.pageSize = 10
  fetchList()
}

function handleSizeChange() {
  query.pageNum = 1
  fetchList()
}

/* ==================== 操作可用性判断（对齐后端业务校验） ==================== */
function canPublish(row) {
  if (row.deleteStatus === 1) return { ok: false, reason: '商品已删除，不能上架' }
  if (row.verifyStatus !== 1) return { ok: false, reason: '商品尚未审核通过，不能上架' }
  if (row.publishStatus === 1) return { ok: false, reason: '商品已处于上架状态' }
  return { ok: true, reason: '' }
}

function canUnpublish(row) {
  if (row.publishStatus === 0) return { ok: false, reason: '商品已处于下架状态' }
  return { ok: true, reason: '' }
}

function stockTagType(stock) {
  const value = Number(stock ?? 0)
  if (value <= 0) return 'danger'
  if (value <= 10) return 'warning'
  return 'primary'
}

/* ==================== 业务操作 ==================== */
async function handlePublish(row) {
  try {
    await ElMessageBox.confirm(`确定上架商品「${row.name}」吗？`, '上架确认', { type: 'warning' })
  } catch {
    return
  }
  await publishProduct(row.id)
  ElMessage.success('上架成功')
  fetchList()
}

async function handleUnpublish(row) {
  try {
    await ElMessageBox.confirm(`确定下架商品「${row.name}」吗？下架后用户将无法下单。`, '下架确认', { type: 'warning' })
  } catch {
    return
  }
  await unpublishProduct(row.id)
  ElMessage.success('下架成功')
  fetchList()
}

async function handleVerifyPass(row) {
  try {
    await ElMessageBox.confirm(`确定审核通过商品「${row.name}」吗？通过后即可上架。`, '审核确认', { type: 'warning' })
  } catch {
    return
  }
  await verifyPassProduct(row.id)
  ElMessage.success('审核通过')
  fetchList()
}

async function handleVerifyReject(row) {
  let reason = ''
  try {
    const { value } = await ElMessageBox.prompt(
      `请填写商品「${row.name}」的驳回原因（后端必填）`,
      '审核驳回',
      {
        confirmButtonText: '确认驳回',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '例如：主图不清晰、类目选择错误、价格异常',
        inputValidator: (val) => (val && val.trim() ? true : '驳回原因不能为空'),
        type: 'warning'
      }
    )
    reason = value.trim()
  } catch {
    return
  }
  await verifyRejectProduct(row.id, reason)
  ElMessage.success('已驳回')
  fetchList()
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除商品「${row.name}」吗？后端为逻辑删除，删除后列表将不再显示。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确认删除' }
    )
  } catch {
    return
  }
  await deleteProduct(row.id)
  ElMessage.success('删除成功')
  fetchList()
}

/** “更多”下拉统一入口 */
function handleCommand(command, row) {
  if (command === 'publish') return handlePublish(row)
  if (command === 'unpublish') return handleUnpublish(row)
  if (command === 'verifyPass') return handleVerifyPass(row)
  if (command === 'verifyReject') return handleVerifyReject(row)
  if (command === 'delete') return handleDelete(row)
}

/* ==================== 新增 / 编辑 ==================== */
const dialogVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const formRef = ref()

const emptyForm = () => ({
  id: null,
  name: '',
  subTitle: '',
  categoryId: undefined,
  brandId: undefined,
  price: 0,
  originalPrice: undefined,
  stock: 0,
  unit: '',
  pic: '',
  albumPics: '',
  description: '',
  detailHtml: '',
  sale: 0,
  sort: 0
})

const form = reactive(emptyForm())

const formRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入销售价', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }]
}

function handleCreate() {
  isEdit.value = false
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

async function handleEdit(row) {
  isEdit.value = true
  Object.assign(form, emptyForm())
  dialogVisible.value = true
  // 拉取最新详情，避免列表字段不全
  try {
    const detail = await getProductDetail(row.id)
    Object.assign(form, {
      id: detail.id,
      name: detail.name ?? '',
      subTitle: detail.subTitle ?? '',
      categoryId: detail.categoryId ?? undefined,
      brandId: detail.brandId ?? undefined,
      price: detail.price ?? 0,
      originalPrice: detail.originalPrice ?? undefined,
      stock: detail.stock ?? 0,
      unit: detail.unit ?? '',
      pic: detail.pic ?? '',
      albumPics: detail.albumPics ?? '',
      description: detail.description ?? '',
      detailHtml: detail.detailHtml ?? '',
      sale: detail.sale ?? 0,
      sort: detail.sort ?? 0
    })
  } catch {
    // 详情接口异常时退回使用列表行数据
    Object.assign(form, { ...emptyForm(), ...row })
  }
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    const payload = { ...form }
    if (isEdit.value) {
      await updateProduct(payload)
      ElMessage.success('更新成功')
    } else {
      delete payload.id
      await createProduct(payload)
      ElMessage.success('新增成功，商品当前为「待审核 / 下架」状态')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    submitting.value = false
  }
}

/* ==================== 详情抽屉 ==================== */
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

const albumList = computed(() => {
  const raw = detail.value?.albumPics
  if (!raw) return detail.value?.pic ? [detail.value.pic] : []
  return String(raw)
    .split(',')
    .map((i) => i.trim())
    .filter(Boolean)
})

async function handleDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getProductDetail(row.id)
  } finally {
    detailLoading.value = false
  }
}

onMounted(fetchList)
</script>

<style scoped>
.search-card {
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.thumb {
  width: 46px;
  height: 46px;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}

.muted {
  color: #c0c4cc;
  font-size: 12px;
}

.name-cell {
  display: flex;
  flex-direction: column;
}

.name {
  color: #303133;
}

.sub-title {
  color: #909399;
  font-size: 12px;
}

.price {
  color: #f56c6c;
  font-weight: 600;
}

.origin-price {
  color: #c0c4cc;
  font-size: 12px;
  text-decoration: line-through;
}

.more-btn {
  margin-left: 8px;
}

.form-tip {
  margin-bottom: 14px;
}

.detail-body {
  min-height: 200px;
}

.detail-block {
  margin-top: 18px;
}

.block-title {
  font-weight: 600;
  margin-bottom: 10px;
  color: #303133;
}

.album {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.album-item {
  width: 96px;
  height: 96px;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}

.rich-text {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 12px;
  background: #fafafa;
  max-height: 320px;
  overflow: auto;
  line-height: 1.7;
}
</style>