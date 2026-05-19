package com.medical.knowledgegraph.exception;

/**
 * 知识图谱异常类
 */
public class KnowledgeGraphException extends RuntimeException {

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 构造函数
     */
    public KnowledgeGraphException(String message) {
        super(message);
    }

    /**
     * 构造函数
     */
    public KnowledgeGraphException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造函数
     */
    public KnowledgeGraphException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 构造函数
     */
    public KnowledgeGraphException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 获取错误码
     */
    public String getErrorCode() {
        return errorCode;
    }
}
