package bank_transaction_management;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.io.*;
import java.util.LinkedList;
import java.util.Queue;

public class BankData {

    // ---------- Stack implementation (for Undo) ----------
    public static class Stack<T> {
        private static class Node<U> {
            U data;
            Node<U> next;
            Node(U data) { this.data = data; }
        }
        private Node<T> top;
        private int size;

        public void push(T item) {
            Node<T> newNode = new Node<>(item);
            newNode.next = top;
            top = newNode;
            size++;
        }

        public T pop() {
            if (isEmpty()) return null;
            T data = top.data;
            top = top.next;
            size--;
            return data;
        }

        public T peek() {
            return isEmpty() ? null : top.data;
        }

        public boolean isEmpty() { return top == null; }
        public int size() { return size; }
    }

    // ---------- Account Node ----------
    public static class AccountNode {
        public String fullName, dob, gender, maritalStatus, address, contact, nic, accountType, accountNumber;
        public int age;
        public double balance;
        public boolean closed;
        public String closeReason;
        public AccountNode next;

        public AccountNode(String fullName, String dob, int age, String gender, String maritalStatus,
                           String address, String contact, String nic, String accountType, String accountNumber, double balance) {
            this.fullName = fullName; this.dob = dob; this.age = age; this.gender = gender;
            this.maritalStatus = maritalStatus; this.address = address; this.contact = contact;
            this.nic = nic; this.accountType = accountType; this.accountNumber = accountNumber;
            this.balance = balance; this.closed = false; this.closeReason = null; this.next = null;
        }

        public AccountNode(String fullName, String dob, int age, String gender,
                           String address, String contact, String accountType, String accountNumber, double balance) {
            this(fullName, dob, age, gender, null, address, contact, null, accountType, accountNumber, balance);
        }
    }

    // ---------- Transaction Node ----------
    public static class TransactionNode {
        public String date, time, accountNumber, type, relatedAccount;
        public double amount, balanceAfter;
        public TransactionNode next;
        public TransactionNode(String date, String time, String accountNumber, String type,
                               double amount, String relatedAccount, double balanceAfter) {
            this.date = date; this.time = time; this.accountNumber = accountNumber;
            this.type = type; this.amount = amount; this.relatedAccount = relatedAccount;
            this.balanceAfter = balanceAfter; this.next = null;
        }
    }

    // ---------- Account List (Linked List) ----------
    public static class AccountList {
        private AccountNode head;
        private int size;
        public AccountList() { head = null; size = 0; }
        public void add(AccountNode node) {
            if (head == null) head = node;
            else { AccountNode temp = head; while (temp.next != null) temp = temp.next; temp.next = node; }
            size++;
        }
        public AccountNode findByNumber(String accNo) {
            AccountNode temp = head;
            while (temp != null) { if (temp.accountNumber.equals(accNo)) return temp; temp = temp.next; }
            return null;
        }
        public AccountNode findActiveByNumber(String accNo) {
            AccountNode temp = head;
            while (temp != null) { if (temp.accountNumber.equals(accNo) && !temp.closed) return temp; temp = temp.next; }
            return null;
        }
        public boolean updateBalance(String accNo, double newBalance) {
            AccountNode acc = findByNumber(accNo);
            if (acc != null && !acc.closed) { acc.balance = newBalance; return true; }
            return false;
        }
        public boolean closeAccount(String accNo, String reason) {
            AccountNode acc = findByNumber(accNo);
            if (acc != null && !acc.closed) { acc.closed = true; acc.closeReason = reason; return true; }
            return false;
        }
        public AccountNode getHead() { return head; }
        public int getSize() { return size; }
    }

