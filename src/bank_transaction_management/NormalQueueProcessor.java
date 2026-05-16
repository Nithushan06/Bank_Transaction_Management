package bank_transaction_management;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

/**
 *
 * @author HP
 */
import javax.swing.*;
import javax.swing.Timer;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
public class NormalQueueProcessor extends javax.swing.JFrame {
 private BankData.AccountNode currentAccount;
    private Timer timer;
    /**
     * Creates new form NormalQueueProcessor
     */
    public NormalQueueProcessor() {
        initComponents();
        setLocationRelativeTo(null);
        startDateTimeUpdater();

        // Set monospaced font for queue display
        jTextArea1.setFont(new Font("Monospaced", Font.PLAIN, 12));
        jTextArea1.setEditable(false);

        // Initially disable receiver field
        jTextField4.setEnabled(false);
        jTextField4.setText("");
        jTextField2.setEditable(false); // Account holder name non-editable

        // Enable/disable receiver field based on transaction type
        jComboBox1.addActionListener(e -> {
            String selected = (String) jComboBox1.getSelectedItem();
            if ("Transfer".equals(selected)) {
                jTextField4.setEnabled(true);
            } else {
                jTextField4.setEnabled(false);
                jTextField4.setText("");
            }
        });

        // Refresh queue display every 2 seconds
     new Timer(2000, e -> updateCombinedDisplay()).start();
        updateCombinedDisplay();
    }
    
        private void startDateTimeUpdater() {
        timer = new Timer(1000, e -> updateDateTime());
        timer.start();
        updateDateTime();
    }

