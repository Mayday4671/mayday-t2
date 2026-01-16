<template>
  <div class="page-container">
    <a-row :gutter="24" type="flex" style="align-items: stretch;">
      <!-- 左侧个人信息卡片 -->
      <a-col :span="8" style="display: flex; flex-direction: column;">
        <a-card hoverable class="user-card" title="个人信息" style="flex: 1;">
          <div class="user-info-header">
            <div class="avatar-container">
              <a-avatar :size="100" :src="userInfo.avatar || defaultAvatar" />
            </div>
            <div class="user-basic-info">
              <h2 class="nickname">{{ userInfo.nickname || userInfo.username }}</h2>
              <p class="username">@{{ userInfo.username }}</p>
            </div>
          </div>
          <a-divider />
          <div class="user-details">

            <div class="detail-item">
              <span class="label"><mail-outlined /> 邮箱</span>
              <span class="value">{{ userInfo.email || '未绑定' }}</span>
            </div>
            <div class="detail-item">
              <span class="label"><phone-outlined /> 手机</span>
              <span class="value">{{ userInfo.phone || '未绑定' }}</span>
            </div>
            <div class="detail-item">
              <span class="label"><calendar-outlined /> 注册时间</span>
              <span class="value">{{ formatDate(userInfo.createTime) }}</span>
            </div>
          </div>
        </a-card>
      </a-col>

      <!-- 右侧设置面板 -->
      <a-col :span="16" style="display: flex; flex-direction: column;">
        <a-card title="个人设置" class="settings-card" style="flex: 1;">
          <a-tabs v-model:activeKey="activeTab">
            
            <!-- 基本信息设置 -->
              <a-tab-pane key="basic" tab="基本资料">
                <div class="account-settings-info-view">
                    <!-- 左侧：表单区域 -->
                    <div class="account-settings-info-left">
                      <a-form :model="formState" layout="vertical" @finish="handleUpdateProfile">
                        <a-form-item label="昵称" name="nickname">
                          <a-input v-model:value="formState.nickname" placeholder="请输入昵称" />
                        </a-form-item>
                        
                        <a-form-item label="手机号" name="phone">
                          <a-input v-model:value="formState.phone" placeholder="请输入手机号" />
                        </a-form-item>

                        <a-form-item label="邮箱" name="email">
                          <a-input v-model:value="formState.email" placeholder="请输入邮箱" />
                        </a-form-item>
                        
                        <a-form-item style="margin-top: 32px;">
                          <a-button type="primary" html-type="submit" :loading="loading">保存修改</a-button>
                        </a-form-item>
                      </a-form>
                    </div>

                    <!-- 右侧：头像区域 -->
                    <div class="account-settings-info-right">
                      <div class="avatar-view">
                         <div class="avatar-title">头像</div>
                         <a-upload
                             name="file"
                             :show-upload-list="false"
                             :before-upload="beforeUpload"
                             :custom-request="handleUploadAvatar"
                             class="avatar-uploader"
                         >
                            <div class="avatar-wrapper">
                                <a-avatar :size="144" :src="userInfo.avatar || defaultAvatar" />
                                <div class="upload-mask">
                                    <cloud-upload-outlined style="font-size: 24px; color: #fff;" />
                                    <div style="font-size: 14px; margin-top: 4px; color: #fff;">更换头像</div>
                                </div>
                            </div>
                         </a-upload>
                         
                         <div style="margin-top: 24px; width: 100%;">
                            <a-input-search
                                v-model:value="avatarUrl"
                                placeholder="粘贴网络图片URL"
                                enter-button="使用"
                                size="default"
                                :loading="downloading"
                                @search="handleSaveAvatarByUrl"
                                style="width: 100%;"
                            />
                         </div>
                      </div>
                    </div>
                </div>
              </a-tab-pane>

            <!-- 安全设置 -->
            <a-tab-pane key="security" tab="安全设置">
              <div style="margin-top: 24px;">
                <a-alert message="为了您的账户安全，建议定期修改密码。" type="info" show-icon style="margin-bottom: 24px" />
                <a-form :model="passwordForm" layout="vertical" @finish="handleUpdatePassword">
                <a-form-item 
                  label="当前密码" 
                  name="oldPassword" 
                  :rules="[{ required: true, message: '请输入当前密码' }]"
                >
                  <a-input-password v-model:value="passwordForm.oldPassword" placeholder="请输入当前密码" />
                </a-form-item>
                
                <a-form-item 
                  label="新密码" 
                  name="newPassword" 
                  :rules="[{ required: true, message: '请输入新密码' }, { min: 6, message: '密码长度不能少于6位' }]"
                >
                  <a-input-password v-model:value="passwordForm.newPassword" placeholder="请输入新密码" />
                </a-form-item>

                <a-form-item 
                  label="确认新密码" 
                  name="confirmPassword"
                  :rules="[{ required: true, message: '请再次输入新密码' }, { validator: validateConfirmPassword }]"
                >
                  <a-input-password v-model:value="passwordForm.confirmPassword" placeholder="请再次输入新密码" />
                </a-form-item>

                <a-form-item>
                  <a-button type="primary" html-type="submit" :loading="loading">修改密码</a-button>
                </a-form-item>
              </a-form>
              </div>
            </a-tab-pane>
          </a-tabs>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue';
