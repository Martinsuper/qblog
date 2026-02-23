# 博客系统 API 接口设计

## 基础信息
- **Base URL**: `/api/v1`
- **认证方式**: JWT Bearer Token
- **数据格式**: JSON

---

## 1. 认证模块 (Auth)

### 1.1 用户登录
```
POST /auth/login
Request:
{
    "username": "string",
    "password": "string"
}
Response:
{
    "code": 200,
    "data": {
        "token": "string",
        "userInfo": {
            "id": 1,
            "username": "admin",
            "nickname": "管理员",
            "avatar": "string",
            "role": 1
        }
    }
}
```

### 1.2 用户注册
```
POST /auth/register
Request:
{
    "username": "string",
    "password": "string",
    "email": "string",
    "code": "string"  // 验证码
}
```

### 1.3 退出登录
```
POST /auth/logout
Header: Authorization: Bearer {token}
```

### 1.4 获取当前用户信息
```
GET /auth/me
Header: Authorization: Bearer {token}
Response:
{
    "code": 200,
    "data": {
        "id": 1,
        "username": "admin",
        "nickname": "管理员",
        "avatar": "string",
        "email": "admin@example.com",
        "role": 1
    }
}
```

### 1.5 刷新 Token
```
POST /auth/refresh
Header: Authorization: Bearer {token}
```

---

## 2. 文章模块 (Article)

### 2.1 获取文章列表
```
GET /articles
Query Params:
- page: 页码 (default: 1)
- size: 每页数量 (default: 10)
- categoryId: 分类 ID
- tagId: 标签 ID
- keyword: 搜索关键词
- sortBy: 排序字段 (createTime/viewCount/likeCount)
- sortOrder: 排序方式 (asc/desc)

Response:
{
    "code": 200,
    "data": {
        "total": 100,
        "list": [
            {
                "id": 1,
                "title": "文章标题",
                "summary": "文章摘要",
                "coverImage": "string",
                "author": {
                    "id": 1,
                    "nickname": "作者",
                    "avatar": "string"
                },
                "category": {
                    "id": 1,
                    "name": "分类名"
                },
                "tags": [{"id": 1, "name": "标签名"}],
                "viewCount": 100,
                "likeCount": 50,
                "commentCount": 10,
                "top": 1,
                "createTime": "2024-01-01 12:00:00",
                "publishTime": "2024-01-01 12:00:00"
            }
        ]
    }
}
```

### 2.2 获取文章详情
```
GET /articles/{id}
Response:
{
    "code": 200,
    "data": {
        "id": 1,
        "title": "文章标题",
        "content": "Markdown 内容",
        "summary": "摘要",
        "coverImage": "string",
        "author": {...},
        "category": {...},
        "tags": [...],
        "viewCount": 101,
        "likeCount": 50,
        "commentCount": 10,
        "createTime": "2024-01-01 12:00:00",
        "publishTime": "2024-01-01 12:00:00",
        "isLiked": false,  // 当前用户是否点赞
        "isFavorited": false  // 当前用户是否收藏
    }
}
```

### 2.3 创建文章
```
POST /articles
Header: Authorization: Bearer {token}
Request:
{
    "title": "文章标题",
    "content": "Markdown 内容",
    "summary": "摘要",
    "coverImage": "string",
    "categoryId": 1,
    "tagIds": [1, 2, 3],
    "status": 1  // 0-草稿 1-发布
}
```

### 2.4 更新文章
```
PUT /articles/{id}
Header: Authorization: Bearer {token}
Request: 同创建文章
```

### 2.5 删除文章
```
DELETE /articles/{id}
Header: Authorization: Bearer {token}
```

### 2.6 获取热门文章
```
GET /articles/hot
Query Params:
- limit: 数量 (default: 10)
```

### 2.7 获取最新文章
```
GET /articles/latest
Query Params:
- limit: 数量 (default: 10)
```

---

## 3. 分类模块 (Category)

### 3.1 获取分类列表
```
GET /categories
Response:
{
    "code": 200,
    "data": [
        {
            "id": 1,
            "name": "分类名",
            "description": "描述",
            "articleCount": 10,
            "sort": 1
        }
    ]
}
```

### 3.2 创建分类 (管理员)
```
POST /categories
Header: Authorization: Bearer {token}
Request:
{
    "name": "分类名",
    "description": "描述",
    "sort": 1
}
```

### 3.3 更新分类 (管理员)
```
PUT /categories/{id}
Header: Authorization: Bearer {token}
```

