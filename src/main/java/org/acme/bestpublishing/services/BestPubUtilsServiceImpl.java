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

import org.acme.bestpublishing.model.BestPubContentModel;
import org.acme.bestpublishing.model.BestPubMetadataFileModel;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.dictionary.constraint.ListOfValuesConstraint;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ISO8601DateFormat;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
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
import static org.acme.bestpublishing.model.BestPubMetadataFileModel.*;

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

        String chapterFolderName = CHAPTER_FOLDER_NAME_PREFIX + "-" + chapterNr;
        NodeRef chapterFolderNodeRef = alfrescoRepoUtilsService.getChildByName(
                destIsbnFolderNodeRef, chapterFolderName);

        return chapterFolderNodeRef;
    }

    @Override
    public NodeRef createChapterFolders(String isbn, Properties bookInfo, List<Properties> chapterList,
                                       String processInfo) {
        // Start by creating the top ISBN folder
        NodeRef publishingYearNodeRef = getBaseFolderForBooks();
        NodeRef isbnFolderNodeRef = serviceRegistry.getFileFolderService().create(
                publishingYearNodeRef, isbn, BookFolderType.QNAME).getNodeRef();
            LOG.debug("Created ISBN folder {} {}",
                    alfrescoRepoUtilsService.getDisplayPathForNode(isbnFolderNodeRef), processInfo);

        // Set up the new /Company Home/Sites/book-management/documentLibrary/<year>/<isbn> folder with Book Info Aspect
        Map<QName, Serializable> bookInfoAspectProps = new HashMap<>();
        bookInfoAspectProps.put(BookInfoAspect.Prop.ISBN, isbn);
        bookInfoAspectProps.put(BookInfoAspect.Prop.BOOK_TITLE,
                bookInfo.getProperty(BOOK_METADATA_TITLE_PROP_NAME));
        bookInfoAspectProps.put(BookInfoAspect.Prop.BOOK_GENRE_NAME,
                bookInfo.getProperty(BOOK_METADATA_GENRE_PROP_NAME));
        bookInfoAspectProps.put(BookInfoAspect.Prop.BOOK_NUMBER_OF_CHAPTERS,
                bookInfo.getProperty(BOOK_METADATA_NR_OF_CHAPTERS_PROP_NAME));
        bookInfoAspectProps.put(BookInfoAspect.Prop.BOOK_NUMBER_OF_PAGES,
                bookInfo.getProperty(BOOK_METADATA_NR_OF_PAGES_PROP_NAME));
        bookInfoAspectProps.put(BookInfoAspect.Prop.BOOK_METADATA_STATUS, BookMetadataStatus.MISSING.toString());
        serviceRegistry.getNodeService().addAspect(isbnFolderNodeRef, BookInfoAspect.QNAME, bookInfoAspectProps);

        // Now create all the chapter sub-folders under the new ISBN folder
        for (Properties chapterInfo: chapterList) {
            String chapterFolderName = CHAPTER_FOLDER_NAME_PREFIX + "-" +
                    chapterInfo.get(CHAPTER_METADATA_NUMBER_PROP_NAME);
                FileInfo chapterFileInfo = serviceRegistry.getFileFolderService().create(
                        isbnFolderNodeRef, chapterFolderName, ChapterFolderType.QNAME);
                LOG.debug("Created chapter folder {} [chapterTitle={}] {}",
                        new Object[]{alfrescoRepoUtilsService.getDisplayPathForNode(chapterFileInfo.getNodeRef()),
                                chapterInfo.get(CHAPTER_METADATA_TITLE_PROP_NAME), processInfo});
                Map<QName, Serializable> chapterMetadataAspectProps = new HashMap<QName, Serializable>();
                chapterMetadataAspectProps.put(ChapterInfoAspect.Prop.CHAPTER_NUMBER,
                        chapterInfo.getProperty(CHAPTER_METADATA_NUMBER_PROP_NAME));
                chapterMetadataAspectProps.put(ChapterInfoAspect.Prop.CHAPTER_TITLE,
                        chapterInfo.getProperty(CHAPTER_METADATA_TITLE_PROP_NAME));
                chapterMetadataAspectProps.put(ChapterInfoAspect.Prop.CHAPTER_AUTHOR_NAME,
                        chapterInfo.getProperty(CHAPTER_METADATA_AUTHOR_PROP_NAME));
                chapterMetadataAspectProps.put(ChapterInfoAspect.Prop.CHAPTER_METADATA_STATUS,
                        BestPubContentModel.ChapterMetadataStatus.MISSING.toString());
                serviceRegistry.getNodeService().addAspect(
                        chapterFileInfo.getNodeRef(), BookInfoAspect.QNAME, bookInfoAspectProps);
                serviceRegistry.getNodeService().addAspect(
                        chapterFileInfo.getNodeRef(), ChapterInfoAspect.QNAME, chapterMetadataAspectProps);
        }

        return isbnFolderNodeRef;
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