import { message } from 'ant-design-vue';
import { MailOutlined, PhoneOutlined, CalendarOutlined, CloudUploadOutlined } from '@ant-design/icons-vue';
import { getProfile, updateProfile, updateAvatar, updatePassword, uploadAvatarFile, saveAvatarByUrl } from '../../../api/admin/profile';
import dayjs from 'dayjs';

const loading = ref(false);
const downloading = ref(false);
const activeTab = ref('basic');
const defaultAvatar = 'https://gw.alipayobjects.com/zos/antfincdn/XAosXuNZyF/BiazfanxmamNRoxxVxka.png';
const avatarUrl = ref('');

const userInfo = ref<any>({});

const formState = reactive({
  nickname: '',
  phone: '',
  email: ''
});

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

// 加载个人信息
const loadProfile = async () => {
  try {
    const data = await getProfile();
    userInfo.value = data as any;
    formState.nickname = userInfo.value.nickname;
    formState.phone = userInfo.value.phone;
    formState.email = userInfo.value.email;
    formState.email = userInfo.value.email;
    
    // 处理头像URL回显，补全域名
    let avatarPath = userInfo.value.avatar || '';
    if (avatarPath && !avatarPath.startsWith('http')) {
        const baseUrl = "http://localhost:9002"; 
        avatarPath = baseUrl + avatarPath;
        userInfo.value.avatar = avatarPath; // 更新显示的头像
    }
    avatarUrl.value = avatarPath;
  } catch (error: any) {
    message.error(error.message || '获取个人信息失败');
  }
};

// 更新基本信息
const handleUpdateProfile = async () => {
  loading.value = true;
  try {
    // 1. 更新基本信息
    await updateProfile(formState);
    
    // 2. 如果输入了头像URL且有变化，尝试更新头像
    if (avatarUrl.value && avatarUrl.value !== userInfo.value.avatar) {
        await updateAvatar(avatarUrl.value);
    }
    
    message.success('个人信息已更新');
    await loadProfile();
  } catch (error: any) {
    message.error(error.message || '更新失败');
  } finally {
    loading.value = false;
  }
};

// 处理网络图片抓取
const handleSaveAvatarByUrl = async () => {
    if (!avatarUrl.value) {
        message.warning('请输入图片URL');
        return;
    }
    
    downloading.value = true;
    try {
        const res = await saveAvatarByUrl(avatarUrl.value);
        // 后端返回相对路径
         const baseUrl = "http://localhost:9002"; 
         const fullUrl = baseUrl + res;
         
         message.success('图片已抓取并设置为头像');
         userInfo.value.avatar = fullUrl;
         await loadProfile();
         
         // 清空输入框
         avatarUrl.value = "";
    } catch (e: any) {
         message.error(e.message || '抓取图片失败，请检查URL是否有效');
    } finally {
        downloading.value = false;
    }
};

// 上传前校验
const beforeUpload = (file: File) => {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png';
  if (!isJpgOrPng) {
    message.error('只支持 JPG/PNG 格式图片!');
  }
  const isLt2M = file.size / 1024 / 1024 < 2;
  if (!isLt2M) {
    message.error('图片大小不能超过 2MB!');
  }
  return isJpgOrPng && isLt2M;
};

