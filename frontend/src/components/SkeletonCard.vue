<template>
  <div class="skeleton-card">
    <div class="skeleton-header">
      <div class="skeleton-avatar skeleton-animate"></div>
      <div class="skeleton-meta">
        <div class="skeleton-line skeleton-short skeleton-animate"></div>
        <div class="skeleton-line skeleton-tiny skeleton-animate"></div>
      </div>
    </div>
    <div class="skeleton-body">
      <div class="skeleton-line skeleton-full skeleton-animate"></div>
      <div class="skeleton-line skeleton-full skeleton-animate"></div>
      <div class="skeleton-line skeleton-medium skeleton-animate"></div>
    </div>
    <div class="skeleton-footer">
      <div class="skeleton-tag skeleton-animate"></div>
      <div class="skeleton-tag skeleton-animate"></div>
      <div class="skeleton-tag skeleton-animate"></div>
    </div>
  </div>
</template>

<style scoped>
.skeleton-card {
  padding: 20px;
  background: var(--bg-primary, #fff);
  border-radius: 8px;
  margin-bottom: 16px;
}

.skeleton-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.skeleton-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #e8e8e8;
}

.skeleton-meta {
  flex: 1;
}

.skeleton-line {
  height: 14px;
  background: #e8e8e8;
  border-radius: 4px;
  margin-bottom: 8px;
}

.skeleton-full { width: 100%; }
.skeleton-medium { width: 70%; }
.skeleton-short { width: 40%; }
.skeleton-tiny { width: 25%; height: 10px; }

.skeleton-body {
  margin-bottom: 16px;
}

.skeleton-footer {
  display: flex;
  gap: 8px;
}

.skeleton-tag {
  width: 50px;
  height: 22px;
  background: #e8e8e8;
  border-radius: 11px;
}

.skeleton-animate {
  background: linear-gradient(90deg, #e8e8e8 25%, #f0f0f0 50%, #e8e8e8 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}
</style>