    // ---------- Transaction History List ----------
    public static class TransactionList {
        public TransactionNode head;
        private int size;
        public TransactionList() { head = null; size = 0; }
        public void add(TransactionNode node) {
            if (head == null) head = node;
            else { TransactionNode temp = head; while (temp.next != null) temp = temp.next; temp.next = node; }
            size++;
        }
        public String getTransactionsForAccount(String accNo) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-12s %-8s %-14s %-10s %-15s %-12s\n",
                    "Date", "Time", "Type", "Amount", "Related Account", "Balance After"));
            sb.append("--------------------------------------------------------------------------\n");
            TransactionNode temp = head;
            boolean found = false;
            while (temp != null) {
                if (temp.accountNumber.equals(accNo)) {
                    sb.append(String.format("%-12s %-8s %-14s %-10.2f %-15s %-12.2f\n",
                            temp.date, temp.time, temp.type, temp.amount,
                            (temp.relatedAccount == null ? "---" : temp.relatedAccount), temp.balanceAfter));
                    found = true;
                }
                temp = temp.next;
            }
            if (!found) sb.append("No transactions found.\n");
            return sb.toString();
        }
        public int getSize() { return size; }
    }

    // ---------- Undo Stacks (per account, in-memory only) ----------
    private static Map<String, Stack<TransactionNode>> undoStacks = new HashMap<>();
    public static Stack<TransactionNode> getUndoStack(String accountNumber) {
        return undoStacks.computeIfAbsent(accountNumber, k -> new Stack<>());
    }
    public static void pushUndoTransaction(TransactionNode trans) {
        getUndoStack(trans.accountNumber).push(trans);
    }
    public static TransactionNode popUndoTransaction(String accountNumber) {
        Stack<TransactionNode> stack = undoStacks.get(accountNumber);
        return (stack != null && !stack.isEmpty()) ? stack.pop() : null;
    }

    // ---------- VIP and Normal Requests ----------
    public static class VIPRequest {
        public String accountNumber;
        public String type;
        public double amount;
        public String toAccount;
        public long timestamp;
        public VIPRequest(String accountNumber, String type, double amount, String toAccount) {
            this.accountNumber = accountNumber;
            this.type = type;
            this.amount = amount;
            this.toAccount = toAccount;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static class NormalRequest {
        public String accountNumber;
        public String type;
        public double amount;
        public String toAccount;
        public long timestamp;
        public NormalRequest(String accountNumber, String type, double amount, String toAccount) {
            this.accountNumber = accountNumber;
            this.type = type;
            this.amount = amount;
            this.toAccount = toAccount;
            this.timestamp = System.currentTimeMillis();
        }
    }

    // Queues
    public static final Queue<NormalRequest> normalQueue = new LinkedList<>();
    public static final PriorityQueue<VIPRequest> vipQueue = new PriorityQueue<>(
        Comparator.comparingInt((VIPRequest r) -> {
            AccountNode acc = BankData.accountList.findByNumber(r.accountNumber);
            return (acc != null && "VIP".equals(acc.accountType)) ? 0 : 1;
        }).thenComparingLong(r -> r.timestamp)
    );

    // Global instances
    public static final AccountList accountList = new AccountList();
    public static final TransactionList transactionList = new TransactionList();

    private static int globalCounter = 0;
    public static int getNextSuffix() { return ++globalCounter; }

    // Helper date/time
    public static String[] getCurrentDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        Date now = new Date();
        return new String[]{dateFormat.format(now), timeFormat.format(now)};
    }

    // ---------- FILE PERSISTENCE ----------
    private static final String ACCOUNTS_FILE = "accounts.txt";
    private static final String COUNTER_FILE = "counter.txt";
    private static final String TRANSACTIONS_FILE = "transactions.txt";   // <-- NEW

    // Save accounts
    public static void saveAccountsToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ACCOUNTS_FILE))) {
            AccountNode temp = accountList.getHead();
            while (temp != null) {
                writer.write(String.format("%s,%s,%d,%s,%s,%s,%s,%s,%s,%s,%.2f,%b,%s",
                    temp.fullName, temp.dob, temp.age, temp.gender,
                    (temp.maritalStatus == null ? "" : temp.maritalStatus),
                    temp.address, temp.contact, (temp.nic == null ? "" : temp.nic),
                    temp.accountType, temp.accountNumber, temp.balance,
                    temp.closed, (temp.closeReason == null ? "" : temp.closeReason)));
                writer.newLine();
                temp = temp.next;
            }
            saveCounterToFile();
        } catch (IOException e) { System.err.println("Error saving accounts: " + e.getMessage()); }
    }

    // Load accounts
    public static void loadAccountsFromFile() {
        File file = new File(ACCOUNTS_FILE);
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length >= 13) {
                    String fullName = parts[0];
                    String dob = parts[1];
                    int age = Integer.parseInt(parts[2]);
                    String gender = parts[3];
                    String maritalStatus = parts[4].isEmpty() ? null : parts[4];
                    String address = parts[5];
                    String contact = parts[6];
                    String nic = parts[7].isEmpty() ? null : parts[7];
                    String accountType = parts[8];
                    String accountNumber = parts[9];
                    double balance = Double.parseDouble(parts[10]);
                    boolean closed = Boolean.parseBoolean(parts[11]);
                    String closeReason = parts[12].isEmpty() ? null : parts[12];

                    AccountNode acc = new AccountNode(fullName, dob, age, gender, maritalStatus,
                            address, contact, nic, accountType, accountNumber, balance);
                    acc.closed = closed;
                    acc.closeReason = closeReason;
                    accountList.add(acc);

                    if (accountNumber.length() >= 4) {
                        try {
                            int suffix = Integer.parseInt(accountNumber.substring(accountNumber.length() - 4));
                            if (suffix >= globalCounter) globalCounter = suffix + 1;
                        } catch (NumberFormatException e) {}
                    }
                }
            }
        } catch (IOException e) { System.err.println("Error loading accounts: " + e.getMessage()); }
    }

    // Counter save/load
    public static void saveCounterToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(COUNTER_FILE))) {
            writer.write(String.valueOf(globalCounter));
        } catch (IOException e) { System.err.println("Error saving counter: " + e.getMessage()); }
    }
    public static void loadCounterFromFile() {
        File file = new File(COUNTER_FILE);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line = reader.readLine();
                if (line != null) globalCounter = Integer.parseInt(line);
            } catch (IOException | NumberFormatException e) { System.err.println("Error loading counter: " + e.getMessage()); }
        }
    }

    // ---------- NEW: Transaction file save/load ----------
    public static void saveTransactionsToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TRANSACTIONS_FILE))) {
            TransactionNode temp = transactionList.head;
            while (temp != null) {
                writer.write(String.format("%s,%s,%s,%s,%.2f,%s,%.2f",
                        temp.date, temp.time, temp.accountNumber, temp.type,
                        temp.amount,
                        (temp.relatedAccount == null ? "" : temp.relatedAccount),
                        temp.balanceAfter));
                writer.newLine();
                temp = temp.next;
            }
        } catch (IOException e) {
            System.err.println("Error saving transactions: " + e.getMessage());
        }
    }

    public static void loadTransactionsFromFile() {
        File file = new File(TRANSACTIONS_FILE);
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length >= 7) {
                    String date = parts[0];
                    String time = parts[1];
                    String accNo = parts[2];
                    String type = parts[3];
                    double amount = Double.parseDouble(parts[4]);
                    String related = parts[5].isEmpty() ? null : parts[5];
                    double balanceAfter = Double.parseDouble(parts[6]);

                    TransactionNode trans = new TransactionNode(date, time, accNo, type, amount, related, balanceAfter);
                    transactionList.add(trans);
                    // Note: Undo stacks are NOT restored from file (session only)
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading transactions: " + e.getMessage());
        }
    }
}