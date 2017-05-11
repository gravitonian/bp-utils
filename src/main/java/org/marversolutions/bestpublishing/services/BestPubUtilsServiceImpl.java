/*
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package org.marversolutions.bestpublishing.services;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.util.ISO8601DateFormat;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;

/*
 * Implementation of the Best Publishing Generic Utility Service
 * with methods that does not fit into the Alfresco Repo or Workflow util services.
 *
 * @author martin.bergljung@marversolutions.org
 * @version 1.0
 */
public class BestPubUtilsServiceImpl implements BestPubUtilsService {
        private static final Logger LOG = LoggerFactory.getLogger(BestPubUtilsServiceImpl.class);

        /**
         * The name of the directory where ZIPs that failed processing are moved
         */
        private static final String FAILED_PROCESSING_DIR_NAME = "failedProcessing";

        /**
         * Best Publishing Services
         */
        private AlfrescoRepoUtilsService alfrescoRepoUtilsService;

        /**
         * Alfresco Services
         */
        private NodeService nodeService;
        private ContentService contentService;
        private DictionaryService dictionaryService;
        private NamespaceService namespaceService;

        /**
         * Spring Dependency Injection
         */

        public void setAlfrescoRepoUtilsService(final AlfrescoRepoUtilsService repoUtils) {
            this.alfrescoRepoUtilsService = repoUtils;
        }
        public void setNodeService(final NodeService nodeService) {
            this.nodeService = nodeService;
        }
        public void setContentService(final ContentService contentService) {
            this.contentService = contentService;
        }
        public void setNamespaceService(NamespaceService namespaceService) {
            this.namespaceService = namespaceService;
        }
        public NodeService getNodeService() {
            return nodeService;
        }
        public void setDictionaryService(final DictionaryService dictionaryService) {
            this.dictionaryService = dictionaryService;
        }

        /**
         * Interface Implementation
         */

        @Override
        public File[] findFilesUsingExtension(final File folderToSearch, final String extension) {
            return folderToSearch.listFiles(new FileFilter() {

                @Override
                public boolean accept(final File pathname) {
                    String fileExtension = FilenameUtils.getExtension(pathname.getPath());
                    return fileExtension.equalsIgnoreCase(extension);
                }
            });
        }

        @Override
        public String formatDate(final String pattern, final Date date) {
            if (date == null) {
                return null;
            }

            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            return sdf.format(date);
        }

        @Override
        public NodeRef getChapterDestinationFolder(final String fileName, final NodeRef destIsbnFolderNodeRef) {
            // Filename naming convention: [ISBN]-chapter[chapter number].[pdf|xml]
            // Such as 9780203807217-chapter8.pdf
            String chapterFolderName = fileName.substring(fileName.indexOf('-') + 1, fileName.lastIndexOf('.'));
            if (StringUtils.isBlank(chapterFolderName)) {
                LOG.error("Could not extract chapter folder name from filename [{}]", fileName);
                return null;
            }

            chapterFolderName = chapterFolderName.toLowerCase();
            NodeRef chapterFolderNodeRef = alfrescoRepoUtilsService.getChildByName(destIsbnFolderNodeRef, chapterFolderName);

            return chapterFolderNodeRef;
        }

        @Override
        public Date checkModifiedDates(final NodeRef nodeRef, final Date publishedDate) {
            if (publishedDate == null) {
                throw new IllegalArgumentException("Published date cannot be null");
            }

            String nodeRefXPath = nodeService.getPath(nodeRef).toPrefixString(namespaceService);
            String publishedDateString = ISO8601DateFormat.format(publishedDate);
            String searchQuery = "PATH:\"" + nodeRefXPath + "//*\" AND @cm\\:modified:[" + publishedDateString+ " TO NOW]";
            Date latestModificationDate = null;

            List<NodeRef> modifiedNodeRefs = alfrescoRepoUtilsService.search(searchQuery);
            for (NodeRef modifiedIsbnChildNode : modifiedNodeRefs) {
                Date modifiedDate = (Date)nodeService.getProperty(modifiedIsbnChildNode, ContentModel.PROP_MODIFIED);
                if (modifiedDate.after(publishedDate)) {
                    if (latestModificationDate == null) {
                        latestModificationDate = modifiedDate;
                    } else {
                        if (modifiedDate.after(latestModificationDate)) {
                            latestModificationDate = modifiedDate;
                        }
                    }
                }
            }

            return latestModificationDate;
        }

