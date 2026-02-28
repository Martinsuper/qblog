# 个人中心修改密码功能设计

## 概述

在用户个人中心页面新增修改密码功能，采用折叠区域设计，需要验证旧密码后才能设置新密码。

## 后端设计

### 新增 API

**接口：** `PUT /api/auth/password`

**请求 DTO:**
```java
public class ChangePasswordDTO {
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, message = "新密码长度至少为6位")
    private String newPassword;
}
```

**处理逻辑：**
1. 从 SecurityContext 获取当前用户 ID
2. 查询用户信息获取当前密码
3. 验证旧密码是否正确（使用 `passwordEncoder.matches()`）
4. 验证新密码与旧密码不能相同
5. 加密新密码并更新数据库

**响应：**
- 成功：`{ code: 200, message: "密码修改成功" }`
- 失败：`{ code: 400, message: "旧密码错误" }`

### 文件变更

| 文件 | 操作 |
|------|------|
| `model/dto/ChangePasswordDTO.java` | 新增 |
| `controller/AuthController.java` | 新增 `changePassword` 方法 |
| `service/UserService.java` | 新增 `changePassword` 方法签名 |
| `service/impl/UserServiceImpl.java` | 实现 `changePassword` 方法 |

## 前端设计

### UI 结构

Profile.vue 页面改造：

1. **基本信息卡片**（保持现有结构）
   - 头像、用户名、昵称、邮箱
   - 保存修改按钮

2. **修改密码折叠区域**（新增）
   - 使用 Element Plus `el-collapse` 组件
   - 展开后显示三个输入框：旧密码、新密码、确认新密码
   - 确认修改按钮

### 交互流程

1. 点击"修改密码"折叠面板展开表单
2. 填写三个密码字段
3. 前端验证通过后调用 API
4. 成功后折叠区域收起，显示成功提示
5. 失败则显示错误信息

### 文件变更

| 文件 | 操作 |
|------|------|
| `views/User/Profile.vue` | 添加修改密码折叠区域 |
| `api/auth.js` | 新增 `changePassword` API 函数 |

## 验证规则

### 后端验证

- `oldPassword` 不能为空
- `newPassword` 长度 >= 6 位
- 新旧密码不能相同
- 旧密码必须正确

### 前端验证

- 所有字段必填
- 新密码长度 >= 6 位
- 两次输入的新密码必须一致

## 错误处理

| 场景 | 提示信息 |
|------|---------|
| 旧密码错误 | "旧密码错误，请重新输入" |
| 新密码太短 | "新密码长度至少为 6 位" |
| 两次密码不一致 | "两次输入的新密码不一致" |
| 新旧密码相同 | "新密码不能与旧密码相同" |

## 安全考虑

- 后端验证旧密码正确性
- 密码使用 BCrypt 加密存储
- 接口需要 JWT 认证
- 响应中不返回密码相关信息