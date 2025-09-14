package model;

public class SM2 {
    /**
     * Compute next review parameters (mutates the given plan):
     * - if rating < 3: reset to first stage (repeats = 0, interval_days = 1)
     * - otherwise follow SM-2 rules
     */
    public static ReviewPlan calculateNext(ReviewPlan plan) {
        int rating = plan.getRating() == null ? 0 : plan.getRating();
        int repeats = plan.getRepeats() == null ? 0 : plan.getRepeats();
        double ef = plan.getEaseFactor() == null ? 2.5 : plan.getEaseFactor();
        int interval;

        if (rating < 3) {
            repeats = 0;
            interval = 1;
        } else {
                 if (repeats == 0)      interval = 1;
                 else if (repeats == 1) interval = 6;
                 else                   interval = (int) Math.round(plan.getIntervalDays() * ef);

                 repeats++;
                 ef = ef + (0.1 - (5 - rating) * (0.08 + (5 - rating) * 0.02));
                 if (ef < 1.3) ef = 1.3;
                }

        plan.setIntervalDays(interval);
        plan.setRepeats(repeats);
        plan.setEaseFactor(ef);
        return plan;
    }
}
