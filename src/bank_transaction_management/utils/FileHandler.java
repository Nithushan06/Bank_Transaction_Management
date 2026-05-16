/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bank_transaction_management.utils;

import bank_transaction_management.model.Account;
import bank_transaction_management.model.Transaction;
import java.io.*;
import java.util.*;

public class FileHandler {

    // ---------- ACCOUNTS (LinkedList) ----------
    public static void saveAccounts(LinkedList<Account> accounts) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("accounts.dat"))) {
            oos.writeObject(accounts);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @SuppressWarnings("unchecked")
    public static LinkedList<Account> loadAccounts() {
        File f = new File("accounts.dat");
        if (!f.exists()) return new LinkedList<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            return (LinkedList<Account>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return new LinkedList<>();
        }
    }

    // ---------- TRANSACTIONS (Stack for Undo) ----------
    // We store all transactions in a single file; for undo we need last transaction per account.
    // Simpler: maintain a global stack of all transactions (for demo). 
    // For real banking, separate per account. Here we use a static stack in Transaction_Management.
    public static void saveTransaction(Transaction t) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("transactions.dat", true))) {
            oos.writeObject(t);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @SuppressWarnings("unchecked")
    public static Stack<Transaction> loadAllTransactions() {
        Stack<Transaction> stack = new Stack<>();
        File f = new File("transactions.dat");
        if (!f.exists()) return stack;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            while (true) {
                Transaction t = (Transaction) ois.readObject();
                stack.push(t);
            }
        } catch (EOFException e) { /* end of file */ }
        catch (IOException | ClassNotFoundException e) { e.printStackTrace(); }
        return stack;
    }

    // ---------- USERS (HashMap) ----------
    public static void saveUsers(HashMap<String, String> users) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("users.dat"))) {
            oos.writeObject(users);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @SuppressWarnings("unchecked")
    public static HashMap<String, String> loadUsers() {
        File f = new File("users.dat");
        if (!f.exists()) return new HashMap<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            return (HashMap<String, String>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return new HashMap<>();
        }
    }
}
