import request from "../../utils/request";

/**
 * 首页初始化数据聚合接口
 * 返回: { menus: [], categories: [], hotArticles: [] }
 */
export function getPortalHomeInit() {
    return request.get("/portal/home/init");
}
