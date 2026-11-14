package com.tianxin.lifeline.spark.common;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;

/**
 * SparkSession 构建工厂。
 *
 * <p>master 参数化：本地模式默认 {@code local[*]} 可直跑调试；
 * 集群提交时经 spark-submit --master yarn/k8s 覆盖。
 * Java 17 运行 Spark 3.5 所需的 --add-opens 模块开放参数
 * 统一在此以 driver/executor extraJavaOptions 兜底注入，
 * 避免本地 IDE 直跑时因 JDK 模块封闭抛 IllegalAccessError。
 */
public final class SparkSessionFactory {

    /** Java 17 运行 Spark 所需 JVM 模块开放参数 */
    private static final String JDK17_OPENS = String.join(" ",
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
            "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
            "--add-opens=java.base/java.io=ALL-UNNAMED",
            "--add-opens=java.base/java.net=ALL-UNNAMED",
            "--add-opens=java.base/java.nio=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED",
            "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
            "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
            "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
            "--add-opens=java.base/sun.nio.cs=ALL-UNNAMED",
            "--add-opens=java.base/sun.security.action=ALL-UNNAMED",
            "--add-opens=java.base/sun.util.calendar=ALL-UNNAMED");

    private SparkSessionFactory() {
    }

    /**
     * 创建 SparkSession。
     *
     * @param appName 作业名（Spark UI 展示）
     * @param params  作业参数（读取 master 等配置）
     * @return SparkSession 实例
     */
    public static SparkSession create(String appName, SparkJobParams params) {
        SparkConf conf = new SparkConf()
                .setAppName(appName)
                .setMaster(params.master())
                .set("spark.sql.session.timeZone", "Asia/Shanghai")
                .set("spark.driver.extraJavaOptions", JDK17_OPENS)
                .set("spark.executor.extraJavaOptions", JDK17_OPENS);
        return SparkSession.builder().config(conf).getOrCreate();
    }
}
