package top.zw.passwd;

/**
 * 密码数据模型 - 9字段Bean
 */
public class DataInfo {

    private int id;
    private String title;        // 名称
    private String regAddr;      // 网址/注册地址
    private String loginUser;    // 用户名
    private String loginPwd;     // 密码
    private String loginMail;    // 邮箱
    private String loginPhone;   // 电话
    private String remarks;      // 备注
    private String idTime;       // 创建时间
    private String updateTime;   // 修改时间

    public DataInfo() {
    }

    public DataInfo(String title, String regAddr, String loginUser, String loginPwd,
                    String loginMail, String loginPhone, String remarks, String idTime) {
        this.title = title;
        this.regAddr = regAddr;
        this.loginUser = loginUser;
        this.loginPwd = loginPwd;
        this.loginMail = loginMail;
        this.loginPhone = loginPhone;
        this.remarks = remarks;
        this.idTime = idTime;
    }

    public DataInfo(String title, String regAddr, String loginUser, String loginPwd,
                    String loginMail, String loginPhone, String remarks, String idTime,
                    String updateTime) {
        this.title = title;
        this.regAddr = regAddr;
        this.loginUser = loginUser;
        this.loginPwd = loginPwd;
        this.loginMail = loginMail;
        this.loginPhone = loginPhone;
        this.remarks = remarks;
        this.idTime = idTime;
        this.updateTime = updateTime;
    }

    // --- Getters & Setters ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRegAddr() {
        return regAddr;
    }

    public void setRegAddr(String regAddr) {
        this.regAddr = regAddr;
    }

    public String getLoginUser() {
        return loginUser;
    }

    public void setLoginUser(String loginUser) {
        this.loginUser = loginUser;
    }

    public String getLoginPwd() {
        return loginPwd;
    }

    public void setLoginPwd(String loginPwd) {
        this.loginPwd = loginPwd;
    }

    public String getLoginMail() {
        return loginMail;
    }

    public void setLoginMail(String loginMail) {
        this.loginMail = loginMail;
    }

    public String getLoginPhone() {
        return loginPhone;
    }

    public void setLoginPhone(String loginPhone) {
        this.loginPhone = loginPhone;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getIdTime() {
        return idTime;
    }

    public void setIdTime(String idTime) {
        this.idTime = idTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "DataInfo{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", regAddr='" + regAddr + '\'' +
                ", loginUser='" + loginUser + '\'' +
                ", loginMail='" + loginMail + '\'' +
                ", loginPhone='" + loginPhone + '\'' +
                ", idTime='" + idTime + '\'' +
                ", updateTime='" + updateTime + '\'' +
                '}';
    }
}