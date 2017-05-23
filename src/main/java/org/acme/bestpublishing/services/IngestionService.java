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

/*
 * Ingestion Service to extract all the different files from a ZIP package with book content or metadata.
 * Then upload to Alfresco folder.
 *
 * @author martin.bergljung@marversolutions.org
 * @version 1.0
 */
public interface IngestionService {
    /**
     * Extracts and imports all the content of the ZIP, such as chapter XHTMLs, artwork, supplementary etc in
     * a content ZIP.
     *
     * @param zipFile the ZIP file to be imported
     * @param alfrescoFolderNodeRef the node ref for /Company Home/Data Dictionary/BestPub/Incoming/[Content|Metadata]
     * @param isbn the book ISBN 13 number
     */
    public void importZipFileContent(final File zipFile, final NodeRef alfrescoFolderNodeRef, final String isbn);
}