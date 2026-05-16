/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bank_transaction_management.model;

import java.io.Serializable;
import java.util.Date;

public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;
    private String accountNo, type, description;
    private double amount;
    private Date date;

    public Transaction(String accountNo, String type, double amount, Date date, String description) {
        this.accountNo = accountNo;
        this.type = type;
        this.amount = amount;
        this.date = date;
        this.description = description;
    }

    // Getters
    public String getAccountNo() { return accountNo; }
    public String getType() { return type; }
    public double getAmount() { return amount; }
    public Date getDate() { return date; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return date + " | " + type + " | Rs." + amount + " | " + description;
    }
}