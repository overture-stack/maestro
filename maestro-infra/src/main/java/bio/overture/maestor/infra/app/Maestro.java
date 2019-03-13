package bio.overture.maestor.infra.app;

import bio.overture.maestro.infra.config.RootConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({RootConfiguration.class})
public class Maestro {
//
//    @Autowired
//    FilesRepositoryStore store;
//
//    @Autowired
//    Indexer indexer;

    public static void main(String[] args) {
        SpringApplication.run(Maestro.class, args);
    }

//    @Bean
//    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
//        return args -> {
//            System.out.println("store" + store.getFilesRepository("collab").block());
//            Mono<IndexResult> resultMono = indexer.indexStudy(IndexStudyCommand.builder().studyId("PEME-CA").repositoryCode("collab").build());
//            System.out.println(resultMono.block());
//        };
//    }
}
