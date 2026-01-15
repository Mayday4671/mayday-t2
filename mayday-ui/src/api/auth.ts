import request from "../utils/request";

// 登录响应类型
export interface LoginResult {
  needSelectDept: boolean;
  tempToken?: string;
  token?: string;
  userId?: number;
  username?: string;
  avatar?: string;
  currentDeptId?: number;
  permissions?: string[];
  deptList?: DeptOption[];
}

export interface DeptOption {
  deptId: number;
  deptName: string;
  isDefault: boolean;
}

// 登录
export function login(
  username: string,
  password: string,
): Promise<LoginResult> {
  return request.post("/login", { username, password });
}

// 选择部门 (多部门用户)
export function selectDept(
  tempToken: string,
  deptId: number,
): Promise<LoginResult> {
  return request.post("/selectDept", { tempToken, deptId });
}

// 切换部门
export function switchDept(deptId: number): Promise<LoginResult> {
  return request.post("/switchDept", { deptId });
}

// 获取当前用户信息
export function getInfo(): Promise<LoginResult> {
  return request.get("/getInfo");
}

// 登出
export function logout(): Promise<void> {
  return request.post("/logout");
}
