import request from "../../utils/request";

/**
 * 分页查询 AI 调用日志
 */
export function listAiCallLog(params: any) {
    return request.get("/ai/log/list", { params });
}

/**
 * 获取 AI 调用日志详情
 */
export function getAiCallLog(id: number) {
    return request.get(`/ai/log/${id}`);
}
