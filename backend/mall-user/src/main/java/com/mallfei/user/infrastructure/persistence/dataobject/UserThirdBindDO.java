package com.mallfei.user.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("ums_user_third_bind")
public class UserThirdBindDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String thirdType;
    private String thirdUid;
    private String thirdNickname;
    private String thirdAvatar;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getThirdType() { return thirdType; }
    public void setThirdType(String thirdType) { this.thirdType = thirdType; }
    public String getThirdUid() { return thirdUid; }
    public void setThirdUid(String thirdUid) { this.thirdUid = thirdUid; }
    public String getThirdNickname() { return thirdNickname; }
    public void setThirdNickname(String thirdNickname) { this.thirdNickname = thirdNickname; }
    public String getThirdAvatar() { return thirdAvatar; }
    public void setThirdAvatar(String thirdAvatar) { this.thirdAvatar = thirdAvatar; }
}
