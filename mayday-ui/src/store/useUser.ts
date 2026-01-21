import { defineStore } from "pinia";
import { ref } from "vue";
import { getInfo, logout, type LoginResult } from "../api/admin/auth";

export const useUserStore = defineStore("user", () => {
    const userInfo = ref<LoginResult | null>(null);
    const permissions = ref<string[]>([]);

    // 获取用户信息
    const getUserInfo = async () => {
        try {
            const res = await getInfo();
            userInfo.value = res;
            permissions.value = res.permissions || [];
            return res;
        } catch (error) {
            throw error;
        }
    };

    // 登出
    const handleLogout = async () => {
        try {
            await logout();
        } catch (error) {
            console.error(error);
        } finally {
            userInfo.value = null;
            permissions.value = [];
            localStorage.removeItem("token");
        }
    };

    return {
        userInfo,
        permissions,
        getUserInfo,
        handleLogout,
    };
});
