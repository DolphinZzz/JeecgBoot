# Flyway 使用说明（JeecgBoot 项目）

> 面向本项目 WMS 模块的数据库变更管理。记录 Flyway 的作用、本项目里的配置、日常加迁移的步骤，以及出问题时的处理办法。

## 一、Flyway 解决什么问题

代码有 Git，改动可追溯、可回滚、能确认线上跑的是哪个版本。数据库结构变化一直没有同等管理，常见做法是手工执行 `ALTER TABLE`，然后口头或文档通知同事。这会带来几个问题：

- 同事漏跑，代码直接报 `Unknown column`；
- 测试库和正式库结构不一致，排查成本高；
- 说不清某个字段是哪次加的、为什么加；
- 上线时忘了执行，或者重复执行（`ADD COLUMN` 执行两次直接报错）。

Flyway 把每一条 DDL 变成一个带版本号的脚本文件，提交进 Git，由程序启动时判断哪些还没执行、按顺序补齐。

核心只有一张表 `flyway_schema_history`，本库当前有 36 行：

```sql
SELECT installed_rank, version, description, script, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

## 二、工作方式

每次启动依次做三件事。

**1. 扫描脚本目录**，解析出所有迁移文件并按版本号排序。

文件名格式 `V<版本号>__<描述>.sql`，注意是**两个下划线**：

```
V20261028_1__wms_warehouse_loca_alter.sql
└──版本──┘  └────────描述───────┘
```

版本号里的 `_` 会被当作 `.`，`V20261028_1` 的实际版本是 `20261028.1`。版本按数字比较，所以 `_10` 排在 `_9` 之后，不存在字符串排序里 `10` 排在 `9` 前面的问题。

**2. 比对历史表**，找出「文件里有、历史表里没有」的，即待执行（Pending）的迁移。

**3. 按顺序执行**，每成功一个就往历史表插一行，并记下该文件的 **checksum**（内容校验和）。

第二次启动时所有脚本都已在历史表里，Flyway 输出 `Schema is up to date. No migration necessary.` 后什么都不做。**幂等性来自历史表，不依赖 SQL 本身能否重复执行。**

**checksum 是 Flyway 的强制机制。** 它用于确认线上的结构确实来自这份脚本文件。已执行过的脚本哪怕只改一个注释字符，checksum 就变，启动时直接报错并拒绝继续：

```
Migration checksum mismatch for migration version 20261028.1
```

背后的道理是：**已执行的脚本不能改**。它可能在别人的库、测试库、以后的正式库上都跑过了，改了会让所有环境的结构悄悄分叉。

## 三、本项目中的配置

### 3.1 脚本目录

```
jeecg-module-system/jeecg-system-start/src/main/resources/flyway/sql/mysql/
```

### 3.2 两套启动模块（单体版 / 微服务版）

本项目有两种启动方式，**它们的 Flyway 配置互相独立**，改动前先确认你在改哪一个。

| | 单体版 | 微服务版 |
| :--- | :--- | :--- |
| 启动类 | `JeecgSystemApplication` | `JeecgSystemCloudApplication` |
| 模块 | `jeecg-module-system/jeecg-system-start` | `jeecg-server-cloud/jeecg-system-cloud-start` |
| 开关 | `spring.flyway.enabled` | **`jeecg.flyway.enabled`** |
| 默认 | 按 profile（见下表） | `true` |
| 配置来源 | 本地 `application-{profile}.yml` | 本地 `application.yml` + Nacos |

**为什么微服务版用另一个开关名**：Nacos 的 `jeecg-dev.yaml` 里写着 `spring.flyway.enabled: false`，那是用来关闭 Spring Boot 自带的 Flyway 自动装配的（`jeecg.yaml` 里同时把 `FlywayAutoConfiguration` 排除了）。如果微服务版的 `FlywayConfig` 沿用同名属性，这个 `false` 会经 Nacos 下发并覆盖本地配置，导致迁移永远不执行。所以微服务版另起了 `jeecg.flyway.enabled`，默认 `true`。

单体版各 profile 的开关：

| profile | `spring.flyway.enabled` |
| :--- | :--- |
| dev | `true` |
| test | `true` |
| **prod** | **`false`** |
| docker / oracle / postgresql 等 | 未配置（默认 `false`） |

**正式环境是关闭的**，这是 JeecgBoot 的既有设定：生产库的结构变更不希望跟着应用重启自动发生。由此留了一个决策点：**未来上线时，是人工在正式库执行这些脚本，还是把 prod 的开关打开？** 当前配置等同「生产环境每次上线需人工执行一遍脚本」，这个决定需要在首次生产部署前定下来。

### 3.3 执行入口

两个模块各有一份 `FlywayConfig`，逻辑基本一致：

- 单体版：[jeecg-system-start/.../flyway/FlywayConfig.java](../../jeecg-module-system/jeecg-system-start/src/main/java/org/jeecg/config/flyway/FlywayConfig.java)
- 微服务版：[jeecg-system-cloud-start/.../flyway/FlywayConfig.java](../../jeecg-server-cloud/jeecg-system-cloud-start/src/main/java/org/jeecg/config/flyway/FlywayConfig.java)

都没有使用 Spring Boot 的 Flyway 自动配置，而是在 `@PostConstruct` 里手工调用 `flyway.migrate()`。几个关键细节：

- 只对 `master` 数据源操作，且 URL 必须包含 `mysql`；
- 整个调用包在 `try / catch (FlywayException)` 里，**失败只打日志，不阻止应用启动**。

第二点是个坑：迁移失败时应用照常起来，但表还是旧的，后续会报一堆莫名其妙的 SQL 错误。**部署后要主动检查日志里有没有「flyway执行sql脚本失败」。**

### 3.4 微服务版的脚本来源

`cloud-start` 不依赖 `system-start`，脚本默认不在它的 classpath 上。解决办法是在 [cloud-start 的 pom.xml](../../jeecg-server-cloud/jeecg-system-cloud-start/pom.xml) 里用 `<resources>` 把脚本目录复制进来：

```xml
<resource>
    <directory>../../jeecg-module-system/jeecg-system-start/src/main/resources/flyway</directory>
    <targetPath>flyway</targetPath>
    <filtering>false</filtering>
    <includes>
        <include>**/*.sql</include>
    </includes>
</resource>
```

**脚本仍然只有一份**，都在 `jeecg-system-start/src/main/resources/flyway/sql/mysql/` 下。加脚本时只在这一处加，两个启动模块都会拿到。`includes` 限定 `**/*.sql`，是为了排除 `backup/` 目录下的历史 zip 包。

> **`<resources>` 一旦声明就会整体替换父 pom 的资源配置**，不是追加。父 pom（`spring-boot-starter-parent`）原本有两条：一条对 `application*.yml` 开 `filtering`（替换 `@config.server-addr@` 这类 Maven 占位符），一条把其余资源原样拷贝。第一次只写了一个不过滤的 `src/main/resources`，结果构建产物里的 `application.yml` 仍是原样的 `@config.server-addr@`，启动时连不上 nacos。现在这两条已照抄父 pom 的写法放进 [cloud-start 的 pom.xml](../../jeecg-server-cloud/jeecg-system-cloud-start/pom.xml)。
>
> 验证办法：构建后检查 `target/classes/application.yml`，`server-addr` 应是 `jeecg-boot-nacos:8848` 而不是 `@config.server-addr@`。

### 3.5 版本

`pom.xml` 锁定 Flyway **7.15.0**。这个版本官方支持到 MySQL 8.0，本机库是 8.4，执行时会有警告：

```
Flyway upgrade recommended: MySQL 8.4 is newer than this version of Flyway
```

目前实测正常，但这个警告未来可能变成真实的不兼容，届时需要升级 Flyway 版本（注意 `flyway-mysql` 在 Flyway 8+ 已拆分为独立依赖）。

### 3.6 数据源密码

`application-dev.yml` 中 `master` 数据源的密码与本地容器一致（`root` / `123456`）。若用 `docker compose up` 起一套自己的 MySQL，`docker-compose.yml` 里的 `MYSQL_ROOT_PASSWORD` 是 `root`，与 dev 配置不一致，需要统一其中一处。

明文密码会随文件进入 Git。仅本地开发库可接受；若仓库要开源或多人共享，应改为 Nacos 配置中心或环境变量覆盖。

## 四、日常：添加一个迁移

### 第 1 步 确定版本号

先看当前最新版本：

```bash
/usr/local/bin/docker exec -i mysql mysql -uroot -p123456 -N -B jeecg-boot \
  -e "SELECT MAX(version) FROM flyway_schema_history WHERE success=1;"
