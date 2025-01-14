package mytrophy.api.member.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMember is a Querydsl query type for Member
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMember extends EntityPathBase<Member> {

    private static final long serialVersionUID = -1624027913L;

    public static final QMember member = new QMember("member1");

    public final mytrophy.api.common.base.QBaseEntity _super = new mytrophy.api.common.base.QBaseEntity(this);

    public final ListPath<mytrophy.api.article.entity.Article, mytrophy.api.article.entity.QArticle> articles = this.<mytrophy.api.article.entity.Article, mytrophy.api.article.entity.QArticle>createList("articles", mytrophy.api.article.entity.Article.class, mytrophy.api.article.entity.QArticle.class, PathInits.DIRECT2);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath email = createString("email");

    public final BooleanPath firstLogin = createBoolean("firstLogin");

    public final ListPath<mytrophy.api.game.entity.GameReview, mytrophy.api.game.entity.QGameReview> gameReviews = this.<mytrophy.api.game.entity.GameReview, mytrophy.api.game.entity.QGameReview>createList("gameReviews", mytrophy.api.game.entity.GameReview.class, mytrophy.api.game.entity.QGameReview.class, PathInits.DIRECT2);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imagePath = createString("imagePath");

    public final StringPath loginType = createString("loginType");

    public final ListPath<MemberCategory, QMemberCategory> memberCategories = this.<MemberCategory, QMemberCategory>createList("memberCategories", MemberCategory.class, QMemberCategory.class, PathInits.DIRECT2);

    public final StringPath name = createString("name");

    public final StringPath nickname = createString("nickname");

    public final StringPath password = createString("password");

    public final StringPath role = createString("role");

    public final StringPath steamId = createString("steamId");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final StringPath username = createString("username");

    public QMember(String variable) {
        super(Member.class, forVariable(variable));
    }

    public QMember(Path<? extends Member> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMember(PathMetadata metadata) {
        super(Member.class, metadata);
    }

}

