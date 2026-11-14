package com.tianxin.lifeline.common.data.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PostGIS Geometry 类型处理器（占位实现）。
 *
 * <p>当前阶段以 WKT（Well-Known Text）字符串形式读写 PostGIS geometry 字段；
 * 后续接入 JTS / postgis-jdbc 后可升级为真正的空间对象映射。
 *
 * <p>使用方式：实体字段标注 {@code @TableField(typeHandler = PostGisGeometryHandler.class)}。
 */
@MappedTypes(String.class)
public class PostGisGeometryHandler extends BaseTypeHandler<String> {

    /**
     * 写入参数：将 WKT 字符串设置到 PreparedStatement。
     *
     * @param ps              预编译语句
     * @param i               参数索引
     * @param parameter       WKT 字符串（如 LINESTRING(...)）
     * @param jdbcType        JDBC 类型
     * @throws SQLException SQL 异常
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        // 占位实现：直接以字符串写入，后续可替换为 ST_GeomFromText 参数绑定
        ps.setObject(i, parameter);
    }

    /**
     * 按列名读取结果。
     *
     * @param rs         结果集
     * @param columnName 列名
     * @return WKT 字符串
     * @throws SQLException SQL 异常
     */
    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getString(columnName);
    }

    /**
     * 按列索引读取结果。
     *
     * @param rs          结果集
     * @param columnIndex 列索引
     * @return WKT 字符串
     * @throws SQLException SQL 异常
     */
    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getString(columnIndex);
    }

    /**
     * 从存储过程结果读取。
     *
     * @param cs          可调用语句
     * @param columnIndex 列索引
     * @return WKT 字符串
     * @throws SQLException SQL 异常
     */
    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getString(columnIndex);
    }
}
