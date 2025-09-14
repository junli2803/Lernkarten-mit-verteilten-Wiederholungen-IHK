package dao;

import model.ReviewStatistic;
import util.DB;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReviewStatisticDAO {
    private final DataSource ds = DB.getDataSource();

    public int insertReviewStatistic(ReviewStatistic s) {
        String sql = "INSERT INTO review_statistic (card_id, reviewed_at, duration_ms, correct, rating, notes) VALUES (?,?,?,?,?,?)";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, s.getCardId());
            LocalDateTime ts = (s.getReviewedAt() == null) ? LocalDateTime.now() : s.getReviewedAt();
            ps.setTimestamp(2, Timestamp.valueOf(ts));
            ps.setInt(3, s.getDurationMs());
            ps.setBoolean(4, s.isCorrect());
            ps.setInt(5, s.getRating());
            ps.setString(6, s.getNotes());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }

    public int countByCard(int cardId) {
        String sql = "SELECT COUNT(*) FROM review_statistic WHERE card_id=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, cardId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    public double avgDurationMsByCard(int cardId) {
        String sql = "SELECT AVG(duration_ms) FROM review_statistic WHERE card_id=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, cardId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0.0;
    }

    public double avgRatingByCard(int cardId) {
        String sql = "SELECT AVG(rating) FROM review_statistic WHERE card_id=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, cardId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0.0;
    }

    public double correctRateByCard(int cardId) {
        String sql = "SELECT AVG(CASE WHEN correct THEN 1 ELSE 0 END) FROM review_statistic WHERE card_id=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, cardId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0.0;
    }

    public List<ReviewStatistic> findByCardAsc(int cardId) {
        String sql = "SELECT id, card_id, reviewed_at, duration_ms, correct, rating, notes " +
                "FROM review_statistic WHERE card_id=? ORDER BY reviewed_at";
        List<ReviewStatistic> list = new ArrayList<>();
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, cardId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("reviewed_at");
                    LocalDateTime reviewedAt = (ts == null) ? null : ts.toLocalDateTime();

                    ReviewStatistic s = new ReviewStatistic(
                            rs.getInt("id"),
                            rs.getInt("card_id"),
                            reviewedAt,
                            rs.getInt("duration_ms"),
                            rs.getBoolean("correct"),
                            rs.getInt("rating"),
                            rs.getString("notes")
                    );
                    list.add(s);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }
}

