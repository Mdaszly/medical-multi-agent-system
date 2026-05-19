package com.medical.knowledgegraph.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 知识图谱节点基类
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseNode {

    /**
     * 唯一标识符 (Neo4j内部ID或业务ID)
     */
    protected String id;

    /**
     * 名称
     */
    protected String name;

    /**
     * 标签类型 (用于Neo4j节点标签)
     */
    protected String label;

    /**
     * 创建时间
     */
    protected LocalDateTime createTime;

    /**
     * 更新时间
     */
    protected LocalDateTime updateTime;

    /**
     * 状态 (0-禁用, 1-启用)
     */
    protected Integer status;

    /**
     * 扩展属性
     */
    protected Map<String, Object> properties;

    /**
     * 添加扩展属性
     */
    public void addProperty(String key, Object value) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.put(key, value);
    }

    /**
     * 获取扩展属性
     */
    public Object getProperty(String key) {
        return properties != null ? properties.get(key) : null;
    }

    /**
     * 转换为Neo4j属性Map
     */
    public Map<String, Object> toNeo4jProperties() {
        Map<String, Object> props = new HashMap<>();
        
        if (id != null) props.put("id", id);
        if (name != null) props.put("name", name);
        if (createTime != null) props.put("createTime", createTime.toString());
        if (updateTime != null) props.put("updateTime", updateTime.toString());
        if (status != null) props.put("status", status);
        
        // 添加扩展属性
        if (properties != null) {
            props.putAll(properties);
        }
        
        return props;
    }
}
