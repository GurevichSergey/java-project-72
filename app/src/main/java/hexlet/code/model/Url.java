package hexlet.code.model;

import io.ebean.Model;
import io.ebean.annotation.WhenCreated;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.time.Instant;
import java.util.List;

@Entity
public class Url extends Model {
    @Id
    private long id;
    private String name;
    @WhenCreated
    private Instant createdAt;
    @OneToMany(mappedBy = "url")
    private List<UrlCheck> urlChecks;
    public Url() {
    }
    public Url(String name) {
        this.name = name;
    }
    public final long getId() {
        return this.id;
    }
    public final String getName() {
        return this.name;
    }
    public final Instant getCreatedAt() {
        return this.createdAt;
    }
    public final List<UrlCheck> getUrlList() {
        return this.urlChecks;
    }
    public final UrlCheck getLastUrlCheck() {
        return getUrlList().get(urlChecks.size() - 1);
    }
}

