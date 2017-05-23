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

import org.alfresco.service.cmr.repository.NodeRef;

import java.io.File;
import java.io.IOException;
import java.util.Date;

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
    public File[] findFilesUsingExtension(final File folderToSearch, final String extension);

    /**
     * Format date by passed in pattern
     *
     * @param pattern the date and time pattern such as "YYYY-MM-dd HH:mm:ss"
     * @param date    the date object to format
     * @return date in string format or empty string if date object was null
     */
    public String formatDate(final String pattern, final Date date);

    /**
     * Returns the {@link org.alfresco.service.cmr.repository.NodeRef} for a chapter folder in
     * /Company Home/RHO/{ISBN} based on chapter number contained in passed in file name
     *
     * @param fileName              name of the file to extract chapter number from, for example 9780203807217-Chapter8.pdf
     *                              (naming convention: [ISBN]-chapter[chapter number].[pdf|xml])
     * @param destIsbnFolderNodeRef the node reference for the ISBN folder under /Company Home/RHO (the destination)
     * @return {@link NodeRef} of the destination chapter folder node reference, or null if it could not be found
     */
    public NodeRef getChapterDestinationFolder(String fileName, NodeRef destIsbnFolderNodeRef);

    /**
     * Recursively check the ISBN's children for last modified date that are after last published date.
     *
     * @param nodeRef ISBN node reference
     * @return last modified date, or null if it has not been modified since last published
     */
    public Date checkModifiedDates(final NodeRef nodeRef);

    /**
     * Recursively check the ISBN's children for last modified date that are after last published date.
     *
     * @param nodeRef
     * @param publishedDate
     * @return true if the metadata has been changed
     */
    public Date checkModifiedDates(final NodeRef nodeRef, final Date publishedDate);

    /**
     * Returns true if passed in text is an ISBN 13 number, false if not
     *
     * @param isbn text that is to be checked if it is an ISBN 13 number, such as for example '9780203093474'
     * @return true if it is an ISBN 13 number, false if not
     */
    public boolean isISBN(final String isbn);

    /**
     * Returns the ISBN number contained in passed in filename, if any
     *
     * @param filename the filename which contains an ISBN number,
     *                 such as 9780203093474.zip or 9780203093474_Law_Chapter 1 - 40.zip
     * @return the ISBN number contained in the filename, such as 9780203093474, or null if not found
     */
    public String getISBNfromFilename(final String filename);

    /**
     * If a content or metadata ZIP could not be processed correctly move it to a special directory for ZIPs
     * that failed processing. This is the same for both Content ZIPs, Metadata Backlist ZIPs and Frontlist ZIPs.
     *
     * @param zipFile the ZIP file that should be moved
     * @param metadataFilesystemPath the filesystem path to the base scanning directory,
     *                               such as /alf_data/Bopp/Incoming/Metadata
     * @throws IOException if could not move the file
     */
    public void moveZipToDirForFailedProcessing(File zipFile, String metadataFilesystemPath) throws IOException;

}