        @Override
        public Date checkModifiedDates(final NodeRef nodeRef) {
           /* Date publishedDate = (Date)nodeService.getProperty(nodeRef,
                    BoppContentModel.WebPublishingInfoAspect.Prop.WEB_PUBLISHED_DATE);
            if (publishedDate != null) {
                return checkModifiedDates(nodeRef, publishedDate);
            } else {
                return null;
            }*/
           return null;
        }

        @Override
        public boolean isISBN(final String isbn) {
            Matcher isbnMatcher = BoppConstants.ISBN_REGEXP_PATTERN.matcher(isbn);
            if (isbnMatcher.matches() == false) {
                return false;
            }

            return true;
        }

        @Override
        public String getISBNfromFilename(final String filename) {
            String isbn = filename.trim().substring(0, BoppConstants.ISBN_NUMBER_LENGTH);
            if (!isISBN(isbn)) {
                LOG.error("Could not extract ISBN number from [{}]", filename);
                return null;
            }

            return isbn;
        }

        @Override
        public Map<ChapterFolderInfo, NodeRef> getSortedChapterFolders(NodeRef isbnFolderNodeRef) {
            Set<QName> childNodeTypes = new HashSet<>();
            childNodeTypes.add(BoppContentModel.ChapterFolderType.QNAME);
            List<ChildAssociationRef> chapterFolderChildAssociations =
                    nodeService.getChildAssocs(isbnFolderNodeRef, childNodeTypes);

            Map<ChapterFolderInfo, NodeRef> existingChapterFolderInfo2NodeRefMap = new TreeMap<>();

            for (ChildAssociationRef chapterFolderChildAssoc : chapterFolderChildAssociations) {
                NodeRef chapterFolderNodeRef = chapterFolderChildAssoc.getChildRef();
                String chapterFolderName = (String) nodeService.getProperty(chapterFolderNodeRef, ContentModel.PROP_NAME);
                String chapterTitle = (String) nodeService.getProperty(
                        chapterFolderNodeRef, BoppContentModel.ChapterMetadataAspect.Prop.CHAPTER_TITLE);
                Serializable chapterNr = nodeService.getProperty(
                        chapterFolderNodeRef, BoppContentModel.ChapterMetadataAspect.Prop.CHAPTER_NUMBER);
                existingChapterFolderInfo2NodeRefMap.put(
                        new ChapterFolderInfo(chapterFolderName, chapterTitle, chapterNr), chapterFolderNodeRef);
            }

            return existingChapterFolderInfo2NodeRefMap;
        }

