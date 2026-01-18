<template>
  <div class="frontend-layout">
    <header class="frontend-header" v-if="route.path !== '/' && route.name !== 'ArticleDetail'">
      <div class="logo">MayDay AI</div>
      <nav>
        <router-link to="/">Home</router-link>
        <router-link to="/admin">Login / Admin</router-link>
      </nav>
    </header>
    <main>
      <router-view />
    </main>

  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router';
const route = useRoute();
</script>

<style scoped>
.frontend-layout {
  min-height: 100vh;
  background-color: #000; /* Dark theme base */
  color: #fff;
  font-family: 'Inter', sans-serif;
}

.frontend-header {
  position: fixed;
  top: 0;
  width: 100%;
  padding: 20px 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  z-index: 100;
  background: rgba(0, 0, 0, 0.5); /* Transparent header */
  backdrop-filter: blur(10px);
}

.logo {
  font-size: 24px;
  font-weight: bold;
  background: linear-gradient(45deg, #00c6fb, #005bea);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

nav a {
  color: #ccc;
  margin-left: 20px;
  text-decoration: none;
  font-size: 16px;
  transition: color 0.3s;
}

nav a:hover {
  color: #fff;
}


</style>
