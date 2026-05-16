/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bank_transaction_management.model;

import java.io.Serializable;

public class Account implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String fullName, dob, age, gender, maritalStatus, address, contact, nic, accountType, accountNo;
    private double balance;
    private boolean active;

    public Account(String fullName, String dob, String age, String gender, String maritalStatus,
                   String address, String contact, String nic, String accountType, String accountNo) {
        this.fullName = fullName;
        this.dob = dob;
        this.age = age;
        this.gender = gender;
        this.maritalStatus = maritalStatus;
        this.address = address;
        this.contact = contact;
        this.nic = nic;
        this.accountType = accountType;
        this.accountNo = accountNo;
        this.balance = 0.0;
        this.active = true;
    }

    // Getters and Setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }
    public String getAge() { return age; }
    public void setAge(String age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getNic() { return nic; }
    public void setNic(String nic) { this.nic = nic; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
