package mytrophy.api.article.repository;

import mytrophy.api.article.entity.Article;
import mytrophy.api.article.entity.Header;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    List<Article> findByHeader(Header header);

    Article findByIdAndHeader(Long id, Header header);

    List<String> deleteByImagePath(String imagePath);
}
