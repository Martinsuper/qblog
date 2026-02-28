# 个人中心修改密码功能实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在个人中心页面添加修改密码功能，用户需要验证旧密码后才能设置新密码。

**Architecture:** 后端新增 ChangePasswordDTO 和 changePassword API，前端在 Profile.vue 中添加折叠式修改密码表单，使用 Element Plus Collapse 组件。

**Tech Stack:** Spring Boot 3.2, MyBatis Plus, Vue 3, Element Plus, BCrypt

---

## Task 1: 后端 - 创建 ChangePasswordDTO

**Files:**
- Create: `backend/src/main/java/com/qblog/model/dto/ChangePasswordDTO.java`

**Step 1: 创建 DTO 文件**

```java
package com.qblog.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改密码 DTO
 */
@Data
public class ChangePasswordDTO {

    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, message = "新密码长度至少为6位")
    private String newPassword;
}
```

**Step 2: 验证文件创建成功**

Run: `ls backend/src/main/java/com/qblog/model/dto/ChangePasswordDTO.java`
Expected: 文件路径输出

**Step 3: Commit**

```bash
git add backend/src/main/java/com/qblog/model/dto/ChangePasswordDTO.java
git commit -m "feat: 添加 ChangePasswordDTO"
```

---

## Task 2: 后端 - UserService 接口新增方法

**Files:**
- Modify: `backend/src/main/java/com/qblog/service/UserService.java`

**Step 1: 在 UserService 接口中添加方法签名**

在 `getUserById` 方法后添加：

```java
    /**
     * 修改密码
     */
    void changePassword(ChangePasswordDTO changePasswordDTO);
```

需要在文件顶部添加 import:
```java
import com.qblog.model.dto.ChangePasswordDTO;
```

**Step 2: 验证编译**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add backend/src/main/java/com/qblog/service/UserService.java
git commit -m "feat: UserService 添加 changePassword 方法签名"
```

---

## Task 3: 后端 - UserServiceImpl 实现修改密码

**Files:**
- Modify: `backend/src/main/java/com/qblog/service/impl/UserServiceImpl.java`

**Step 1: 添加 import**

在文件顶部添加：
```java
import com.qblog.model.dto.ChangePasswordDTO;
```

**Step 2: 实现 changePassword 方法**

在 `getUserById` 方法后添加：

```java
    @Override
    public void changePassword(ChangePasswordDTO changePasswordDTO) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        User user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(changePasswordDTO.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }

        // 验证新旧密码不能相同
        if (changePasswordDTO.getOldPassword().equals(changePasswordDTO.getNewPassword())) {
            throw new RuntimeException("新密码不能与旧密码相同");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
        updateById(user);
    }
```

**Step 3: 验证编译**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add backend/src/main/java/com/qblog/service/impl/UserServiceImpl.java
git commit -m "feat: UserServiceImpl 实现 changePassword 方法"
```

---

## Task 4: 后端 - AuthController 添加 API 端点

**Files:**
- Modify: `backend/src/main/java/com/qblog/controller/AuthController.java`

**Step 1: 添加 import**

在文件顶部添加：
```java
import com.qblog.model.dto.ChangePasswordDTO;
```

**Step 2: 添加 changePassword 端点**

在 `logout` 方法后添加：

```java
    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO changePasswordDTO) {
        userService.changePassword(changePasswordDTO);
        return Result.success();
    }
```