    private void updateDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        Date now = new Date();
        // If you have date/time fields, set them here (optional)
    }
    
    private void addTransaction(String accNo, String type, double amount, String relatedAcc, double newBalance) {
        String[] dt = BankData.getCurrentDateTime();
        BankData.TransactionNode trans = new BankData.TransactionNode(dt[0], dt[1], accNo, type, amount, relatedAcc, newBalance);
        BankData.transactionList.add(trans);
        BankData.pushUndoTransaction(trans);
        BankData.saveTransactionsToFile();
    }
    
 private void updateCombinedDisplay() {
        List<RequestWrapper> combined = new ArrayList<>();
        for (BankData.VIPRequest req : BankData.vipQueue) {
            combined.add(new RequestWrapper(req, true, req.timestamp));
        }
        for (BankData.NormalRequest req : BankData.normalQueue) {
            combined.add(new RequestWrapper(req, false, req.timestamp));
        }
        // Sort by arrival order (oldest first)
        combined.sort(Comparator.comparingLong(r -> r.timestamp));

        StringBuilder sb = new StringBuilder();
        sb.append("========================= PENDING TRANSACTIONS (Customer Order) =========================\n");
        sb.append(String.format("%-4s %-15s %-12s %-12s %-12s %-15s %-10s\n",
                "No.", "Account No", "Type", "Transaction", "Amount", "To Account", "Status"));
        sb.append("-----------------------------------------------------------------------------------------\n");

        if (combined.isEmpty()) {
            sb.append("No pending requests.\n");
        } else {
            int i = 1;
            for (RequestWrapper wrapper : combined) {
                String toAcc = "";
                String type = "";
                double amount = 0;
                String accNo = "";
                if (wrapper.isVip) {
                    BankData.VIPRequest req = (BankData.VIPRequest) wrapper.request;
                    accNo = req.accountNumber;
                    type = req.type;
                    amount = req.amount;
                    toAcc = (req.type.equals("Transfer") && req.toAccount != null) ? req.toAccount : "---";
                } else {
                    BankData.NormalRequest req = (BankData.NormalRequest) wrapper.request;
                    accNo = req.accountNumber;
                    type = req.type;
                    amount = req.amount;
                    toAcc = (req.type.equals("Transfer") && req.toAccount != null) ? req.toAccount : "---";
                }
                String transactionType = wrapper.isVip ? "VIP" : "Normal";
                sb.append(String.format("%-4d %-15s %-12s %-12s %-12.2f %-15s %-10s\n",
                        i++, accNo, type, transactionType, amount, toAcc, "Waiting"));
            }
        }
        jTextArea1.setText(sb.toString());
    }

    // Wrapper class to hold request and its timestamp
    private static class RequestWrapper {
        Object request;
        boolean isVip;
        long timestamp;
        RequestWrapper(Object req, boolean vip, long ts) {
            this.request = req;
            this.isVip = vip;
            this.timestamp = ts;
        }
    }

    
    

    private void executeVIP(BankData.VIPRequest req) {
        BankData.AccountNode acc = BankData.accountList.findActiveByNumber(req.accountNumber);
        if (acc == null) {
            JOptionPane.showMessageDialog(this, "Account " + req.accountNumber + " not found or closed.");
            return;
        }
        // Show message that VIP is being processed first
        JOptionPane.showMessageDialog(this,
                "This account number " + req.accountNumber + " is VIP Account, so processing FIRST.\n"
                + "Request: " + req.type + " of Rs." + req.amount,
                "VIP Priority", JOptionPane.INFORMATION_MESSAGE);

        boolean success = false;
        String msg = "";
        if (req.type.equals("Deposit")) {
            double newBal = acc.balance + req.amount;
            success = BankData.accountList.updateBalance(acc.accountNumber, newBal);
            if (success) {
                addTransaction(acc.accountNumber, "Deposit (VIP)", req.amount, null, newBal);
                msg = "VIP Deposit successful. New balance: " + newBal;
            }
        } else if (req.type.equals("Withdraw")) {
            if (acc.balance >= req.amount) {
                double newBal = acc.balance - req.amount;
                success = BankData.accountList.updateBalance(acc.accountNumber, newBal);
                if (success) {
                    addTransaction(acc.accountNumber, "Withdraw (VIP)", req.amount, null, newBal);
                    msg = "VIP Withdrawal successful. New balance: " + newBal;
                }
            } else {
                msg = "Insufficient balance.";
            }
        } else if (req.type.equals("Transfer")) {
            BankData.AccountNode receiver = BankData.accountList.findActiveByNumber(req.toAccount);
            if (receiver == null) {
                msg = "Receiver not found.";
            } else if (acc.balance >= req.amount) {
                double newSender = acc.balance - req.amount;
                double newReceiver = receiver.balance + req.amount;
                if (BankData.accountList.updateBalance(acc.accountNumber, newSender) &&
                    BankData.accountList.updateBalance(receiver.accountNumber, newReceiver)) {
                    success = true;
                    addTransaction(acc.accountNumber, "Transfer Out (VIP)", req.amount, req.toAccount, newSender);
                    addTransaction(receiver.accountNumber, "Transfer In (VIP)", req.amount, acc.accountNumber, newReceiver);
                    msg = "VIP Transfer successful.";
                } else {
                    msg = "Transfer update failed.";
                }
            } else {
                msg = "Insufficient balance.";
            }
        }
        if (success) {
            BankData.saveAccountsToFile();
            BankData.saveTransactionsToFile();
            JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, msg, "Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Execute a normal request (FIFO)
 private void executeNormal(BankData.NormalRequest req) {
        BankData.AccountNode acc = BankData.accountList.findActiveByNumber(req.accountNumber);
        if (acc == null) {
            JOptionPane.showMessageDialog(this, "Account not found.");
            return;
        }
        boolean success = false;
        String msg = "";
        if (req.type.equals("Deposit")) {
            double newBal = acc.balance + req.amount;
            success = BankData.accountList.updateBalance(acc.accountNumber, newBal);
            if (success) {
                addTransaction(acc.accountNumber, "Deposit", req.amount, null, newBal);
                msg = "Deposit successful. New balance: " + newBal;
            }
        } else if (req.type.equals("Withdraw")) {
            if (acc.balance >= req.amount) {
                double newBal = acc.balance - req.amount;
                success = BankData.accountList.updateBalance(acc.accountNumber, newBal);
                if (success) {
                    addTransaction(acc.accountNumber, "Withdraw", req.amount, null, newBal);
                    msg = "Withdrawal successful. New balance: " + newBal;
                }
            } else {
                msg = "Insufficient balance.";
            }
        } else if (req.type.equals("Transfer")) {
            BankData.AccountNode receiver = BankData.accountList.findActiveByNumber(req.toAccount);
            if (receiver == null) {
                msg = "Receiver not found.";
            } else if (acc.balance >= req.amount) {
                double newSender = acc.balance - req.amount;
                double newReceiver = receiver.balance + req.amount;
                if (BankData.accountList.updateBalance(acc.accountNumber, newSender) &&
                    BankData.accountList.updateBalance(receiver.accountNumber, newReceiver)) {
                    success = true;
                    addTransaction(acc.accountNumber, "Transfer Out", req.amount, req.toAccount, newSender);
                    addTransaction(receiver.accountNumber, "Transfer In", req.amount, acc.accountNumber, newReceiver);
                    msg = "Transfer successful.";
                } else {
                    msg = "Transfer update failed.";
                }
            } else {
                msg = "Insufficient balance.";
            }
        }
        if (success) {
            BankData.saveAccountsToFile();
            BankData.saveTransactionsToFile();
            JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, msg, "Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        jTextField3 = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jTextField4 = new javax.swing.JTextField();
        jButton2 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(153, 204, 255));

        jPanel2.setBackground(new java.awt.Color(153, 204, 255));

        jLabel1.setText("Enter the Account Number");

        jButton1.setText("Search");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabel2.setText("Account holder name");

        jLabel3.setText("Transaction Type");

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Deposit", "Withdraw", "Transfer" }));

        jLabel4.setText("Amount");

        jLabel5.setText("Receiver Account Number");

        jButton2.setText("Sumbit");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        jButton3.setText("Back");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButton4.setText("Process Next");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jButton2)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel1)
                                    .addComponent(jLabel2)
                                    .addComponent(jLabel3)
                                    .addComponent(jLabel4)
                                    .addComponent(jLabel5))
                                .addGap(33, 33, 33)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jButton1)
                                    .addComponent(jTextField1)
                                    .addComponent(jTextField2)
                                    .addComponent(jComboBox1, 0, 166, Short.MAX_VALUE)
                                    .addComponent(jTextField3)
                                    .addComponent(jTextField4)))
                            .addComponent(jButton4)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(29, 29, 29)
                        .addComponent(jButton3))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 661, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(46, 46, 46)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(11, 11, 11)
                .addComponent(jButton1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton3)
                .addGap(20, 20, 20))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        String accNo = jTextField1.getText().trim();
        if (accNo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter Account Number", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        BankData.AccountNode acc = BankData.accountList.findByNumber(accNo);
        if (acc == null || acc.closed) {
            JOptionPane.showMessageDialog(this, "Account not found or closed!", "Error", JOptionPane.ERROR_MESSAGE);
            jTextField2.setText("");
            currentAccount = null;
            return;
        }
        currentAccount = acc;
        jTextField2.setText(acc.fullName);
    
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // TODO add your handling code here:
        if (currentAccount == null) {
            JOptionPane.showMessageDialog(this, "Please search for an account first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String type = (String) jComboBox1.getSelectedItem();
        String amountStr = jTextField3.getText().trim();
        if (amountStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter amount.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Enter a valid positive amount.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String toAccount = null;
        if (type.equals("Transfer")) {
            toAccount = jTextField4.getText().trim();
            if (toAccount.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter receiver account number.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            BankData.AccountNode receiver = BankData.accountList.findActiveByNumber(toAccount);
            if (receiver == null) {
                JOptionPane.showMessageDialog(this, "Receiver account not found or closed.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (toAccount.equals(currentAccount.accountNumber)) {
                JOptionPane.showMessageDialog(this, "Cannot transfer to the same account.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Add to appropriate queue
        if ("VIP".equals(currentAccount.accountType)) {
            BankData.VIPRequest request = new BankData.VIPRequest(currentAccount.accountNumber, type, amount, toAccount);
            BankData.vipQueue.add(request);
            JOptionPane.showMessageDialog(this, "VIP request added ", "Queued", JOptionPane.INFORMATION_MESSAGE);
        } else {
            BankData.NormalRequest request = new BankData.NormalRequest(currentAccount.accountNumber, type, amount, toAccount);
            BankData.normalQueue.add(request);
            JOptionPane.showMessageDialog(this, "Normal request added.", "Queued", JOptionPane.INFORMATION_MESSAGE);
        }

        // Clear input fields
        jTextField3.setText("");
        if (type.equals("Transfer")) jTextField4.setText("");
        updateCombinedDisplay();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        // TODO add your handling code here:
                new Transaction_Management().setVisible(true);
        this.dispose();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        // TODO add your handling code here:
    if (!BankData.vipQueue.isEmpty()) {
            BankData.VIPRequest req = BankData.vipQueue.poll();
            executeVIP(req);
        } 
        // Then process normal queue (FIFO)
        else if (!BankData.normalQueue.isEmpty()) {
            BankData.NormalRequest req = BankData.normalQueue.poll();
            executeNormal(req);
        } 
        else {
            JOptionPane.showMessageDialog(this, "No pending requests.", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
        updateCombinedDisplay();
    
    }//GEN-LAST:event_jButton4ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(NormalQueueProcessor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(NormalQueueProcessor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(NormalQueueProcessor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(NormalQueueProcessor.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new NormalQueueProcessor().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    // End of variables declaration//GEN-END:variables
}
