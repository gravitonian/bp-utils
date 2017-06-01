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

import org.alfresco.model.ContentModel;
import org.alfresco.repo.dictionary.constraint.ListOfValuesConstraint;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ISO8601DateFormat;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.acme.bestpublishing.constants.BestPubConstants;
import org.acme.bestpublishing.model.BestPubContentModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;

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
    public NodeRef getChapterDestinationFolder(String fileName, NodeRef destIsbnFolderNodeRef) {
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

    /**
     * Create the chapter hierarchy for the book in the Book Management site.
     * This would be under /Company Home/Sites/book-management/documentLibrary/2017/{ISBN}/....
     *
     * @param isbn the ISBN number for the book
     * @param bookGenre the book genre name
     * @param bookTitle the book title
     * @param chapterCount the number of chapters
     * @param chapterTitles the chapter titles
     * @param processInfo information about the process instance that is making this call
     * @return true if all went OK
     */
    /*
    public boolean createChapterFolder(String isbn, Map<String, Properties> allMetadata, String processInfo) {
        // Start by creating the top ISBN folder
        NodeRef isbnFolderInfo = alfrescoRepoUtilsService.getChildByName(rhoFolderNodeRef, isbn);
        if (isbnFolderInfo != null) {
            LOG.warn("ISBN folder [{}] already exist, let's create folder hierarchy {}", isbn, processInfo);
        } else {
            isbnFolderInfo = serviceRegistry.getFileFolderService().create(
                    rhoFolderNodeRef, isbn, BookFolderType.QNAME).getNodeRef();
            LOG.debug("Created ISBN folder /Company Home/{}/{} [Subject={}]{}",
                    new Object[]{RHO_FOLDER_NAME, isbn, bookGenre, processInfo});
        }

        // Setup the new /Company Home/RHO/<isbn> folder with Book Metadata Aspect
        Map<QName, Serializable> bookMetadataAspectProps = new HashMap<>();
        bookMetadataAspectProps.put(BookInfoAspect.Prop.ISBN, isbn);
        bookMetadataAspectProps.put(BookInfoAspect.Prop.BOOK_TITLE, bookTitle);
        bookMetadataAspectProps.put(BookInfoAspect.Prop.BOOK_GENRE_NAME, bookGenre);
        bookMetadataAspectProps.put(BookInfoAspect.Prop.BOOK_NUMBER_OF_CHAPTERS, chapterCount);
        bookMetadataAspectProps.put(BookInfoAspect.Prop.BOOK_METADATA_STATUS, BookMetadataStatus.MISSING.toString());
        serviceRegistry.getNodeService().addAspect(isbnFolderInfo, BookInfoAspect.QNAME, bookMetadataAspectProps);

        // Now create all the chapter sub-folders under the new ISBN folder
        LOG.debug("The chapter titles for our book are as follows: {} {}", chapterTitles, processInfo);
        if (chapterTitles == null) {
            LOG.error("Missing chapter titles from T2 task, cannot setup chapter folders {}", processInfo);
            return false;
        }
        int currentChapterNumber = 1;
        for (String title : chapterTitles) {
            String chapterFolderName = BestPubConstants.CHAPTER_FOLDER_NAME_PREFIX + currentChapterNumber;
            if (alfrescoRepoUtilsService.getChildByName(isbnFolderInfo, chapterFolderName)==null) {
                FileInfo chapterFileInfo = serviceRegistry.getFileFolderService().create(
                        isbnFolderInfo, chapterFolderName, BestPubContentModel.ChapterFolderType.QNAME);
                LOG.debug("Created chapter folder /Company Home/{}/{}/{} [chapterNo={}][chapterTitle={}][metaStatus={}]{}",
                        new Object[]{RHO_FOLDER_NAME, isbn, chapterFileInfo.getName(), currentChapterNumber,
                                title, BestPubContentModel.ChapterMetadataStatus.MISSING.toString(), processInfo});
                Map<QName, Serializable> chapterMetadataAspectProps = new HashMap<QName, Serializable>();
                chapterMetadataAspectProps.put(ChapterInfoAspect.Prop.CHAPTER_NUMBER, currentChapterNumber);
                chapterMetadataAspectProps.put(ChapterInfoAspect.Prop.CHAPTER_TITLE, title);
                chapterMetadataAspectProps.put(ChapterInfoAspect.Prop.CHAPTER_METADATA_STATUS,
                        BestPubContentModel.ChapterMetadataStatus.MISSING.toString());
                serviceRegistry.getNodeService().addAspect(
                        chapterFileInfo.getNodeRef(), BookInfoAspect.QNAME, bookMetadataAspectProps);
                serviceRegistry.getNodeService().addAspect(
                        chapterFileInfo.getNodeRef(), ChapterInfoAspect.QNAME, chapterMetadataAspectProps);
            } else {
                LOG.debug("Already created [{}] in folder [{}], moving on... {}",
                        new Object[] {title, isbnFolderInfo, processInfo});
            }

            currentChapterNumber++;
        }

        return true;
    }
*/
    @Override
    public List<String> getAvailableGenreNames() {
        return (List<String>)serviceRegistry.getDictionaryService().getConstraint(
                BestPubContentModel.GENRE_LIST_CONSTRAINT).getConstraint().
                getParameters().get(ListOfValuesConstraint.ALLOWED_VALUES_PARAM);
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
                BestPubContentModel.WebPublishingInfoAspect.Prop.WEB_PUBLISHED_DATE);
        if (publishedDate != null) {
            return checkModifiedDates(nodeRef, publishedDate);
        } else {
            return null;
        }
    }

    @Override
    public boolean isISBN(String isbn) {
        Matcher isbnMatcher = BestPubConstants.ISBN_REGEXP_PATTERN.matcher(isbn);
        if (isbnMatcher.matches() == false) {
            return false;
        }

        return true;
    }

    @Override
    public String getISBNfromFilename(String filename) {
        String isbn = filename.trim().substring(0, BestPubConstants.ISBN_NUMBER_LENGTH);
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