**Step 3: 验证编译**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add backend/src/main/java/com/qblog/controller/AuthController.java
git commit -m "feat: AuthController 添加修改密码 API"
```

---

## Task 5: 后端 - 测试 API

**Step 1: 启动后端服务**

Run: `cd backend && mvn spring-boot:run`

**Step 2: 使用 curl 测试 API**

```bash
# 先登录获取 token
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 使用返回的 token 测试修改密码
curl -X PUT http://localhost:8081/api/auth/password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"oldPassword":"admin123","newPassword":"newpass123"}'
```

Expected: `{"code":200,"message":"success","data":null}`

**Step 3: 测试旧密码错误场景**

```bash
curl -X PUT http://localhost:8081/api/auth/password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"oldPassword":"wrongpassword","newPassword":"newpass123"}'
```

Expected: `{"code":400,"message":"旧密码错误","data":null}`

---

## Task 6: 前端 - 添加 API 函数

**Files:**
- Modify: `frontend/src/api/auth.js`

**Step 1: 添加 changePassword 函数**

在 `logout` 函数后添加：

```javascript
export function changePassword(data) {
  return request({
    url: '/auth/password',
    method: 'put',
    data
  })
}
```

**Step 2: 验证语法**

Run: `cd frontend && npm run lint -- --fix api/auth.js`
Expected: 无错误

**Step 3: Commit**

```bash
git add frontend/src/api/auth.js
git commit -m "feat: auth.js 添加 changePassword API"
```

---

## Task 7: 前端 - Profile.vue 添加修改密码功能

**Files:**
- Modify: `frontend/src/views/User/Profile.vue`

**Step 1: 修改 template**

将整个 template 替换为：

```vue
<template>
  <div class="py-6 md:py-8">
    <div class="max-w-2xl mx-auto">
      <!-- 基本信息卡片 -->
      <div class="card p-6 md:p-8 mb-4">
        <h3 class="text-xl font-semibold mb-6" style="color: var(--text-primary)">个人中心</h3>
        <el-form :model="userForm" label-width="100px">
          <el-form-item label="头像">
            <el-avatar :size="80" :src="userForm.avatar">
              {{ userForm.nickname?.charAt(0) }}
            </el-avatar>
          </el-form-item>
          <el-form-item label="用户名">
            <el-input v-model="userForm.username" disabled />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="userForm.nickname" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="userForm.email" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleUpdate">保存修改</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 修改密码折叠区域 -->
      <el-collapse class="card">
        <el-collapse-item title="修改密码" name="password">
          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="100px"
            class="px-4 pb-4"
          >
            <el-form-item label="旧密码" prop="oldPassword">
              <el-input
                v-model="passwordForm.oldPassword"
                type="password"
                show-password
                placeholder="请输入旧密码"
              />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                show-password
                placeholder="请输入新密码（至少6位）"
              />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                show-password
                placeholder="请再次输入新密码"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleChangePassword" :loading="loading">
                确认修改
              </el-button>
              <el-button @click="resetPasswordForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-collapse-item>
      </el-collapse>
    </div>
  </div>
</template>
```

**Step 2: 修改 script**

将 script 部分替换为：

```vue
<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { changePassword } from '@/api/auth'

const userStore = useUserStore()

const userForm = reactive({
  username: '',
  nickname: '',
  email: '',
  avatar: ''
})

const passwordFormRef = ref()
const loading = ref(false)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 验证新密码与确认密码一致
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 验证新旧密码不同
const validateNewPassword = (rule, value, callback) => {
  if (value === passwordForm.oldPassword) {
    callback(new Error('新密码不能与旧密码相同'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入旧密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少为6位', trigger: 'blur' },
    { validator: validateNewPassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleUpdate = () => {
  ElMessage.info('修改功能开发中...')
}

const handleChangePassword = async () => {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功')
    resetPasswordForm()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '密码修改失败')
  } finally {
    loading.value = false
  }
}

const resetPasswordForm = () => {
  passwordFormRef.value?.resetFields()
}

onMounted(() => {
  if (userStore.userInfo) {
    Object.assign(userForm, userStore.userInfo)
  }
})
</script>
```

**Step 3: 修改 style**

添加 collapse 样式：

```css
.card {
  background: var(--bg-secondary);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-sm);
}

.max-w-2xl {
  max-width: 600px;
}

:deep(.el-collapse-item__header) {
  background: transparent;
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
  padding: 16px 20px;
  border: none;
}

:deep(.el-collapse-item__wrap) {
  background: transparent;
  border: none;
}

:deep(.el-collapse-item__content) {
  padding: 0;
}
```

**Step 4: 验证语法**

Run: `cd frontend && npm run lint -- --fix views/User/Profile.vue`
Expected: 无错误

**Step 5: Commit**

```bash
git add frontend/src/views/User/Profile.vue
git commit -m "feat: Profile.vue 添加修改密码功能"
```

---

## Task 8: 集成测试

**Step 1: 启动后端服务**

Run: `cd backend && mvn spring-boot:run`

**Step 2: 启动前端服务**

Run: `cd frontend && npm run dev`

**Step 3: 手动测试**

1. 打开 http://localhost:3001
2. 使用 admin/admin123 登录
3. 进入个人中心页面
4. 展开"修改密码"区域
5. 输入旧密码和新密码
6. 点击"确认修改"
7. 验证成功提示

**Step 4: 测试验证规则**

- 旧密码错误：应显示错误提示
- 新密码少于6位：应显示验证错误
- 两次密码不一致：应显示验证错误
- 新旧密码相同：应显示验证错误

---

## Task 9: 最终提交

```bash
git add -A
git commit -m "feat: 完成个人中心修改密码功能"
```