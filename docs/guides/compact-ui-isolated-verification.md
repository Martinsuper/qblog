# 完整工作区隔离验证

## 快照

- 基线：`911ce349`。
- 本地隔离目录：`/tmp/qblog-release-verify-APxEhf`（临时目录，不作为长期备份）。
- 复制时纳入实际存在的 Git 跟踪文件与未忽略的未跟踪文件，共 340 个文件；不复制现有 node_modules、target、dist 或运行时数据库。
- `verification-manifest.json` 记录每个文件的 SHA-256，摘要为 `79d692a9c85a37422232fe506f871e0e16a9af9077f2c95b3334bb05295c530a`。
- 该清单用于确认验证输入，不是可跨环境部署的签名或发布产物。

## 结果

后端：

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home mvn clean verify
```

- 119 项测试，0 失败、0 错误、0 跳过。
- Spring Boot JAR 打包成功。
- 显式指定 Java 17：当前机器默认 Maven 可能选用 Java 26，不能仅凭 `java -version` 判断 Maven 实际 JDK。

前端：

```sh
npm ci
npm run lint
npm test
npm run build
```

- 从锁文件独立安装成功，npm 安装审计报告为 0 个漏洞。
- 全量 lint 通过。
- 67 项测试全部通过。
- 生产构建成功。
- 仍有编辑器分包约 515 kB 的构建警告；不是构建失败。
- npm 有安装脚本审批提示，当前安装和构建成功；未修改项目依赖或脚本审批策略。

原工作区：

- 未暂存、未提交、未部署。
- `git diff --check` 通过。
- 验证后比对快照，发现 `docs/guides/private-knowledge-base.md` 在验证期间发生变化；未覆盖或还原该变动。其余清单内文件与验证快照一致。

## 边界

这是包含此前备份、MCP、编辑器、品牌等改动的完整工作区验证，不是纯 UI 补丁验证，也没有验证各建议拆分提交。

没有启动隔离后端去连接或修改现有数据库；Maven 测试成功不等价于目标部署环境的数据库、存储权限和代理配置已通过验收。

未执行备份恢复、导入、真实改密、令牌创建撤销。真实设备和浏览器缩放验收仍按 `compact-ui-release-checklist.md` 保留待办。

下一步：依据 `compact-ui-commit-plan.md` 审查并批准前置业务能力范围，然后逐组暂存、检查暂存差异，在只含该组及前置提交的环境重新验证。提交或部署前需明确确认。
