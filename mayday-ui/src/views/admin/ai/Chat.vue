<template>
  <div class="chat-container">
    <a-card title="AI 智能助手" :bordered="false" class="chat-card">
      <div class="chat-window">
        <div class="messages" ref="messagesRef">
          <div v-for="(msg, index) in messages" :key="index" :class="['message', msg.role]">
            <div class="avatar">
              <a-avatar v-if="msg.role === 'ai'" style="background-color: #1677ff">AI</a-avatar>
              <a-avatar v-else style="background-color: #f56a00">U</a-avatar>
            </div>
            <div class="content">
              <div v-if="msg.role === 'ai'" class="bubble markdown-body" v-html="renderMarkdown(msg.content)"></div>
              <div v-else class="bubble">{{ msg.content }}</div>
            </div>
          </div>
          <!-- 流式生成中的指示器 -->
          <div v-if="streaming" class="message ai">
            <div class="avatar">
              <a-avatar style="background-color: #1677ff">AI</a-avatar>
            </div>
            <div class="content">
              <div class="bubble markdown-body" v-html="renderMarkdown(streamingContent || '▌')"></div>
            </div>
          </div>
        </div>
        <div class="input-area">
          <a-input-search
            v-model:value="inputValue"
            :placeholder="streaming ? '正在生成中...' : '请输入您的问题...'"
            :enter-button="streaming ? '停止' : '发送'"
            size="large"
            :loading="loading && !streaming"
            :disabled="loading && !streaming"
            @search="handleSendOrStop"
          />
        </div>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue';
import { message } from 'ant-design-vue';
import { streamChat } from '@/api/ai/stream';
import { marked } from 'marked';
import DOMPurify from 'dompurify';
import hljs from 'highlight.js';
import 'highlight.js/styles/atom-one-dark.css';

interface Message {
  role: 'user' | 'ai';
  content: string;
}

// 配置 marked 使用 highlight.js
marked.use({
  renderer: {
    // @ts-ignore
    code(code: string, language: string) {
      const validLanguage = hljs.getLanguage(language || '') ? language : 'plaintext';
      // @ts-ignore
      const highlighted = hljs.highlight(code, { language: validLanguage }).value;
      return `<pre><code class="hljs language-${validLanguage}">${highlighted}</code></pre>`;
    }
  }
});

const inputValue = ref('');
const loading = ref(false);
const streaming = ref(false);
const streamingContent = ref('');
const messagesRef = ref<HTMLElement | null>(null);
const messages = ref<Message[]>([
  { role: 'ai', content: '您好，我是您的 AI 智能助手，有什么可以帮您？' }
]);

// 用于中断流式请求
let abortFn: (() => void) | null = null;

const renderMarkdown = (content: string) => {
  return DOMPurify.sanitize(marked.parse(content) as string);
};

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight;
    }
  });
};

// 发送或停止
const handleSendOrStop = async () => {
  if (streaming.value) {
    // 停止生成
    abortFn?.();
    streaming.value = false;
    // 将当前流式内容保存为消息
    if (streamingContent.value) {
      messages.value.push({ role: 'ai', content: streamingContent.value });
      streamingContent.value = '';
    }
    return;
  }

  if (!inputValue.value.trim()) return;

  const query = inputValue.value;
  messages.value.push({ role: 'user', content: query });
  inputValue.value = '';
  loading.value = true;
  streaming.value = true;
  streamingContent.value = '';
  scrollToBottom();

  try {
    const { promise, abort } = streamChat(
      query,
      'chat',
      (token) => {
        // 每个 token 追加到临时内容
        streamingContent.value += token;
        scrollToBottom();
      },
      (error) => {
        message.error(`AI 响应错误: ${error}`);
      }
    );

    abortFn = abort;
    await promise;

    // 完成后将内容添加到消息列表
    if (streamingContent.value) {
      messages.value.push({ role: 'ai', content: streamingContent.value });
    } else {
      messages.value.push({ role: 'ai', content: '抱歉，没有收到 AI 的回复。' });
    }
  } catch (e: any) {
    if (streamingContent.value) {
      // 即使出错也保留已生成的内容
      messages.value.push({ role: 'ai', content: streamingContent.value + '\n\n[生成中断]' });
    } else {
      messages.value.push({ role: 'ai', content: '抱歉，服务暂时不可用，请稍后再试。' });
    }
  } finally {
    loading.value = false;
    streaming.value = false;
    streamingContent.value = '';
    abortFn = null;
    scrollToBottom();
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
    max-width: 80%;
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

/* Markdown Styles */
.markdown-body {
  font-size: 14px;
  line-height: 1.6;
}
:deep(.markdown-body h1), 
:deep(.markdown-body h2), 
:deep(.markdown-body h3) {
  margin-top: 10px;
  margin-bottom: 10px;
  font-weight: 600;
}
:deep(.markdown-body p) {
  margin-bottom: 10px;
}
:deep(.markdown-body code) {
  background-color: rgba(175, 184, 193, 0.2);
  padding: 0.2em 0.4em;
  border-radius: 6px;
  font-family: monospace;
}
:deep(.markdown-body pre) {
  background-color: #282c34;
  padding: 16px;
  border-radius: 6px;
  overflow: auto;
  margin-bottom: 16px;
}
:deep(.markdown-body pre code) {
  background-color: transparent;
  padding: 0;
  color: #abb2bf;
}
:deep(.markdown-body ul), :deep(.markdown-body ol) {
    padding-left: 20px;
    margin-bottom: 10px;
}
</style>

