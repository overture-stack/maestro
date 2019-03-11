package bio.overture.maestor.infra.app;

import bio.overture.maestro.domain.api.Indexer;
import bio.overture.maestro.domain.message.in.IndexResult;
import bio.overture.maestro.domain.message.in.IndexStudyCommand;
import bio.overture.maestro.domain.port.outbound.FilesRepositoryStore;
import bio.overture.maestro.infra.config.RootConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Mono;

@SpringBootApplication
@Import({RootConfiguration.class})
public class Maestro {

    @Autowired
    FilesRepositoryStore store;

    @Autowired
    Indexer indexer;

    public static void main(String[] args) {
        SpringApplication.run(Maestro.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            System.out.println("store" + store.getFilesRepository("collab").block());
            Mono<IndexResult> resultMono = indexer.indexStudy(IndexStudyCommand.builder().studyId("PEME-CA").repositoryCode("collab").build());
            System.out.println(resultMono.block());
        };
    }
}
