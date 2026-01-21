import { useUserStore } from "../store/useUser";

/**
 * 检查是否有权限
 * @param value 权限字符或字符数组
 * @returns boolean
 */
export function hasPermission(value: string | string[]): boolean {
    if (!value) return true;

    const userStore = useUserStore();
    const perms = userStore.permissions;

    if (!perms || perms.length === 0) return false;

    // 超级管理员权限（通常 *:* 或 *:*:* 表示）
    if (perms.includes("*:*:*")) return true;

    if (Array.isArray(value)) {
        return value.some((p) => perms.includes(p));
    } else {
        return perms.includes(value);
    }
}
