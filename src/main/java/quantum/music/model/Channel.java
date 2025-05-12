package quantum.music.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.PanacheMongoEntity;

@MongoEntity(collection="channels")
public class Channel extends PanacheMongoEntity {

    public String code;
    public String url;

    public static Channel findByCode(String code) {
        return find("code", code).firstResult();
    }
}

