import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * axios 统一封装
 * 约定后端统一返回格式 CommonResult：{ code, message, data }
 * code === 200 表示成功；分页数据 data 为 CommonPage：{ total, pageNum, pageSize, list }
 */
const service = axios.create({
  baseURL: '/api', // 走 vite 代理，转发到后端
  timeout: 10000
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // TODO: 如后端有登录鉴权，可在这里附加 token
    // const token = localStorage.getItem('token')
    // if (token) config.headers.Authorization = token
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端返回 CommonResult，code 非 200 视为业务失败
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    // 直接返回 data 部分，业务代码无需再解包
    return res.data
  },
  (error) => {
    const msg = error.response?.data?.message || error.message || '网络异常'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default service
