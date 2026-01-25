import request from "../../utils/request";

// ============== 文章分类 API ==============

export function listCmsCategory(params?: any) {
    return request.get("/admin/cms/category/list", { params });
}

export function addCmsCategory(data: any) {
    return request.post("/admin/cms/category", data);
}

export function updateCmsCategory(data: any) {
    return request.put("/admin/cms/category", data);
}

export function deleteCmsCategory(id: number) {
    return request.delete(`/admin/cms/category/${id}`);
}

// ============== 门户菜单 API ==============

export function listPortalMenu(params?: any) {
    return request.get("/admin/portal/menu/list", { params });
}

export function addPortalMenu(data: any) {
    return request.post("/admin/portal/menu", data);
}

export function updatePortalMenu(data: any) {
    return request.put("/admin/portal/menu", data);
}

export function deletePortalMenu(id: number) {
    return request.delete(`/admin/portal/menu/${id}`);
}
