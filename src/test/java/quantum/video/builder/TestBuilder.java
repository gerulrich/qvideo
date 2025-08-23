package quantum.video.builder;

import quantum.video.model.Channel;
import org.bson.types.ObjectId;
import quantum.video.model.DrmConfig;
import quantum.video.model.DrmKey;
import quantum.video.model.Program;

import java.time.Instant;
import java.util.List;

public class TestBuilder {

    public static class ChannelBuilder {
        private ObjectId id;
        private String name;
        private String url;
        private String logo;
        private boolean proxy;
        private DrmConfig drm;

        public ChannelBuilder id(ObjectId id) {
            this.id = id;
            return this;
        }
        public ChannelBuilder name(String name) {
            this.name = name;
            return this;
        }
        public ChannelBuilder url(String url) {
            this.url = url;
            return this;
        }
        public ChannelBuilder logo(String logo) {
            this.logo = logo;
            return this;
        }
        public ChannelBuilder drm(String type, String licenseUrl) {
            this.drm = new DrmConfig(type, licenseUrl, null);
            return this;
        }
        public ChannelBuilder proxy(boolean proxy) {
            this.proxy = proxy;
            return this;
        }
        public ChannelBuilder drm(String type, String kid, String key) {
            this.drm = new DrmConfig(type,null, List.of(new DrmKey(kid, key)));
            return this;
        }

        public Channel build() {
            Channel channel = new Channel();
            channel.id = this.id;
            channel.name = this.name;
            channel.url = this.url;
            channel.logo = this.logo;
            channel.drm = this.drm;
            channel.proxy = proxy;
            return channel;
        }
    }

    public static class ProgramBuilder {
        private ObjectId id;
        private String title;
        private String description;
        private String episode;
        private String url;
        private Instant start;
        private Instant end;

        public ProgramBuilder id(ObjectId id) {
            this.id = id;
            return this;
        }
        public ProgramBuilder title(String title) {
            this.title = title;
            return this;
        }
        public ProgramBuilder description(String description) {
            this.description = description;
            return this;
        }
        public ProgramBuilder episode(String episode) {
            this.episode = episode;
            return this;
        }
        public ProgramBuilder url(String url) {
            this.url = url;
            return this;
        }

        public ProgramBuilder start(Instant start) {
            this.start = start;
            return this;
        }

        public ProgramBuilder end(Instant end) {
            this.end = end;
            return this;
        }

        public Program build() {
            Program program = new Program();
            program.id = this.id;
            program.title = this.title;
            program.description = this.description;
            program.episode = this.episode;
            program.url = this.url;
            program.start = this.start;
            program.end = this.end;
            return program;
        }
    }



}
