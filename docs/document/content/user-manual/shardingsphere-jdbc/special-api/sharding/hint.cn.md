+++
title = "强制路由"
weight = 1
+++

## 背景信息

Apache ShardingSphere 使用 ThreadLocal 管理分片键值进行强制路由。 可以通过编程的方式向 HintManager 中添加分片值，该分片值仅在当前线程内生效。

Hint 的主要使用场景：
- 分片字段不存在 SQL 和数据库表结构中，而存在于外部业务逻辑。
- 强制在指定数据库进行某些数据操作。

## 操作步骤

1. 调用 HintManager.getInstance() 获取 HintManager 实例；
2. 调用 HintManager.addDatabaseShardingValue，HintManager.addTableShardingValue 方法设置分片键值；
3. 执行 SQL 语句完成路由和执行；
4. 调用 HintManager.close 清理 ThreadLocal 中的内容。

## 配置示例

### 规则配置

Hint 分片算法需要用户实现 `org.apache.shardingsphere.sharding.api.sharding.hint.HintShardingAlgorithm` 接口。
`org.apache.shardingsphere.sharding.api.sharding.hint.HintShardingAlgorithm` 存在两个内置实现为，

- `org.apache.shardingsphere.sharding.algorithm.sharding.hint.HintInlineShardingAlgorithm`
- `org.apache.shardingsphere.sharding.algorithm.sharding.classbased.ClassBasedShardingAlgorithm`

Apache ShardingSphere 在进行路由时，将会从 HintManager 中获取分片值进行路由操作。

参考配置如下：

```yaml
rules:
- !SHARDING
  tables:
    t_order:
      actualDataNodes: demo_ds_${0..1}.t_order_${0..1}
      databaseStrategy:
        hint:
          shardingColumn: order_id
          shardingAlgorithmName: hint_class_based
      tableStrategy:
        hint:
          shardingColumn: order_id
          shardingAlgorithmName: hint_inline
  shardingAlgorithms:
    hint_class_based:
      type: CLASS_BASED
      props:
        strategy: STANDARD
        algorithmClassName: xxx.xxx.xxx.HintXXXAlgorithm
    hint_inline:
      type: HINT_INLINE
      props:
        algorithm-expression: t_order_$->{value % 4}
  defaultTableStrategy:
    none:
  defaultKeyGenerateStrategy:
    type: SNOWFLAKE
    column: order_id

props:
    sql-show: true
```

### 获取 HintManager

```java
HintManager hintManager = HintManager.getInstance();
```

### 添加分片键值

- 使用 `hintManager.addDatabaseShardingValue` 来添加数据源分片键值。
- 使用 `hintManager.addTableShardingValue` 来添加表分片键值。

> 分库不分表情况下，强制路由至某一个分库时，可使用 `hintManager.setDatabaseShardingValue` 方式设置分片值。

### 清除分片键值

分片键值保存在 ThreadLocal 中，所以需要在操作结束时调用 `hintManager.close()` 来清除 ThreadLocal 中的内容。

__hintManager 实现了 AutoCloseable 接口，可推荐使用 try with resource 自动关闭。__

### 完整代码示例

```java
// Sharding database and table with using HintManager
String sql = "SELECT * FROM t_order";
try (HintManager hintManager = HintManager.getInstance();
     Connection conn = dataSource.getConnection();
     PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
    hintManager.addDatabaseShardingValue("t_order", 1);
    hintManager.addTableShardingValue("t_order", 2);
    try (ResultSet rs = preparedStatement.executeQuery()) {
        while (rs.next()) {
            // ...
        }
    }
}

// Sharding database without sharding table and routing to only one database with using HintManager
String sql = "SELECT * FROM t_order";
try (HintManager hintManager = HintManager.getInstance();
     Connection conn = dataSource.getConnection();
     PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
    hintManager.setDatabaseShardingValue(3);
    try (ResultSet rs = preparedStatement.executeQuery()) {
        while (rs.next()) {
            // ...
        }
    }
}
```

## 相关参考

- [核心特性：数据分片](/cn/features/sharding/)
- [开发者指南：数据分片](/cn/dev-manual/sharding/)
