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
package org.acme.bestpublishing.services;

import org.acme.bestpublishing.model.BestPubMetadataFileModel;
import org.acme.bestpublishing.props.ChapterFolderProperties;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.dictionary.constraint.ListOfValuesConstraint;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ISO8601DateFormat;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;

import static org.acme.bestpublishing.constants.BestPubConstants.*;
import static org.acme.bestpublishing.model.BestPubContentModel.*;

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
    private ServiceRegistry serviceRegistry;

    /**
     * Spring Dependency Injection
     */

    public void setAlfrescoRepoUtilsService(AlfrescoRepoUtilsService repoUtils) {
        this.alfrescoRepoUtilsService = repoUtils;
    }

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * Interface Implementation
     */

    @Override
    public File[] findFilesUsingExtension(File folderToSearch, final String extension) {
        return folderToSearch.listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                String fileExtension = FilenameUtils.getExtension(pathname.getPath());
                return fileExtension.equalsIgnoreCase(extension);
            }
        });
    }

    @Override
    public String formatDate(String pattern, Date date) {
        if (date == null) {
            return null;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(date);
    }

    @Override
    public String getChapterFolderName(int chapterNumber) {
        String chapterFolderName = CHAPTER_FOLDER_NAME_PREFIX + "-" + chapterNumber;
        return chapterFolderName;
    }

    @Override
    public NodeRef getChapterDestinationFolder(String fileName, NodeRef destIsbnFolderNodeRef) {
        // Filename naming convention: [ISBN]-Chapter-[chapter number].xhtml
        // Such as 9780486282145-Chapter-001.xhtml
        int indexOfDot = fileName.lastIndexOf('.');
        int indexOfLastDash = indexOfDot - 4;
        int chapterNr = Integer.parseInt(fileName.substring(indexOfLastDash + 1, indexOfDot));
        if (chapterNr < 0 || chapterNr > 200) {
            LOG.error("Incorrect chapter number from filename [{}]", fileName);
            return null;
        }

        NodeRef chapterFolderNodeRef = alfrescoRepoUtilsService.getChildByName(
                destIsbnFolderNodeRef, getChapterFolderName(chapterNr));

        return chapterFolderNodeRef;
    }

    @Override
    public Map<ChapterFolderProperties, NodeRef> getSortedChapterFolders(NodeRef isbnFolderNodeRef) {
        Set<QName> childNodeTypes = new HashSet<>();
        childNodeTypes.add(ChapterFolderType.QNAME);
        List<ChildAssociationRef> chapterFolderChildAssociations =
                serviceRegistry.getNodeService().getChildAssocs(isbnFolderNodeRef, childNodeTypes);

        Map<ChapterFolderProperties, NodeRef> existingChapterFolderProps2NodeRefMap = new TreeMap<>();

        for (ChildAssociationRef chapterFolderChildAssoc : chapterFolderChildAssociations) {
            NodeRef chapterFolderNodeRef = chapterFolderChildAssoc.getChildRef();
            ChapterFolderProperties chapterFolderProps = new ChapterFolderProperties();

            String chapterFolderName = (String) serviceRegistry.getNodeService().getProperty(
                    chapterFolderNodeRef, ContentModel.PROP_NAME);
            chapterFolderProps.put(BestPubMetadataFileModel.CHAPTER_FOLDER_NAME_PROP_NAME, chapterFolderName);
            Integer chapterNr = (Integer)serviceRegistry.getNodeService().getProperty(
                    chapterFolderNodeRef, ChapterInfoAspect.Prop.CHAPTER_NUMBER);
            chapterFolderProps.put(BestPubMetadataFileModel.CHAPTER_METADATA_NUMBER_PROP_NAME, chapterNr);
            String chapterTitle = (String) serviceRegistry.getNodeService().getProperty(
                    chapterFolderNodeRef, ChapterInfoAspect.Prop.CHAPTER_TITLE);
            chapterFolderProps.put(BestPubMetadataFileModel.CHAPTER_METADATA_TITLE_PROP_NAME, chapterTitle);
            String chapterAuthor = (String) serviceRegistry.getNodeService().getProperty(
                    chapterFolderNodeRef, ChapterInfoAspect.Prop.CHAPTER_AUTHOR_NAME);
            chapterFolderProps.put(BestPubMetadataFileModel.CHAPTER_METADATA_AUTHOR_PROP_NAME, chapterAuthor);

            existingChapterFolderProps2NodeRefMap.put(chapterFolderProps, chapterFolderNodeRef);
        }

        return existingChapterFolderProps2NodeRefMap;
    }

    @Override
    public NodeRef getBaseFolderForBooks() {
        Integer year = Calendar.getInstance().get(Calendar.YEAR);
        NodeRef docLibNodeRef = alfrescoRepoUtilsService.getNodeByDisplayPath(getBookManagementSiteDocLibPath());

        return alfrescoRepoUtilsService.getOrCreateFolder(docLibNodeRef, year.toString());
    }

    @Override
    public NodeRef getBaseFolderForIsbn(String isbn) {
        NodeRef baseFolderForBooksNodeRef = getBaseFolderForBooks();
        NodeRef isbnFolderNodeRef = alfrescoRepoUtilsService.getChildByName(baseFolderForBooksNodeRef, isbn);

        return isbnFolderNodeRef;
    }

    @Override
    public String getBookManagementSiteDocLibPath() {
        String siteDocLibPath = "/" + SITES_NAME + "/" + BOOK_MANAGEMENT_SITE_NAME + "/" + DOCUMENT_LIBRARY_NAME;

        return siteDocLibPath;
    }

    @Override
    public List<String> getAvailableGenreNames() {
        return (List<String>)serviceRegistry.getDictionaryService().getConstraint(
                GENRE_LIST_CONSTRAINT).getConstraint().getParameters().get(ListOfValuesConstraint.ALLOWED_VALUES_PARAM);
    }

    @Override
    public Date checkModifiedDates(NodeRef nodeRef, Date publishedDate) {
        if (publishedDate == null) {
            throw new IllegalArgumentException("Published date cannot be null");
        }

        String nodeRefXPath = serviceRegistry.getNodeService().getPath(nodeRef).
                toPrefixString(serviceRegistry.getNamespaceService());
        String publishedDateString = ISO8601DateFormat.format(publishedDate);
        String searchQuery = "PATH:\"" + nodeRefXPath + "//*\" AND @cm\\:modified:[" + publishedDateString + " TO NOW]";
        Date latestModificationDate = null;

        List<NodeRef> modifiedNodeRefs = alfrescoRepoUtilsService.search(searchQuery);
        for (NodeRef modifiedIsbnChildNode : modifiedNodeRefs) {
            Date modifiedDate = (Date) serviceRegistry.getNodeService().getProperty(
                    modifiedIsbnChildNode, ContentModel.PROP_MODIFIED);
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
    public Date checkModifiedDates(NodeRef nodeRef) {
        Date publishedDate = (Date) serviceRegistry.getNodeService().getProperty(nodeRef,
                WebPublishingInfoAspect.Prop.WEB_PUBLISHED_DATE);
        if (publishedDate != null) {
            return checkModifiedDates(nodeRef, publishedDate);
        } else {
            return null;
        }
    }

    @Override
    public boolean isISBN(String isbn) {
        Matcher isbnMatcher = ISBN_REGEXP_PATTERN.matcher(isbn);
        if (isbnMatcher.matches() == false) {
            return false;
        }

        return true;
    }

    @Override
    public String getISBNfromFilename(String filename) {
        String isbn = filename.trim().substring(0, ISBN_NUMBER_LENGTH);
        if (!isISBN(isbn)) {
            LOG.error("Could not extract ISBN number from [{}]", filename);
            return null;
        }

        return isbn;
    }

    @Override
    public void moveZipToDirForFailedProcessing(File zipFile, String metadataFilesystemPath) throws IOException {
        File failedMetadataDirectory =
                new File(metadataFilesystemPath + File.separator + FAILED_PROCESSING_DIR_NAME);
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
    private String removeSpecialCharacters(String source) {
        return source.replaceAll("<b>", "").
                replaceAll("</b>", "").
                replaceAll("<i>", "").
                replace("</i>", "").
                replaceAll(">>", "");
    }


}