<template>
  <div class="login-container">
    <a-card class="login-card" :bordered="false">
      <template #title>
        <div class="login-title">
          <h2>🔐 MayDay Auth</h2>
          <p>权限管理系统登录</p>
        </div>
      </template>

      <!-- 登录表单 -->
      <a-form :model="loginForm" @finish="handleLogin" layout="vertical">
        <a-form-item
          label="用户名"
          name="username"
          :rules="[{ required: true, message: '请输入用户名' }]"
        >
          <a-input
            v-model:value="loginForm.username"
            placeholder="admin / zhangsan / lisi"
            size="large"
          />
        </a-form-item>
        <a-form-item
          label="密码"
          name="password"
          :rules="[{ required: true, message: '请输入密码' }]"
        >
          <a-input-password
            v-model:value="loginForm.password"
            placeholder="123456"
            size="large"
          />
        </a-form-item>
        <a-form-item>
          <a-button
            type="primary"
            html-type="submit"
            :loading="loading"
            block
            size="large"
          >
            登录
          </a-button>
        </a-form-item>
      </a-form>

      <!-- 错误信息 -->
      <a-alert
        v-if="errorMsg"
        :message="errorMsg"
        type="error"
        show-icon
        style="margin-top: 16px"
        closable
      />
    </a-card>

    <!-- 测试账号提示 -->
    <div class="test-accounts">
      <h4>测试账号</h4>
      <p>admin / 123456 (管理员 - 看全部数据)</p>
      <p>zhangsan / 123456 (多部门用户 - 可切换部门)</p>
      <p>lisi / 123456 (普通员工 - 仅看自己)</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from "vue";
import { useRouter } from "vue-router";
import { login } from "../api/auth";

const router = useRouter();
const loading = ref(false);
const errorMsg = ref("");

const loginForm = reactive({
  username: "",
  password: "",
});

// 登录 - 现在直接使用默认部门登录，不再需要选择部门
const handleLogin = async () => {
  loading.value = true;
  errorMsg.value = "";
  try {
    const res = await login(loginForm.username, loginForm.password);
    // 登录成功，保存 token 和部门列表
    localStorage.setItem("token", res.token!);
    // 保存部门列表供首页切换使用
    if (res.deptList && res.deptList.length > 0) {
      localStorage.setItem("deptList", JSON.stringify(res.deptList));
    } else {
      localStorage.removeItem("deptList");
    }
    router.push("/home");
  } catch (e: any) {
    errorMsg.value = e.message || "登录失败";
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}
.login-card {
  width: 100%;
  max-width: 400px;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}
.login-title {
  text-align: center;
}
.login-title h2 {
  margin: 0;
  color: #1890ff;
}
.login-title p {
  margin: 8px 0 0;
  color: #888;
}
.test-accounts {
  margin-top: 24px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: white;
  text-align: center;
}
.test-accounts h4 {
  margin: 0 0 8px;
}
.test-accounts p {
  margin: 4px 0;
  font-size: 13px;
}
</style>