        @Override
        public void applySurveyMetadata(NodeRef fileOrFolderNodeRef, Survey survey) {
            SurveyFields surveyFields = survey.getBasicSurveyFields();

            // check if isbn is in the RHO list. If it's not we have to prevent the metadata extraction for non RHO isbns
            String eISBN = surveyFields.getBook().geteISBN();
            if (!isRhoISBN(eISBN)) {
                return;
            }

            if (!nodeService.hasAspect(fileOrFolderNodeRef,  BoppContentModel.BookMetadataAspect.QNAME)) {
                // Most likely only for CSV
                Map<QName, Serializable> bookMetadataAspectProps = new HashMap<QName, Serializable>();
                bookMetadataAspectProps.put(BoppContentModel.BookMetadataAspect.Prop.ISBN, surveyFields.getBook().geteISBN());
                bookMetadataAspectProps.put(BoppContentModel.BookMetadataAspect.Prop.BOOK_TITLE, surveyFields.getBook().getBookTitle());
                bookMetadataAspectProps.put(BoppContentModel.BookMetadataAspect.Prop.BOOK_SUBJECT_NAME, ""); // Not available at this point for a CSV
                bookMetadataAspectProps.put(BoppContentModel.BookMetadataAspect.Prop.BOOK_NUMBER_OF_CHAPTERS, "999");  // not relevant for CSV
                bookMetadataAspectProps.put(BoppContentModel.BookMetadataAspect.Prop.BOOK_METADATA_STATUS, BoppContentModel.BookMetadataStatus.MISSING.toString());
                nodeService.addAspect(fileOrFolderNodeRef, BoppContentModel.BookMetadataAspect.QNAME, bookMetadataAspectProps);

            }

            if (!nodeService.hasAspect(fileOrFolderNodeRef, BoppContentModel.ChapterMetadataAspect.QNAME)) {
                // Most likely only for CSV
                Map<QName, Serializable> chapterMetadataAspectProps = new HashMap<QName, Serializable>();
                chapterMetadataAspectProps.put(BoppContentModel.ChapterMetadataAspect.Prop.CHAPTER_NUMBER, "999"); // not relevant for CSV
                chapterMetadataAspectProps.put(BoppContentModel.ChapterMetadataAspect.Prop.CHAPTER_TITLE, surveyFields.getBook().getChapterTitle());
                chapterMetadataAspectProps.put(BoppContentModel.ChapterMetadataAspect.Prop.CHAPTER_METADATA_STATUS, BoppContentModel.ChapterMetadataStatus.COMPLETED.toString());
                nodeService.addAspect(fileOrFolderNodeRef, BoppContentModel.ChapterMetadataAspect.QNAME, chapterMetadataAspectProps);
            }

            Map<QName, Serializable> existingProperties = nodeService.getProperties(fileOrFolderNodeRef);
            Map<QName, Serializable> chapterMetadataAspectProps = new HashMap<>();
            chapterMetadataAspectProps.put(BoppContentModel.ChapterMetadataAspect.Prop.RESPONDENT_ID, surveyFields.getRespondentID());
            chapterMetadataAspectProps.put(BoppContentModel.ChapterMetadataAspect.Prop.AUTHOR_FIRST_NAME, surveyFields.getAuthor().getProvidedFirstName());
            chapterMetadataAspectProps.put(BoppContentModel.ChapterMetadataAspect.Prop.AUTHOR_MIDDLE_NAME, surveyFields.getAuthor().getProvidedMiddleNames());
            chapterMetadataAspectProps.put(BoppContentModel.ChapterMetadataAspect.Prop.AUTHOR_LAST_NAME, surveyFields.getAuthor().getProvidedLastName());
            chapterMetadataAspectProps.put(BoppContentModel.ChapterMetadataAspect.Prop.ACADEMIC_AUDIENCE, survey.getAudiences().contains("Academic"));
            chapterMetadataAspectProps.put(BoppContentModel.ChapterMetadataAspect.Prop.PROFESSIONAL_AUDIENCE, survey.getAudiences().contains("Professional"));
            chapterMetadataAspectProps.put(BoppContentModel.ChapterMetadataAspect.Prop.CHAPTER_METADATA_STATUS, BoppContentModel.ChapterMetadataStatus.COMPLETED.toString());
            Map<QName, Serializable> allProperties = new HashMap<>();
            allProperties.putAll(existingProperties);
            allProperties.putAll(chapterMetadataAspectProps);
            nodeService.setProperties(fileOrFolderNodeRef, allProperties);

            // Setup the Subject Metadata aspect on the chapter
            Map<QName, Serializable> metadataProps = new HashMap<>();
            SurveyFreeTextFields surveyFreeTextFields = survey.getFreeTextSurveyFields();
            metadataProps.put(BoppContentModel.boppc("notableFigure"), surveyFreeTextFields.getNotablyFigure(0));
            metadataProps.put(BoppContentModel.boppc("notableFigure2"), surveyFreeTextFields.getNotablyFigure(1));
            metadataProps.put(BoppContentModel.boppc("notableFigure3"), surveyFreeTextFields.getNotablyFigure(2));
            metadataProps.put(BoppContentModel.boppc("theorist"), surveyFreeTextFields.getTheoristFigure(0));
            metadataProps.put(BoppContentModel.boppc("theorist2"), surveyFreeTextFields.getTheoristFigure(1));
            metadataProps.put(BoppContentModel.boppc("theorist3"), surveyFreeTextFields.getTheoristFigure(2));
            metadataProps.put(BoppContentModel.boppc("associatedWork"), surveyFreeTextFields.getAssociatedWork(0));
            metadataProps.put(BoppContentModel.boppc("associatedWork2"), surveyFreeTextFields.getAssociatedWork(1));
            metadataProps.put(BoppContentModel.boppc("associatedWork3"), surveyFreeTextFields.getAssociatedWork(2));
            metadataProps.put(BoppContentModel.boppc("caseStudy"), surveyFreeTextFields.getCaseStudy(0));
            metadataProps.put(BoppContentModel.boppc("caseStudy2"), surveyFreeTextFields.getCaseStudy(1));
            metadataProps.put(BoppContentModel.boppc("caseStudy3"), surveyFreeTextFields.getCaseStudy(2));
            metadataProps.put(BoppContentModel.boppc("methodology"), surveyFreeTextFields.getTheory(0));
            metadataProps.put(BoppContentModel.boppc("methodology2"), surveyFreeTextFields.getTheory(1));
            metadataProps.put(BoppContentModel.boppc("methodology3"), surveyFreeTextFields.getTheory(2));
            metadataProps.put(BoppContentModel.boppc("legislation"), surveyFreeTextFields.getTreaty(0));
            metadataProps.put(BoppContentModel.boppc("legislation2"), surveyFreeTextFields.getTreaty(1));
            metadataProps.put(BoppContentModel.boppc("legislation3"), surveyFreeTextFields.getTreaty(2));
            metadataProps.put(BoppContentModel.boppc("entity"), surveyFreeTextFields.getEntity(0));
            metadataProps.put(BoppContentModel.boppc("entity2"), surveyFreeTextFields.getEntity(1));
            metadataProps.put(BoppContentModel.boppc("entity3"), surveyFreeTextFields.getEntity(2));
            metadataProps.put(BoppContentModel.boppc("event"), surveyFreeTextFields.getEvent(0));
            metadataProps.put(BoppContentModel.boppc("event2"), surveyFreeTextFields.getEvent(1));
            metadataProps.put(BoppContentModel.boppc("event3"), surveyFreeTextFields.getEvent(2));
            metadataProps.put(BoppContentModel.boppc("landmark"), surveyFreeTextFields.getLandmark(0));
            metadataProps.put(BoppContentModel.boppc("landmark2"), surveyFreeTextFields.getLandmark(1));
            metadataProps.put(BoppContentModel.boppc("landmark3"), surveyFreeTextFields.getLandmark(2));
            metadataProps.put(BoppContentModel.boppc("era"), surveyFreeTextFields.getEra(0));
            metadataProps.put(BoppContentModel.boppc("era2"), surveyFreeTextFields.getEra(1));
            metadataProps.put(BoppContentModel.boppc("era3"), surveyFreeTextFields.getEra(2));
            metadataProps.put(BoppContentModel.boppc("time1From"), surveyFreeTextFields.getTime(0));
            metadataProps.put(BoppContentModel.boppc("time1To"), surveyFreeTextFields.getTime(1));
            metadataProps.put(BoppContentModel.boppc("time2From"), surveyFreeTextFields.getTime(2));
            metadataProps.put(BoppContentModel.boppc("time2To"), surveyFreeTextFields.getTime(3));
            metadataProps.put(BoppContentModel.boppc("country"), surveyFreeTextFields.getCountry(0));
            metadataProps.put(BoppContentModel.boppc("country2"), surveyFreeTextFields.getCountry(1));
            metadataProps.put(BoppContentModel.boppc("country3"), surveyFreeTextFields.getCountry(2));
            metadataProps.put(BoppContentModel.boppc("regionDefinitions"), surveyFreeTextFields.getAllRegions());
            metadataProps.put(BoppContentModel.boppc("regionSelections"), surveyFreeTextFields.getSelectedRegions());
            metadataProps.put(BoppContentModel.boppc("keywordDefinitions"), survey.getAllKeyTerms());
            metadataProps.put(BoppContentModel.boppc("keywordSelections"), survey.getSelectedKeyTerms());
            metadataProps.put(BoppContentModel.boppc("lastKeywordDefinition"), survey.getLastKeytermDefinition());
            metadataProps.put(BoppContentModel.boppc("lastKeywordSelection"), survey.getLastKeytermSelection());
            metadataProps.put(BoppContentModel.boppc("disciplineDefinitions"), survey.getAllDisciplines());
            metadataProps.put(BoppContentModel.boppc("disciplineSelections"), survey.getSelectedDisciplines());
            metadataProps.put(BoppContentModel.boppc("lastDisciplineDefinition"), survey.getLastDisciplineDefinition());
            metadataProps.put(BoppContentModel.boppc("lastDisciplineSelection"), survey.getLastDisciplineSelection());
            metadataProps.put(BoppContentModel.boppc("interdisciplinaryDefinitions"), survey.getAllInterDisciplines());
            metadataProps.put(BoppContentModel.boppc("interdisciplinarySelections"), survey.getSelectedInterDisciplines());
            metadataProps.put(BoppContentModel.boppc("lastInterDisciplineDefinition"), survey.getLastKeytermDefinition());
            metadataProps.put(BoppContentModel.boppc("lastInterDisciplineSelection"), survey.getLastKeytermSelection());
            metadataProps.put(BoppContentModel.boppc("abstract"), survey.getDescription());
            nodeService.addAspect(fileOrFolderNodeRef, BoppContentModel.boppc("subjectMetadata"), metadataProps);
        }

