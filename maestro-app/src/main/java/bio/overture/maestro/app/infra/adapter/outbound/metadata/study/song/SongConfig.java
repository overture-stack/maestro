package bio.overture.maestro.app.infra.adapter.outbound.metadata.study.song;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
    SongStudyDAO.class
})
public class SongConfig {
}
