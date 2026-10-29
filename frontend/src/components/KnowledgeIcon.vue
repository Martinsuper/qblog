<template>
  <svg
    viewBox="0 0 24 24"
    aria-hidden="true"
    v-html="paths[name] || paths.folder"
  />
</template>
<script setup>
defineProps({ name: { type: String, default: 'folder' } })
const paths = {search:'<circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 4 4"/>',layers:'<path d="m12 3 9 5-9 5-9-5 9-5Z"/><path d="m3 12 9 5 9-5M3 16l9 5 9-5"/>',inbox:'<path d="M4 5h16l2 13H2L4 5Z"/><path d="M2 13h6l2 3h4l2-3h6"/>',star:'<path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2-5.6-3-5.6 3 1.1-6.2L3 9.6l6.2-.9L12 3Z"/>',folder:'<path d="M3 6h6l2 2h10v11H3V6Z"/>',chevron:'<path d="m9 5 7 7-7 7"/>',fold:'<path d="M4 5h16M4 19h16m5-10 3 3 3-3m-6 6 3-3 3 3"/>',plus:'<path d="M12 5v14M5 12h14"/>',edit:'<path d="m15 4 5 5M4 20l5-1L21 7l-5-5L4 14v6Z"/>',panel:'<rect x="3" y="4" width="18" height="16" rx="2"/><path d="M15 4v16m3-11h0m0 4h0"/>',spark:'<path d="m12 3 2.3 6.7L21 12l-6.7 2.3L12 21l-2.3-6.7L3 12l6.7-2.3L12 3Z"/>',x:'<path d="m6 6 12 12M6 18 18 6"/>'}
</script>
