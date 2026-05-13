package Assets;

import java.io.Serializable;

public class Mark implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final double MAX_FIRST  = 30.0;
    public static final double MAX_SECOND = 30.0;
    public static final double MAX_FINAL  = 40.0;
    public static final double PASS_TOTAL = 50.0;

    private double firstAttestation;
    private double secondAttestation;
    private double finalExam;

    public Mark() {}

    public Mark(double first, double second, double finalExam) {
        setFirstAttestation(first);
        setSecondAttestation(second);
        setFinalExam(finalExam);
    }

    public double getFirstAttestation()  { return firstAttestation; }
    public double getSecondAttestation() { return secondAttestation; }
    public double getFinalExam()         { return finalExam; }
    public double getTotal()             { return firstAttestation + secondAttestation + finalExam; }

    public boolean isPassed() { return getTotal() >= PASS_TOTAL; }

    public char getGradeLetter() {
        double t = getTotal();
        if (t >= 90) return 'A';
        if (t >= 80) return 'B';
        if (t >= 70) return 'C';
        if (t >= 60) return 'D';
        return 'F';
    }

    public void setFirstAttestation(double score) {
        if (score < 0 || score > MAX_FIRST) throw new IllegalArgumentException("ATT1 must be 0-30");
        firstAttestation = score;
    }

    public void setSecondAttestation(double score) {
        if (score < 0 || score > MAX_SECOND) throw new IllegalArgumentException("ATT2 must be 0-30");
        secondAttestation = score;
    }

    public void setFinalExam(double score) {
        if (score < 0 || score > MAX_FINAL) throw new IllegalArgumentException("Final must be 0-40");
        finalExam = score;
    }

    @Override
    public String toString() {
        return String.format("ATT1: %.1f | ATT2: %.1f | Final: %.1f | Total: %.1f (%c)",
                firstAttestation, secondAttestation, finalExam, getTotal(), getGradeLetter());
    }
}
