import request from "../utils/request";

/**
 * 获取个人信息
 */
export const getProfile = () => {
    return request.get("/profile");
};

/**
 * 更新个人信息
 * @param data 包含 nickname, email, phone
 */
export const updateProfile = (data: any) => {
    return request.put("/profile", data);
};

/**
 * 更新头像
 * @param avatar 头像URL
 */
export const updateAvatar = (avatar: string) => {
    return request.put("/profile/avatar", { avatar });
};

/**
 * 修改密码
 * @param data 包含 oldPassword, newPassword
 */
/**
 * 修改密码
 * @param data 包含 oldPassword, newPassword
 */
export const updatePassword = (data: any) => {
    return request.put("/profile/password", data);
};

/**
 * 上传头像文件
 * @param file 文件对象
 */
export const uploadAvatarFile = (file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return request.post("/profile/avatar/upload", formData, {
        headers: {
            "Content-Type": "multipart/form-data",
        },
    });
};

/**
 * 保存网络图片头像
 * @param avatarUrl 图片URL
 */
export const saveAvatarByUrl = (avatarUrl: string) => {
    return request.post("/profile/avatar/url", { avatar: avatarUrl });
};