```

当前号段是 `20261028.x`，下一个文件是 `V20261028_10__xxx.sql`；隔天写的用 `V20261029_1__xxx.sql` 更清晰。

### 第 2 步 写脚本

放在 `flyway/sql/mysql/` 下，内容是普通 SQL。注意：

- **一条语句一个分号结尾**。Flyway 默认不支持 `DELIMITER`，存储过程、触发器需单独评估；
- MySQL 的 DDL 会隐式提交，**脚本中途失败会停在半路**，需要人工介入，因此脚本应尽量小、单一职责；
- `IF NOT EXISTS` / `WHERE NOT EXISTS` 只是防止自身的重跑问题，**不能替代版本管理**，防重主力仍是历史表。

### 第 3 步 先在克隆库试跑

```bash
# 1. 备份
/usr/local/bin/docker exec -i mysql mysqldump -uroot -p123456 \
  --single-transaction --no-tablespaces jeecg-boot \
  > ~/wms-db-backups/before-$(date +%Y%m%d-%H%M).sql

# 2. 建克隆库并导入
/usr/local/bin/docker exec -i mysql mysql -uroot -p123456 \
  -e "CREATE DATABASE wms_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;"
/usr/local/bin/docker exec -i mysql mysql -uroot -p123456 --default-character-set=utf8mb4 \
  wms_test < ~/wms-db-backups/before-XXX.sql