        @Override
        public boolean isRhoISBN(final String isbn) {
            boolean isRhoISBN = false;

            NodeRef isbnFilterFileNodeRef = alfrescoRepoUtilsService.getNodeByDisplayPath(BoppConstants.ISBN_FILTER_FILE_PATH);
            byte[] contentBytes = alfrescoRepoUtilsService.getDocumentContentBytes(isbnFilterFileNodeRef);
            String contentString = new String(contentBytes, Charset.forName("UTF-8"));
            if (contentString.contains(isbn)) {
                isRhoISBN = true;
            }

            return isRhoISBN;
        }

        @Override
        public void moveZipToDirForFailedProcessing(File zipFile, String metadataFilesystemPath) throws IOException {
            File failedMetadataDirectory = new File(metadataFilesystemPath + File.separator + FAILED_PROCESSING_DIR_NAME);
            if (!failedMetadataDirectory.exists()) {
                // First time around, create it
                boolean success = failedMetadataDirectory.mkdir();
                if (!success) {
                    throw new IllegalArgumentException("The " + metadataFilesystemPath +
                            File.separator + FAILED_PROCESSING_DIR_NAME + " directory could not be created");
                }
            }
            java.nio.file.Path zipFilePath = zipFile.toPath();
            java.nio.file.Path errorDirPath = failedMetadataDirectory.toPath();
            Files.move(zipFilePath, errorDirPath.resolve(zipFilePath.getFileName()),
                    StandardCopyOption.REPLACE_EXISTING);
        }

        /**
         * Clean values of special characters.
         *
         * @param source
         * @return the string after sanitize
         */
        private String removeSpecialCharacters(final String source) {
            return source.replaceAll("<b>", "").replaceAll("</b>", "").replaceAll("<i>", "").replace("</i>", "").replaceAll(">>", "");
        }



    }