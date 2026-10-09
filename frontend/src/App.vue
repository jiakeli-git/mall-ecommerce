<template>
  <!-- 路由出口，匹配 router 里的页面组件 -->
  <router-view />
</template>

<script setup>
// 这里不需要额外代码，路由在main.js全局注册好了
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
body {
  background-color: #f5f7fa;
}
</style>