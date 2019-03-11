package bio.overture.maestro.domain.api;

import bio.overture.maestro.domain.entities.*;
import bio.overture.maestro.domain.message.out.metadata.Analysis;
import bio.overture.maestro.domain.message.out.metadata.File;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class FileCentricDocumentConverter {

    static List<FileCentricDocument> fromAnalysis(Analysis analysis, FilesRepository repository) {
        return convertFiles(analysis, repository);
    }

    private static List<FileCentricDocument> convertFiles(Analysis analysis, FilesRepository repository) {
        return analysis.getFile()
            .stream()
            .filter(FileCentricDocumentConverter::isDataFile)
            .map(f -> convert(f, analysis, repository))
            .collect(Collectors.toList());
    }

    private static  FileCentricDocument convert(File file, Analysis analysis, FilesRepository repository) {
        val id = file.getObjectId();
        val repoFile = FileCentricDocument.builder()
                // TODO: ask why song doens;t set file id
                .id(id)
                .objectId(id)
                .study(List.of(file.getStudyId()))
                .access(file.getFileAccess())
                .analysis(FileCentricAnalysis.builder()
                    .analysisId(analysis.getAnalysisId())
                    .analysisState(analysis.getAnalysisState())
                    .analysisType(analysis.getAnalysisType())
                    .study(analysis.getStudy())
                    .experiment(analysis.getExperiment())
                    .build()
                )
                .fileCopies(List.of(getFileCopy(analysis, file,repository)))
                .donors(getDonors(analysis));
        return repoFile.build();
    }

    private static FileCopy getFileCopy(Analysis a, File file, FilesRepository repository) {
        val id = a.getAnalysisId();
        val fileId = file.getObjectId();
        val fileName = file.getFileName();
        val indexFile = getIndexFile(a.getFile(), fileName);
        val metadataPath = getMetadataPath(a);

        return FileCopy.builder()
                .fileName(fileName)
                .fileFormat(file.getFileType())
                .fileSize(file.getFileSize())
                .fileMd5sum(file.getFileMd5sum())
                .repoDataBundleId(id)
                .repoFileId(fileId)
                .repoType(repository.getStorageType())
                .repoOrg(repository.getOrganization())
                .repoName(repository.getName())
                .repoCode(repository.getCode())
                .repoCountry(repository.getCountry())
                .repoBaseUrl(repository.getBaseUrl())
                .repoDataPath(repository.getDataPath() + "/" + fileId)
                .repoMetadataPath(repository.getMetadataPath() + "/" + metadataPath)
                .indexFile(indexFile)
                .build();
    }

    private static String getMetadataPath(Analysis analysis) {
        val xmlFile = analysis.getFile().stream()
                .filter(f -> isXMLFile(f.getFileName()))
                .findFirst()
                .orElse(null);

        return xmlFile == null ? "" : xmlFile.getObjectId();
    }

    private static IndexFile getIndexFile(List<File> files, String fileName) {
        Optional<File> sf = Optional.empty();
        if (hasExtension(fileName, "BAM")) {
            sf = getSongIndexFile(files, fileName + ".BAI");
        } else if (hasExtension(fileName, "VCF")) {
            sf = Stream.of(".TBI", ".IDX", ".TCG")
                    .map(suffix -> getSongIndexFile(files, fileName + suffix))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst();
        }
        return sf
                .map(FileCentricDocumentConverter::createIndexFile)
                .orElse(IndexFile.builder().build());
    }

    private static IndexFile createIndexFile(File file) {
        return IndexFile.builder()
            .id(file.getObjectId())
            .objectId(file.getObjectId())
            .fileName(file.getFileName())
            .fileFormat(indexFileFormat(file.getFileName()))
            .fileSize(file.getFileSize())
            .fileMd5sum(file.getFileMd5sum())
            .build();
    }

    private static Optional<File> getSongIndexFile(List<File> files, String name) {
        return files.stream()
            .filter(f -> f.getFileName().equalsIgnoreCase(name))
            .findFirst();
    }

    private static List<FileCentricDonor> getDonors(Analysis analysis) {
        return List.of(getDonor(analysis));
    }

    private static FileCentricDonor getDonor(Analysis analysis) {
        val studyId = analysis.getStudy();
        val sample = analysis.getSample().stream().findFirst().orElseThrow(() -> new RuntimeException("incorrect structure of song data, sample is empty"));
        val donor = sample.getDonor();
        val specimen = sample.getSpecimen();
        return FileCentricDonor.builder()
            .study(studyId)
            .primarySite("<ADD TO SONG>")
            .donorId(donor.getDonorId())
            .specimenId(List.of(specimen.getSpecimenId()))
            .specimenType(List.of(specimen.getSpecimenType()))
            .sampleId(List.of(sample.getSampleId()))
            .donorSubmitterId(donor.getDonorSubmitterId())
            .specimenSubmitterId(List.of(specimen.getSpecimenSubmitterId()))
            .sampleSubmitterId(List.of(sample.getSampleSubmitterId()))
            .build();
    }

    private static boolean isDataFile(File f) {
        val name = f.getFileName();
        return !(isIndexFile(name) || isXMLFile(name));
    }

    private static boolean isXMLFile(String filename) {
        return hasExtension(filename, "XML");
    }

    private static boolean isBAIFile(String filename) {
        return hasExtension(filename, "BAI");
    }

    private static boolean isTBIFile(String filename) {
        return hasExtension(filename, "TBI");
    }

    private static boolean isIDXFile(String filename) {
        return hasExtension(filename, "IDX");
    }

    private static boolean isIndexFile(String filename) {
        if (isBAIFile(filename) || isIDXFile(filename) || isTBIFile(filename)) {
            return true;
        }
        return false;
    }

    private static String indexFileFormat(String fileName) {
        if (isBAIFile(fileName)) {
            return "BAI";
        }
        if (isTBIFile(fileName)) {
            return "TBI";
        }
        if (isIDXFile(fileName)) {
            return "IDX";
        }
        return null;
    }

    private static boolean hasExtension(String filename, String extension) {
        String[] suffixes = { "", ".gz", ".zip", ".b2zip" };
        val f = filename.toLowerCase();
        val ext = extension.toLowerCase();
        for (val s : suffixes) {
            if (f.endsWith(ext + s)) {
                return true;
            }
            if (f.endsWith(s + ext)) {
                return true;
            }
        }
        return false;
    }
}
