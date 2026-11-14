# 安塞区城市安全生命线管网 AI 智慧平台

城市生命线（供水、燃气、热力、排水等地下管网）安全监测与预警平台的工程实现，包含微服务后端、大数据实时链路、AI 智能体、机器学习预警算法与前端可视化大屏。

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 后端框架 | Java 17、Spring Boot 3.2.5、Spring Cloud 2023.0.1、Spring Cloud Alibaba |
| 数据访问 | MyBatis-Plus 3.5.6、PostgreSQL、TDengine 3.2.7 |
| 中间件 | Kafka、Redis / Redisson、Nacos、MinIO |
| 大数据 | Flink 1.18.1（实时清洗 / 特征窗口 / 规则预警）、Spark（数据质量稽核） |
| AI | Python FastAPI（智能体服务 + 机器学习服务），GM(1,1)、3σ、孤立森林 |
| 前端 | Vue 3、TypeScript、Vite、Element Plus、ECharts、Leaflet |
| 部署 | Docker Compose、Nginx |

## 目录结构

```
lifeline-platform/
├── deploy/                    # Docker Compose、Nginx、初始化 SQL、压测脚本
│   ├── docker-compose.yml
│   ├── sql/                   # PostgreSQL / TDengine 建表与种子数据
│   └── perf/                  # JMeter 压测脚本
├── lifeline-common/           # 公共组件（通用返回、异常、工具、鉴权）
├── lifeline-service-api/      # 服务间 Feign 接口定义
├── lifeline-gateway/          # API 网关（路由、JWT 校验、限流）
├── lifeline-services/         # 业务微服务
│   ├── lifeline-sys-auth-service      # 认证与权限
│   ├── lifeline-pipe-asset-service    # 管网资产
│   ├── lifeline-monitor-data-service  # 监测数据查询
│   ├── lifeline-warning-service       # 预警事件
│   ├── lifeline-plan-service          # 预案与预案实例
│   ├── lifeline-construction-service  # 施工破坏防控
│   ├── lifeline-operation-service     # 运维工单
│   ├── lifeline-blockchain-service    # 区块链存证
│   ├── lifeline-ai-gateway-service    # AI 能力网关
│   └── lifeline-digital-twin-service  # 数字孪生
├── lifeline-bigdata/          # Flink 实时作业、Spark 离线作业
├── lifeline-ai/               # agent-service、ml-service（FastAPI）
├── lifeline-mock/             # 传感器数据模拟器
└── lifeline-ui/               # Vue 3 前端
```

## 快速启动

前置要求：Docker 与 Docker Compose。

```bash
cd deploy
docker compose up -d
```

服务编排包含 PostgreSQL、TDengine、Redis、Kafka、Nacos、MinIO、Flink、各业务微服务、AI 服务与前端。数据库表结构与种子数据由 `deploy/sql/` 下的脚本按序号初始化。

单独本地开发：

```bash
# 后端（Java 17 + Maven）
mvn clean package -DskipTests

# 前端
cd lifeline-ui
npm install
npm run dev
```

## 配置说明

各服务的 `application.yml` 通过环境变量覆盖默认值，例如：

| 环境变量 | 说明 | 默认值 |
| --- | --- | --- |
| `PG_PASSWORD` | PostgreSQL 密码 | `lifeline123` |
| `LIFELINE_JWT_SECRET` | JWT 签名密钥（≥256 bit） | 开发用内置值 |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | 对象存储凭据 | `minioadmin` / `minioadmin123` |

> ⚠️ 上述均为**本地开发默认值**，仅用于开箱即用的演示环境。生产部署前必须通过环境变量全部覆盖，并更换 JWT 密钥。

## 许可证

仓库尚未附带开源许可证。在添加明确许可证前，默认保留所有权利。
