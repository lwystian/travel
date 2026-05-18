package org.example.springboot.common;

/**
 * 日志操作类型常量
 */
public class LogType {

    /**
     * 操作类型
     */
    public static class Operation {
        // 认证相关
        public static final String LOGIN = "LOGIN";
        public static final String LOGOUT = "LOGOUT";
        public static final String REGISTER = "REGISTER";

        // 用户相关
        public static final String USER_LOGIN = "USER_LOGIN";
        public static final String USER_LOGOUT = "USER_LOGOUT";
        public static final String USER_REGISTER = "USER_REGISTER";
        public static final String USER_UPDATE = "USER_UPDATE";
        public static final String USER_DELETE = "USER_DELETE";
        public static final String USER_STATUS_CHANGE = "USER_STATUS_CHANGE";

        // 景点相关
        public static final String SCENIC_VIEW = "SCENIC_VIEW";
        public static final String SCENIC_CREATE = "SCENIC_CREATE";
        public static final String SCENIC_UPDATE = "SCENIC_UPDATE";
        public static final String SCENIC_DELETE = "SCENIC_DELETE";

        // 评论相关
        public static final String COMMENT_CREATE = "COMMENT_CREATE";
        public static final String COMMENT_UPDATE = "COMMENT_UPDATE";
        public static final String COMMENT_DELETE = "COMMENT_DELETE";
        public static final String COMMENT_LIKE = "COMMENT_LIKE";

        // 攻略相关
        public static final String GUIDE_CREATE = "GUIDE_CREATE";
        public static final String GUIDE_UPDATE = "GUIDE_UPDATE";
        public static final String GUIDE_DELETE = "GUIDE_DELETE";
        public static final String GUIDE_VIEW = "GUIDE_VIEW";

        // 订单相关
        public static final String ORDER_CREATE = "ORDER_CREATE";
        public static final String ORDER_CANCEL = "ORDER_CANCEL";
        public static final String ORDER_PAY = "ORDER_PAY";
        public static final String ORDER_REFUND = "ORDER_REFUND";
        public static final String ORDER_COMPLETE = "ORDER_COMPLETE";

        // 支付相关
        public static final String PAY_CREATE = "PAY_CREATE";
        public static final String PAY_SUCCESS = "PAY_SUCCESS";
        public static final String PAY_FAIL = "PAY_FAIL";
        public static final String PAY_REFUND = "PAY_REFUND";

        // 收藏相关
        public static final String COLLECTION_ADD = "COLLECTION_ADD";
        public static final String COLLECTION_REMOVE = "COLLECTION_REMOVE";

        // 文件上传相关
        public static final String FILE_UPLOAD = "FILE_UPLOAD";
        public static final String FILE_DELETE = "FILE_DELETE";

        // 审核相关
        public static final String REVIEW_PASS = "REVIEW_PASS";
        public static final String REVIEW_REJECT = "REVIEW_REJECT";

        // 系统管理
        public static final String SYS_CONFIG_UPDATE = "SYS_CONFIG_UPDATE";
        public static final String SYS_BACKUP = "SYS_BACKUP";
        public static final String SYS_RESTORE = "SYS_RESTORE";

        // 敏感词
        public static final String SENSITIVE_DETECTED = "SENSITIVE_DETECTED";
    }

    /**
     * 操作对象类型
     */
    public static class ObjectType {
        public static final String USER = "USER";
        public static final String SCENIC = "SCENIC";
        public static final String COMMENT = "COMMENT";
        public static final String GUIDE = "GUIDE";
        public static final String ORDER = "ORDER";
        public static final String PAYMENT = "PAYMENT";
        public static final String FILE = "FILE";
        public static final String COLLECTION = "COLLECTION";
        public static final String SENSITIVE = "SENSITIVE";
        public static final String REVIEW = "REVIEW";
        public static final String SYSTEM = "SYSTEM";
    }

    /**
     * 操作结果
     */
    public static class Result {
        public static final String SUCCESS = "SUCCESS";
        public static final String FAIL = "FAIL";
        public static final String ERROR = "ERROR";
    }

    /**
     * 登录类型
     */
    public static class LoginType {
        public static final String NORMAL = "NORMAL";
        public static final String TOKEN = "TOKEN";
        public static final String WECHAT = "WECHAT";
    }

    /**
     * 审核类型
     */
    public static class ReviewType {
        public static final String AUTO = "AUTO";
        public static final String MANUAL = "MANUAL";
    }

    /**
     * 审核状态
     */
    public static class ReviewStatus {
        public static final String PENDING = "PENDING";
        public static final String APPROVED = "APPROVED";
        public static final String REJECTED = "REJECTED";
    }

    /**
     * 备份类型
     */
    public static class BackupType {
        public static final String LOG = "LOG";
        public static final String DB = "DB";
        public static final String FILE = "FILE";
    }
}