// 处理头像上传
const uploading = ref(false);
const handleUploadAvatar = async (options: any) => {
    const { file, onSuccess, onError } = options;
    uploading.value = true;
    try {
        const res = await uploadAvatarFile(file);
        // 上传成功后，后端返回的是相对路径，需要拼接完整的访问URL
        // 这里假设后端返回 /upload/avatar/xxx.jpg
        // 前端需要配置代理或直接访问后端服务
        const baseUrl = "http://localhost:9002"; // 临时硬编码，建议从环境变量获取
        const fullUrl = baseUrl + res; // 假设res直接是URL字符串
        
        message.success('头像上传成功');
        userInfo.value.avatar = fullUrl; // 立即更新预览
        avatarUrl.value = fullUrl; // 填充到URL输入框
        
        // 重新加载用户信息确保同步
        await loadProfile();
        
        onSuccess(res);
    } catch (e: any) {
        message.error(e.message || '上传失败');
        onError(e);
    } finally {
        uploading.value = false;
    }
};



// 验证确认密码
const validateConfirmPassword = async (_rule: any, value: string) => {
  if (value && value !== passwordForm.newPassword) {
    return Promise.reject('两次输入的密码不一致！');
  }
  return Promise.resolve();
};

// 修改密码
const handleUpdatePassword = async () => {
  loading.value = true;
  try {
    await updatePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    });
    message.success('密码已修改，请重新登录');
    // 这里可以添加退出登录的逻辑
    passwordForm.oldPassword = '';
    passwordForm.newPassword = '';
    passwordForm.confirmPassword = '';
  } catch (error: any) {
    message.error(error.message || '密码修改失败');
  } finally {
    loading.value = false;
  }
};

const formatDate = (date: string) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-';
};

onMounted(() => {
  loadProfile();
});
</script>

<style scoped>
.page-container {
  padding: 24px;
}

.user-card {
  text-align: center;
}

.user-info-header {
  padding: 20px 0;
}

.avatar-container {
  margin-bottom: 20px;
  text-align: center;
}

.nickname {
  margin-bottom: 4px;
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.username {
  color: #888;
  font-size: 14px;
}

.user-details {
  text-align: left;
  padding: 0 20px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-item .label {
  color: #666;
}

.detail-item .label .anticon {
  margin-right: 8px;
}

.detail-item .value {
  color: #333;
  font-weight: 500;
}

.settings-card {
  min-height: 400px;
}

.avatar-setting {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 20px;
}

.current-avatar {
    margin-bottom: 30px;
    text-align: center;
}

.avatar-input {
    width: 100%;
    max-width: 500px;
}

.avatar-wrapper {
    position: relative;
    width: 140px;
    height: 140px;
    margin: 0 auto;
    border-radius: 50%;
    overflow: hidden;
    cursor: pointer;
    border: 2px solid transparent;
    transition: all 0.3s;
}

.avatar-wrapper:hover {
    border-color: #1890ff;
}

.upload-mask {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.5);
    color: white;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    opacity: 0;
    transition: opacity 0.3s;
}

.avatar-wrapper:hover .upload-mask,
.upload-mask.is-uploading {
    opacity: 1;
}
.account-settings-info-view {
  display: flex;
  overflow: hidden;
  padding-top: 12px;
}

.account-settings-info-left {
  flex: 1;
  border-right: 1px solid #f0f0f0;
  padding-right: 40px;
}

.account-settings-info-right {
  flex: 1;
  padding-left: 40px;
  display: flex;
  flex-direction: column;
}

.avatar-view {
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 100%; /* 关键修复：占满父容器宽度 */
    max-width: 600px;
    margin: 0 auto;
}

.avatar-title {
    margin-bottom: 20px;
    color: rgba(0, 0, 0, 0.85);
    font-size: 14px;
    font-weight: 500;
}

.avatar-wrapper {
    position: relative;
    width: 144px;
    height: 144px;
    margin: 0 auto 12px;
    border-radius: 50%;
    overflow: hidden;
    cursor: pointer;
    border: 2px solid transparent;
    transition: all 0.3s;
}

.avatar-wrapper:hover {
    border-color: #1890ff;
}

.upload-mask {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    opacity: 0;
    transition: opacity 0.3s;
}

.avatar-wrapper:hover .upload-mask {
    opacity: 1;
}

.avatar-uploader {
    display: block;
    width: auto;
}

/* 响应式调整: 小屏幕下堆叠显示 */
@media screen and (max-width: 768px) {
  .account-settings-info-view {
    flex-direction: column-reverse; /* 头像在上方 */
  }
  .account-settings-info-right {
    padding-left: 0;
    margin-bottom: 24px;
  }
}
</style>
