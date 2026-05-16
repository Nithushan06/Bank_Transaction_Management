/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bank_transaction_management.model;

public class TransactionRequest implements Comparable<TransactionRequest> {
    private String accountNo;
    private double amount;
    private String type;
    private boolean isVip;

    public TransactionRequest(String accountNo, double amount, String type, boolean isVip) {
        this.accountNo = accountNo;
        this.amount = amount;
        this.type = type;
        this.isVip = isVip;
    }

    @Override
    public int compareTo(TransactionRequest other) {
        // VIP requests have higher priority (negative means this comes first)
        if (this.isVip && !other.isVip) return -1;
        if (!this.isVip && other.isVip) return 1;
        return 0;
    }

    // Getters
    public String getAccountNo() { return accountNo; }
    public double getAmount() { return amount; }
    public String getType() { return type; }
    public boolean isVip() { return isVip; }
}
