package team_10.client.data.models;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Vector;

import team_10.client.data.UserInputField;
import team_10.client.utility.General;

/**
 * Loan Account Type. Add transactions of absolute interest
 * rate or change in principal. Interest compounds daily.
 */
public class Loan extends Account {

    public Loan() {
    }

    /**
     * Overloaded addTransaction method for Loan.
     *
     * @param d            Date of transaction.
     * @param value        Value of transaction, aka change of principal value. (negative or positive)
     * @param interestRate Interest rate at the time of the transaction.
     */
    public void addTransaction(LocalDate d, double value, double interestRate, int recurring) {
        addTransaction(d, value, interestRate, transactions.size(), recurring);
    }

    public void addTransaction(LocalDate d, double value, double interestRate, int transactionID, int recurring) {
        Transaction t = new Transaction(General.round(value, 2), General.round(interestRate, 3), transactionID, recurring, d);
        t.setAccount(this);
        transactions.put(d, t);
    }

    /**
     * Get the value of this Loan at a specified date.
     *
     * @param d LocalDate which to calculate value.
     * @return Value at LocalDate <code>d</code>
     */
    public double getValue(LocalDate d) {
        Vector<LocalDate> transaction_dates = new Vector<LocalDate>(transactions.keySet());

        double total = 0;

        if (transaction_dates.size() <= 0) {
            return 0.0;
        } else {
            for (int i = 0; i < transaction_dates.size(); i++) {

                LocalDate fromDate = transaction_dates.get(i);
                LocalDate toDate;

                if ((i + 1) >= transaction_dates.size() || transaction_dates.get(i + 1).isAfter(d)) {
                    toDate = d;
                    i = Integer.MAX_VALUE - 1; // Stop calculating if date d is before last transaction
                } else {
                    toDate = transaction_dates.get(i + 1);
                }

                //A = P(1 + r/n)^nt -> Daily Compound Interest
                double principle = total + ((Transaction) transactions.get(fromDate)).getValue();
                double rate = ((Transaction) transactions.get(fromDate)).getInterestRate() / 100.0;
                long n = fromDate.until(toDate, ChronoUnit.DAYS); //number of compounding periods (DAYS) per unit t
                double t = n / (double) fromDate.lengthOfYear();

                total = principle * Math.pow(1 + (rate / n), (n * t));
            }
        }

        return General.round(total, 2); // round to nearest cent
    }

    public team_10.client.data.models.Transaction getTransaction(LocalDate date) {
        if (date != null && transactions.containsKey(date)) {
            return transactions.get(date);
        } else {
            return new Transaction();
        }
    }

    /**
     * Loan specific Transaction object.
     */
    private class Transaction extends team_10.client.data.models.Transaction {
        @UserInputField(
                priority = 2,
                name = "Interest Rate",
                inputType = Double.class
        )
        Double interestRate;

        Transaction() { }

        Transaction(double value, double interestRate, int transactionID, int recurring, LocalDate date) {
            this.value = value;
            this.interestRate = interestRate;
            this.transactionID = transactionID;
            this.recurring = recurring;
            this.date = date;
        }

        public double getInterestRate() {
            return this.interestRate;
        }

        public void setInterestRate(double interestRate) {
            this.interestRate = interestRate;
        }
    }
}