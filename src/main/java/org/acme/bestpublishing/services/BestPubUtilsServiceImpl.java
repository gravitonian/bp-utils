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

import org.acme.bestpublishing.model.ChapterMetadataInfo;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.ISO8601DateFormat;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.acme.bestpublishing.constants.BestPubConstants;
import org.acme.bestpublishing.model.BestPubContentModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
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
    private ServiceRegistry serviceRegistry;

    /**
     * Spring Dependency Injection
     */

    public void setAlfrescoRepoUtilsService(final AlfrescoRepoUtilsService repoUtils) {
        this.alfrescoRepoUtilsService = repoUtils;
    }

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
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
    public List<ChapterMetadataInfo> extractChapterMetadata(NodeRef metadataZipNodeRef)
            throws IOException {
        List<ChapterMetadataInfo> chapterMetadataInfos = new ArrayList<>();
        String metadataFilename = (String) serviceRegistry.getNodeService().getProperty(
                metadataZipNodeRef, ContentModel.PROP_NAME);
        ContentData metadataContentInfo = (ContentData)serviceRegistry.getNodeService().getProperty(
                metadataZipNodeRef, ContentModel.PROP_CONTENT);
        String metadataFileMimeType = metadataContentInfo.getMimetype();
        ContentReader metadataContentReader = serviceRegistry.getContentService().getReader(
                metadataZipNodeRef, ContentModel.PROP_CONTENT);

        if (StringUtils.equals(metadataFileMimeType, MimetypeMap.MIMETYPE_ZIP) ||
                StringUtils.equals(metadataFileMimeType, BestPubConstants.MIMETYPE_ZIP_COMPRESSED)) {
            LOG.debug("Extracting chapter metadata from ZIP [{}]", metadataFilename);
            InputStream zipInputStream = metadataContentReader.getContentInputStream();


        /*
            Map<String, List<Survey>> csvFilename2surveysMap =
                    SurveyMonkeyParser.parseZipFile(metadataFilename, zipInputStream);
            for (String csvFilename : csvFilename2surveysMap.keySet()) {
                List<Survey> surveysInCSV = csvFilename2surveysMap.get(csvFilename);
                String chapterNoText = SurveyMonkeyParser.extractChapterNoTextFromCSVFilename(csvFilename);
                Integer chapterNumber = SurveyMonkeyParser.extractChapterNumberFromChapterNoText(chapterNoText);
                if (chapterNumber == -1) {
                    LOG.warn("Could not extract chapter number from [{}] [csv={}], content matching will not work",
                            chapterNoText, csvFilename);
                }
                ChapterMetadataInfo chapterMetadataInfo =
                        new ChapterMetadataInfo(chapterNumber, csvFilename, surveysInCSV.get(0));
                chapterMetadataInfos.add(chapterMetadataInfo);
            }
            */
        } else {
            LOG.debug("Found file [{}] which is a [{}] instead of a ZIP, so can't process it",
                    metadataFilename, metadataFileMimeType);
        }

        // Sorted on chapter numbers
        Collections.sort(chapterMetadataInfos);

        return chapterMetadataInfos;
    }

    @Override
    public String getBookGenreName(NodeRef metadataZipNodeRef) throws IOException {
        return null;
    }

    @Override
    public Date checkModifiedDates(final NodeRef nodeRef, final Date publishedDate) {
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
    public Date checkModifiedDates(final NodeRef nodeRef) {
        Date publishedDate = (Date) serviceRegistry.getNodeService().getProperty(nodeRef,
                BestPubContentModel.WebPublishingInfoAspect.Prop.WEB_PUBLISHED_DATE);
        if (publishedDate != null) {
            return checkModifiedDates(nodeRef, publishedDate);
        } else {
            return null;
        }
    }

    @Override
    public boolean isISBN(final String isbn) {
        Matcher isbnMatcher = BestPubConstants.ISBN_REGEXP_PATTERN.matcher(isbn);
        if (isbnMatcher.matches() == false) {
            return false;
        }

        return true;
    }

    @Override
    public String getISBNfromFilename(final String filename) {
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
    private String removeSpecialCharacters(final String source) {
        return source.replaceAll("<b>", "").
                replaceAll("</b>", "").
                replaceAll("<i>", "").
                replace("</i>", "").
                replaceAll(">>", "");
    }


}