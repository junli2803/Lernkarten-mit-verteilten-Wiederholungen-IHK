package dao;

import model.Card;
import util.DB;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CardDAO {
    private final DataSource ds = DB.getDataSource();

    public List<Card> findAll() {
        List<Card> list = new ArrayList<>();
        String sql = "SELECT id, question, answer, created_at FROM card ORDER BY id DESC";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Card k = new Card();
                k.setId(rs.getInt("id"));
                k.setQuestion(rs.getString("question"));
                k.setAnswer(rs.getString("answer"));
                k.setCreatedAt(rs.getDate("created_at").toLocalDate());
                list.add(k);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public Card findById(int id) {
        String sql = "SELECT id, question, answer, created_at FROM card WHERE id=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Card k = new Card();
                    k.setId(rs.getInt("id"));
                    k.setQuestion(rs.getString("question"));
                    k.setAnswer(rs.getString("answer"));
                    k.setCreatedAt(rs.getDate("created_at").toLocalDate());
                    return k;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public int insertCard(Card k) {
        String sql = "INSERT INTO card (question, answer, created_at) VALUES (?,?,?)";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, k.getQuestion());
            ps.setString(2, k.getAnswer());
            ps.setDate(3, Date.valueOf(k.getCreatedAt() == null ? LocalDate.now() : k.getCreatedAt()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }

    public boolean updateCard(Card k) {
        String sql = "UPDATE card SET question=?, answer=?, created_at=? WHERE id=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, k.getQuestion());
            ps.setString(2, k.getAnswer());
            ps.setDate(3, Date.valueOf(k.getCreatedAt()));
            ps.setInt(4, k.getId());
            return ps.executeUpdate() == 1;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public boolean deleteById(int id) {
        String sql = "DELETE FROM card WHERE id=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }
}

