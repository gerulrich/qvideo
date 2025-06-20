package quantum.music.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import quantum.music.model.Channel;


@ApplicationScoped
public class ChannelRepository implements ReactivePanacheMongoRepository<Channel> {

    public Uni<Channel> findByCode(String code) {
        return find("code", code).firstResult();
    }
}