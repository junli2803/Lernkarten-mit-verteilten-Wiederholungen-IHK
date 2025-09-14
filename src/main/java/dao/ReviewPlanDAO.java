package dao;

import model.ReviewPlan;
import util.DB;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReviewPlanDAO {
    private final DataSource ds = DB.getDataSource();

    /** New card's first plan: tomorrow, interval_days=1, repeats=0, ease_factor=2.5 */
    public void generateFirstPlan(int cardId) {
        String sql = "INSERT INTO review_plan " +
                "(card_id, planned_on, reviewed_on, rating, interval_days, repeats, ease_factor) " +
                "VALUES (?, CURDATE() + INTERVAL 1 DAY, NULL, NULL, 1, 0, 2.5)";
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, cardId);
            stmt.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    /** Plans due today */
    public List<ReviewPlan> getTodayPlans() {
        List<ReviewPlan> list = new ArrayList<>();
        String sql = "SELECT id, card_id, planned_on, reviewed_on, rating, interval_days, repeats, ease_factor " +
                "FROM review_plan WHERE planned_on <= CURDATE() ORDER BY id";
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                ReviewPlan rp = new ReviewPlan();
                rp.setId(rs.getInt("id"));
                rp.setCardId(rs.getInt("card_id"));

                Date po = rs.getDate("planned_on");
                if (po != null) rp.setPlannedOn(po.toLocalDate());

                Date ro = rs.getDate("reviewed_on");
                if (ro != null) rp.setReviewedOn(ro.toLocalDate());

                Object q = rs.getObject("rating");
                rp.setRating(q == null ? null : ((Number) q).intValue());

                rp.setIntervalDays(rs.getInt("interval_days"));
                rp.setRepeats(rs.getInt("repeats"));
                rp.setEaseFactor(rs.getDouble("ease_factor"));
                list.add(rp);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    /** After a review: set reviewed_on/rating, push planned_on by interval_days */
    public void updatePlanAfterReview(ReviewPlan plan, int rating) {
        String sql = "UPDATE review_plan SET reviewed_on=?, rating=?, interval_days=?, repeats=?, ease_factor=?," +
                " planned_on = CURDATE() + INTERVAL ? DAY WHERE id=?";
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(LocalDate.now()));
            stmt.setInt(2, rating);
            stmt.setInt(3, plan.getIntervalDays());
            stmt.setInt(4, plan.getRepeats());
            stmt.setDouble(5, plan.getEaseFactor());
            stmt.setInt(6, plan.getIntervalDays());
            stmt.setInt(7, plan.getId());
            stmt.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }
}

