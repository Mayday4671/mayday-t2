import request from "../../utils/request";

/**
 * 分页查询 AI 配置
 */
export function listAiConfig(params: any) {
    return request.get("/ai/config/list", { params });
}

/**
 * 获取 AI 配置详情
 */
export function getAiConfig(id: number) {
    return request.get(`/ai/config/${id}`);
}

/**
 * 新增 AI 配置
 */
export function addAiConfig(data: any) {
    return request.post("/ai/config", data);
}

/**
 * 修改 AI 配置
 */
export function updateAiConfig(data: any) {
    return request.put("/ai/config", data);
}

/**
 * 删除 AI 配置
 */
export function deleteAiConfig(id: number) {
    return request.delete(`/ai/config/${id}`);
}

/**
 * 切换 AI 配置启用状态
 */
export function switchAiConfigStatus(id: number, enabled: number) {
    return request.post("/ai/config/status", { id, enabled });
}