# 3. 在克隆库上执行新脚本
/usr/local/bin/docker exec -i mysql mysql -uroot -p123456 --default-character-set=utf8mb4 \
  wms_test < path/to/V20261029_1__xxx.sql

# 4. 用完清理
/usr/local/bin/docker exec -i mysql mysql -uroot -p123456 -e "DROP DATABASE wms_test;"
```

> **必须带 `-i`。** 少了它 `docker exec ... mysql ... < file` 会返回 0 但实际什么都没执行，因为 stdin 没有被转发进去。

这一步不能省。WMS 这一批 9 个脚本中，`--（` 中文注释不生效、字典 ID 超 32 位、ID 补零后重复，三个问题都是在这一步暴露的，没有落到业务库。

### 第 4 步 启动应用自动执行

dev 环境直接启动即可。日志会出现：

```
Migrating schema `jeecg-boot` to version "20261029.1 - wms xxx"
Successfully applied 1 migration
```

### 第 5 步 提交

脚本文件本身就是交付物，和代码一起提交。

## 五、出问题时的处理

### 5.1 查看状态

```bash
/usr/local/bin/docker exec -i mysql mysql -uroot -p123456 jeecg-boot -e "
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank DESC LIMIT 10;"
```

### 5.2 checksum 冲突

如果确实改动了已执行的脚本（例如只改了注释），可以执行 `repair()` 让其重算 checksum：

```java
Flyway.configure().dataSource(url, user, pass).locations(locations).load().repair();
```

**`repair()` 只让校验通过，不会让数据库跟上脚本的改动。** 如果改的是真实 DDL，仍需再写一个新脚本去实际修改。

等价的手工做法是直接修正历史表对应行的 `checksum` 列。

### 5.3 迁移失败

MySQL 中会留下 `success = 0` 的行，Flyway 之后会拒绝继续执行，必须先处理：

- 确认脚本的副作用没有部分生效 → 手工删除该行，重新跑；
- 确认脚本的副作用已完整生效 → 把该行改为 `success = 1`。

两条路都要先看清楚实际改了什么，不能凭猜测选。

### 5.4 历史表与文件对不上

典型场景：脚本是手工执行过的，历史表里没有记录。此时 Flyway 会重新执行，而 `ADD COLUMN` 之类不幂等的语句会直接报错。

处理办法是补登历史行，`checksum` 用 Flyway 的算法计算：

- 逐行读入文件；
- 去掉行尾换行符；
- 对每行 UTF-8 字节累加计算 CRC32；
- 转成 32 位有符号整数。

WMS 的 `V20261027_1` ~ `V20261027_6` 就是这么补登的，算法用库中已有的 20 个脚本反证过（20/20 一致）。补登时 `execution_time` 填 0、`installed_on` 按原执行日期填写，以便与真正的迁移区分。

## 六、五条铁律

1. **执行过的脚本永不修改**。要改就加新脚本，例如 `V20261029_2__fix_xxx.sql`。
2. **版本号只增不减，且不能重复**。两个文件同版本号 Flyway 直接报错。
3. **只用 `V` 前缀**。`R__` 可重复脚本、`U__` 撤销脚本本项目均未启用。
4. **脚本要能独立回滚**。不要把 `ALTER` 和一批 `INSERT` 塞进同一个文件，出事时不好拆。
5. **生产环境格外谨慎**。prod 的 Flyway 是关的，等于每次上线人工执行。上线前先在测试库跑一遍，并确认 `flyway_schema_history` 里没有 `success = 0`。

## 七、本库当前状态（2026-10-28）

微服务版首次启动实测（2026-10-28，`jeecg-system-cloud-start` 打包后 `java -jar` 启动）：

- 启动日志出现 `Current version of schema ...: 20261028.9` 与 `Schema ... is up to date. No migration necessary.`；
- `FlywayConfig` 打印 `【数据库升级】数据库结构已是最新，无需迁移。`；
- 另在克隆库上追加一个测试脚本（`V20261029_1`，建表）复跑，日志为 `Successfully applied 1 migration` / `【数据库升级】微服务 Flyway 执行了 1 个迁移脚本。`，`wms_fw_test.flyway_schema_history` 新增 `success=1` 的 `20261029.1` 行，新表建出；业务库未受影响，测试库用完已删除。


- `flyway_schema_history` 共 36 行：1 行 baseline + 20 行 JeecgBoot 原有 + 6 行补登的 `V20261027_*` + 9 行 `V20261028_*`；
- **没有 `success = 0` 的行**；
- 二次启动已验证返回 `up to date`；
- 业务库 183 张表、36 张 `wms_*`、22 个 `wms_*` 字典。

WMS 这 9 个脚本的明细见 [WMS业务流程与数据库演进方案.md](WMS业务流程与数据库演进方案.md) 的 8.1 节。
