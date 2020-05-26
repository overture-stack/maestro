/*
 *  Copyright (c) 2019. Ontario Institute for Cancer Research
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation, either version 3 of the
 *   License, or (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package bio.overture.maestro.domain.api;

import bio.overture.maestro.domain.entities.indexing.*;
import bio.overture.maestro.domain.entities.metadata.repository.StudyRepository;
import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bio.overture.maestro.domain.api.AnalysisCentricDocumentConverter.buildSpecimen;
import static bio.overture.maestro.domain.api.AnalysisCentricDocumentConverter.groupSpecimensBySample;
import static bio.overture.maestro.domain.api.exception.NotFoundException.checkNotFound;


/**
 * This class holds the structural changes that the indexer applies to prepare the File documents
 * according to the needed final index document structure.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class FileCentricDocumentConverter {

    /**
     * Entry point for this converter, it extracts analysis files according to the FileCentricDocument structure
     * this process is per each analysis and there is no effects on other analyses.
     *
     * @param analysis the analysis coming from the studyId metadata source
     * @param repository the repository is needed to add its information to the final document.
     * @return a list of documents each representing files that an analysis produced/used.
     */
    static List<FileCentricDocument> fromAnalysis(Analysis analysis, StudyRepository repository) {
        return extractFiles(analysis, repository);
    }

    /**
     * iterate over the files list of analysis and build a document for each one
     */
    private static List<FileCentricDocument> extractFiles(Analysis analysis, StudyRepository repository) {
        return analysis.getFiles()
            .stream()
            .filter(FileCentricDocumentConverter::isDataFile)
            .map(f -> buildFileDocument(f, analysis, repository))
            .collect(Collectors.toList());
    }

    /**
     * builds the files document from the analysis files
     *
     * @param file a files as represented from the source in the analysis
     */
    private static FileCentricDocument buildFileDocument(bio.overture.maestro.domain.entities.metadata.study.File file,
                                                          Analysis analysis,
                                                          StudyRepository repository) {
        val id = file.getObjectId();
        val repoFileBuilder = FileCentricDocument.builder()
            .objectId(id)
            .studyId(file.getStudyId())
            .dataType(file.getDataType())
            .fileType(file.getFileType())
            .fileAccess(file.getFileAccess())
            .analysis(FileCentricAnalysis.builder()
                .analysisId(analysis.getAnalysisId())
                .analysisState(analysis.getAnalysisState())
                .analysisType(analysis.getAnalysisType().getName())
                .analysisVersion(analysis.getAnalysisType().getVersion())
                .experiment(analysis.getExperiment())
                .build()
            )
            .file(buildGenomeFileInfo(analysis, file))
            .repositories(List.of(Repository.builder()
                .type(repository.getStorageType().name().toUpperCase())
                .organization(repository.getOrganization())
                .name(repository.getName())
                .code(repository.getCode())
                .country(repository.getCountry())
                .url(repository.getUrl())
                .build()))
            .donors(getDonors(analysis));
        val repoFile = repoFileBuilder.build();
        repoFile.getAnalysis().replaceData(analysis.getData());
        return repoFile;
    }

    private static File buildGenomeFileInfo(Analysis analysis,
                                            bio.overture.maestro.domain.entities.metadata.study.File file) {
        val fileName = file.getFileName();
        val indexFile = getIndexFile(analysis.getFiles(), fileName);
        return File.builder()
            .name(fileName)
            .format(file.getFileType())
            .size(file.getFileSize())
            .md5sum(file.getFileMd5sum())
            .dataType(file.getDataType())
            .indexFile(indexFile)
            .build();
    }

    /**
     * get the index files associated with that files
     * note that this will be removed, when the source explicitly handles associating index files to a files.
     *
     */
    private static IndexFile getIndexFile(List<bio.overture.maestro.domain.entities.metadata.study.File> files,
                                          String fileName) {
        Optional<bio.overture.maestro.domain.entities.metadata.study.File> sf = Optional.empty();
        if (hasExtension(fileName, BAM)) {
            sf = findIndexFile(files, fileName + BAI_EXT);
        } else if (hasExtension(fileName, VCF)) {
            sf = Stream.of(TBI_EXT, IDX_EXT, TCG_EXT)
                .map(suffix -> findIndexFile(files, fileName + suffix))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
        }
        return sf
            .map(FileCentricDocumentConverter::createIndexFile)
            .orElse(null);
    }

    private static IndexFile createIndexFile(bio.overture.maestro.domain.entities.metadata.study.File file) {
        return IndexFile.builder()
            .objectId(file.getObjectId())
            .name(file.getFileName())
            .format(indexFileFormat(file.getFileName()))
            .size(file.getFileSize())
            .md5sum(file.getFileMd5sum())
            .dataType(file.getDataType())
            .build();
    }

    private static Optional<bio.overture.maestro.domain.entities.metadata.study.File>
        findIndexFile(List<bio.overture.maestro.domain.entities.metadata.study.File> files, String name) {

        return files.stream()
            .filter(f -> f.getFileName().equalsIgnoreCase(name))
            .findFirst();
    }

    private static List<FileCentricDonor> getDonors(@NonNull Analysis analysis) {
        val groupedByDonormap = analysis.getSamples()
            .stream()
            .map(sample -> extractDonor(sample))
            .collect(Collectors.groupingBy(FileCentricDonor :: getId, Collectors.toList()));

        return groupedByDonormap.values()
            .stream()
            .collect(Collectors.toList())
            .stream()
            .map(donorList -> mergeDonorBySpecimen(donorList))
            .collect(Collectors.toList());
    }

    /**
     * Converts song metadata sample to FileCentricDonor,
     * each song Sample has one donor and one specimen.
     * @param sample song metadata Sample object
     * @return converted FileCentricDonor object
     */
    private static FileCentricDonor extractDonor(@NonNull bio.overture.maestro.domain.entities.metadata.study.Sample sample){
        val donor = sample.getDonor();
        val specimen = sample.getSpecimen();
        return FileCentricDonor.builder()
            .id(donor.getDonorId())
            .submitterDonorId(donor.getSubmitterDonorId())
            .gender(donor.getGender())
            .specimens(buildSpecimen(specimen, sample))
            .build();
    }

    private static FileCentricDonor mergeDonorBySpecimen(@NonNull List<FileCentricDonor> list){
        checkNotFound(list.size() > 0,
            "Failed to merge FileCentricDonor by specimen: donor list is empty.");

        // Every element in list has the same donor, so just use the first donor
        val anyDonor = list.get(0);

        checkNotFound(anyDonor.getSpecimens() != null && anyDonor.getSpecimens().size() > 0,
            "Failed to merge FileCentricDonor by specimen: donor doesn't have specimen,");

        val specimenList = list.stream()
            .map(fileCentricDonor -> fileCentricDonor.getSpecimens().get(0))
            .collect(Collectors.toList());

        val specimenMap = specimenList.stream()
            .collect(Collectors.groupingBy(
                Specimen :: getId, Collectors.toList()
            ));

        val specimens = specimenMap.values()
            .stream()
            .collect(Collectors.toList())
            .stream()
            .map(speList -> groupSpecimensBySample(speList))
            .collect(Collectors.toList());

        return FileCentricDonor.builder()
            .id(anyDonor.getId())
            .submitterDonorId(anyDonor.getSubmitterDonorId())
            .gender(anyDonor.getGender())
            .specimens(specimens)
            .build();
    }

    private static boolean isDataFile(bio.overture.maestro.domain.entities.metadata.study.File f) {
        val name = f.getFileName();
        return !(isIndexFile(name) || isXMLFile(name));
    }

    private static boolean isXMLFile(String filename) {
        return hasExtension(filename, XML);
    }

    private static boolean isBAIFile(String filename) {
        return hasExtension(filename, BAI);
    }

    private static boolean isTBIFile(String filename) {
        return hasExtension(filename, TBI);
    }

    private static boolean isIDXFile(String filename) {
        return hasExtension(filename, IDX);
    }

    private static boolean isIndexFile(String filename) {
        return isBAIFile(filename) || isIDXFile(filename) || isTBIFile(filename);
    }

    private static String indexFileFormat(String fileName) {
        if (isBAIFile(fileName)) {
            return BAI;
        }
        if (isTBIFile(fileName)) {
            return TBI;
        }
        if (isIDXFile(fileName)) {
            return IDX;
        }
        return null;
    }

    private static boolean hasExtension(String filename, String extension) {
        String[] suffixes = { EMPTY_STRING, GZ, ZIP, B_2_ZIP };
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

    private static final String BAM = "BAM";
    private final static String EMPTY_STRING = "";
    private static final String BAI = "BAI";
    private static final String XML = "XML";
    private static final String TBI = "TBI";
    private static final String IDX = "IDX";
    private static final String GZ = ".gz";
    private static final String ZIP = ".zip";
    private static final String B_2_ZIP = ".b2zip";
    private static final String VCF = "VCF";
    private static final String TCG = "TCG";
    private static final String IDX_EXT = "." + IDX;
    private static final String TCG_EXT = "." + TCG;
    private static final String BAI_EXT = "." + BAI;
    private static final String TBI_EXT = "." + TBI;
}
