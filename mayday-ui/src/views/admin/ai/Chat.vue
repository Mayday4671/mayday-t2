<template>
  <div class="chat-container">
    <a-card title="AI 智能助手" :bordered="false" class="chat-card">
      <div class="chat-window">
        <div class="messages">
          <div v-for="(msg, index) in messages" :key="index" :class="['message', msg.role]">
            <div class="avatar">
              <a-avatar v-if="msg.role === 'ai'" style="background-color: #1677ff">AI</a-avatar>
              <a-avatar v-else style="background-color: #f56a00">U</a-avatar>
            </div>
            <div class="content">
              <div class="bubble">{{ msg.content }}</div>
            </div>
          </div>
        </div>
        <div class="input-area">
          <a-input-search
            v-model:value="inputValue"
            placeholder="请输入您的问题..."
            enter-button="发送"
            size="large"
            :loading="loading"
            @search="handleSend"
          />
        </div>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { message } from 'ant-design-vue';
import request from '../../../utils/request';

interface Message {
  role: 'user' | 'ai';
  content: string;
}

const inputValue = ref('');
const loading = ref(false);
const messages = ref<Message[]>([
  { role: 'ai', content: '您好，我是您的 AI 智能助手，有什么可以帮您？' }
]);

const handleSend = async () => {
    if (!inputValue.value.trim()) return;

    const query = inputValue.value;
    messages.value.push({ role: 'user', content: query });
    inputValue.value = '';
    loading.value = true;

    try {
        // Call backend API
        const res: any = await request.get('/ai/chat', { params: { query } });
        messages.value.push({ role: 'ai', content: res });
    } catch (e) {
        message.error('请求失败');
    } finally {
        loading.value = false;
    }
};
</script>

<style scoped>
.chat-container {
    height: calc(100vh - 230px);
}
.chat-card {
    height: 100%;
    display: flex;
    flex-direction: column;
}
:deep(.ant-card-body) {
    flex: 1;
    overflow: hidden;
    padding: 24px;
    height: 100%;
}
.chat-window {
    height: 100%;
    display: flex;
    flex-direction: column;
}
.messages {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    background: #f0f2f5;
    border-radius: 8px;
    margin-bottom: 20px;
}
.message {
    display: flex;
    margin-bottom: 20px;
}
.message.user {
    flex-direction: row-reverse;
}
.avatar {
    margin: 0 10px;
}
.content {
    max-width: 60%;
}
.bubble {
    padding: 10px 15px;
    border-radius: 8px;
    background: #fff;
    box-shadow: 0 1px 2px rgba(0,0,0,0.1);
    word-break: break-word;
}
.message.user .bubble {
    background: #95de64;
}
</style>
