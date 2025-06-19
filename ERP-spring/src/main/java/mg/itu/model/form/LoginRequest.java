package mg.itu.model.form;

public class LoginRequest {
    private String usr;
    private String pwd;

    public LoginRequest(String email, String password) {
        this.usr = email;
        this.pwd = password;
    }

    public String getUsr() {
        return usr;
    }

    public void setUsr(String usr) {
        this.usr = usr;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }
    
}