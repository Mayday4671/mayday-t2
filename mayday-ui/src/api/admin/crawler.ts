import request from "../../utils/request";

// ========== 爬虫任务管理 ==========
export function fetchGetTaskList(params: any) {
  return request.post("/crawlerTask/list", params);
}

// 新增任务
export function fetchAddTask(data: any) {
  return request.post("/crawlerTask", data);
}

// 编辑任务
export function fetchEditTask(data: any) {
  return request.put("/crawlerTask", data);
}

// 删除任务
export function fetchDeleteTask(id: number) {
  return request.delete(`/crawlerTask/${id}`);
}

// 启动任务
export function fetchStartTask(id: number) {
  return request.post(`/crawlerTask/${id}/start`);
}

// 暂停任务
export function fetchPauseTask(id: number) {
  return request.post(`/crawlerTask/${id}/pause`);
}

// 恢复任务
export function fetchResumeTask(id: number) {
  return request.post(`/crawlerTask/${id}/resume`);
}

// 停止任务
export function fetchStopTask(id: number) {
  return request.post(`/crawlerTask/${id}/stop`);
}

// 重跑任务
export function fetchRerunTask(id: number) {
  return request.post(`/crawlerTask/${id}/rerun`);
}

// ========== 文章管理 ==========
export function fetchGetArticleList(params: any) {
  return request.post("/crawlerArticle/list", params);
}

// 文章详情
export function fetchGetArticleDetail(id: number) {
  return request.get(`/crawlerArticle/${id}`);
}

// 删除文章
export function fetchDeleteArticle(id: number) {
  return request.delete(`/crawlerArticle/${id}`);
}

// ========== 图片管理 ==========
export function fetchGetImageList(params: any) {
  return request.post("/crawlerImage/list", params);
}

// 图片按文章聚合（封面模式）
export function fetchGetImageArticleCoverList(params: any) {
  return request.post("/crawlerImage/articleCover/list", params);
}

// 查询文章下所有图片（轮播）
export function fetchGetImagesByArticle(articleId: number) {
  return request.get(`/crawlerImage/byArticle/${articleId}`);
}

// 按文章删除图片（批量删除某篇文章下的所有图片）
export function fetchDeleteImagesByArticle(articleId: number | string) {
  return request.delete(`/crawlerImage/byArticle/${articleId}`);
}

// 查询文章下图片分页（用于图片分页展示）
export function fetchGetImagesByArticlePage(articleId: number, params: any) {
  return request.post(`/crawlerImage/byArticle/${articleId}/page`, params);
}

// 图片详情
export function fetchGetImageDetail(id: number) {
  return request.get(`/crawlerImage/${id}`);
}

// 删除图片
export function fetchDeleteImage(id: number) {
  return request.delete(`/crawlerImage/${id}`);
}

// ========== 爬虫日志管理 ==========
export function fetchGetLogList(params: any) {
  return request.post("/crawlerLog/list", params);
}

// 删除日志
export function fetchDeleteLog(id: number) {
  return request.delete(`/crawlerLog/${id}`);
}

// ========== 全局代理配置 ==========
export function fetchGetProxyList(params: any) {
  return request.post("/crawlerProxy/list", params);
}

// 新增代理
export function fetchAddProxy(data: any) {
  return request.post("/crawlerProxy", data);
}

// 编辑代理
export function fetchEditProxy(data: any) {
  return request.put("/crawlerProxy", data);
}

// 删除代理
export function fetchDeleteProxy(id: number) {
  return request.delete(`/crawlerProxy/${id}`);
}
