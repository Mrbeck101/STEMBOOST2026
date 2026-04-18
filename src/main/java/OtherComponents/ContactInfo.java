package OtherComponents;

import java.util.HashMap;

public class ContactInfo {
    private String email;
    private String phone;
    private String address;

    public ContactInfo(String email, String phone, String address) {
        this.email = email;
        this.phone = phone;
        this.address = address;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public HashMap<String, String> getContactInfo() {
        HashMap<String, String> contactInfo =  new HashMap<>();
        contactInfo.put("email", this.email);
        contactInfo.put("phone", this.phone);
        contactInfo.put("address", this.address);

        return contactInfo;
    }
}
