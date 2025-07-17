public class AmortizationEntry {
    private int month;
    private double emi, interest, principal, remaining;

    public AmortizationEntry(int month, double emi, double interest, double principal, double remaining) {
        this.month = month;
        this.emi = emi;
        this.interest = interest;
        this.principal = principal;
        this.remaining = remaining;
    }

    // Getters
    public int getMonth() { return month; }
    public double getEmi() { return emi; }
    public double getInterest() { return interest; }
    public double getPrincipal() { return principal; }
    public double getRemaining() { return remaining; }
}
