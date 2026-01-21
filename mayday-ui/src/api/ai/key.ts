import request from "../../utils/request";

/**
 * 分页查询 AI 密钥
 */
export function listAiKey(params: any) {
    return request.get("/ai/key/list", { params });
}

/**
 * 获取 AI 密钥详情
 */
export function getAiKey(id: number) {
    return request.get(`/ai/key/${id}`);
}

/**
 * 新增 AI 密钥
 */
export function addAiKey(data: any) {
    return request.post("/ai/key", data);
}

/**
 * 修改 AI 密钥
 */
export function updateAiKey(data: any) {
    return request.put("/ai/key", data);
}

/**
 * 删除 AI 密钥
 */
export function deleteAiKey(id: number) {
    return request.delete(`/ai/key/${id}`);
}

/**
 * 禁用 AI 密钥
 */
export function disableAiKey(id: number) {
    return request.post(`/ai/key/disable/${id}`);
}

/**
 * 启用 AI 密钥
 */
export function enableAiKey(id: number) {
    return request.post(`/ai/key/enable/${id}`);
}
