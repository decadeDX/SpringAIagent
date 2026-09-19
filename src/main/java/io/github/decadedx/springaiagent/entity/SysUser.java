package io.github.decadedx.springaiagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.decadedx.springaiagent.enums.TrainingStatus;
import io.github.decadedx.springaiagent.enums.UserRole;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 持久化系统账号；密码仅保存 BCrypt 摘要，绝不作为 API 响应输出。
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("sys_user")
public class SysUser {

    /** 用户的数据库主键，也是 JWT subject 使用的可信身份。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用于登录且在系统内唯一的账号名。 */
    private String username;

    /** BCrypt 密码摘要，只能由认证服务读取并进行比对。 */
    @TableField("password_hash")
    private String passwordHash;

    /** 已验证用户在业务系统中的角色。 */
    private UserRole role;

    /** 学生安全培训状态；管理员不以该字段作为权限依据。 */
    @TableField("training_status")
    private TrainingStatus trainingStatus;

    /** 账号创建时间，数据库会话统一使用 UTC。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 账号最近更新时间，数据库会话统一使用 UTC。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
