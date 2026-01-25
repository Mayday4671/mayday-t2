import request from "../../utils/request";

// Public Portal Article APIs
export const fetchPortalArticleList = (params: {
  current: number; pageSize: number; title: string;
  categoryId?: number;
  sortType?: string;
}) => {
  return request.get<any>('/portal/article/list', { params });
};

export const fetchPortalArticleDetail = (id: number) => {
  return request.get<any>(`/portal/article/${id}`);
};
