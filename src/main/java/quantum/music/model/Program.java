package quantum.music.model;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.types.ObjectId;

@MongoEntity(collection="programs")
public class Program extends PanacheMongoEntity {

    public String url;

    public static Program findById(String id) {
        return findById(new ObjectId(id));
    }
}

