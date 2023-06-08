package cart.member.persistence;

import cart.member.application.MemberRepository;
import cart.member.domain.Member;
import cart.member.domain.Point;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MemberDao implements MemberRepository {

    private final JdbcTemplate jdbcTemplate;

    public MemberDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Member getMemberById(Long id) {
        String sql = "SELECT * FROM member WHERE id = ?";
        List<Member> members = jdbcTemplate.query(sql, new Object[]{id}, new MemberRowMapper());
        return members.isEmpty() ? null : members.get(0);
    }

    public Member getMemberByEmail(String email) {
        String sql = "SELECT * FROM member WHERE email = ?";
        List<Member> members = jdbcTemplate.query(sql, new Object[]{email}, new MemberRowMapper());
        return members.isEmpty() ? null : members.get(0);
    }

    public void addMemberWithoutPoint(Member member) {
        String sql = "INSERT INTO member (email, password) VALUES (?, ?)";
        jdbcTemplate.update(sql, member.getEmail(), member.getPassword());
    }

    public void addMemberWithPoint(Member member) {
        String sql = "INSERT INTO member (email, password, point) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, member.getEmail(), member.getPassword(), member.getPoint().getValue());
    }

    public void updateMember(Member member) {
        String sql = "UPDATE member SET email = ?, password = ?, point = ? WHERE id = ?";
        jdbcTemplate.update(sql, member.getEmail(), member.getPassword(), member.getPoint().getValue(), member.getId());
    }

    public void updatePoint(Member member) {
        String sql = "UPDATE member SET point = ? WHERE id = ?";
        jdbcTemplate.update(sql, member.getPoint().getValue(), member.getId());
    }

    public void deleteMember(Long id) {
        String sql = "DELETE FROM member WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    public List<Member> getAllMembers() {
        String sql = "SELECT * from member";
        return jdbcTemplate.query(sql, new MemberRowMapper());
    }

    private static class MemberRowMapper implements RowMapper<Member> {
        @Override
        public Member mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Member(rs.getLong("id"), rs.getString("email"), rs.getString("password"),
                    new Point(rs.getInt("point")));
        }
    }
}