### 3.4 删除分类 (管理员)
```
DELETE /categories/{id}
Header: Authorization: Bearer {token}
```

---

## 4. 标签模块 (Tag)

### 4.1 获取标签列表
```
GET /tags
Query Params:
- keyword: 搜索关键词
Response:
{
    "code": 200,
    "data": [
        {
            "id": 1,
            "name": "标签名",
            "articleCount": 10
        }
    ]
}
```

### 4.2 创建标签 (管理员)
```
POST /tags
Header: Authorization: Bearer {token}
Request:
{
    "name": "标签名"
}
```

### 4.3 删除标签 (管理员)
```
DELETE /tags/{id}
Header: Authorization: Bearer {token}
```

---

## 5. 评论模块 (Comment)

### 5.1 获取文章评论列表
```
GET /articles/{articleId}/comments
Query Params:
- page: 页码
- size: 每页数量
Response:
{
    "code": 200,
    "data": {
        "total": 50,
        "list": [
            {
                "id": 1,
                "user": {
                    "id": 1,
                    "nickname": "用户",
                    "avatar": "string"
                },
                "content": "评论内容",
                "likeCount": 10,
                "replyCount": 5,
                "isLiked": false,
                "createTime": "2024-01-01 12:00:00",
                "replies": [...]  // 子评论
            }
        ]
    }
}
```

### 5.2 发表评论
```
POST /articles/{articleId}/comments
Header: Authorization: Bearer {token}
Request:
{
    "content": "评论内容",
    "parentId": 0  // 回复评论时填写父评论 ID
}
```

### 5.3 删除评论
```
DELETE /comments/{id}
Header: Authorization: Bearer {token}
```

### 5.4 点赞评论
```
POST /comments/{id}/like
Header: Authorization: Bearer {token}
```

---

## 6. 互动模块 (Interaction)

### 6.1 点赞文章
```
POST /articles/{id}/like
Header: Authorization: Bearer {token}
```

### 6.2 取消点赞
```
DELETE /articles/{id}/like
Header: Authorization: Bearer {token}
```

### 6.3 收藏文章
```
POST /articles/{id}/favorite
Header: Authorization: Bearer {token}
```

### 6.4 取消收藏
```
DELETE /articles/{id}/favorite
Header: Authorization: Bearer {token}
```

### 6.5 获取我的收藏列表
```
GET /favorites
Query Params:
- page: 页码
- size: 每页数量
```

---

## 7. 用户模块 (User)

### 7.1 获取用户信息
```
GET /users/{id}
Response:
{
    "code": 200,
    "data": {
        "id": 1,
        "nickname": "用户",
        "avatar": "string",
        "articleCount": 10,
        "likeCount": 100
    }
}
```

### 7.2 更新用户信息
```
PUT /users/me
Header: Authorization: Bearer {token}
Request:
{
    "nickname": "新昵称",
    "avatar": "新头像 URL",
    "email": "新邮箱"
}
```

### 7.3 修改密码
```
PUT /users/me/password
Header: Authorization: Bearer {token}
Request:
{
    "oldPassword": "旧密码",
    "newPassword": "新密码"
}
```

### 7.4 获取用户文章列表
```
GET /users/{userId}/articles
Query Params:
- page: 页码
- size: 每页数量
```

---

## 8. 文件上传 (Upload)

### 8.1 上传图片
```
POST /upload/image
Header: Authorization: Bearer {token}
Content-Type: multipart/form-data
Request:
- file: 图片文件

Response:
{
    "code": 200,
    "data": {
        "url": "https://example.com/uploads/2024/01/xxx.png"
    }
}
```

---

## 9. 统计模块 (Statistics) - 管理员

### 9.1 获取统计数据
```
GET /admin/statistics
Header: Authorization: Bearer {token}
Response:
{
    "code": 200,
    "data": {
        "articleCount": 100,
        "userCount": 500,
        "viewCount": 10000,
        "commentCount": 200
    }
}
```

---

## 10. 系统配置 (Config) - 管理员

### 10.1 获取系统配置
```
GET /admin/config
Header: Authorization: Bearer {token}
```

### 10.2 更新系统配置
```
PUT /admin/config
Header: Authorization: Bearer {token}
Request:
{
    "siteName": "Q 博客",
    "siteDescription": "描述",
    "articlePageSize": 10
}
```

---

## 错误响应格式

```json
{
    "code": 400,
    "message": "错误信息",
    "data": null
}
```

## 常见状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权/Token 过期 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
