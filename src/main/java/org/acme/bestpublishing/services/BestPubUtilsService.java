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

import org.acme.bestpublishing.props.ChapterFolderProperties;
import org.alfresco.service.cmr.repository.NodeRef;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/*
 * Generic Utility Service with methods that does not fit into
 * the Alfresco Repo or Workflow util services.
 *
 * @author martin.bergljung@marversolutions.org
 * @version 1.0
 */
public interface BestPubUtilsService {
    /**
     * Find files based on passed in file extension in passed in folder
     *
     * @param folderToSearch the folder to search in
     * @param extension      the file extension we are looking for (e.g. "zip")
     * @return an array of files with matching file extension
     */
    File[] findFilesUsingExtension(File folderToSearch, String extension);

    /**
     * Format date by passed in pattern
     *
     * @param pattern the date and time pattern such as "YYYY-MM-dd HH:mm:ss"
     * @param date    the date object to format
     * @return date in string format or empty string if date object was null
     */
    String formatDate(String pattern, Date date);

    /**
     * Returns the chapter folder name for passed in chapter number.
     *
     * @param chapterNumber a chapter number, such as 9
     * @return the chapter folder name, such as 'chapter-9'
     */
    String getChapterFolderName(int chapterNumber);

    /**
     * Returns the {@link org.alfresco.service.cmr.repository.NodeRef} for a chapter folder in
     * /Company Home/Sites/book-management/documentLibrary/{year}/{isbn}
     * based on chapter number contained in passed in file name.
     *
     * @param fileName              name of the file to extract chapter number from, for example 9780486282145-Chapter-001.xhtml
     *                              (naming convention: [ISBN]-Chapter-[chapter number].xhtml)
     * @param destIsbnFolderNodeRef the node reference for the ISBN folder under
     *                              /Company Home/Sites/book-management/documentLibrary/{year}
     * @return {@link NodeRef} of the destination chapter folder node reference, or null if it could not be found
     */
    NodeRef getChapterDestinationFolder(String fileName, NodeRef destIsbnFolderNodeRef);

    /**
     * Get all the chapter folders for an ISBN sorted on chapter number (1, 2, 3, ...).
     *
     * @param isbnFolderNodeRef the alfresco node reference for the ISBN folder
     * @return a tree map with all the node references for the chapter folders sorted on chapter number, starting with
     * first chapter
     */
    public Map<ChapterFolderProperties, NodeRef> getSortedChapterFolders(NodeRef isbnFolderNodeRef);

    /**
     * Get the Display path to the Book Management Site Document Library.
     *
     * @return the display path to docLib for book management
     */
    public String getBookManagementSiteDocLibPath();

    /**
     * Get the base folder for where books are stored and managed.
     * This will be the /Company Home/Sites/book-management/documentLibrary/{current year} folder.
     *
     * @return and Alfresco Node Reference pointing to the folder
     */
    NodeRef getBaseFolderForBooks();

    /**
     * Get the base folder for where content is stored for the book with passed in ISBN.
     * This will be the /Company Home/Sites/book-management/documentLibrary/{current year}/{isbn} folder.
     *
     * @return and Alfresco Node Reference pointing to the ISBN folder
     */
    NodeRef getBaseFolderForIsbn(String isbn);

    /**
     * Get the available book genre names, as specified in the content model contstraint.
     *
     * @return a list of available book genre names
     */
    List<String> getAvailableGenreNames();

    /**
     * Recursively check the ISBN's children for last modified dates that are after last published date.
     *
     * @param nodeRef ISBN node reference
     * @return last modified date, or null if it has not been modified since last published
     */
    Date checkModifiedDates(NodeRef nodeRef);

    /**
     * Recursively check the ISBN's children for last modified date that are after last published date.
     *
     * @param nodeRef
     * @param publishedDate
     * @return true if the metadata has been changed
     */
    Date checkModifiedDates(NodeRef nodeRef, Date publishedDate);

    /**
     * Returns true if passed in text is an ISBN 13 number, false if not
     *
     * @param isbn text that is to be checked if it is an ISBN 13 number, such as for example '9780203093474'
     * @return true if it is an ISBN 13 number, false if not
     */
    boolean isISBN(String isbn);

    /**
     * Returns the ISBN number contained in passed in filename, if any
     *
     * @param filename the filename which contains an ISBN number,
     *                 such as 9780203093474.zip or 9780203093474_Law_Chapter 1 - 40.zip
     * @return the ISBN number contained in the filename, such as 9780203093474, or null if not found
     */
    String getISBNfromFilename(String filename);

    /**
     * If a content or metadata ZIP could not be processed correctly move it to a special directory for ZIPs
     * that failed processing. This is the same for both Content ZIPs and Metadata ZIPs.
     *
     * @param zipFile the ZIP file that should be moved
     * @param metadataFilesystemPath the filesystem path to the base scanning directory,
     *                               such as /alf_data/BestPub/Incoming/Metadata
     * @throws IOException if could not move the file
     */
    void moveZipToDirForFailedProcessing(File zipFile, String metadataFilesystemPath) throws IOException;

}
